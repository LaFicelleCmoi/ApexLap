import ApexLapLive
import SwiftUI

struct LiveView: View {
    @State private var model = LiveViewModel()
    @State private var account = OpenF1Account.shared
    @State private var showAccount = false
    @State private var showGap = true

    var body: some View {
        List {
            Section {
                sessionMenu
                if model.liveSession != nil && model.mode != .live {
                    Button {
                        if account.isSignedIn { model.goLive() } else { showAccount = true }
                    } label: {
                        Label("Passer en direct", systemImage: "dot.radiowaves.left.and.right")
                    }
                }
            }
            .listRowBackground(ApexColor.carbon)

            if let error = model.error {
                OfflineBanner(message: error) { Task { if let s = model.session { await model.select(s) } else { await model.load() } } }
                    .listRowInsets(EdgeInsets()).listRowBackground(Color.clear)
            }

            if let timing = model.timing {
                statusRow(timing).listRowBackground(Color.clear)
                if let replay = model.replay { replayControls(replay, timing: timing).listRowBackground(ApexColor.carbon) }
                Section {
                    let leaderBest = timing.tower.first?.bestLap
                    ForEach(timing.tower) { row in towerRow(row, timing: timing, leaderBest: leaderBest) }
                } header: {
                    towerHeader(isRace: timing.session?.isRace == true)
                }
                if !timing.raceControl.isEmpty {
                    Section("Direction de course") {
                        ForEach(timing.raceControl.prefix(8)) { entry in
                            HStack(alignment: .top) {
                                Text(Format.time(entry.date) + (entry.lap.map { " · T\($0)" } ?? ""))
                                    .font(.caption2).foregroundStyle(ApexColor.telemetry).frame(width: 80, alignment: .leading)
                                Text(entry.message).font(.caption)
                            }
                        }
                    }
                }
            } else if model.isLoading {
                ProgressView().frame(maxWidth: .infinity).listRowBackground(Color.clear)
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(ApexColor.asphalt)
        .navigationTitle("Live timing")
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                NavigationLink(value: AppRoute.telemetry(sessionKey: model.session?.key)) {
                    Image(systemName: "waveform.path.ecg").accessibilityLabel("Comparer des tours")
                }
                Button { showAccount = true } label: {
                    Image(systemName: account.isSignedIn ? "person.crop.circle.fill" : "person.crop.circle")
                        .accessibilityLabel("Compte OpenF1")
                }
            }
        }
        .sheet(isPresented: $showAccount) {
            AccountSheet(onSignedIn: { if model.liveSession != nil { model.goLive() } }, onSignedOut: { Task { await model.signedOut() } })
        }
        .task { await model.load() }
    }

    private var sessionMenu: some View {
        Menu {
            ForEach(model.sessions.prefix(40)) { session in
                Button("\(session.circuit) · \(session.frenchName) — \(Format.day(session.start))") { Task { await model.select(session) } }
            }
        } label: {
            HStack {
                VStack(alignment: .leading) {
                    Text(model.session.map { "\($0.circuit) · \($0.frenchName)" } ?? "Choisir une séance").font(ApexFont.title)
                    if let session = model.session { Text(Format.day(session.start)).font(.caption).foregroundStyle(ApexColor.telemetry) }
                }
                Spacer()
                Image(systemName: "chevron.up.chevron.down").foregroundStyle(ApexColor.telemetry)
            }
        }
        .foregroundStyle(ApexColor.floodlight)
    }

    private func statusRow(_ timing: LiveTimingState) -> some View {
        HStack(spacing: 10) {
            if model.mode == .live {
                Circle().fill(StatusColor.critical).frame(width: 8, height: 8)
                Text("EN DIRECT").font(ApexFont.label)
            } else {
                Text("REPLAY").font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
            }
            TrackStatusPill(status: timing.trackStatus)
            Spacer()
            if timing.session?.isRace == true, timing.leaderLap > 0 { Text("TOUR \(timing.leaderLap)").font(ApexFont.label) }
        }
    }

