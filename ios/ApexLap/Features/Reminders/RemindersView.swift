import ApexLapDomain
import SwiftUI
import UserNotifications
import WidgetKit

struct RemindersView: View {
    @Environment(\.strings) private var strings
    @State private var scheduler = ReminderScheduler.shared
    @State private var denied = false

    var body: some View {
        Form {
            Section {
                Toggle(strings.notifyBeforeSessions, isOn: Binding(
                    get: { scheduler.settings.enabled },
                    set: { value in Task { denied = !(await scheduler.update { $0.enabled = value }) } }
                ))
                if denied || (scheduler.settings.enabled && scheduler.authorization == .denied) {
                    HStack {
                        Text(strings.notificationsDisabled).font(.footnote).foregroundStyle(.red)
                        Spacer()
                        Button(strings.settings) {
                            if let url = URL(string: UIApplication.openNotificationSettingsURLString) { UIApplication.shared.open(url) }
                        }
                    }
                }
            } footer: {
                Text(strings.notifyBeforeSessionsFooter)
            }

            if scheduler.settings.enabled {
                Section(strings.howLongBefore) {
                    Picker(strings.delay, selection: Binding(
                        get: { scheduler.settings.leadMinutes },
                        set: { value in Task { _ = await scheduler.update { $0.leadMinutes = value } } }
                    )) {
                        ForEach(ReminderSettings.leadOptions, id: \.self) { Text($0 < 60 ? strings.minutes($0) : strings.hours($0 / 60)).tag($0) }
                    }
                    .pickerStyle(.segmented)
                }

                Section(strings.whichSessions) {
                    Picker(strings.whichSessions, selection: Binding(
                        get: { scheduler.settings.scope },
                        set: { value in Task { _ = await scheduler.update { $0.scope = value } } }
                    )) {
                        ForEach(ReminderScope.allCases, id: \.self) { Text(strings.reminderScope($0)).tag($0) }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }

                Section(strings.upcomingReminders) {
                    let upcoming = scheduler.upcoming()
                    if upcoming.isEmpty {
                        Text(strings.noSessionIn14Days).foregroundStyle(ApexColor.telemetry)
                    }
                    ForEach(upcoming.prefix(8)) { reminder in
                        HStack {
                            Text(Format.flag(reminder.race.circuit.country)).font(.title2)
                            VStack(alignment: .leading) {
                                Text("\(strings.session(reminder.session.type)) · \(reminder.race.name)").font(.subheadline.weight(.semibold))
                                if let start = reminder.session.startsAt {
                                    Text(strings.reminderLine(Format.day(start), Format.time(start), Format.time(reminder.fireAt)))
                                        .font(.caption).foregroundStyle(ApexColor.telemetry)
                                }
                            }
                        }
                    }
                }
            }

            Section {
                Text(strings.widgetHowTo)
                    .font(.subheadline)
            } header: {
                Text(strings.homeWidget)
            }
        }
        .scrollContentBackground(.hidden)
        .background(ApexColor.asphalt)
        .navigationTitle(strings.remindersAndWidget)
        .navigationBarTitleDisplayMode(.inline)
        .task { await scheduler.refreshAuthorization() }
    }
}
