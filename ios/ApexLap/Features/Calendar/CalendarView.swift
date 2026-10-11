import ApexLapDomain
import SwiftUI

struct CalendarView: View {
    @Environment(\.repository) private var repository
    @State private var model = CalendarViewModel()

    var body: some View {
        Group {
            if model.races.isEmpty && model.calendar.isRefreshing {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if model.races.isEmpty {
                EmptyStateView(message: model.calendar.error ?? "Aucun Grand Prix pour \(model.season)") {
                    Task { await model.refresh(using: repository) }
                }
            } else {
                content
            }
        }
        .background(ApexColor.asphalt)
        .navigationTitle("Saison \(String(model.season))")
        .task { await model.load(using: repository) }
    }

    private var content: some View {
        // Re-evaluated every second so the countdown and "next" markers stay live.
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let now = context.date
            let nextRace = CalendarLogic.nextRace(in: model.races, now: now)
            let nextSession = CalendarLogic.nextSession(in: model.races, now: now)

            List {
                if let error = model.calendar.error {
                    OfflineBanner(message: error) { Task { await model.refresh(using: repository) } }
                        .listRowInsets(EdgeInsets()).listRowBackground(Color.clear)
                }
                if let nextRace {
                    NavigationLink(value: AppRoute.race(season: nextRace.season, round: nextRace.round)) {
                        NextRaceHero(race: nextRace, next: nextSession, now: now)
                    }
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                }
                Section {
                    ForEach(model.races) { race in
                        NavigationLink(value: AppRoute.race(season: race.season, round: race.round)) {
                            RaceRow(race: race, isFinished: race.isFinished(at: now), isNext: race.round == nextRace?.round)
                        }
                        .listRowBackground(ApexColor.asphalt)
                    }
                } header: {
                    SectionHeader(title: "Calendrier").listRowInsets(EdgeInsets())
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .refreshable { await model.refresh(using: repository) }
        }
    }
}

private struct NextRaceHero: View {
    let race: Race
    let next: UpcomingSession?
    let now: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("PROCHAIN GRAND PRIX · MANCHE \(race.round)").font(ApexFont.label)
            Text("\(Format.flag(race.circuit.country))  \(race.name)").font(ApexFont.headline)
            Text(race.circuit.name).font(.subheadline).foregroundStyle(ApexColor.telemetry)
            if let next, let start = next.session.startsAt {
                Text("\(next.session.type.label.uppercased()) · \(Format.day(start)) \(Format.time(start))")
                    .font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
                    .padding(.top, 10)
                CountdownRow(countdown: CalendarLogic.countdown(to: start, now: now))
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(colors: [ApexColor.racingRed.opacity(0.6), ApexColor.carbon],
                           startPoint: .topLeading, endPoint: .bottomTrailing),
            in: RoundedRectangle(cornerRadius: 20)
        )
    }
}

struct CountdownRow: View {
    let countdown: Countdown

    var body: some View {
        HStack(spacing: 10) {
            unit(countdown.days, "JOURS")
            unit(countdown.hours, "H")
            unit(countdown.minutes, "MIN")
            unit(countdown.seconds, "SEC")
        }
        .accessibilityElement(children: .combine)
    }

    private func unit(_ value: Int, _ label: String) -> some View {
        VStack(spacing: 2) {
            Text(String(format: "%02d", value)).font(ApexFont.mono).contentTransition(.numericText())
            Text(label).font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(ApexColor.asphalt.opacity(0.7), in: RoundedRectangle(cornerRadius: 10))
    }
}

private struct RaceRow: View {
    let race: Race
    let isFinished: Bool
    let isNext: Bool

    var body: some View {
        HStack(spacing: 12) {
            Text(String(format: "%02d", race.round))
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(isNext ? ApexColor.racingRed : ApexColor.telemetry)
                .frame(width: 32)
            Text(Format.flag(race.circuit.country)).font(.title2)
            VStack(alignment: .leading, spacing: 2) {
                Text(race.name).font(ApexFont.title)
                Text(race.circuit.locality + (race.isSprintWeekend ? " · Sprint" : ""))
                    .font(.caption).foregroundStyle(ApexColor.telemetry)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                if let day = race.raceSession?.day {
                    Text(Format.shortUTCDay(day)).font(ApexFont.label)
                }
                if isFinished {
                    Text("TERMINÉ").font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
                } else if isNext {
                    Text("PROCHAIN").font(ApexFont.label).foregroundStyle(ApexColor.racingRed)
                }
            }
        }
        .opacity(isFinished ? 0.55 : 1)
        .padding(.vertical, 4)
    }
}
