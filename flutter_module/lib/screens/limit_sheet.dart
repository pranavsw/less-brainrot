import 'package:flutter/material.dart';

import '../bridge.dart';

/// Bottom sheet for choosing the daily scroll limit. A limit of 0 means none.
Future<void> showLimitSheet(BuildContext context, Bridge bridge, int current) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    builder: (context) => _LimitSheet(bridge: bridge, initial: current),
  );
}

class _LimitSheet extends StatefulWidget {
  const _LimitSheet({required this.bridge, required this.initial});

  final Bridge bridge;
  final int initial;

  @override
  State<_LimitSheet> createState() => _LimitSheetState();
}

class _LimitSheetState extends State<_LimitSheet> {
  late final controller = TextEditingController(
    text: widget.initial > 0 ? widget.initial.toString() : '',
  );

  static const presets = [50, 100, 150, 200, 300];

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  Future<void> save(int value) async {
    await widget.bridge.setLimit(value);
    if (mounted) Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final parsed = int.tryParse(controller.text) ?? 0;
    return Padding(
      padding: EdgeInsets.only(
        left: 24,
        right: 24,
        top: 24,
        bottom: 24 + MediaQuery.viewInsetsOf(context).bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Daily scroll limit',
              style: theme.textTheme.titleMedium),
          const SizedBox(height: 4),
          Text(
            'You\'ll get one notification when you reach it.',
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 20),
          TextField(
            controller: controller,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              labelText: 'Limit (empty = no limit)',
            ),
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            children: [
              for (final preset in presets)
                FilterChip(
                  label: Text('$preset'),
                  selected: parsed == preset,
                  onSelected: (_) {
                    controller.text = preset.toString();
                    setState(() {});
                  },
                ),
            ],
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              Expanded(
                child: FilledButton(
                  onPressed: () => save(parsed),
                  child: Text(parsed > 0 ? 'Set limit' : 'Remove limit'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
