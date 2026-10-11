import ApexLapDomain
import SwiftUI
import UserNotifications
import WidgetKit

struct RemindersView: View {
    @State private var scheduler = ReminderScheduler.shared
    @State private var denied = false

    var body: some View {
        Form {
            Section {
                Toggle("Me prévenir avant les séances", isOn: Binding(
                    get: { scheduler.settings.enabled },
                    set: { value in Task { denied = !(await scheduler.update { $0.enabled = value }) } }
                ))
                if denied || (scheduler.settings.enabled && scheduler.authorization == .denied) {
                    HStack {
                        Text("Les notifications sont désactivées pour Apex Lap.").font(.footnote).foregroundStyle(.red)
                        Spacer()
                        Button("Réglages") {
                            if let url = URL(string: UIApplication.openNotificationSettingsURLString) { UIApplication.shared.open(url) }
                        }
                    }
                }
            } footer: {
                Text("Une notification avant chaque séance choisie, même app fermée. Ouvrez l'app de temps en temps pour que les rappels suivent les changements de calendrier.")
            }

            if scheduler.settings.enabled {
                Section("Combien de temps avant") {
                    Picker("Délai", selection: Binding(
                        get: { scheduler.settings.leadMinutes },
                        set: { value in Task { _ = await scheduler.update { $0.leadMinutes = value } } }
                    )) {
                        ForEach(ReminderSettings.leadOptions, id: \.self) { Text($0 < 60 ? "\($0) min" : "\($0 / 60) h").tag($0) }
                    }
                    .pickerStyle(.segmented)
                }

                Section("Quelles séances") {
                    Picker("Séances", selection: Binding(
                        get: { scheduler.settings.scope },
                        set: { value in Task { _ = await scheduler.update { $0.scope = value } } }
                    )) {
                        ForEach(ReminderScope.allCases, id: \.self) { Text($0.label).tag($0) }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }

                Section("Prochains rappels") {
                    let upcoming = scheduler.upcoming()
                    if upcoming.isEmpty {
                        Text("Aucune séance dans les 14 prochains jours.").foregroundStyle(ApexColor.telemetry)
                    }
                    ForEach(upcoming.prefix(8)) { reminder in
                        HStack {
                            Text(Format.flag(reminder.race.circuit.country)).font(.title2)
                            VStack(alignment: .leading) {
                                Text("\(reminder.session.type.label) · \(reminder.race.name)").font(.subheadline.weight(.semibold))
                                if let start = reminder.session.startsAt {
                                    Text("\(Format.day(start)) à \(Format.time(start)) · rappel à \(Format.time(reminder.fireAt))")
                                        .font(.caption).foregroundStyle(ApexColor.telemetry)
                                }
                            }
                        }
                    }
                }
            }

            Section {
                Text("Ajoutez le widget Apex Lap : appui long sur l'écran d'accueil → bouton + → Apex Lap. Il affiche la prochaine séance avec un compte à rebours en direct.")
                    .font(.subheadline)
            } header: {
                Text("Widget d'écran d'accueil")
            }
        }
        .scrollContentBackground(.hidden)
        .background(ApexColor.asphalt)
        .navigationTitle("Rappels & widget")
        .navigationBarTitleDisplayMode(.inline)
        .task { await scheduler.refreshAuthorization() }
    }
}
