import Foundation

/// Every user-facing text of the app, in French, English and Spanish side by side (same keys as Android's `Strings.kt`).
public struct Strings: Sendable {
    public let language: AppLanguage

    public init(_ language: AppLanguage) {
        self.language = language
    }

    private func t(_ fr: String, _ en: String, _ es: String) -> String {
        switch language {
        case .fr: fr
        case .en: en
        case .es: es
        }
    }

    /// "1 victoire" / "2 victoires": `one` and `many` already in the right language.
    private func count(_ n: Int, _ one: String, _ many: String) -> String { "\(n) \(n > 1 ? many : one)" }

    // MARK: Common
    public var retry: String { t("Réessayer", "Retry", "Reintentar") }
    public var close: String { t("Fermer", "Close", "Cerrar") }
    public var none: String { t("Aucun", "None", "Ninguno") }
    public var pts: String { "pts" }
    public var ptsUpper: String { "PTS" }
    public var driver: String { t("Pilote", "Driver", "Piloto") }
    public var team: String { t("Écurie", "Team", "Escudería") }
    public func results(_ n: Int) -> String { count(n, t("résultat", "result", "resultado"), t("résultats", "results", "resultados")) }
    public func winsShort(_ n: Int) -> String { t("\(n) VICT.", "\(n) WINS", "\(n) VICT.") }
    public func round(_ n: Int) -> String { t("MANCHE \(n)", "ROUND \(n)", "RONDA \(n)") }
    public func roundShort(_ n: Int) -> String { t("M\(n)", "R\(n)", "R\(n)") }
    public func lapShort(_ n: Int) -> String { t("T\(n)", "L\(n)", "V\(n)") }
    public func ordinal(_ n: Int) -> String {
        switch language {
        case .fr: n == 1 ? "1re" : "\(n)e"
        case .es: "\(n).º"
        case .en:
            if (11...13).contains(n % 100) { "\(n)th" } else {
                switch n % 10 {
                case 1: "\(n)st"
                case 2: "\(n)nd"
                case 3: "\(n)rd"
                default: "\(n)th"
                }
            }
        }
    }

    // MARK: Errors
    public var offlineCached: String { t("Hors ligne — affichage des données en cache", "Offline — showing cached data", "Sin conexión — mostrando datos en caché") }
    public func serverDown(_ code: Int) -> String { t("Serveur indisponible (HTTP \(code))", "Server unavailable (HTTP \(code))", "Servidor no disponible (HTTP \(code))") }
    public var loadFailed: String { t("Impossible de charger les données", "Couldn't load the data", "No se pudieron cargar los datos") }
    public var noOpenF1Account: String { t("Aucun compte OpenF1 configuré", "No OpenF1 account set up", "No hay cuenta de OpenF1 configurada") }
    public func openF1Refused(_ code: Int) -> String { t("Connexion OpenF1 refusée (code \(code))", "OpenF1 connection refused (code \(code))", "Conexión a OpenF1 rechazada (código \(code))") }
    public var openF1SignInFailed: String { t("Identifiants OpenF1 incorrects ou serveur indisponible", "Wrong OpenF1 credentials or server unavailable", "Credenciales de OpenF1 incorrectas o servidor no disponible") }
    public var streamInterrupted: String { t("Flux interrompu, reconnexion…", "Stream interrupted, reconnecting…", "Transmisión interrumpida, reconectando…") }
    public var telemetryUnavailable: String { t("Télémétrie indisponible pour ce tour", "Telemetry unavailable for this lap", "Telemetría no disponible para esta vuelta") }

    // MARK: Navigation
    public var navCalendar: String { t("Calendrier", "Calendar", "Calendario") }
    public var navResults: String { t("Résultats", "Results", "Resultados") }
    public var navLive: String { t("Live", "Live", "Directo") }
    public var navStandings: String { t("Classements", "Standings", "Clasificación") }
    public var navStats: String { t("Stats", "Stats", "Estadísticas") }

    // MARK: Sessions
    public func session(_ type: SessionType) -> String {
        switch type {
        case .practice1: t("Essais libres 1", "Practice 1", "Libres 1")
        case .practice2: t("Essais libres 2", "Practice 2", "Libres 2")
        case .practice3: t("Essais libres 3", "Practice 3", "Libres 3")
        case .sprintQualifying: t("Qualifications sprint", "Sprint qualifying", "Clasificación sprint")
        case .sprint: "Sprint"
        case .qualifying: t("Qualifications", "Qualifying", "Clasificación")
        case .race: t("Course", "Race", "Carrera")
        }
    }

