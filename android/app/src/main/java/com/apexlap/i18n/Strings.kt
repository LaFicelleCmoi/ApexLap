package com.apexlap.i18n

import com.apexlap.domain.encyclopedia.GlossaryCategory
import com.apexlap.domain.live.TrackStatus
import com.apexlap.domain.model.SessionType
import com.apexlap.domain.usecase.LeaderCategory
import com.apexlap.domain.usecase.ReminderScope

/** Every user-facing text of the app, in French, English and Spanish side by side. */
class Strings(val language: AppLanguage) {
    private fun t(fr: String, en: String, es: String) = when (language) {
        AppLanguage.FR -> fr
        AppLanguage.EN -> en
        AppLanguage.ES -> es
    }

    /** "1 victoire" / "2 victoires": [one] and [many] already in the right language. */
    private fun count(n: Int, one: String, many: String) = "$n ${if (n > 1) many else one}"

    // Common
    val retry = t("RÉESSAYER", "RETRY", "REINTENTAR")
    val back = t("Retour", "Back", "Atrás")
    val close = t("Fermer", "Close", "Cerrar")
    val none = t("Aucun", "None", "Ninguno")
    val pts = t("pts", "pts", "pts")
    val ptsUpper = t("PTS", "PTS", "PTS")
    val driver = t("Pilote", "Driver", "Piloto")
    val team = t("Écurie", "Team", "Escudería")
    val search = t("Rechercher", "Search", "Buscar")
    fun results(n: Int) = count(n, t("résultat", "result", "resultado"), t("résultats", "results", "resultados"))
    fun winsShort(n: Int) = t("$n VICT.", "$n WINS", "$n VICT.")
    fun round(n: Int) = t("MANCHE $n", "ROUND $n", "RONDA $n")
    fun roundShort(n: Int) = t("M$n", "R$n", "R$n")
    fun lapShort(n: Int) = t("T$n", "L$n", "V$n")
    fun position(n: Int) = "P$n"
    fun ordinal(n: Int) = t(if (n == 1) "1re" else "${n}e", "$n" + englishSuffix(n), "$n.º")
    private fun englishSuffix(n: Int) = when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }

    // Errors
    val offlineCached = t("Hors ligne — affichage des données en cache", "Offline — showing cached data", "Sin conexión — mostrando datos en caché")
    fun serverDown(code: Int) = t("Serveur indisponible (HTTP $code)", "Server unavailable (HTTP $code)", "Servidor no disponible (HTTP $code)")
    val loadFailed = t("Impossible de charger les données", "Couldn't load the data", "No se pudieron cargar los datos")
    val noOpenF1Account = t("Aucun compte OpenF1 configuré", "No OpenF1 account set up", "No hay cuenta de OpenF1 configurada")
    val noLiveSession = t("Aucune séance en cours", "No session in progress", "No hay ninguna sesión en curso")
    fun openF1Refused(code: Int) = t("Connexion OpenF1 refusée (code $code)", "OpenF1 connection refused (code $code)", "Conexión a OpenF1 rechazada (código $code)")
    val openF1TokenRefused = t("Jeton OpenF1 refusé", "OpenF1 token refused", "Token de OpenF1 rechazado")
    val openF1BadCredentials = t("Identifiants OpenF1 incorrects", "Wrong OpenF1 credentials", "Credenciales de OpenF1 incorrectas")
    val streamInterrupted = t("Flux interrompu, reconnexion…", "Stream interrupted, reconnecting…", "Transmisión interrumpida, reconectando…")
    val telemetryUnavailable = t("Télémétrie indisponible pour ce tour", "Telemetry unavailable for this lap", "Telemetría no disponible para esta vuelta")

    // Navigation
    val navCalendar = t("Calendrier", "Calendar", "Calendario")
    val navResults = t("Résultats", "Results", "Resultados")
    val navLive = t("Live", "Live", "Directo")
    val navStandings = t("Classements", "Standings", "Clasificación")
    val navStats = t("Stats", "Stats", "Estadísticas")

    // Sessions
    fun session(type: SessionType) = when (type) {
        SessionType.PRACTICE_1 -> t("Essais libres 1", "Practice 1", "Libres 1")
        SessionType.PRACTICE_2 -> t("Essais libres 2", "Practice 2", "Libres 2")
        SessionType.PRACTICE_3 -> t("Essais libres 3", "Practice 3", "Libres 3")
        SessionType.SPRINT_QUALIFYING -> t("Qualifications sprint", "Sprint qualifying", "Clasificación sprint")
        SessionType.SPRINT -> "Sprint"
        SessionType.QUALIFYING -> t("Qualifications", "Qualifying", "Clasificación")
        SessionType.RACE -> t("Course", "Race", "Carrera")
    }
    fun sessionShort(type: SessionType) = when (type) {
        SessionType.PRACTICE_1 -> t("EL1", "FP1", "L1")
        SessionType.PRACTICE_2 -> t("EL2", "FP2", "L2")
        SessionType.PRACTICE_3 -> t("EL3", "FP3", "L3")
        SessionType.SPRINT_QUALIFYING -> "SQ"
        SessionType.SPRINT -> "SPR"
        SessionType.QUALIFYING -> "Q"
        SessionType.RACE -> "GP"
    }
    /** OpenF1 session names ("Race", "Practice 1"…). */
    fun openF1Session(name: String) = when (name) {
        "Race" -> session(SessionType.RACE)
        "Qualifying" -> session(SessionType.QUALIFYING)
        "Sprint" -> session(SessionType.SPRINT)
        "Sprint Qualifying", "Sprint Shootout" -> t("Qualifs sprint", "Sprint quali", "Clasif. sprint")
        "Practice 1" -> sessionShort(SessionType.PRACTICE_1)
        "Practice 2" -> sessionShort(SessionType.PRACTICE_2)
        "Practice 3" -> sessionShort(SessionType.PRACTICE_3)
        else -> name
    }

    // Calendar
    fun seasonTitle(season: Int) = t("SAISON $season", "SEASON $season", "TEMPORADA $season")
    fun noGrandPrix(season: Int) = t("Aucun Grand Prix pour $season", "No Grand Prix for $season", "Ningún Gran Premio en $season")
    val encyclopedia = t("Encyclopédie", "Encyclopedia", "Enciclopedia")
    val sessionReminders = t("Rappels de séance", "Session reminders", "Recordatorios de sesión")
    val settings = t("Réglages", "Settings", "Ajustes")
    fun nextGrandPrix(round: Int) = t("PROCHAIN GRAND PRIX · MANCHE $round", "NEXT GRAND PRIX · ROUND $round", "PRÓXIMO GRAN PREMIO · RONDA $round")
    val calendar = navCalendar
    val days = t("JOURS", "DAYS", "DÍAS")
    val hoursShort = "H"
    val minutesShort = "MIN"
    val secondsShort = t("SEC", "SEC", "SEG")
    val finished = t("TERMINÉ", "FINISHED", "TERMINADO")
    val next = t("PROCHAIN", "NEXT", "PRÓXIMO")

    // Favorites
    val myFavorites = t("Mes favoris", "My favourites", "Mis favoritos")
    val chooseFavorites = t("Choisir mon pilote et mon écurie favoris", "Pick my favourite driver and team", "Elegir mi piloto y escudería favoritos")
    val follow = t("Suivre", "Follow", "Seguir")
    val unfollow = t("Ne plus suivre", "Unfollow", "Dejar de seguir")
    val favoriteDriver = t("Pilote favori", "Favourite driver", "Piloto favorito")
    val favoriteTeam = t("Écurie favorite", "Favourite team", "Escudería favorita")
    val favoriteHint = t(
        "Mis en avant dans l'accueil et les classements. Vous pouvez aussi toucher ★ sur la fiche d'un pilote ou d'une écurie.",
        "Highlighted on the home screen and in the standings. You can also tap ★ on a driver or team page.",
        "Destacados en la portada y en las clasificaciones. También puedes tocar ★ en la ficha de un piloto o escudería.",
    )
    fun standingLine(position: Int, points: String, wins: Int) =
        "P$position · $points ${pts}" + if (wins > 0) " · " + count(wins, t("victoire", "win", "victoria"), t("victoires", "wins", "victorias")) else ""
    val notRankedYet = t("Pas encore classé cette saison", "Not ranked yet this season", "Aún sin clasificar esta temporada")

    // Settings
    val languageLabel = t("Langue", "Language", "Idioma")
    val systemLanguage = t("Langue du téléphone", "Phone language", "Idioma del teléfono")
    val notifications = t("Notifications", "Notifications", "Notificaciones")
    val remindersAndWidget = t("Rappels & widget", "Reminders & widget", "Recordatorios y widget")
    val remindersAndWidgetHint = t("Rappels avant les séances et widget d'écran d'accueil", "Reminders before sessions and home screen widget", "Recordatorios antes de las sesiones y widget de inicio")
    val openF1Account = t("Compte OpenF1", "OpenF1 account", "Cuenta OpenF1")
    val about = t("À propos", "About", "Acerca de")
    fun version(name: String) = t("Version $name", "Version $name", "Versión $name")
    val dataSources = t(
        "Données : Jolpica-F1 (résultats et historique) et OpenF1 (live et télémétrie). Application non officielle, sans lien avec la Formule 1.",
        "Data: Jolpica-F1 (results and history) and OpenF1 (live and telemetry). Unofficial app, not affiliated with Formula 1.",
        "Datos: Jolpica-F1 (resultados e historia) y OpenF1 (directo y telemetría). Aplicación no oficial, sin relación con la Fórmula 1.",
    )
    val loadingDrivers = t("Chargement du classement…", "Loading the standings…", "Cargando la clasificación…")

    // Results
    fun noFinishedGrandPrix(season: Int) = t("Aucun Grand Prix terminé en $season", "No Grand Prix finished in $season", "Ningún Gran Premio terminado en $season")
    fun resultsTitle(season: Int) = t("RÉSULTATS $season", "RESULTS $season", "RESULTADOS $season")

    // Standings
    fun championshipTitle(season: Int) = t("CHAMPIONNAT $season", "CHAMPIONSHIP $season", "CAMPEONATO $season")
    val searchDriverTeam = t("Rechercher un pilote, une écurie…", "Search a driver, a team…", "Buscar un piloto, una escudería…")
    val drivers = t("PILOTES", "DRIVERS", "PILOTOS")
    val constructors = t("CONSTRUCTEURS", "CONSTRUCTORS", "CONSTRUCTORES")
    fun noResultFor(query: String) = t("Aucun résultat pour « $query »", "No result for “$query”", "Sin resultados para «$query»")
    val noStandingsYet = t("Pas encore de classement", "No standings yet", "Todavía no hay clasificación")

    // Race
    val grandPrix = t("Grand Prix", "Grand Prix", "Gran Premio")
    fun roundOfSeason(round: Int, season: Int) = t("MANCHE $round · $season", "ROUND $round · $season", "RONDA $round · $season")
    val schedule = t("Programme (heure locale)", "Schedule (local time)", "Horario (hora local)")
    val timeTba = t("Horaire à venir", "Time TBA", "Hora por confirmar")
    val tabRace = t("COURSE", "RACE", "CARRERA")
    val tabQualifying = t("QUALIFS", "QUALIFYING", "CLASIF.")
    val tabSprint = "SPRINT"
    val resultsSoon = t("Les résultats apparaîtront ici dès le début du week-end.", "Results will show up here once the weekend starts.", "Los resultados aparecerán aquí cuando empiece el fin de semana.")
    val noSessionResults = t("Pas encore de résultats pour cette séance.", "No results for this session yet.", "Todavía no hay resultados de esta sesión.")
    val fastestLapShort = t("⏱ MT", "⏱ FL", "⏱ VR")
    fun eliminatedIn(session: String) = t("ÉLIMINÉ EN $session", "OUT IN $session", "ELIMINADO EN $session")
    val winner = t("🏆 Vainqueur", "🏆 Winner", "🏆 Ganador")
    val polePosition = "Pole position"
    val fastestLap = t("⏱ Meilleur tour", "⏱ Fastest lap", "⏱ Vuelta rápida")
    val context = t("Contexte", "Background", "Contexto")
    fun edition(n: Int) = if (n == 1) {
        t("Premier Grand Prix disputé sur ce circuit.", "First Grand Prix held at this circuit.", "Primer Gran Premio disputado en este circuito.")
    } else {
        t("${n}e Grand Prix disputé sur ce circuit.", "${ordinal(n)} Grand Prix held at this circuit.", "${n}.º Gran Premio disputado en este circuito.")
    }
    fun previousWinner(driver: String, team: String, season: Int) =
        t("Vainqueur précédent : $driver ($team, $season)", "Previous winner: $driver ($team, $season)", "Ganador anterior: $driver ($team, $season)")
    fun mostWinsHere(driver: String, wins: Int) =
        t("Recordman des victoires ici : $driver ($wins)", "Most wins here: $driver ($wins)", "Más victorias aquí: $driver ($wins)")
    val circuitPage = t("Fiche du circuit ›", "Circuit page ›", "Ficha del circuito ›")

    // Driver & constructor pages
    val teamBadge = t("ÉCURIE", "TEAM", "EQUIPO")
    fun seasonHeader(season: Int) = t("Saison $season", "Season $season", "Temporada $season")
    val bio = "Bio"
    val worldTitles = t("Titres mondiaux", "World titles", "Títulos mundiales")
    val constructorTitles = t("Titres constructeurs", "Constructors' titles", "Títulos de constructores")
    val career = t("Carrière", "Career", "Trayectoria")
    val history = t("Historique", "History", "Historia")
    val statPosition = t("Position", "Position", "Posición")
    val statPoints = t("Points", "Points", "Puntos")
    val statWins = t("Victoires", "Wins", "Victorias")
    val statSeasons = t("Saisons", "Seasons", "Temporadas")
    val statRaces = t("Courses", "Races", "Carreras")
    val statStarts = t("Départs", "Starts", "Salidas")
    val statPoles = t("Pole positions", "Pole positions", "Poles")
    val statPolesShort = t("Poles", "Poles", "Poles")
    val statPodiums = t("Podiums", "Podiums", "Podios")
    val statWinRate = t("% victoires", "Win %", "% victorias")
    val driverNotFound = t("Pilote introuvable", "Driver not found", "Piloto no encontrado")
    val teamNotFound = t("Écurie introuvable", "Team not found", "Escudería no encontrada")
    fun born(date: String) = t("Né le $date", "Born $date", "Nacido el $date")
    val constructorStatsNote = t(
        "Départs, victoires et podiums comptent chaque voiture classée.",
        "Starts, wins and podiums count every classified car.",
        "Salidas, victorias y podios cuentan cada coche clasificado.",
    )
    fun titles(n: Int) = if (n > 1) t("Titres", "Titles", "Títulos") else t("Titre", "Title", "Título")

    // Stats
    fun statsTitle(season: Int) = t("STATS $season", "STATS $season", "ESTADÍSTICAS $season")
    fun noRaceYet(season: Int) = t("Pas encore de course disputée en $season", "No race run yet in $season", "Todavía no se ha corrido ninguna carrera en $season")
    val championshipProgression = t("Évolution du championnat", "Championship progression", "Evolución del campeonato")
    fun cumulativePointsHint(n: Int) = t(
        "Points cumulés des $n premiers · touchez le graphique pour voir chaque manche",
        "Cumulative points of the top $n · tap the chart to see each round",
        "Puntos acumulados de los $n primeros · toca el gráfico para ver cada ronda",
    )
    val lapComparison = t("Comparateur de tours", "Lap comparison", "Comparador de vueltas")
    val lapComparisonHint = t(
        "Télémétrie de deux pilotes superposée : vitesse, gaz, freins, écart et carte du circuit.",
        "Two drivers' telemetry overlaid: speed, throttle, brakes, gap and track map.",
        "Telemetría de dos pilotos superpuesta: velocidad, acelerador, frenos, diferencia y mapa del circuito.",
    )
    val encyclopediaHint = t(
        "Toutes les saisons depuis 1950, 881 pilotes, les écuries, les circuits et un glossaire.",
        "Every season since 1950, 881 drivers, the teams, the circuits and a glossary.",
        "Todas las temporadas desde 1950, 881 pilotos, las escuderías, los circuitos y un glosario.",
    )
    val seasonLeaders = t("Leaders de la saison", "Season leaders", "Líderes de la temporada")
    fun leader(category: LeaderCategory) = when (category) {
        LeaderCategory.WINS -> statWins
        LeaderCategory.PODIUMS -> statPodiums
        LeaderCategory.POLES -> statPolesShort
        LeaderCategory.FASTEST_LAPS -> t("Meilleurs tours", "Fastest laps", "Vueltas rápidas")
        LeaderCategory.POSITIONS_GAINED -> t("Places gagnées", "Places gained", "Posiciones ganadas")
        LeaderCategory.DNFS -> retirements
    }
    val duel = t("Duel", "Head to head", "Duelo")
    val versus = "VS"
    val pickTwoDrivers = t("Choisissez deux pilotes différents.", "Pick two different drivers.", "Elige dos pilotos diferentes.")
    val pickDriver = t("Choisir un pilote", "Pick a driver", "Elegir un piloto")
    fun sharedRaces(n: Int) = "$n GP"
    val aheadInRace = t("Devant en course", "Ahead in races", "Delante en carrera")
    val aheadOnGrid = t("Devant au départ", "Ahead on the grid", "Delante en la parrilla")
    val bestFinish = t("Meilleur résultat", "Best finish", "Mejor resultado")
    val averageFinish = t("Arrivée moyenne", "Average finish", "Llegada media")
    val retirements = t("Abandons", "Retirements", "Abandonos")
    fun noRetirement(names: String) = t("Aucun abandon : $names", "No retirement: $names", "Ningún abandono: $names")
    val nonStarters = t("Non-partants (DNS) : ", "Did not start (DNS): ", "No tomaron la salida (DNS): ")
    val retirementLegend = t(
        "* abandon mais classé quand même · NC : non classé",
        "* retired but still classified · NC: not classified",
        "* abandonó pero quedó clasificado · NC: no clasificado",
    )
    fun chartDescription(rounds: Int, leader: String, points: String) = t(
        "Évolution des points sur $rounds manches. En tête : $leader avec $points points.",
        "Points over $rounds rounds. Leader: $leader with $points points.",
        "Evolución de los puntos en $rounds rondas. Líder: $leader con $points puntos.",
    )
    fun afterRound(round: Int) = t("APRÈS LA MANCHE $round", "AFTER ROUND $round", "TRAS LA RONDA $round")

    // Live timing
    val liveTiming = "LIVE TIMING"
    val compareLaps = t("Comparer des tours", "Compare laps", "Comparar vueltas")
    val liveBadge = t("  DIRECT", "  LIVE", "  DIRECTO")
    val onAir = t("  EN DIRECT", "  LIVE", "  EN DIRECTO")
    val replay = "REPLAY"
    val noSessionAvailable = t("Aucune séance disponible", "No session available", "Ninguna sesión disponible")
    val raceControl = t("Direction de course", "Race control", "Dirección de carrera")
    val chooseSession = t("Choisir une séance", "Pick a session", "Elegir una sesión")
    val changeSession = t("Changer de séance", "Change session", "Cambiar de sesión")
    fun lap(n: Int) = t("TOUR $n", "LAP $n", "VUELTA $n")
    val pause = "Pause"
    val play = t("Lecture", "Play", "Reproducir")
    val localTimeSuffix = t(" (heure locale)", " (local time)", " (hora local)")
    val colPos = "POS"
    val colDriver = t("PILOTE", "DRIVER", "PILOTO")
    val colGap = t("ÉCART", "GAP", "DIF.")
    val colGapSwitch = t("ÉCART ⇄", "GAP ⇄", "DIF. ⇄")
    val colIntervalSwitch = t("INTERVALLE ⇄", "INTERVAL ⇄", "INTERVALO ⇄")
    val colLast = t("DERNIER", "LAST", "ÚLTIMA")
    val colTyre = t("PNEU", "TYRE", "NEUM.")
    fun pitStops(n: Int) = count(n, t("arrêt", "stop", "parada"), t("arrêts", "stops", "paradas"))
    val leaderGap = t("Leader", "Leader", "Líder")
    val lapWord = t("tour", "lap", "vuelta")
    val lapsWord = t("tours", "laps", "vueltas")
    fun connectedAs(account: String) = t("Connecté : $account", "Signed in: $account", "Conectado: $account")
    val liveAvailableDuringSessions = t("Le direct est disponible pendant les séances.", "Live data is available during sessions.", "El directo está disponible durante las sesiones.")
    val openF1Explanation = t(
        "Le replay des séances passées est gratuit. Le direct pendant les séances nécessite un compte sponsor OpenF1.",
        "Replaying past sessions is free. Live data during sessions needs an OpenF1 sponsor account.",
        "La repetición de sesiones pasadas es gratuita. El directo durante las sesiones requiere una cuenta de patrocinador de OpenF1.",
    )
    val email = t("E-mail", "Email", "Correo")
    val password = t("Mot de passe", "Password", "Contraseña")
    val becomeSponsor = t("DEVENIR SPONSOR OPENF1", "BECOME AN OPENF1 SPONSOR", "HAZTE PATROCINADOR DE OPENF1")
    val signOut = t("Se déconnecter", "Sign out", "Cerrar sesión")
    val signIn = t("Se connecter", "Sign in", "Iniciar sesión")
    val signingIn = t("Connexion…", "Signing in…", "Conectando…")
    fun trackStatus(status: TrackStatus) = when (status) {
        TrackStatus.GREEN -> t("Piste dégagée", "Track clear", "Pista despejada")
        TrackStatus.YELLOW -> t("Drapeau jaune", "Yellow flag", "Bandera amarilla")
        TrackStatus.VSC -> "Virtual Safety Car"
        TrackStatus.SAFETY_CAR -> "Safety Car"
        TrackStatus.RED -> t("Drapeau rouge", "Red flag", "Bandera roja")
        TrackStatus.CHEQUERED -> t("Drapeau à damier", "Chequered flag", "Bandera a cuadros")
    }
    fun unknownDriver(number: Int) = t("Pilote $number", "Driver $number", "Piloto $number")

    // Telemetry
    val sessionLabel = t("Séance", "Session", "Sesión")
    val lapLabel = t("Tour", "Lap", "Vuelta")
    val lapTimes = t("Temps au tour", "Lap times", "Tiempos por vuelta")
    val readout = t("Lecture", "Readout", "Lectura")
    val speedChart = t("Vitesse (km/h)", "Speed (km/h)", "Velocidad (km/h)")
    fun speedDescription(a: String, b: String) = t("Vitesse de $a et $b sur le tour", "Speed of $a and $b over the lap", "Velocidad de $a y $b en la vuelta")
    fun deltaChart(a: String) = t("Écart cumulé (s) — au-dessus de 0 : $a devant", "Cumulative gap (s) — above 0: $a ahead", "Diferencia acumulada (s) — por encima de 0: $a delante")
    fun deltaDescription(a: String, b: String) = t("Écart de temps cumulé entre $a et $b", "Cumulative time gap between $a and $b", "Diferencia de tiempo acumulada entre $a y $b")
    val throttleChart = t("Accélérateur (%)", "Throttle (%)", "Acelerador (%)")
    val throttleDescription = t("Pression sur l'accélérateur", "Throttle application", "Presión del acelerador")
    val brakeChart = t("Freinage", "Braking", "Frenada")
    val brakeDescription = t("Phases de freinage", "Braking zones", "Fases de frenada")
    val miniSectors = t("Domination par mini-secteur", "Mini-sector dominance", "Dominio por minisector")
    fun miniSectorsWon(code: String, n: Int) = t("$code · $n mini-secteurs", "$code · $n mini-sectors", "$code · $n minisectores")
    fun telemetryNote(b: String) = t(
        "Distance intégrée depuis la vitesse (OpenF1 ne fournit pas de canal distance) : les positions sont précises à quelques mètres près. Freinage de $b légèrement décalé pour rester lisible.",
        "Distance integrated from speed (OpenF1 has no distance channel): positions are accurate to a few metres. $b's braking is slightly offset to stay readable.",
        "Distancia integrada a partir de la velocidad (OpenF1 no ofrece canal de distancia): las posiciones son precisas a unos metros. La frenada de $b está ligeramente desplazada para que se lea bien.",
    )
    val trackMapDescription = t(
        "Carte du circuit colorée par pilote le plus rapide par mini-secteur",
        "Track map coloured by the fastest driver in each mini-sector",
        "Mapa del circuito coloreado según el piloto más rápido en cada minisector",
    )
    val chartTouchHint = t(
        "Touchez ou glissez sur un graphique pour lire les valeurs à un point du tour.",
        "Tap or drag on a chart to read the values at any point of the lap.",
        "Toca o desliza sobre un gráfico para leer los valores en un punto de la vuelta.",
    )
    val lapColumn = t("TOUR", "LAP", "VUELTA")
    fun lapOption(n: Int, time: String, best: Boolean) =
        t("Tour $n · $time", "Lap $n · $time", "Vuelta $n · $time") + if (best) t(" (meilleur)", " (best)", " (mejor)") else ""

    // Reminders
    val notifyBeforeSessions = t("Me prévenir avant les séances", "Notify me before sessions", "Avisarme antes de las sesiones")
    val notifyBeforeSessionsHint = t(
        "Une notification avant chaque séance choisie, même app fermée.",
        "A notification before each chosen session, even with the app closed.",
        "Una notificación antes de cada sesión elegida, incluso con la app cerrada.",
    )
    val notificationsDisabled = t("Les notifications sont désactivées pour Apex Lap.", "Notifications are turned off for Apex Lap.", "Las notificaciones están desactivadas para Apex Lap.")
    val exactAlarmsWarning = t(
        "Sans l'autorisation « alarmes exactes », un rappel peut arriver quelques minutes en retard.",
        "Without the “exact alarms” permission, a reminder may arrive a few minutes late.",
        "Sin el permiso de «alarmas exactas», un recordatorio puede llegar unos minutos tarde.",
    )
    val allow = t("Autoriser", "Allow", "Permitir")
    val howLongBefore = t("Combien de temps avant", "How long before", "Con cuánta antelación")
    fun minutes(n: Int) = "$n min"
    fun hours(n: Int) = "$n h"
    val whichSessions = t("Quelles séances", "Which sessions", "Qué sesiones")
    fun reminderScope(scope: ReminderScope) = when (scope) {
        ReminderScope.ALL_SESSIONS -> t("Toutes les séances", "All sessions", "Todas las sesiones")
        ReminderScope.COMPETITIVE -> t("Qualifs, sprints et courses", "Qualifying, sprints and races", "Clasificaciones, sprints y carreras")
        ReminderScope.RACES_ONLY -> t("Courses uniquement", "Races only", "Solo carreras")
    }
    val upcomingReminders = t("Prochains rappels", "Upcoming reminders", "Próximos recordatorios")
    val noSessionIn14Days = t("Aucune séance dans les 14 prochains jours.", "No session in the next 14 days.", "Ninguna sesión en los próximos 14 días.")
    fun reminderLine(day: String, time: String, reminderTime: String) =
        t("$day à $time · rappel à $reminderTime", "$day at $time · reminder at $reminderTime", "$day a las $time · aviso a las $reminderTime")
    val homeWidget = t("Widget d'écran d'accueil", "Home screen widget", "Widget de la pantalla de inicio")
    val widgetHint = t(
        "Affiche la prochaine séance et le temps restant sur votre écran d'accueil.",
        "Shows the next session and the time left on your home screen.",
        "Muestra la próxima sesión y el tiempo restante en tu pantalla de inicio.",
    )
    val addWidget = t("AJOUTER LE WIDGET", "ADD THE WIDGET", "AÑADIR EL WIDGET")
    val addWidgetManually = t(
        "Appui long sur l'écran d'accueil → Widgets → Apex Lap.",
        "Long-press the home screen → Widgets → Apex Lap.",
        "Mantén pulsada la pantalla de inicio → Widgets → Apex Lap.",
    )
    val nextSession = t("PROCHAINE SÉANCE", "NEXT SESSION", "PRÓXIMA SESIÓN")
    val seasonOver = t("Saison terminée", "Season over", "Temporada terminada")
    val openAppToLoad = t("Ouvrez Apex Lap pour charger le calendrier", "Open Apex Lap to load the calendar", "Abre Apex Lap para cargar el calendario")
    fun startsIn(relative: String, time: String) = t("Départ $relative ($time)", "Starts $relative ($time)", "Empieza $relative ($time)")
    val reminderChannelDescription = t(
        "Prévient quelques minutes avant chaque séance choisie",
        "Alerts you a few minutes before each chosen session",
        "Avisa unos minutos antes de cada sesión elegida",
    )
    val now = t("maintenant", "now", "ahora")
    fun inMinutes(m: Long) = t("dans $m min", "in $m min", "en $m min")
    fun inHours(h: Long, m: Long) = t("dans $h h", "in $h h", "en $h h") + if (m > 0) " ${m.toString().padStart(2, '0')}" else ""
    fun inDays(d: Long, h: Long) = t("dans $d j", "in $d d", "en $d d") + if (h > 0) " $h h" else ""

    // Encyclopedia
    val tabSeasons = t("SAISONS", "SEASONS", "TEMPORADAS")
    val tabDrivers = drivers
    val tabTeams = t("ÉCURIES", "TEAMS", "ESCUDERÍAS")
    val tabCircuits = t("CIRCUITS", "CIRCUITS", "CIRCUITOS")
    val tabGlossary = t("GLOSSAIRE", "GLOSSARY", "GLOSARIO")
    val worldChampionsFilter = t("🏆 Champions du monde", "🏆 World champions", "🏆 Campeones del mundo")
    val noDriverFound = t("Aucun pilote trouvé", "No driver found", "Ningún piloto encontrado")
    val noTeamFound = t("Aucune écurie trouvée", "No team found", "Ninguna escudería encontrada")
    val noCircuitFound = t("Aucun circuit trouvé", "No circuit found", "Ningún circuito encontrado")
    val noTermFound = t("Aucun terme trouvé", "No term found", "Ningún término encontrado")
    fun bornIn(year: Int) = t("né en $year", "born $year", "nacido en $year")
    val searchDriver = t("Rechercher un pilote, une nationalité…", "Search a driver, a nationality…", "Buscar un piloto, una nacionalidad…")
    val searchTeam = t("Rechercher une écurie…", "Search a team…", "Buscar una escudería…")
    val searchCircuit = t("Rechercher un circuit, une ville, un pays…", "Search a circuit, a city, a country…", "Buscar un circuito, una ciudad, un país…")
    val searchTerm = t("Rechercher un terme…", "Search a term…", "Buscar un término…")
    fun leaderLine(driver: String, points: String) = t("En tête : $driver · $points pts", "Leader: $driver · $points pts", "Líder: $driver · $points pts")
    val seasonInProgress = t("Saison en cours", "Season in progress", "Temporada en curso")
    fun constructorsChampion(team: String) = t("Constructeurs : $team", "Constructors: $team", "Constructores: $team")
    val inProgress = t("EN COURS", "IN PROGRESS", "EN CURSO")
    fun glossaryCategory(category: GlossaryCategory) = when (category) {
        GlossaryCategory.RACE -> t("Course", "Race", "Carrera")
        GlossaryCategory.STRATEGY -> t("Stratégie", "Strategy", "Estrategia")
        GlossaryCategory.TECHNICAL -> t("Technique", "Technical", "Técnica")
        GlossaryCategory.FLAGS -> t("Drapeaux & sécurité", "Flags & safety", "Banderas y seguridad")
        GlossaryCategory.RULES -> t("Règlement", "Rules", "Reglamento")
    }

    // Season page
    fun seasonPageTitle(season: Int) = t("Saison $season", "Season $season", "Temporada $season")
    val worldChampion = t("🏆 CHAMPION DU MONDE", "🏆 WORLD CHAMPION", "🏆 CAMPEÓN DEL MUNDO")
    fun championLine(team: String, points: String, wins: Int, races: Int) =
        "$team · $points pts · " + count(wins, t("victoire", "win", "victoria"), t("victoires", "wins", "victorias")) +
            t(" en $races GP", " in $races GP", " en $races GP")
    val constructorsPrefix = t("Constructeurs : ", "Constructors: ", "Constructores: ")
    fun grandsPrixCount(n: Int) = t("Grands Prix · $n", "Grands Prix · $n", "Grandes Premios · $n")
    val driversChampionship = t("Championnat pilotes", "Drivers' championship", "Campeonato de pilotos")
    val constructorsChampionship = t("Championnat constructeurs", "Constructors' championship", "Campeonato de constructores")
    val noConstructorsBefore1958 = t(
        "Le championnat des constructeurs n'existe que depuis 1958.",
        "The constructors' championship only exists since 1958.",
        "El campeonato de constructores solo existe desde 1958.",
    )
    val highlights = t("Moments forts", "Highlights", "Momentos destacados")
    fun mostWinsHighlight(driver: String, wins: Int) =
        t("$driver remporte le plus de Grands Prix ($wins)", "$driver wins the most Grands Prix ($wins)", "$driver gana más Grandes Premios ($wins)")
    fun teamWinsHighlight(team: String, wins: Int) =
        "$team " + t("totalise ", "takes ", "suma ") + count(wins, t("victoire", "win", "victoria"), t("victoires", "wins", "victorias"))
    fun differentWinners(n: Int) = if (n > 1) {
        t("$n vainqueurs différents", "$n different winners", "$n ganadores distintos")
    } else {
        t("1 vainqueur unique", "1 single winner", "1 único ganador")
    }
    fun winsFromPole(n: Int) = count(n, t("victoire", "win", "victoria"), t("victoires", "wins", "victorias")) +
        t(" depuis la pole position", " from pole position", " desde la pole")
    fun deepestWin(driver: String, grid: Int, race: String) =
        t("$driver gagne en partant ${grid}e ($race)", "$driver wins from ${ordinal(grid)} on the grid ($race)", "$driver gana saliendo ${grid}.º ($race)")
    fun titleMargin(margin: String, runnerUp: String, finished: Boolean) = if (finished) {
        t("Titre joué avec $margin pts d'avance sur $runnerUp", "Title won by $margin pts over $runnerUp", "Título ganado con $margin pts de ventaja sobre $runnerUp")
    } else {
        t("$margin pts d'avance sur $runnerUp", "$margin pts ahead of $runnerUp", "$margin pts de ventaja sobre $runnerUp")
    }

    // Circuit page
    val circuit = t("Circuit", "Circuit", "Circuito")
    val circuitNotFound = t("Circuit introuvable", "Circuit not found", "Circuito no encontrado")
    val statGrandsPrix = t("Grands Prix", "Grands Prix", "Grandes Premios")
    val statFirst = t("Premier", "First", "Primero")
    val statLast = t("Dernier", "Last", "Último")
    val keyCorners = t("Virages clés", "Key corners", "Curvas clave")
    val records = t("Records", "Records", "Récords")
    val raceLapRecord = t("Record du tour en course", "Race lap record", "Récord de vuelta en carrera")
    val mostPoleStarts = t("Le plus de départs en pole", "Most pole starts", "Más salidas desde la pole")
    val recordsNote = t(
        "Temps au tour connus depuis 2004, toutes configurations du circuit confondues.",
        "Lap times known since 2004, all circuit layouts combined.",
        "Tiempos por vuelta conocidos desde 2004, con todas las configuraciones del circuito.",
    )
    val noGrandPrixHereYet = t("Aucun Grand Prix disputé ici pour l'instant", "No Grand Prix held here yet", "Todavía no se ha disputado ningún Gran Premio aquí")
    val kingsOfCircuit = t("Rois du circuit", "Kings of the circuit", "Reyes del circuito")
    fun wins(n: Int) = count(n, t("victoire", "win", "victoria"), t("victoires", "wins", "victorias"))
    val roll = t("Palmarès", "Roll of honour", "Palmarés")
}