    private func replayControls(_ progress: ReplayProgress, timing: LiveTimingState) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Button { model.togglePlay() } label: {
                    Image(systemName: progress.playing ? "pause.fill" : "play.fill").frame(width: 28, height: 28)
                }
                .buttonStyle(.borderedProminent)
                .accessibilityLabel(progress.playing ? "Pause" : "Lecture")
                Picker("Vitesse", selection: Binding(get: { progress.speed }, set: { model.setSpeed($0) })) {
                    ForEach(LiveViewModel.speeds, id: \.self) { Text("×\($0)").tag($0) }
                }
                .pickerStyle(.segmented)
            }
            Slider(value: Binding(get: { progress.fraction }, set: { model.seek(to: $0) }))
            Text("\(Format.time(timing.clock ?? progress.current)) (heure locale)").font(.caption2).foregroundStyle(ApexColor.telemetry)
        }
    }

    private func towerHeader(isRace: Bool) -> some View {
        HStack {
            Text("POS").frame(width: 44, alignment: .leading)
            Text("PILOTE")
            Spacer()
            Button(isRace ? (showGap ? "ÉCART ⇄" : "INTERVALLE ⇄") : "ÉCART") { showGap.toggle() }
                .disabled(!isRace).frame(width: 96, alignment: .leading)
            Text("DERNIER").frame(width: 70, alignment: .leading)
            Text("PNEU").frame(width: 40, alignment: .leading)
        }
        .font(ApexFont.label)
        .foregroundStyle(ApexColor.telemetry)
    }

    private func towerRow(_ row: DriverTiming, timing: LiveTimingState, leaderBest: Double?) -> some View {
        let teamColor = row.driver.teamColour.map { Color(hex: $0) } ?? ApexColor.telemetry
        let gap: String
        if timing.session?.isRace == true {
            gap = (showGap ? row.gapToLeader : row.interval)?.display ?? "—"
        } else if let best = row.bestLap {
            gap = leaderBest == nil || best == leaderBest ? LapTimeFormat.string(best) : String(format: "+%.3f", best - (leaderBest ?? best))
        } else {
            gap = "—"
        }
        let fastest = timing.fastestLap?.driver == row.driver.number && row.lastLap == row.bestLap
        return HStack(spacing: 6) {
            Text(row.position.map(String.init) ?? "–").font(.headline).frame(width: 26, alignment: .leading)
            TeamStripe(color: teamColor, height: 22)
            VStack(alignment: .leading, spacing: 0) {
                Text(row.driver.acronym).font(.subheadline.weight(.semibold))
                if row.pitStops > 0 { Text("\(row.pitStops) arrêt\(row.pitStops > 1 ? "s" : "")").font(.caption2).foregroundStyle(ApexColor.telemetry) }
            }
            Spacer()
            Text(gap).font(.caption.monospacedDigit()).frame(width: 96, alignment: .leading)
            Text(row.lastLap.map(LapTimeFormat.string) ?? "—").font(.caption.monospacedDigit())
                .foregroundStyle(fastest ? ApexColor.purpleSector : ApexColor.floodlight)
                .frame(width: 70, alignment: .leading)
            if let stint = row.currentStint { TyreBadge(compound: stint.compound, age: row.tyreAge).frame(width: 40, alignment: .leading) }
            else { Color.clear.frame(width: 40) }
        }
        .listRowBackground(ApexColor.asphalt)
    }
}

enum LapTimeFormat {
    /// 97.558 → "1:37.558".
    static func string(_ seconds: Double) -> String {
        let minutes = Int(seconds / 60)
        let rest = seconds - Double(minutes * 60)
        return minutes > 0 ? String(format: "%d:%06.3f", minutes, rest) : String(format: "%.3f", rest)
    }
}