    public func sessionShort(_ type: SessionType) -> String {
        switch type {
        case .practice1: t("EL1", "FP1", "L1")
        case .practice2: t("EL2", "FP2", "L2")
        case .practice3: t("EL3", "FP3", "L3")
        case .sprintQualifying: "SQ"
        case .sprint: "SPR"
        case .qualifying: "Q"
        case .race: "GP"
        }
    }

    /// OpenF1 session names ("Race", "Practice 1"…).
    public func openF1Session(_ name: String) -> String {
        switch name {
        case "Race": session(.race)
        case "Qualifying": session(.qualifying)
        case "Sprint": session(.sprint)
        case "Sprint Qualifying", "Sprint Shootout": t("Qualifs sprint", "Sprint quali", "Clasif. sprint")
        case "Practice 1": sessionShort(.practice1)
        case "Practice 2": sessionShort(.practice2)
        case "Practice 3": sessionShort(.practice3)
        default: name
        }
    }

    // MARK: Calendar
    public func seasonTitle(_ season: Int) -> String { t("Saison \(season)", "Season \(season)", "Temporada \(season)") }
    public func noGrandPrix(_ season: Int) -> String { t("Aucun Grand Prix pour \(season)", "No Grand Prix for \(season)", "Ningún Gran Premio en \(season)") }
    public var encyclopedia: String { t("Encyclopédie", "Encyclopedia", "Enciclopedia") }
    public var remindersAndWidget: String { t("Rappels & widget", "Reminders & widget", "Recordatorios y widget") }
    public var settings: String { t("Réglages", "Settings", "Ajustes") }
    public func nextGrandPrix(_ round: Int) -> String { t("PROCHAIN GRAND PRIX · MANCHE \(round)", "NEXT GRAND PRIX · ROUND \(round)", "PRÓXIMO GRAN PREMIO · RONDA \(round)") }
    public var calendar: String { navCalendar }
    public var days: String { t("JOURS", "DAYS", "DÍAS") }
    public var hoursShort: String { "H" }
    public var minutesShort: String { "MIN" }
    public var secondsShort: String { t("SEC", "SEC", "SEG") }
    public var finished: String { t("TERMINÉ", "FINISHED", "TERMINADO") }
    public var next: String { t("PROCHAIN", "NEXT", "PRÓXIMO") }

    // MARK: Favourites
    public var myFavorites: String { t("Mes favoris", "My favourites", "Mis favoritos") }
    public var chooseFavorites: String { t("Choisir mon pilote et mon écurie favoris", "Pick my favourite driver and team", "Elegir mi piloto y escudería favoritos") }
    public var follow: String { t("Suivre", "Follow", "Seguir") }
    public var unfollow: String { t("Ne plus suivre", "Unfollow", "Dejar de seguir") }
    public var favoriteDriver: String { t("Pilote favori", "Favourite driver", "Piloto favorito") }
    public var favoriteTeam: String { t("Écurie favorite", "Favourite team", "Escudería favorita") }
    public var favoriteHint: String {
        t("Mis en avant dans l'accueil, les classements et le widget. Vous pouvez aussi toucher ★ sur la fiche d'un pilote ou d'une écurie.",
          "Highlighted on the home screen, in the standings and in the widget. You can also tap ★ on a driver or team page.",
          "Destacados en la portada, en las clasificaciones y en el widget. También puedes tocar ★ en la ficha de un piloto o escudería.")
    }
    public func standingLine(_ position: Int, _ points: String, _ wins: Int) -> String {
        "P\(position) · \(points) \(pts)" + (wins > 0 ? " · " + self.wins(wins) : "")
    }
    public var notRankedYet: String { t("Pas encore classé cette saison", "Not ranked yet this season", "Aún sin clasificar esta temporada") }

    // MARK: Settings
    public var languageLabel: String { t("Langue", "Language", "Idioma") }
    public var systemLanguage: String { t("Langue du téléphone", "Phone language", "Idioma del teléfono") }
    public var notifications: String { t("Notifications", "Notifications", "Notificaciones") }
    public var remindersAndWidgetHint: String { t("Rappels avant les séances et widget d'écran d'accueil", "Reminders before sessions and home screen widget", "Recordatorios antes de las sesiones y widget de inicio") }
    public var about: String { t("À propos", "About", "Acerca de") }
    public func version(_ name: String) -> String { t("Version \(name)", "Version \(name)", "Versión \(name)") }
    public var dataSources: String {
        t("Données : Jolpica-F1 (résultats et historique) et OpenF1 (live et télémétrie). Application non officielle, sans lien avec la Formule 1.",
          "Data: Jolpica-F1 (results and history) and OpenF1 (live and telemetry). Unofficial app, not affiliated with Formula 1.",
          "Datos: Jolpica-F1 (resultados e historia) y OpenF1 (directo y telemetría). Aplicación no oficial, sin relación con la Fórmula 1.")
    }

