import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

/// Spaces requests (and optionally caps them per minute) across all callers.
public actor RequestThrottle {
    private let minInterval: TimeInterval
    private let maxPerMinute: Int?
    private let now: @Sendable () -> Date
    private var nextSlot = Date.distantPast
    private var recent: [Date] = []

    public init(minInterval: TimeInterval, maxPerMinute: Int? = nil, now: @escaping @Sendable () -> Date = Date.init) {
        self.minInterval = minInterval
        self.maxPerMinute = maxPerMinute
        self.now = now
    }

    /// Reserves the next slot and returns how long the caller must wait for it.
    public func reserve() -> TimeInterval {
        let current = now()
        var slot = max(current, nextSlot)
        if let limit = maxPerMinute {
            recent.removeAll { $0 <= slot.addingTimeInterval(-60) }
            if recent.count >= limit, let oldest = recent.first { slot = max(slot, oldest.addingTimeInterval(60)) }
            recent.append(slot)
        }
        nextSlot = slot.addingTimeInterval(minInterval)
        return slot.timeIntervalSince(current)
    }
}

/// Rate-limited transport: waits for a throttle slot, retries HTTP 429 with exponential backoff.
public struct ThrottledHTTPClient: HTTPClient {
    private let base: HTTPClient
    private let throttle: RequestThrottle
    private let maxRetries: Int

    public init(_ base: HTTPClient, minInterval: TimeInterval, maxPerMinute: Int? = nil, maxRetries: Int = 3) {
        self.base = base
        throttle = RequestThrottle(minInterval: minInterval, maxPerMinute: maxPerMinute)
        self.maxRetries = maxRetries
    }

    public func send(_ request: URLRequest) async throws -> Data {
        var attempt = 0
        while true {
            let wait = await throttle.reserve()
            if wait > 0 { try await Task.sleep(nanoseconds: UInt64(wait * 1_000_000_000)) }
            do {
                return try await base.send(request)
            } catch NetworkError.http(status: 429) where attempt < maxRetries {
                try await Task.sleep(nanoseconds: UInt64(1 << attempt) * 1_000_000_000)
                attempt += 1
            }
        }
    }
}
