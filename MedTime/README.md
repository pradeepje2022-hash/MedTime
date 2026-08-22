# MedTime – Medicine Reminder

A native Android app (Kotlin + Jetpack Compose + Room + AlarmManager) that reminds
users to take their medicines on time, even when the app is closed or the phone is locked.

## What's included

- Add/edit medicines: name, dosage, type, multiple daily reminder times, repeat days,
  optional start/end date, notes
- Exact alarms via `AlarmManager` that survive app-close, screen-lock, and reboot
  (`BootReceiver` reschedules everything after restart)
- Full-screen lock-screen alarm with sound + vibration + Taken/Snooze/Skip
- Notification with Taken / Snooze (5,10,15,30 min) / Skip action buttons
- Today's dashboard, medicine history (last 30 days), edit/delete/pause
- Settings: light/dark/system theme, default snooze duration, 12/24-hour format
- All data stored locally on-device with Room (works fully offline)
- Safety disclaimer shown in Settings

## Opening the project

1. Install **Android Studio** (Koala or newer).
2. Unzip this project and open the `MedTime` folder in Android Studio
   (`File > Open`, select the folder that contains `settings.gradle.kts`).
3. Android Studio will detect there's no Gradle wrapper jar yet and offer to
   generate one automatically — accept that prompt (needs internet once).
   If it doesn't prompt, run `gradle wrapper` from a terminal inside the
   project folder (requires Gradle installed), or just let Android Studio's
   "Sync Project with Gradle Files" handle it.
4. Let Gradle sync, then press **Run ▶** on a device or emulator (minSdk 26 / Android 8+).

## Building the APK automatically via GitHub Actions

Since you want to build the APK from GitHub rather than locally:

1. Create a new **empty** repository on GitHub (don't initialize with a README).
2. Push this project to it:
   ```bash
   cd MedTime
   git init
   git add .
   git commit -m "Initial commit: MedTime medicine reminder app"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
3. Go to the **Actions** tab on your GitHub repo — the "Build APK" workflow
   (`.github/workflows/build-apk.yml`) will run automatically on push.
4. When it finishes (green check), open the workflow run and download the
   **MedTime-debug-apk** artifact from the bottom of the page — that's your
   installable APK.

This workflow builds a **debug** APK (fine for personal installs/testing).
If you later want a signed **release** APK for wider distribution, that needs
a signing keystore — ask and I can add that step too.

## Notes on reliability

- Android may ask the user to allow "exact alarms" (Android 12+) and to
  disable battery optimization for the app — the app shows a banner
  prompting the exact-alarm permission on first launch.
- For most phones (especially Xiaomi/Redmi/Oppo/Vivo with aggressive battery
  managers), also suggest the user manually **lock the app in recents** and
  allow **autostart**, since these OEMs can kill background alarms outside
  of stock Android's guarantees.