    // MARK: Results & standings
    public func noFinishedGrandPrix(_ season: Int) -> String { t("Aucun Grand Prix terminé en \(season)", "No Grand Prix finished in \(season)", "Ningún Gran Premio terminado en \(season)") }
    public func resultsTitle(_ season: Int) -> String { t("Résultats \(season)", "Results \(season)", "Resultados \(season)") }
    public func championshipTitle(_ season: Int) -> String { t("Championnat \(season)", "Championship \(season)", "Campeonato \(season)") }
    public var searchDriverTeam: String { t("Pilote, écurie…", "Driver, team…", "Piloto, escudería…") }
    public var drivers: String { t("Pilotes", "Drivers", "Pilotos") }
    public var constructors: String { t("Constructeurs", "Constructors", "Constructores") }
    public var noStandingsYet: String { t("Pas encore de classement", "No standings yet", "Todavía no hay clasificación") }

    // MARK: Race
    public var grandPrix: String { t("Grand Prix", "Grand Prix", "Gran Premio") }
    public func roundOfSeason(_ round: Int, _ season: Int) -> String { t("MANCHE \(round) · \(season)", "ROUND \(round) · \(season)", "RONDA \(round) · \(season)") }
    public var schedule: String { t("Programme (heure locale)", "Schedule (local time)", "Horario (hora local)") }
    public var timeTba: String { t("Horaire à venir", "Time TBA", "Hora por confirmar") }
    public var tabRace: String { t("Course", "Race", "Carrera") }
    public var tabQualifying: String { t("Qualifs", "Qualifying", "Clasif.") }
    public var tabSprint: String { "Sprint" }
    public var sessionLabel: String { t("Séance", "Session", "Sesión") }
    public var resultsSoon: String { t("Les résultats apparaîtront ici dès le début du week-end.", "Results will show up here once the weekend starts.", "Los resultados aparecerán aquí cuando empiece el fin de semana.") }
    public var noSessionResults: String { t("Pas encore de résultats pour cette séance.", "No results for this session yet.", "Todavía no hay resultados de esta sesión.") }
    public var fastestLapShort: String { t("⏱ MT", "⏱ FL", "⏱ VR") }
    public func eliminatedIn(_ session: String) -> String { t("ÉLIMINÉ EN \(session)", "OUT IN \(session)", "ELIMINADO EN \(session)") }
    public var winner: String { t("🏆 Vainqueur", "🏆 Winner", "🏆 Ganador") }
    public var polePosition: String { "Pole position" }
    public var fastestLap: String { t("⏱ Meilleur tour", "⏱ Fastest lap", "⏱ Vuelta rápida") }
    public var context: String { t("Contexte", "Background", "Contexto") }
    public func edition(_ n: Int) -> String {
        n == 1
            ? t("Premier Grand Prix disputé sur ce circuit.", "First Grand Prix held at this circuit.", "Primer Gran Premio disputado en este circuito.")
            : t("\(n)e Grand Prix disputé sur ce circuit.", "\(ordinal(n)) Grand Prix held at this circuit.", "\(n).º Gran Premio disputado en este circuito.")
    }
    public func previousWinner(_ driver: String, _ team: String, _ season: Int) -> String {
        t("Vainqueur précédent : \(driver) (\(team), \(season))", "Previous winner: \(driver) (\(team), \(season))", "Ganador anterior: \(driver) (\(team), \(season))")
    }
    public func mostWinsHere(_ driver: String, _ wins: Int) -> String {
        t("Recordman des victoires ici : \(driver) (\(wins))", "Most wins here: \(driver) (\(wins))", "Más victorias aquí: \(driver) (\(wins))")
    }
    public var circuitPage: String { t("Fiche du circuit", "Circuit page", "Ficha del circuito") }

