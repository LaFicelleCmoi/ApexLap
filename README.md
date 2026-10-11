# 🏁 Apex Lap

Application F1 native **Android (Kotlin / Jetpack Compose)** et **iOS (Swift / SwiftUI)** : calendrier de la saison avec compte à rebours, résultats, championnats pilotes & constructeurs, fiches pilotes / écuries, mode hors-ligne.

Données : [Jolpica-F1](https://github.com/jolpica/jolpica-f1) (successeur d'Ergast, gratuit, sans authentification).
Cahier des charges : [apex-lap-application-f1-multiplateforme-kotlin-&-swift.md](apex-lap-application-f1-multiplateforme-kotlin-&-swift.md).

## Périmètre livré — MVP (v1)

| Fonctionnalité | Android | iOS |
|---|---|---|
| Calendrier + prochain GP + compte à rebours live | ✅ | ✅ |
| Programme du week-end (EL, sprint, qualifs, course) en heure locale | ✅ | ✅ |
| Résultats de course (temps, points, meilleur tour, places gagnées) | ✅ | ✅ |
| Championnats pilotes / constructeurs + recherche | ✅ | ✅ |
| Fiche pilote (numéro, casque aux couleurs de l'écurie, saison, carrière) | ✅ | ✅ |
| Fiche écurie (saison, pilotes, historique) | ✅ | ✅ |
| Offline-first (cache local, bandeau hors-ligne, pull-to-refresh) | Room | SwiftData |
| Thème sombre « F1 night race » | ✅ | ✅ |
| Résultats de la saison (podiums, vainqueur du sprint) | ✅ | — |
| Détail d'un GP : Course / Qualifs (Q1-Q2-Q3) / Sprint | ✅ | — |
| Stats : évolution du championnat, leaders, duel entre deux pilotes | ✅ | — |
| Rappels avant les séances (notifications locales) | ✅ | — |
| Widget « prochaine séance » (Glance) | ✅ | — |

Les ajouts au-delà du MVP n'existent pour l'instant que sur Android. Le live timing (OpenF1) n'est pas encore commencé.

## Architecture

Les deux apps suivent la même découpe Clean Architecture / MVVM :

```
Présentation  Compose + ViewModel (StateFlow)    │  SwiftUI + @Observable view models
Domaine       Modèles, contrat F1Repository, logique calendrier (prochaine session, compte à rebours)
Données       API Jolpica → mappers → repository offline-first → Room / SwiftData
```

**Offline-first** : l'UI lit toujours le cache local, qui est la seule source de vérité. Un rafraîchissement réseau réécrit le cache. Si le réseau échoue, les données en cache restent affichées avec un bandeau « Hors ligne ».

- Android : les DAO Room exposent des `Flow`, et `OfflineFirstLoader` combine flux de cache et état de rafraîchissement.
- iOS : `OfflineFirst<Value>` charge le cache instantanément puis rafraîchit. `SwiftDataF1Store` (`@ModelActor`) persiste les modèles `Codable`.

**Réseau** : Jolpica limite à ~4 requêtes/s. Un intercepteur OkHttp espace les appels et réessaie les réponses 429 (en respectant `Retry-After`). Les fiches sont mises en cache 12 h, les résultats de la saison 10 min.

**Rappels & widget** : `PlanRemindersUseCase` choisit les séances des 14 prochains jours selon les réglages (délai, types de séances). `ReminderScheduler` les transforme en alarmes `AlarmManager` (exactes si l'utilisateur l'autorise, sinon approximatives). Un `SyncWorker` WorkManager rafraîchit le calendrier toutes les 6 h, reprogramme les rappels et redessine le widget. Les rappels sont aussi reprogrammés au redémarrage du téléphone et aux changements d'heure.

**Statistiques de carrière** : Jolpica n'expose pas de résumé de carrière. Les chiffres sont donc calculés à partir des compteurs `total` (saisons, départs, `results/1|2|3`, `grid/1`). On compte les poles via `grid/1`, car l'endpoint `qualifying/1` surcompte (119 au lieu de 104 pour Hamilton).

```
android/                         Projet Gradle (un module app, packages domain / data / ui)
ios/
  ApexLapKit/                    Package Swift : ApexLapDomain + ApexLapData (compile aussi sous Linux)
  ApexLap/                       App SwiftUI (persistance SwiftData, design system, écrans)
  project.yml                    Spécification XcodeGen (le .xcodeproj est généré, pas versionné)
.github/workflows/               CI Android + iOS
```

## Lancer le projet

### Android

Prérequis : JDK 17, SDK Android 35.

```bash
cd android && ./gradlew installDebug
```

APK release signé (minifié par R8) dans `android/app/build/outputs/apk/release/` :

```bash
cd android && ./gradlew assembleRelease
```

La signature lit `android/keystore.properties` (non versionné : `storeFile`, `storePassword`, `keyAlias`, `keyPassword`), ou en CI les variables `APEXLAP_KEYSTORE_PATH`, `APEXLAP_KEYSTORE_PASSWORD`, `APEXLAP_KEY_ALIAS`, `APEXLAP_KEY_PASSWORD`. Sans clé, le build release reste non signé.

Tests unitaires et lint :

```bash
cd android && ./gradlew testDebugUnitTest lintDebug
```

### iOS

Prérequis : Xcode 16+, iOS 17+, [XcodeGen](https://github.com/yonaskolb/XcodeGen).

```bash
cd ios && xcodegen generate && open ApexLap.xcodeproj
```

Tests du cœur métier (fonctionne aussi sous Linux) :

```bash
cd ios/ApexLapKit && swift test
```

## Tests

| | Couverture |
|---|---|
| Android (JUnit, Turbine, coroutines-test) | mappers sur fixtures Jolpica réelles, logique calendrier / compte à rebours, repository offline-first (cache conservé si hors-ligne, remplacement sans doublons, stats de carrière), `StandingsViewModel` (recherche, erreur hors-ligne) |
| iOS (Swift Testing + XCTest) | mêmes scénarios côté `ApexLapKit`, plus `OfflineFirst` et view model dans `ApexLapTests` |

## CI

- **Android** : lint + tests unitaires + APK debug, publiés en artefacts.
- **iOS** : `swift test` sur `ApexLapKit` (conteneur Linux `swift:6.0`), puis génération XcodeGen + `xcodebuild test` sur simulateur (macOS 15).

## Feuille de route

1. ~~Sprint 1–4 : setup, CI, design system, calendrier, résultats, classements, cache, fiches, recherche~~
2. ~~Sprint 5–6 (Android) : rappels de séance, widget, graphiques, duel entre pilotes~~
3. Live timing (OpenF1, WebSocket), comparateur de tours (télémétrie), portage iOS des ajouts (WidgetKit, notifications locales, Swift Charts)
4. Sprint 7 : polish, tests UI (Compose UI tests / XCUITest), captures Store
