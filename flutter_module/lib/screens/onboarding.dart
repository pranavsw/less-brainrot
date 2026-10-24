import 'package:flutter/material.dart';

import '../bridge.dart';

class OnboardingFlow extends StatefulWidget {
  const OnboardingFlow({super.key, required this.bridge, required this.onDone});

  final Bridge bridge;
  final Future<void> Function() onDone;

  @override
  State<OnboardingFlow> createState() => _OnboardingFlowState();
}

class _OnboardingFlowState extends State<OnboardingFlow> {
  final controller = PageController();
  int page = 0;

  static const pageCount = 4;

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  Future<void> next() async {
    if (page < pageCount - 1) {
      controller.nextPage(
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOutCubic,
      );
    } else {
      await widget.bridge.completeOnboarding();
      await widget.onDone();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Align(
              alignment: Alignment.centerRight,
              child: TextButton(
                onPressed: () async {
                  await widget.bridge.completeOnboarding();
                  await widget.onDone();
                },
                child: const Text('Skip'),
              ),
            ),
            Expanded(
              child: PageView(
                controller: controller,
                onPageChanged: (index) => setState(() => page = index),
                children: [
                  _WelcomePage(onNext: next),
                  _AccessibilityPage(bridge: widget.bridge, onNext: next),
                  _OptionalPermissionsPage(bridge: widget.bridge, onNext: next),
                  _PrivacyPage(onNext: next),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
              child: Row(
                children: [
                  for (var i = 0; i < pageCount; i++)
                    Expanded(
                      child: Container(
                        margin: const EdgeInsets.symmetric(horizontal: 3),
                        height: 4,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(2),
                          color: i <= page
                              ? Theme.of(context).colorScheme.primary
                              : Theme.of(context)
                                  .colorScheme
                                  .surfaceContainerHighest,
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _WelcomePage extends StatelessWidget {
  const _WelcomePage({required this.onNext});

  final Future<void> Function() onNext;

  @override
  Widget build(BuildContext context) {
    return _Page(
      icon: Icons.swipe_up_outlined,
      title: 'Become aware of\nyour scrolling',
      body: 'Scroll Aware quietly counts how many times you scroll through '
          'short-form video feeds like Instagram Reels and YouTube Shorts.\n\n'
          'It never blocks anything and never judges — it simply shows you the '
          'number, so you can decide what to do with it.',
      actionLabel: 'Get started',
      onNext: onNext,
    );
  }
}

class _AccessibilityPage extends StatelessWidget {
  const _AccessibilityPage({required this.bridge, required this.onNext});

  final Bridge bridge;
  final Future<void> Function() onNext;

  @override
  Widget build(BuildContext context) {
    return _Page(
      icon: Icons.accessibility_new_outlined,
      title: 'Enable the\naccessibility service',
      body: 'Scroll detection works through Android\'s accessibility service. '
          'Scroll Aware uses it to observe only structural scroll events — '
          'scroll direction and container type.\n\n'
          'It never reads text, usernames, captions, or anything you type.',
      actionLabel: 'Open accessibility settings',
      secondaryLabel: 'I\'ve enabled it',
      onNext: onNext,
      onSecondary: onNext,
      onAction: bridge.openAccessibilitySettings,
    );
  }
}

class _OptionalPermissionsPage extends StatelessWidget {
  const _OptionalPermissionsPage({required this.bridge, required this.onNext});

  final Bridge bridge;
  final Future<void> Function() onNext;

  @override
  Widget build(BuildContext context) {
    return _Page(
      icon: Icons.picture_in_picture_alt_outlined,
      title: 'Optional extras',
      body: 'A small floating counter can show your count while you\'re in a '
          'feed — it needs the "display over other apps" permission.\n\n'
          'If you set a daily limit, Scroll Aware can send you a notification '
          'when you reach it.\n\n'
          'Both are optional. You can change them anytime in Settings.',
      actionLabel: 'Continue',
      onAction: () async {
        await bridge.requestNotificationPermission();
        await onNext();
      },
      onNext: onNext,
    );
  }
}

class _PrivacyPage extends StatelessWidget {
  const _PrivacyPage({required this.onNext});

  final Future<void> Function() onNext;

  @override
  Widget build(BuildContext context) {
    return _Page(
      icon: Icons.lock_outline,
      title: 'Your data stays\non your device',
      body: 'Scroll Aware works entirely offline. There is no account, no '
          'cloud sync, no analytics, and no ads.\n\n'
          'All counters and history live in local storage on this phone, and '
          'you can reset or delete them anytime in Settings.\n\n'
          'The app has no internet permission at all — the system itself '
          'blocks it from going online.',
      actionLabel: 'Start using Scroll Aware',
      onNext: onNext,
    );
  }
}

class _Page extends StatelessWidget {
  const _Page({
    required this.icon,
    required this.title,
    required this.body,
    required this.actionLabel,
    required this.onNext,
    this.secondaryLabel,
    this.onSecondary,
    this.onAction,
  });

  final IconData icon;
  final String title;
  final String body;
  final String actionLabel;
  final String? secondaryLabel;
  final Future<void> Function() onNext;
  final Future<void> Function()? onSecondary;
  final Future<void> Function()? onAction;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Icon(icon, size: 56, color: theme.colorScheme.primary),
          const SizedBox(height: 24),
          Text(
            title,
            style: theme.textTheme.headlineSmall?.copyWith(
              fontWeight: FontWeight.w600,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 16),
          Text(
            body,
            style: theme.textTheme.bodyLarge?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
              height: 1.5,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 32),
          FilledButton(
            onPressed: () async {
              if (onAction != null) {
                await onAction!();
              } else {
                await onNext();
              }
            },
            child: Text(actionLabel),
          ),
          if (secondaryLabel != null && onSecondary != null) ...[
            const SizedBox(height: 8),
            TextButton(onPressed: onSecondary, child: Text(secondaryLabel!)),
          ],
        ],
      ),
    );
  }
}