    // MARK: Driver & constructor pages
    public var teamBadge: String { t("ÉCURIE", "TEAM", "EQUIPO") }
    public func seasonHeader(_ season: Int) -> String { t("Saison \(season)", "Season \(season)", "Temporada \(season)") }
    public var bio: String { "Bio" }
    public var worldTitles: String { t("Titres mondiaux", "World titles", "Títulos mundiales") }
    public var constructorTitles: String { t("Titres constructeurs", "Constructors' titles", "Títulos de constructores") }
    public var career: String { t("Carrière", "Career", "Trayectoria") }
    public var history: String { t("Historique", "History", "Historia") }
    public var statPosition: String { t("Position", "Position", "Posición") }
    public var statPoints: String { t("Points", "Points", "Puntos") }
    public var statWins: String { t("Victoires", "Wins", "Victorias") }
    public var statSeasons: String { t("Saisons", "Seasons", "Temporadas") }
    public var statRaces: String { t("Courses", "Races", "Carreras") }
    public var statStarts: String { t("Départs", "Starts", "Salidas") }
    public var statPoles: String { "Poles" }
    public var statPodiums: String { t("Podiums", "Podiums", "Podios") }
    public var statWinRate: String { t("% victoires", "Win %", "% victorias") }
    public var driverNotFound: String { t("Pilote introuvable", "Driver not found", "Piloto no encontrado") }
    public var teamNotFound: String { t("Écurie introuvable", "Team not found", "Escudería no encontrada") }
    public func born(_ date: String) -> String { t("Né le \(date)", "Born \(date)", "Nacido el \(date)") }
    public var constructorStatsNote: String {
        t("Départs, victoires et podiums comptent chaque voiture classée.", "Starts, wins and podiums count every classified car.", "Salidas, victorias y podios cuentan cada coche clasificado.")
    }
    public func titles(_ n: Int) -> String { n > 1 ? t("Titres", "Titles", "Títulos") : t("Titre", "Title", "Título") }

    // MARK: Stats
    public func statsTitle(_ season: Int) -> String { t("Stats \(season)", "Stats \(season)", "Estadísticas \(season)") }
    public var noRaceYet: String { t("Pas encore de course disputée", "No race run yet", "Todavía no se ha corrido ninguna carrera") }
    public var championshipProgression: String { t("Évolution du championnat", "Championship progression", "Evolución del campeonato") }
    public func cumulativePointsHint(_ n: Int) -> String {
        t("Points cumulés des \(n) premiers · touchez le graphique pour voir chaque manche",
          "Cumulative points of the top \(n) · tap the chart to see each round",
          "Puntos acumulados de los \(n) primeros · toca el gráfico para ver cada ronda")
    }
    public var lapComparison: String { t("Comparateur de tours", "Lap comparison", "Comparador de vueltas") }
    public var lapComparisonHint: String {
        t("Télémétrie de deux pilotes : vitesse, gaz, freins, écart et carte du circuit.",
          "Two drivers' telemetry: speed, throttle, brakes, gap and track map.",
          "Telemetría de dos pilotos: velocidad, acelerador, frenos, diferencia y mapa del circuito.")
    }
    public var encyclopediaHint: String {
        t("Toutes les saisons depuis 1950, les pilotes, les écuries, les circuits et un glossaire.",
          "Every season since 1950, the drivers, the teams, the circuits and a glossary.",
          "Todas las temporadas desde 1950, los pilotos, las escuderías, los circuitos y un glosario.")
    }
    public var seasonLeaders: String { t("Leaders de la saison", "Season leaders", "Líderes de la temporada") }
    public func leader(_ category: LeaderCategory) -> String {
        switch category {
        case .wins: statWins
        case .podiums: statPodiums
        case .poles: statPoles
        case .fastestLaps: t("Meilleurs tours", "Fastest laps", "Vueltas rápidas")
        case .positionsGained: t("Places gagnées", "Places gained", "Posiciones ganadas")
        case .dnfs: retirements
        }
    }
    public var duel: String { t("Duel", "Head to head", "Duelo") }
    public var pickTwoDrivers: String { t("Choisissez deux pilotes différents.", "Pick two different drivers.", "Elige dos pilotos diferentes.") }
    public func sharedRaces(_ n: Int) -> String { "\(n) GP" }
    public var aheadInRace: String { t("Devant en course", "Ahead in races", "Delante en carrera") }
    public var aheadOnGrid: String { t("Devant au départ", "Ahead on the grid", "Delante en la parrilla") }
    public var bestFinish: String { t("Meilleur résultat", "Best finish", "Mejor resultado") }
    public var averageFinish: String { t("Arrivée moyenne", "Average finish", "Llegada media") }
    public var retirements: String { t("Abandons", "Retirements", "Abandonos") }
    public func noRetirement(_ names: String) -> String { t("Aucun abandon : \(names)", "No retirement: \(names)", "Ningún abandono: \(names)") }
    public var nonStarters: String { t("Non-partants (DNS) : ", "Did not start (DNS): ", "No tomaron la salida (DNS): ") }
    public var retirementLegend: String {
        t("* abandon mais classé quand même · NC : non classé", "* retired but still classified · NC: not classified", "* abandonó pero quedó clasificado · NC: no clasificado")
    }
    public func chartDescription(_ rounds: Int) -> String {
        t("Évolution des points sur \(rounds) manches", "Points over \(rounds) rounds", "Evolución de los puntos en \(rounds) rondas")
    }
    public func afterRound(_ round: Int) -> String { t("APRÈS LA MANCHE \(round)", "AFTER ROUND \(round)", "TRAS LA RONDA \(round)") }

