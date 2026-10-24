import 'package:flutter/material.dart';

import '../app.dart';
import '../apps_meta.dart';
import '../bridge.dart';

class AppsScreen extends StatelessWidget {
  const AppsScreen({
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

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
        children: [
          Text(
            'Tracked apps',
            style: theme.textTheme.headlineSmall
                ?.copyWith(fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 4),
          Text(
            'Counting applies only to the short-form feed inside each app — '
            'not DMs, comments, search, or the regular feed.',
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          for (final app in TrackedAppInfo.all) ...[
            _AppCard(
              app: app,
              enabled: status.trackApps[app.packageName] ?? false,
              serviceReady: status.serviceEnabled,
              onToggle: (value) async {
                await bridge.setTrackEnabled(app.packageName, value);
                await onRefresh();
              },
            ),
            const SizedBox(height: 12),
          ],
          const SizedBox(height: 4),
          Card(
            elevation: 0,
            color: theme.colorScheme.surfaceContainerLow,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  Icon(Icons.science_outlined,
                      color: theme.colorScheme.onSurfaceVariant),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      'Detection is metadata-based and experimental. Apps update '
                      'their UIs often, so counts are approximate and an app may '
                      'need retesting after an update.',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _AppCard extends StatelessWidget {
  const _AppCard({
    required this.app,
    required this.enabled,
    required this.serviceReady,
    required this.onToggle,
  });

  final TrackedAppInfo app;
  final bool enabled;
  final bool serviceReady;
  final ValueChanged<bool> onToggle;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final stateText = !serviceReady
        ? 'Needs the accessibility service enabled'
        : enabled
            ? 'Ready to track ${app.section}'
            : 'Tracking off';

    return Card(
      elevation: 0,
      color: theme.colorScheme.surfaceContainerLow,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(8, 4, 8, 4),
        child: ListTile(
          leading: appAvatar(app, radius: 24),
          title: Text('${app.name} · ${app.section}'),
          subtitle: Padding(
            padding: const EdgeInsets.only(top: 2),
            child: Row(
              children: [
                Icon(
                  serviceReady && enabled
                      ? Icons.check_circle_outline
                      : Icons.info_outline,
                  size: 14,
                  color: serviceReady && enabled
                      ? theme.colorScheme.primary
                      : theme.colorScheme.onSurfaceVariant,
                ),
                const SizedBox(width: 4),
                Flexible(
                  child: Text(
                    stateText,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ),
              ],
            ),
          ),
          trailing: Switch(value: enabled, onChanged: onToggle),
        ),
      ),
    );
  }
}