/// Status colours are reserved for track state and always paired with an icon and a label.
enum StatusColor {
    static let good = Color(hex: 0x0CA30C)
    static let warning = Color(hex: 0xFAB219)
    static let critical = Color(hex: 0xD03B3B)
}

struct TrackStatusPill: View {
    let status: TrackStatus

    var body: some View {
        let (color, icon): (Color, String) = switch status {
        case .green: (StatusColor.good, "checkmark.circle")
        case .yellow, .vsc, .safetyCar: (StatusColor.warning, "exclamationmark.triangle")
        case .red: (StatusColor.critical, "xmark.octagon")
        case .chequered: (ApexColor.floodlight, "flag.checkered")
        }
        Label(status.label.uppercased(), systemImage: icon)
            .font(ApexFont.label)
            .foregroundStyle(ApexColor.floodlight)
            .labelStyle(PillLabelStyle(tint: color))
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .overlay(Capsule().stroke(color, lineWidth: 1))
    }

    private struct PillLabelStyle: LabelStyle {
        let tint: Color
        func makeBody(configuration: Configuration) -> some View {
            HStack(spacing: 6) {
                configuration.icon.foregroundStyle(tint)
                configuration.title
            }
        }
    }
}

/// Official Pirelli compound colours, with the letter so identity never relies on colour alone.
struct TyreBadge: View {
    let compound: TyreCompound
    let age: Int?

    var body: some View {
        let color: Color = switch compound {
        case .soft: Color(hex: 0xDA291C)
        case .medium: Color(hex: 0xFFD12E)
        case .hard: Color(hex: 0xF0F0EC)
        case .intermediate: Color(hex: 0x43B02A)
        case .wet: Color(hex: 0x0067AD)
        case .unknown: ApexColor.telemetry
        }
        HStack(spacing: 2) {
            Text(compound.letter).font(.system(size: 10, weight: .black))
                .frame(width: 18, height: 18)
                .overlay(Circle().stroke(color, lineWidth: 2))
            if let age { Text("\(age)").font(.caption2).foregroundStyle(ApexColor.telemetry) }
        }
    }
}

private struct AccountSheet: View {
    let onSignedIn: () -> Void
    let onSignedOut: () -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var account = OpenF1Account.shared
    @State private var username = ""
    @State private var password = ""
    @State private var error: String?
    @State private var busy = false

    var body: some View {
        NavigationStack {
            Form {
                if let user = account.username {
                    Section {
                        Text("Connecté : \(user)")
                        Button("Se déconnecter", role: .destructive) {
                            Task {
                                await account.signOut()
                                onSignedOut()
                                dismiss()
                            }
                        }
                    } footer: {
                        Text("Le direct est disponible pendant les séances.")
                    }
                } else {
                    Section {
                        TextField("E-mail", text: $username).textContentType(.username).keyboardType(.emailAddress).textInputAutocapitalization(.never)
                        SecureField("Mot de passe", text: $password).textContentType(.password)
                        if let error { Text(error).font(.footnote).foregroundStyle(.red) }
                        Button(busy ? "Connexion…" : "Se connecter") {
                            busy = true
                            Task {
                                do {
                                    try await account.signIn(username: username, password: password)
                                    onSignedIn()
                                    dismiss()
                                } catch {
                                    self.error = "Identifiants OpenF1 incorrects ou serveur indisponible"
                                }
                                busy = false
                            }
                        }
                        .disabled(busy || username.isEmpty || password.isEmpty)
                    } footer: {
                        Text("Le replay des séances passées est gratuit. Le direct pendant les séances nécessite un compte sponsor OpenF1 ; le mot de passe est stocké dans le trousseau iOS.")
                    }
                    Section {
                        Link("Devenir sponsor OpenF1", destination: URL(string: "https://openf1.org/#sponsor")!)
                    }
                }
            }
            .navigationTitle("Compte OpenF1")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Fermer") { dismiss() } } }
        }
        .presentationDetents([.medium, .large])
    }
}