    // MARK: Live timing
    public var liveTiming: String { "Live timing" }
    public var compareLaps: String { t("Comparer des tours", "Compare laps", "Comparar vueltas") }
    public var onAir: String { t("EN DIRECT", "LIVE", "EN DIRECTO") }
    public var replay: String { "REPLAY" }
    public var raceControl: String { t("Direction de course", "Race control", "Dirección de carrera") }
    public var chooseSession: String { t("Choisir une séance", "Pick a session", "Elegir una sesión") }
    public func lap(_ n: Int) -> String { t("TOUR \(n)", "LAP \(n)", "VUELTA \(n)") }
    public var pause: String { "Pause" }
    public var play: String { t("Lecture", "Play", "Reproducir") }
    public var speed: String { t("Vitesse", "Speed", "Velocidad") }
    public var localTimeSuffix: String { t(" (heure locale)", " (local time)", " (hora local)") }
    public var colPos: String { "POS" }
    public var colDriver: String { t("PILOTE", "DRIVER", "PILOTO") }
    public var colGap: String { t("ÉCART", "GAP", "DIF.") }
    public var colGapSwitch: String { t("ÉCART ⇄", "GAP ⇄", "DIF. ⇄") }
    public var colIntervalSwitch: String { t("INTERVALLE ⇄", "INTERVAL ⇄", "INTERVALO ⇄") }
    public var colLast: String { t("DERNIER", "LAST", "ÚLTIMA") }
    public var colTyre: String { t("PNEU", "TYRE", "NEUM.") }
    public func pitStops(_ n: Int) -> String { count(n, t("arrêt", "stop", "parada"), t("arrêts", "stops", "paradas")) }
    public var leaderGap: String { t("Leader", "Leader", "Líder") }
    public var lapWord: String { t("tour", "lap", "vuelta") }
    public var lapsWord: String { t("tours", "laps", "vueltas") }
    public var openF1Account: String { t("Compte OpenF1", "OpenF1 account", "Cuenta OpenF1") }
    public func connectedAs(_ account: String) -> String { t("Connecté : \(account)", "Signed in: \(account)", "Conectado: \(account)") }
    public var liveAvailableDuringSessions: String { t("Le direct est disponible pendant les séances.", "Live data is available during sessions.", "El directo está disponible durante las sesiones.") }
    public var openF1Explanation: String {
        t("Le replay des séances passées est gratuit. Le direct pendant les séances nécessite un compte sponsor OpenF1 ; le mot de passe est stocké dans le trousseau iOS.",
          "Replaying past sessions is free. Live data during sessions needs an OpenF1 sponsor account; the password is stored in the iOS keychain.",
          "La repetición de sesiones pasadas es gratuita. El directo durante las sesiones requiere una cuenta de patrocinador de OpenF1; la contraseña se guarda en el llavero de iOS.")
    }
    public var email: String { t("E-mail", "Email", "Correo") }
    public var password: String { t("Mot de passe", "Password", "Contraseña") }
    public var signOut: String { t("Se déconnecter", "Sign out", "Cerrar sesión") }
    public var signIn: String { t("Se connecter", "Sign in", "Iniciar sesión") }
    public var signingIn: String { t("Connexion…", "Signing in…", "Conectando…") }

