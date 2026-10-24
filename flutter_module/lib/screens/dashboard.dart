import 'package:flutter/material.dart';

import '../app.dart';
import '../apps_meta.dart';
import '../bridge.dart';
import 'limit_sheet.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({
    super.key,
    required this.bridge,
    required this.appState,
    required this.onRefresh,
  });

  final Bridge bridge;
  final AppState appState;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final status = appState.status;
    final today = appState.today;

    return SafeArea(
      child: RefreshIndicator(
        onRefresh: onRefresh,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    'Today',
                    style: theme.textTheme.headlineSmall
                        ?.copyWith(fontWeight: FontWeight.w600),
                  ),
                ),
                IconButton(
                  tooltip: 'Refresh',
                  onPressed: onRefresh,
                  icon: const Icon(Icons.refresh),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (!status.serviceEnabled) ...[
              _AttentionCard(
                icon: Icons.accessibility_new_outlined,
                title: 'Tracking is off',
                body: 'The accessibility service is not enabled, so nothing is '
                    'being counted right now.',
                actionLabel: 'Enable',
                onAction: bridge.openAccessibilitySettings,
              ),
              const SizedBox(height: 12),
            ],
            _TodayCard(
              today: today.total,
              limit: status.limit,
              onEditLimit: () => showLimitSheet(context, bridge, status.limit),
            ),
            const SizedBox(height: 12),
            _AppsCard(today: today, status: status),
            const SizedBox(height: 12),
            if (status.overlayEnabled && !status.overlayPermission)
              _AttentionCard(
                icon: Icons.picture_in_picture_alt_outlined,
                title: 'Floating counter can\'t show',
                body: 'The "display over other apps" permission is missing.',
                actionLabel: 'Grant',
                onAction: bridge.openOverlaySettings,
              ),
            if (status.limit > 0 &&
                status.notificationsPreference &&
                !status.notificationsEnabled)
              _AttentionCard(
                icon: Icons.notifications_outlined,
                title: 'Notifications are off',
                body: 'You won\'t be notified when you reach your daily limit.',
                actionLabel: 'Allow',
                onAction: bridge.requestNotificationPermission,
              ),
          ],
        ),
      ),
    );
  }
}

class _TodayCard extends StatelessWidget {
  const _TodayCard({
    required this.today,
    required this.limit,
    required this.onEditLimit,
  });

  final int today;
  final int limit;
  final VoidCallback onEditLimit;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final progress = limit > 0 ? (today / limit).clamp(0.0, 1.0) : null;
    final remaining = limit > 0 ? (limit - today).clamp(0, limit) : null;
    final overLimit = limit > 0 && today >= limit;

    return Card(
      elevation: 0,
      color: theme.colorScheme.surfaceContainerLow,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Scrolls today',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 4),
            Row(
              crossAxisAlignment: CrossAxisAlignment.baseline,
              textBaseline: TextBaseline.alphabetic,
              children: [
                Text(
                  '$today',
                  style: theme.textTheme.displayMedium
                      ?.copyWith(fontWeight: FontWeight.w600),
                ),
                if (limit > 0) ...[
                  const SizedBox(width: 8),
                  Text(
                    '/ $limit',
                    style: theme.textTheme.headlineSmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ],
            ),
            if (progress != null) ...[
              const SizedBox(height: 16),
              ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: LinearProgressIndicator(
                  value: progress,
                  minHeight: 8,
                  color: overLimit
                      ? theme.colorScheme.error
                      : theme.colorScheme.primary,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                overLimit
                    ? 'You\'ve reached your limit for today'
                    : '$remaining scrolls left today',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: overLimit
                      ? theme.colorScheme.error
                      : theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ] else
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: TextButton.icon(
                  onPressed: onEditLimit,
                  icon: const Icon(Icons.flag_outlined, size: 18),
                  label: const Text('Set a daily limit'),
                ),
              ),
            if (limit > 0)
              Align(
                alignment: Alignment.centerRight,
                child: IconButton(
                  tooltip: 'Change limit',
                  onPressed: onEditLimit,
                  icon: const Icon(Icons.edit_outlined, size: 20),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _AppsCard extends StatelessWidget {
  const _AppsCard({required this.today, required this.status});

  final TodayCounts today;
  final Status status;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      elevation: 0,
      color: theme.colorScheme.surfaceContainerLow,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        child: Column(
          children: [
            for (final app in TrackedAppInfo.all)
              if (status.trackApps[app.packageName] ?? false)
                ListTile(
                  leading: appAvatar(app),
                  title: Text('${app.name} ${app.section}'),
                  trailing: Text(
                    '${today.perApp[app.packageName] ?? 0}',
                    style: theme.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.w600),
                  ),
                ),
          ],
        ),
      ),
    );
  }
}

class _AttentionCard extends StatelessWidget {
  const _AttentionCard({
    required this.icon,
    required this.title,
    required this.body,
    required this.actionLabel,
    required this.onAction,
  });

  final IconData icon;
  final String title;
  final String body;
  final String actionLabel;
  final Future<void> Function() onAction;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      elevation: 0,
      color: theme.colorScheme.errorContainer.withValues(alpha: 0.5),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(icon, color: theme.colorScheme.error),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title,
                      style: theme.textTheme.titleSmall
                          ?.copyWith(fontWeight: FontWeight.w600)),
                  const SizedBox(height: 2),
                  Text(
                    body,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            FilledButton.tonal(
              onPressed: onAction,
              child: Text(actionLabel),
            ),
          ],
        ),
      ),
    );
  }
}
