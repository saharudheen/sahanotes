# Saha Notes

A simple notepad app for Android, in the style of Easy Notes.

## Features
- Notes with title and body, auto-saved (no Save button)
- Grid or list view, 10 note colours, pin to top, search
- Long-press a note to pin, share or delete (with Undo)
- **Reminders**: one-time, daily, weekly or monthly, with notification and "Snooze 10 min"; survive phone restarts
- **Voice typing** in English or Tamil (tap the language chip to switch); quick voice-note button on the home screen
- "Share to Saha Notes" from any app
- Light and dark mode

## Build the APK (no Android Studio needed)
Every push to GitHub builds the app automatically:
1. Open the repository's **Actions** tab.
2. Open the latest **Build APK** run.
3. Download **SahaNotes-debug-apk** under *Artifacts*, unzip it, and install `app-debug.apk` on your phone (allow "Install unknown apps" when asked).

## Notes
- Voice typing uses Google's speech recognizer, so the phone needs the Google app. Tamil works best with internet on.
- On Android 12+, allow **Alarms & reminders** for the app so reminders ring exactly on time; the app prompts you the first time.

Tech: Kotlin, Jetpack Compose (Material 3), Room, AlarmManager. minSdk 26 (Android 8.0).