    // MARK: Telemetry
    public var lapLabel: String { t("Tour", "Lap", "Vuelta") }
    public var lapTimes: String { t("Temps au tour", "Lap times", "Tiempos por vuelta") }
    public var readout: String { t("Lecture", "Readout", "Lectura") }
    public var speedChart: String { t("Vitesse (km/h)", "Speed (km/h)", "Velocidad (km/h)") }
    public func deltaChart(_ a: String) -> String { t("Écart cumulé (s) — au-dessus de 0 : \(a) devant", "Cumulative gap (s) — above 0: \(a) ahead", "Diferencia acumulada (s) — por encima de 0: \(a) delante") }
    public var throttleChart: String { t("Accélérateur (%)", "Throttle (%)", "Acelerador (%)") }
    public var brakeChart: String { t("Freinage", "Braking", "Frenada") }
    public var miniSectors: String { t("Domination par mini-secteur", "Mini-sector dominance", "Dominio por minisector") }
    public func miniSectorsWon(_ code: String, _ n: Int) -> String { t("\(code) · \(n) mini-secteurs", "\(code) · \(n) mini-sectors", "\(code) · \(n) minisectores") }
    public var telemetryNote: String {
        t("Distance intégrée depuis la vitesse (OpenF1 ne fournit pas de canal distance) : positions précises à quelques mètres près.",
          "Distance integrated from speed (OpenF1 has no distance channel): positions are accurate to a few metres.",
          "Distancia integrada a partir de la velocidad (OpenF1 no ofrece canal de distancia): posiciones precisas a unos metros.")
    }
    public var lapColumn: String { t("TOUR", "LAP", "VUELTA") }
    public func lapOption(_ n: Int, _ time: String, best: Bool) -> String {
        t("Tour \(n) · \(time)", "Lap \(n) · \(time)", "Vuelta \(n) · \(time)") + (best ? t(" (meilleur)", " (best)", " (mejor)") : "")
    }

    // MARK: Reminders & widget
    public var notifyBeforeSessions: String { t("Me prévenir avant les séances", "Notify me before sessions", "Avisarme antes de las sesiones") }
    public var notifyBeforeSessionsFooter: String {
        t("Une notification avant chaque séance choisie, même app fermée. Ouvrez l'app de temps en temps pour que les rappels suivent les changements de calendrier.",
          "A notification before each chosen session, even with the app closed. Open the app now and then so reminders follow calendar changes.",
          "Una notificación antes de cada sesión elegida, incluso con la app cerrada. Abre la app de vez en cuando para que los recordatorios sigan los cambios del calendario.")
    }
    public var notificationsDisabled: String { t("Les notifications sont désactivées pour Apex Lap.", "Notifications are turned off for Apex Lap.", "Las notificaciones están desactivadas para Apex Lap.") }
    public var howLongBefore: String { t("Combien de temps avant", "How long before", "Con cuánta antelación") }
    public var delay: String { t("Délai", "Delay", "Antelación") }
    public func minutes(_ n: Int) -> String { "\(n) min" }
    public func hours(_ n: Int) -> String { "\(n) h" }
    public var whichSessions: String { t("Quelles séances", "Which sessions", "Qué sesiones") }
    public func reminderScope(_ scope: ReminderScope) -> String {
        switch scope {
        case .allSessions: t("Toutes les séances", "All sessions", "Todas las sesiones")
        case .competitive: t("Qualifs, sprints et courses", "Qualifying, sprints and races", "Clasificaciones, sprints y carreras")
        case .racesOnly: t("Courses uniquement", "Races only", "Solo carreras")
        }
    }
    public var upcomingReminders: String { t("Prochains rappels", "Upcoming reminders", "Próximos recordatorios") }
    public var noSessionIn14Days: String { t("Aucune séance dans les 14 prochains jours.", "No session in the next 14 days.", "Ninguna sesión en los próximos 14 días.") }
    public func reminderLine(_ day: String, _ time: String, _ reminderTime: String) -> String {
        t("\(day) à \(time) · rappel à \(reminderTime)", "\(day) at \(time) · reminder at \(reminderTime)", "\(day) a las \(time) · aviso a las \(reminderTime)")
    }
    public var homeWidget: String { t("Widget d'écran d'accueil", "Home screen widget", "Widget de la pantalla de inicio") }
    public var widgetHowTo: String {
        t("Ajoutez le widget Apex Lap : appui long sur l'écran d'accueil → bouton + → Apex Lap. Trois tailles : compte à rebours, week-end en cours, ou programme complet avec vos favoris.",
          "Add the Apex Lap widget: long-press the home screen → + button → Apex Lap. Three sizes: countdown, current weekend, or full schedule with your favourites.",
          "Añade el widget de Apex Lap: mantén pulsada la pantalla de inicio → botón + → Apex Lap. Tres tamaños: cuenta atrás, fin de semana en curso o horario completo con tus favoritos.")
    }
    public func notificationBody(_ time: String, _ minutes: Int) -> String {
        t("Départ à \(time) — dans \(minutes) min", "Starts at \(time) — in \(minutes) min", "Empieza a las \(time) — en \(minutes) min")
    }
    public var nextSession: String { t("PROCHAINE SÉANCE", "NEXT SESSION", "PRÓXIMA SESIÓN") }
    public var nextSessionTitle: String { t("Prochaine séance", "Next session", "Próxima sesión") }
    public var widgetDescription: String { t("La prochaine séance de F1, le week-end en cours et vos favoris.", "The next F1 session, the current weekend and your favourites.", "La próxima sesión de F1, el fin de semana en curso y tus favoritos.") }
    public var seasonOver: String { t("Saison terminée", "Season over", "Temporada terminada") }
    public var openAppToLoad: String { t("Ouvrez Apex Lap pour charger le calendrier", "Open Apex Lap to load the calendar", "Abre Apex Lap para cargar el calendario") }
    public var liveNow: String { t("EN DIRECT", "LIVE", "EN DIRECTO") }
    public var startsInLabel: String { t("DÉPART DANS", "STARTS IN", "EMPIEZA EN") }
    public var weekendSchedule: String { t("PROGRAMME", "SCHEDULE", "HORARIO") }
    public func pointsBehind(_ points: String) -> String { t("à \(points) pts du leader", "\(points) pts behind the leader", "a \(points) pts del líder") }
    public var now: String { t("maintenant", "now", "ahora") }
    /// "dans 2 j 4 h", "in 3 h 05", "en 45 min".
    public func relativeStart(minutes: Int) -> String {
        minutes <= 0 ? now : t("dans ", "in ", "en ") + compactDuration(minutes: minutes)
    }
    /// "2 j 4 h", "3 h 05", "45 min": a countdown without the "in".
    public func compactDuration(minutes: Int) -> String {
        if minutes < 60 { return "\(minutes) min" }
        if minutes < 24 * 60 {
            let rest = minutes % 60
            return "\(minutes / 60) h" + (rest > 0 ? String(format: " %02d", rest) : "")
        }
        let days = minutes / (24 * 60), hours = (minutes / 60) % 24
        return t("\(days) j", "\(days) d", "\(days) d") + (hours > 0 ? " \(hours) h" : "")
    }

