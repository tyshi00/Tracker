# Tracker

A tiny health tracker for the Light Phone III; just a simple way to keep tabs on your water intake, sleep, and steps without any distractions.

## What it does

- **Water** — log how much you're drinking today, in whatever unit you prefer
- **Sleep** — jot down how many hours of sleep you got each night
- **Steps** — track your daily step count
- **Settings** — flip to a light theme if you'd rather not stare at black-on-white all day (or vice versa)

## Using it

Install the APK on your Light Phone III and you're done — no accounts, no setup, no companion app required. It just runs.

## Built with

This is a Light SDK tool — built in Kotlin with Jetpack Compose, using Light's own UI components so it looks and feels consistent with the rest of LightOS.

## Building from source

If you want to build the APK yourself instead of using a prebuilt release, you'll need Android Studio and a GitHub personal access token with `read:packages` scope (Light's SDK packages are private).

```bash
echo "sdk.dir=/path/to/your/Android/sdk" > local.properties
echo "gpr.user=YOUR_GITHUB_USERNAME" >> local.properties
echo "gpr.key=YOUR_GITHUB_TOKEN" >> local.properties

./gradlew :tool:assembleDebug
```

The APK will show up at `tool/build/outputs/apk/debug/tool-debug.apk`.
