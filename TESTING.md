# Testing Scroll Aware

## Build and install

Hybrid app: Kotlin native core + Flutter UI module. See README.md for full build steps — in short, build the Flutter AARs first (`flutter build aar`), then assemble/install with Gradle.

Minimum Android 10 (API 29). Enable the accessibility service explicitly. Overlay permission is only needed for the floating counter; notifications only for limit reminders.

Before each run, record app version, Android version, enabled packages, and whether diagnostic recording is on. Diagnostic output contains only event metadata and structural identifiers; it intentionally excludes text, usernames, captions, URLs, and screenshots.

## Repeatable experiment

1. Reset today's counters (Settings → Privacy & data).
2. Enable diagnostic recording (Settings → Diagnostics).
3. For each scenario below, record deliberate gestures, reported `TYPE_VIEW_SCROLLED` events, counted gestures, false positives, and missed gestures.
4. View the diagnostic log (Settings → View diagnostic log) after each scenario and save it.
5. Repeat after each Instagram/YouTube update. Do not treat a view ID as stable without observing it again.

## Scenarios

1. Instagram Reels: ten deliberate vertical swipes, ~4 s apart (outside the 1 s debounce).
2. Instagram Reels: rapid swipes (~1 per second) — verify debounce behavior is understood.
3. Instagram DMs: scroll.
4. Instagram comments: scroll.
5. Instagram regular home feed: scroll.
6. Instagram search results: scroll.
7. YouTube Shorts: ten deliberate vertical swipes.
8. Ordinary YouTube search results: scroll.
9. YouTube descriptions and comments: scroll.
10. Tap buttons without scrolling.
11. Horizontal gestures.
12. Switch between supported and unrelated apps (overlay should retire ~2.5 s after leaving the feed).
13. Leave the feed and return.
14. Disable and re-enable the accessibility service.
15. Revoke overlay permission while running (must not crash).
16. Restart the phone and verify daily and per-app persistence, plus history day rollover (change device date by one day to test archiving).
17. Reach the daily limit (set limit to 2) and verify exactly one notification arrives.
18. Delete all data and verify counters, history, and settings are wiped.

## Interpretation

The counter is intentionally conservative. `FEED_UNKNOWN` is not a successful feed detection, and accessibility events are not guaranteed to map one-to-one to physical swipes. Report precision and recall separately; do not present the total as an exact gesture count unless the observed event stream supports that conclusion.
