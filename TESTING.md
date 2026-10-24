# Prototype testing procedure

## Build and install

This is a native Android Views prototype using Kotlin 2.2.21, Android Gradle Plugin 8.13.2, compileSdk 36, targetSdk 35, and minimum SDK 29. Install it on a real Android 10+ device. Enable the accessibility service explicitly and grant overlay permission only if the floating counter is being tested. Stock Android keeps an enabled accessibility service alive on its own; if your device ships an aggressive battery manager (Xiaomi/HyperOS, Samsung, etc.) that stops the service, grant the in-app battery-optimization exemption.

Before each run, record the app versions, Android version, enabled packages, and whether diagnostic mode is enabled. Diagnostic output contains only event metadata and structural identifiers; it intentionally excludes text, usernames, captions, URLs, and screenshots.

## Repeatable experiment

1. Reset today's counters.
2. Enable diagnostic mode in developer settings or by setting the prototype's `diagnostics` preference.
3. For each scenario below, record deliberate gestures, reported `TYPE_VIEW_SCROLLED` events, counted gestures, false positives, and missed gestures.
4. Export diagnostics after each scenario and label the export with the app version.
5. Repeat after each Instagram/YouTube update. Do not treat a view ID as stable without observing it again.

## Scenarios

1. Instagram Reels: ten deliberate vertical swipes.
2. Instagram DMs: scroll.
3. Instagram comments: scroll.
4. Instagram regular home feed: scroll.
5. Instagram search results: scroll.
6. YouTube Shorts: ten deliberate vertical swipes.
7. Ordinary YouTube search results: scroll.
8. YouTube descriptions and comments: scroll.
9. Tap buttons without scrolling.
10. Horizontal gestures.
11. Rapid swipes and slow swipes.
12. Switch between supported and unrelated apps.
13. Leave the feed and return.
14. Disable and re-enable the accessibility service.
15. Revoke overlay permission while running.
16. Restart the phone and verify daily and per-app persistence.
17. Repeat with missing accessibility metadata and after app UI changes.

## Interpretation

The counter is intentionally conservative. `FEED_UNKNOWN` is not a successful feed detection, and accessibility events are not guaranteed to map one-to-one to physical swipes. Report precision and recall separately; do not present the prototype total as an exact gesture count unless the observed event stream supports that conclusion.
