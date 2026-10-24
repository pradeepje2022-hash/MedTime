# J Pinger Pro 1.1.0

Build: open in Android Studio, or `gradle assembleDebug` (Gradle 8.5+, JDK 17). APK: app/build/outputs/apk/debug/app-debug.apk

What's new vs 1.0.0
- Foreground service: keeps pinging in the background / screen off (notification with Stop button)
- Wi-Fi low-latency lock + partial wake lock while running
- Resolves DNS once (pure TCP connect time), re-resolves after 3 losses or network change
- Fixed-delay scheduling (no overlapping pings), timeouts now counted
- Stats: Last/Min/Avg/Max, Loss% + Jitter (last 20), quality verdict (Excellent/Good/Fair/Poor)
- Detects Wi-Fi <-> Mobile Data switches and warns when quality turns Poor
- More targets (YouTube, Facebook) + Custom host[:port]; settings are remembered
- No AppCompat dependency, no debuggable flag in release builds

Tip: Settings > Battery > J Pinger Pro > "Unrestricted" so Android never kills the service.
