import 'package:flutter/material.dart';

import '../app.dart';
import '../bridge.dart';
import 'limit_sheet.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({
    super.key,
    required this.bridge,
    required this.appState,
    required this.onRefresh,
  });

  final Bridge bridge;
  final AppState appState;
  final Future<void> Function() onRefresh;

  Future<void> _confirm(
    BuildContext context, {
    required String title,
    required String body,
    required String actionLabel,
    required Future<void> Function() onConfirm,
  }) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(body),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(actionLabel),
          ),
        ],
      ),
    );
    if (confirmed == true) await onConfirm();
  }

  Future<void> _shareDiagnostics(BuildContext context) async {
    final exported = await bridge.exportDiagnostics();
    if (!context.mounted) return;
    await showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Diagnostic metadata'),
        content: SizedBox(
          width: 400,
          child: SingleChildScrollView(
            child: Text(
              exported.isEmpty
                  ? 'Nothing recorded yet. Enable diagnostic mode first, then '
                      'scroll in a supported app.'
                  : exported,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: Navigator.of(context).pop,
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final status = appState.status;

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
        children: [
          Text(
            'Settings',
            style: theme.textTheme.headlineSmall
                ?.copyWith(fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 16),
          _Section(title: 'Daily limit', children: [
            ListTile(
              leading: const Icon(Icons.flag_outlined),
              title: const Text('Daily scroll limit'),
              subtitle: Text(
                status.limit > 0
                    ? '${status.limit} scrolls per day'
                    : 'No limit set',
              ),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => showLimitSheet(context, bridge, status.limit),
            ),
            SwitchListTile(
              secondary: const Icon(Icons.notifications_outlined),
              title: const Text('Limit reminders'),
              subtitle: const Text('One notification per day when reached'),
              value: status.notificationsPreference,
              onChanged: (value) async {
                if (value) await bridge.requestNotificationPermission();
                await bridge.setNotificationsEnabled(value);
                await onRefresh();
              },
            ),
          ]),
          _Section(title: 'Counter', children: [
            SwitchListTile(
              secondary: const Icon(Icons.picture_in_picture_alt_outlined),
              title: const Text('Floating counter'),
              subtitle: Text(status.overlayEnabled && !status.overlayPermission
                  ? 'Needs the "display over other apps" permission'
                  : 'Show the count while in a feed'),
              value: status.overlayEnabled,
              onChanged: (value) async {
                await bridge.setOverlayEnabled(value);
                if (value) await bridge.openOverlaySettings();
                await onRefresh();
              },
            ),
          ]),
          _Section(title: 'Privacy & data', children: [
            const Padding(
              padding: EdgeInsets.fromLTRB(16, 12, 16, 4),
              child: Text(
                'Scroll Aware works entirely offline — no account, no cloud, '
                'no analytics. The app holds no internet permission, so the '
                'system blocks it from sending anything anywhere. All data '
                'lives in this app\'s local storage on this device.',
              ),
            ),
            ListTile(
              leading: const Icon(Icons.restart_alt_outlined),
              title: const Text('Reset today\'s counters'),
              subtitle: const Text('History from previous days is kept'),
              onTap: () => _confirm(
                context,
                title: 'Reset today\'s counters?',
                body:
                    'Today\'s counts go back to zero. Previous days are not '
                    'affected.',
                actionLabel: 'Reset',
                onConfirm: () async {
                  await bridge.resetCounters();
                  await onRefresh();
                },
              ),
            ),
            ListTile(
              leading: const Icon(Icons.delete_outline),
              title: const Text('Delete all local data'),
              subtitle: const Text('Counters, history, and settings'),
              onTap: () => _confirm(
                context,
                title: 'Delete everything?',
                body: 'All counters, history, and settings will be deleted '
                    'from this device. This cannot be undone.',
                actionLabel: 'Delete',
                onConfirm: () async {
                  await bridge.deleteAllData();
                  await onRefresh();
                },
              ),
            ),
          ]),
          _Section(title: 'Diagnostics (developer)', children: [
            SwitchListTile(
              secondary: const Icon(Icons.bug_report_outlined),
              title: const Text('Diagnostic recording'),
              subtitle: const Text(
                  'Logs scroll event metadata to help debug detection'),
              value: status.diagnosticsEnabled,
              onChanged: (value) async {
                await bridge.setDiagnosticsEnabled(value);
                await onRefresh();
              },
            ),
            ListTile(
              leading: const Icon(Icons.terminal_outlined),
              title: const Text('View diagnostic log'),
              onTap: () => _shareDiagnostics(context),
            ),
          ]),
          _Section(title: 'About', children: [
            ListTile(
              leading: const Icon(Icons.info_outline),
              title: const Text('Version'),
              trailing: Text(status.appVersion.isEmpty ? '0.2.0' : status.appVersion),
            ),
            const ListTile(
              leading: Icon(Icons.code_outlined),
              title: Text('Open source'),
              subtitle: Text('MIT License · github.com/pranavsw/less-brainrot'),
            ),
          ]),
          const SizedBox(height: 8),
          Center(
            child: Text(
              'Scroll Aware helps you notice your scrolling.\nIt never blocks '
              'anything — awareness is the whole idea.',
              textAlign: TextAlign.center,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({required this.title, required this.children});

  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 16, bottom: 4),
          child: Text(
            title.toUpperCase(),
            style: theme.textTheme.labelSmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
              letterSpacing: 1.2,
            ),
          ),
        ),
        Card(
          elevation: 0,
          color: theme.colorScheme.surfaceContainerLow,
          clipBehavior: Clip.antiAlias,
          margin: const EdgeInsets.only(bottom: 16),
          child: Column(children: children),
        ),
      ],
    );
  }
}