    // MARK: Encyclopedia
    public var tabSeasons: String { t("Saisons", "Seasons", "Temporadas") }
    public var tabTeams: String { t("Écuries", "Teams", "Escuderías") }
    public var tabCircuits: String { t("Circuits", "Circuits", "Circuitos") }
    public var tabGlossary: String { t("Glossaire", "Glossary", "Glosario") }
    public var worldChampionsFilter: String { t("🏆 Champions du monde", "🏆 World champions", "🏆 Campeones del mundo") }
    public var noDriverFound: String { t("Aucun pilote trouvé", "No driver found", "Ningún piloto encontrado") }
    public var noTeamFound: String { t("Aucune écurie trouvée", "No team found", "Ninguna escudería encontrada") }
    public var noCircuitFound: String { t("Aucun circuit trouvé", "No circuit found", "Ningún circuito encontrado") }
    public var noTermFound: String { t("Aucun terme trouvé", "No term found", "Ningún término encontrado") }
    public func bornIn(_ year: Int) -> String { t("né en \(year)", "born \(year)", "nacido en \(year)") }
    public var searchPrompt: String { t("Rechercher", "Search", "Buscar") }
    public func leaderLine(_ driver: String, _ points: String) -> String { t("En tête : \(driver) · \(points) pts", "Leader: \(driver) · \(points) pts", "Líder: \(driver) · \(points) pts") }
    public var seasonInProgress: String { t("Saison en cours", "Season in progress", "Temporada en curso") }
    public func constructorsChampion(_ team: String) -> String { t("Constructeurs : \(team)", "Constructors: \(team)", "Constructores: \(team)") }
    public var inProgress: String { t("EN COURS", "IN PROGRESS", "EN CURSO") }
    public func glossaryCategory(_ category: GlossaryCategory) -> String {
        switch category {
        case .race: t("Course", "Race", "Carrera")
        case .strategy: t("Stratégie", "Strategy", "Estrategia")
        case .technical: t("Technique", "Technical", "Técnica")
        case .flags: t("Drapeaux & sécurité", "Flags & safety", "Banderas y seguridad")
        case .rules: t("Règlement", "Rules", "Reglamento")
        }
    }

