# Privacy Policy

Scroll Aware is designed so that trusting it does not need to be an act of faith: the app holds **no internet permission**, which means the operating system itself makes it impossible for Scroll Aware to transmit any data anywhere. This is verifiable from the source code and the built APK's manifest.

Last updated: 2026-10-25

## What Scroll Aware observes

While the accessibility service is enabled, it receives accessibility events from other apps. Scroll Aware processes **only structural, scroll-related metadata**:

- the event type (scroll, window content changed, window state changed),
- the source view's class name (e.g. "ViewPager", "RecyclerView"),
- scroll direction and distance,
- scroll position indices,
- the originating app's package name (compared against a fixed allow-list: Instagram, YouTube).

It explicitly does **not** read, store, or transmit: screen text, usernames, captions, comments, direct messages, URLs, images, screenshots, video, audio, contacts, or any personal identifier. There is no screen capture of any kind.

## What Scroll Aware stores

Everything is stored in the app's private local storage on your device:

- today's scroll counts (total and per app),
- per-day history of past counts (date + counts),
- your settings (daily limit, which apps to track, overlay and notification preferences).

## What Scroll Aware sends

Nothing. There is no account, no registration, no cloud synchronization, no backend, no analytics, no crash reporting, no advertising, and no third-party SDK that could collect data. The app cannot access the network.

## Data retention and deletion

- Daily counters reset automatically at local midnight.
- "Reset today's counters" (Settings) zeroes the current day.
- "Delete all local data" (Settings) erases all counters, history, and settings from the device.
- Uninstalling the app removes everything the app has ever stored.

## Permissions

The permissions Scroll Aware requests, and why, are documented in [PERMISSIONS.md](PERMISSIONS.md). None of them grant network access or access to message content.

## Contact

This is an open-source project. Questions about privacy can be raised in the repository's issue tracker.
