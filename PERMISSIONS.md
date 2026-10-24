# Permissions

Scroll Aware requests as few permissions as possible. Notably, it does **not** hold the `INTERNET` permission — it is explicitly removed from the merged manifest (including the copy Flutter's embedding would add), so the app cannot access the network at all.

## Accessibility service (required for the core function)

**What**: Android Settings → Accessibility → Scroll Aware.

**Why**: The app's entire purpose is counting scroll gestures in other apps' short-form feeds. Android exposes this only through accessibility events. The service observes structural scroll metadata (event type, container class, scroll deltas, position indices) from a fixed allow-list of apps (Instagram, YouTube) and nothing else. It does not read text, content, or input, and it does not perform gestures or respond to events in any way that affects the apps you use.

**Removal**: Disable it anytime in Android accessibility settings; counting simply stops.

## Display over other apps / `SYSTEM_ALERT_WINDOW` (optional)

**What**: Settings link provided in-app.

**Why**: Shows the small floating scroll counter while you are inside a supported feed. The overlay is not touchable, not focusable, and disappears ~2.5 s after you leave the feed. Entirely optional — the app works fully without it.

## Notifications / `POST_NOTIFICATIONS` (optional)

**What**: Runtime notification permission (Android 13+), requested only when you enable limit reminders.

**Why**: Sends exactly one local notification per day, when you reach the daily limit you set yourself. No other notifications exist.

## Battery-optimization exemption / `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (optional)

**What**: In-app button that opens the system dialog.

**Why**: On some devices (notably Samsung/Xiaomi), aggressive battery managers kill accessibility services even while the user wants them enabled. The exemption is offered as a fix for those devices; on stock Android it is unnecessary. Requesting it does not change any other behavior.

## What is deliberately absent

- `INTERNET` — removed from every build variant; the OS blocks all network access.
- Storage, contacts, location, camera, microphone, telephony — never requested.
- Device identifiers — never read.