    // MARK: Season page
    public var worldChampion: String { t("🏆 CHAMPION DU MONDE", "🏆 WORLD CHAMPION", "🏆 CAMPEÓN DEL MUNDO") }
    public func championLine(_ team: String, _ points: String, _ wins: Int, _ races: Int) -> String {
        "\(team) · \(points) pts · " + self.wins(wins) + t(" en \(races) GP", " in \(races) GP", " en \(races) GP")
    }
    public func grandsPrixCount(_ n: Int) -> String { t("Grands Prix · \(n)", "Grands Prix · \(n)", "Grandes Premios · \(n)") }
    public var driversChampionship: String { t("Championnat pilotes", "Drivers' championship", "Campeonato de pilotos") }
    public var constructorsChampionship: String { t("Championnat constructeurs", "Constructors' championship", "Campeonato de constructores") }
    public var noConstructorsBefore1958: String {
        t("Le championnat des constructeurs n'existe que depuis 1958.", "The constructors' championship only exists since 1958.", "El campeonato de constructores solo existe desde 1958.")
    }
    public var highlights: String { t("Moments forts", "Highlights", "Momentos destacados") }
    public func mostWinsHighlight(_ driver: String, _ wins: Int) -> String {
        t("\(driver) remporte le plus de Grands Prix (\(wins))", "\(driver) wins the most Grands Prix (\(wins))", "\(driver) gana más Grandes Premios (\(wins))")
    }
    public func teamWinsHighlight(_ team: String, _ wins: Int) -> String { "\(team) " + t("totalise ", "takes ", "suma ") + self.wins(wins) }
    public func differentWinners(_ n: Int) -> String {
        n > 1 ? t("\(n) vainqueurs différents", "\(n) different winners", "\(n) ganadores distintos") : t("1 vainqueur unique", "1 single winner", "1 único ganador")
    }
    public func winsFromPole(_ n: Int) -> String { self.wins(n) + t(" depuis la pole position", " from pole position", " desde la pole") }
    public func deepestWin(_ driver: String, _ grid: Int, _ race: String) -> String {
        t("\(driver) gagne en partant \(grid)e (\(race))", "\(driver) wins from \(ordinal(grid)) on the grid (\(race))", "\(driver) gana saliendo \(grid).º (\(race))")
    }
    public func titleMargin(_ margin: String, _ runnerUp: String, finished: Bool) -> String {
        finished
            ? t("Titre joué avec \(margin) pts d'avance sur \(runnerUp)", "Title won by \(margin) pts over \(runnerUp)", "Título ganado con \(margin) pts de ventaja sobre \(runnerUp)")
            : t("\(margin) pts d'avance sur \(runnerUp)", "\(margin) pts ahead of \(runnerUp)", "\(margin) pts de ventaja sobre \(runnerUp)")
    }

    // MARK: Circuit page
    public var circuit: String { t("Circuit", "Circuit", "Circuito") }
    public var circuitNotFound: String { t("Circuit introuvable", "Circuit not found", "Circuito no encontrado") }
    public var statGrandsPrix: String { t("Grands Prix", "Grands Prix", "Grandes Premios") }
    public var statFirst: String { t("Premier", "First", "Primero") }
    public var statLast: String { t("Dernier", "Last", "Último") }
    public var keyCorners: String { t("Virages clés", "Key corners", "Curvas clave") }
    public var records: String { t("Records", "Records", "Récords") }
    public var raceLapRecord: String { t("Record du tour en course", "Race lap record", "Récord de vuelta en carrera") }
    public var mostPoleStarts: String { t("Le plus de départs en pole", "Most pole starts", "Más salidas desde la pole") }
    public var recordsNote: String {
        t("Temps au tour connus depuis 2004, toutes configurations du circuit confondues.", "Lap times known since 2004, all circuit layouts combined.", "Tiempos por vuelta conocidos desde 2004, con todas las configuraciones del circuito.")
    }
    public var noGrandPrixHereYet: String { t("Aucun Grand Prix disputé ici pour l'instant", "No Grand Prix held here yet", "Todavía no se ha disputado ningún Gran Premio aquí") }
    public var kingsOfCircuit: String { t("Rois du circuit", "Kings of the circuit", "Reyes del circuito") }
    public func wins(_ n: Int) -> String { count(n, t("victoire", "win", "victoria"), t("victoires", "wins", "victorias")) }
    public var roll: String { t("Palmarès", "Roll of honour", "Palmarés") }
}
