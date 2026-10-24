# Scroll Aware

A privacy-first, fully offline digital-wellbeing app that counts how many times you scroll through short-form video feeds (Instagram Reels, YouTube Shorts), shows your daily activity, and lets you set a personal daily limit.

Scroll Aware never blocks anything and never sends anything anywhere. Awareness is the whole idea.

## How it works

An Android accessibility service observes **structural scroll events only** — scroll direction, container class, and scroll position metadata. A classifier confirms you are inside a short-form feed (not DMs, comments, or search) and a gesture counter debounces raw scroll events into counted swipes. The counting core is deliberately conservative: it prefers missing a swipe over counting a false one.

- **Today screen** — live count, limit progress, per-app breakdown, permission status.
- **Apps screen** — enable/disable tracking per supported app.
- **History screen** — 7/30-day charts, averages, per-app totals.
- **Settings** — daily limit, reminders, floating counter, privacy controls, data deletion.
- **Floating counter** — a small overlay showing your count while you're in a feed.

## Privacy in one paragraph

Scroll Aware has **no internet permission** — the app manifest removes it explicitly, so the operating system itself blocks the app from any network access. There is no account, no cloud, no analytics, no ads. All counters and history live in local storage on your device, and you can reset or delete everything in Settings. The accessibility service never reads text, usernames, captions, images, or video content — only scroll metadata. See [PRIVACY.md](PRIVACY.md) and [PERMISSIONS.md](PERMISSIONS.md).

## Project structure

This is a hybrid app:

| Path | What it is |
|---|---|
| `app/` | Native Android core (Kotlin): accessibility service, feed detection, gesture counting, storage, notifications, floating overlay, platform bridge |
| `flutter_module/` | Flutter add-to-app module: the entire UI (Material 3) |

The two halves talk over platform channels (`scrollaware/bridge`, `scrollaware/counter`). The native side owns all storage and Android integrations; Flutter renders the interface.

## Building

Requirements:

- JDK 17
- Android SDK (compileSdk 36)
- Flutter SDK (any recent stable; the module currently builds with the toolchain on the dev machine)

```bash
# 1. Build the Flutter UI into AARs (needed once, and after any UI change)
cd flutter_module
flutter pub get
flutter build aar --debug
flutter build aar --release

# 2. Build and install the app (from the repo root)
../gradlew :app:assembleDebug          # or use Android Studio
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Notes:

- The Gradle wrapper pins Gradle 9.5. On AGP 8.13, do **not** upgrade beyond Gradle 9.5 (newer Gradle versions remove an internal API AGP 8.x relies on).
- The debug APK is large (Flutter debug embedding, all ABIs). The release APK is R8-minified. For Play distribution, build an App Bundle (`:app:bundleRelease`) and configure a release keystore — local builds currently sign with the debug key.
- Unit tests: `gradlew :app:testDebugUnitTest`.

## Setup on a phone

1. Install the app and open it.
2. Complete onboarding — it walks you through the required permissions.
3. Enable **Scroll Aware** in Android's accessibility settings (required — this is what detects scrolling).
4. Optionally grant "display over other apps" for the floating counter, allow notifications for limit reminders, and (on aggressive ROMs like One UI) request the battery-optimization exemption.
5. Scroll Reels or Shorts; the counter follows.

## Supported apps & known limitations

- **Instagram Reels** and **YouTube Shorts** are supported. Detection is metadata-based and experimental: apps update their UIs often, so counts are approximate and detection may need adjustment after an app update.
- Counting is intentionally conservative (debounce + warm-up windows). Rapid successive swipes within ~1 s may count as one.
- YouTube Shorts on some devices reports only generic content-change events; those are counted approximately (see `FeedDetector`).
- **iOS**: the detection mechanism (Android accessibility events) has no iOS equivalent; a future iOS version would require a fundamentally different approach.
- Accessibility services can be disabled by aggressive battery managers (Xiaomi, Samsung, Huawei). The app offers a battery-optimization exemption request for those devices; stock Android keeps enabled accessibility services alive on its own.

## Testing the counting accuracy

See [TESTING.md](TESTING.md) for the repeatable experiment protocol (scenarios, diagnostics export, precision/recall interpretation).

## Contributing & license

MIT License — see [LICENSE](LICENSE). The app is open source; contributions welcome.
