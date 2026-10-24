# ClubFirst — Android football companion (working prototype 0.1.0)

**ClubFirst is a temporary project name, not a brand decision.** This is a real native Android Jetpack Compose / Material 3 project, not a screenshot, web mockup or finished live-data service.

## Implemented now
- Native Material 3 five-tab app: Home, Live, Calendar, Updates, Clubs
- First-run selection of multiple clubs and followed leagues; stored locally
- My Clubs / My Leagues mode switch; individual-club selection; All Clubs neutral theme
- Club-aware light/dark themes, with **preliminary illustrative** club colours (not verified official brand values)
- Home briefing/fixture placeholders clearly marked where no data is available
- Matchday Centre layout with radio and broadcaster outbound links
- Advanced stats and physical-performance fields that never invent unavailable measurements
- Club hub: Overview, Analysis, Squad, History, Stadium/Tickets, Memories
- Editable and deletable local match memories
- Editable and deletable personal calendar events; Android Calendar insertion shortcut
- Trusted news and official club outbound links, social/video integration placeholders
- Settings for appearance and followed clubs/leagues
- GitHub Actions workflow that compiles and uploads a debug APK

## Not yet implemented / requires providers and licences
- Live scores, fixtures, league tables, squads, xG, tracking-chip metrics, live commentary audio, broadcaster schedules, ticket inventory, social timelines, images/crests and push notifications.
- A global searchable club database (the starter catalogue contains ten teams).
- Actual licensed club brand colours, crests, photos, fonts and artwork. Brand colours currently shown are **unverified examples**.
- Automatic fixture calendar sync, in-app radio streaming, Android live score notifications, photo attachments to memories.

No fixture, performance number, TV listing or article is fabricated in the app. External links do not imply rights to republish or embed third-party content.

## Build an APK
1. Upload this project to a GitHub repository (the contents of this folder at the repository root).
2. Open **Actions → Build Android APK → Run workflow**.
3. Download the `ClubFirst-debug-apk` artifact and install `app-debug.apk` on your Android device.

Alternatively, open the project in Android Studio (JDK 17, Android SDK 35) and run **Build → Build APK(s)**. The GitHub workflow uses Gradle 8.11.1; a wrapper is not bundled.

## Development notes
- Source: `app/src/main/java/com/clubfirst/football/MainActivity.kt`
- `minSdk=26`, `targetSdk=35`, Compose BOM 2025.04.01, AGP 8.9.2, Kotlin 2.1.20.
- Data is currently saved to app-local SharedPreferences. A Room migration will be appropriate before major feature expansion.
- Next engineering step: agree on a licensed football API and create a provider/repository layer, then integrate live match data and fixture/calendar synchronisation.
