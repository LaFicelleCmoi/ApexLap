import ApexLapData
import ApexLapDomain
import ApexLapLive
import Foundation
import Observation

enum LiveMode { case replay, live }

struct ReplayProgress {
    var start: Date
    var end: Date
    var current: Date
    var playing: Bool
    var speed: Int
    var fraction: Double { min(1, max(0, current.timeIntervalSince(start) / end.timeIntervalSince(start))) }
}

@MainActor
@Observable
final class LiveViewModel {
    static let speeds = [1, 5, 10, 30, 60]

    private(set) var sessions: [LiveSession] = []
    private(set) var session: LiveSession?
    private(set) var mode = LiveMode.replay
    private(set) var timing: LiveTimingState?
    private(set) var replay: ReplayProgress?
    /// A session running right now (live data needs a sponsor account).
    private(set) var liveSession: LiveSession?
    private(set) var isLoading = true
    private(set) var error: String?

    private let account = OpenF1Account.shared
    private var timeline: ReplayTimeline?
    private var playback: Task<Void, Never>?
    private var liveTask: Task<Void, Never>?

    func load() async {
        guard sessions.isEmpty else { return }
        isLoading = true
        error = nil
        do {
            let now = Date()
            sessions = try await account.repository.sessions(year: CalendarLogic.currentSeason(now: now))
            liveSession = sessions.first { $0.isLive(at: now) }
            if liveSession != nil, account.isSignedIn {
                goLive()
            } else if let last = sessions.first(where: { $0.hasEnded(at: now) }) {
                await select(last)
            } else {
                isLoading = false
            }
        } catch {
            isLoading = false
            self.error = OfflineFirst<Int>.message(for: error)
        }
    }

    func select(_ session: LiveSession) async {
        stopAll()
        self.session = session
        mode = .replay
        timing = nil
        replay = nil
        isLoading = true
        error = nil
        do {
            let loaded = try await account.repository.replay(session)
            timeline = loaded
            timing = loaded.advance(to: loaded.start)
            replay = ReplayProgress(start: loaded.start, end: loaded.end, current: loaded.start, playing: false, speed: 10)
        } catch {
            self.error = OfflineFirst<Int>.message(for: error)
        }
        isLoading = false
    }

    func togglePlay() {
        guard let progress = replay else { return }
        if progress.playing {
            playback?.cancel()
            replay?.playing = false
        } else {
            play()
        }
    }

    func setSpeed(_ speed: Int) { replay?.speed = speed }

    func seek(to fraction: Double) {
        guard let timeline, var progress = replay else { return }
        progress.current = progress.start.addingTimeInterval(progress.end.timeIntervalSince(progress.start) * fraction)
        timing = timeline.advance(to: progress.current)
        replay = progress
    }

    private func play() {
        guard let timeline else { return }
        replay?.playing = true
        playback = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 250_000_000)
                guard let self, var progress = self.replay, progress.playing else { return }
                progress.current = min(progress.end, progress.current.addingTimeInterval(0.25 * Double(progress.speed)))
                if progress.current >= progress.end { progress.playing = false }
                self.timing = timeline.advance(to: progress.current)
                self.replay = progress
                if !progress.playing { return }
            }
        }
    }

    /// Seeds the tower from the authenticated REST snapshot, then applies the WebSocket feed, reconnecting on drops.
    func goLive() {
        stopAll()
        mode = .live
        session = liveSession
        replay = nil
        timing = nil
        isLoading = true
        error = nil
        liveTask = Task { [weak self] in
            var attempt = 0
            while !Task.isCancelled {
                guard let self else { return }
                do {
                    guard let token = try await self.account.tokens.token(), let username = self.account.username else {
                        self.error = I18n.strings.noOpenF1Account
                        self.isLoading = false
                        return
                    }
                    var state = try await self.account.repository.liveSnapshot(token: token)
                    self.timing = state
                    self.session = state.session ?? self.session
                    self.isLoading = false
                    self.error = nil
                    attempt = 0
                    for try await event in OpenF1LiveStream.events(username: username, token: token) {
                        state = LiveTimingReducer.reduce(state, event)
                        self.timing = state
                    }
                } catch is CancellationError {
                    return
                } catch let error as LiveStreamError {
                    self.error = error.localizedDescription
                    self.isLoading = false
                    return
                } catch {
                    self.error = I18n.strings.streamInterrupted
                    self.isLoading = false
                }
                attempt += 1
                try? await Task.sleep(nanoseconds: UInt64(min(30, 2 << min(attempt, 4))) * 1_000_000_000)
            }
        }
    }

    func signedOut() async {
        if mode == .live, let last = sessions.first(where: { $0.hasEnded(at: .now) }) { await select(last) }
    }

    private func stopAll() {
        playback?.cancel()
        liveTask?.cancel()
    }
}
