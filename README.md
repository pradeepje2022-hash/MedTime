# Blue Pinger

Network ping monitor for Android (Kotlin + Jetpack Compose) with a blue theme.

- Live latency gauge, min / avg / max / jitter / loss, live chart, reply log
- Runs in the background (foreground service + notification with Stop button)
- Quick Settings tile to start / stop
- Targets: any host or IP, intervals 0.5s / 1s / 2s / 5s

## Build with GitHub Actions
1. Create a new GitHub repo and upload all files in this folder (keep `.github/workflows/build.yml`).
2. Open the **Actions** tab, run **Build APK** (it also runs on every push).
3. Download the `BluePinger-apk` artifact, unzip, install `app-release.apk`.
