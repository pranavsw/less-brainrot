import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';

import '../app.dart';
import '../apps_meta.dart';
import '../bridge.dart';

class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key, required this.bridge, required this.appState});

  final Bridge bridge;
  final AppState appState;

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  int range = 7;
  List<DayCount>? days;

  @override
  void initState() {
    super.initState();
    load();
  }

  @override
  void didUpdateWidget(HistoryScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    // Today's count changing on the dashboard should also refresh "today" here.
    if (oldWidget.appState.today.total != widget.appState.today.total) load();
  }

  Future<void> load() async {
    final history = await widget.bridge.getHistory(range);
    if (mounted) setState(() => days = history);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final history = days;

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
        children: [
          Text(
            'History',
            style: theme.textTheme.headlineSmall
                ?.copyWith(fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 12),
          SegmentedButton<int>(
            segments: const [
              ButtonSegment(value: 7, label: Text('7 days')),
              ButtonSegment(value: 30, label: Text('30 days')),
            ],
            selected: {range},
            onSelectionChanged: (selection) {
              setState(() => range = selection.first);
              load();
            },
          ),
          const SizedBox(height: 16),
          if (history == null)
            const Center(child: Padding(
              padding: EdgeInsets.all(40),
              child: CircularProgressIndicator(),
            ))
          else ...[
            _SummaryRow(days: history),
            const SizedBox(height: 12),
            Card(
              elevation: 0,
              color: theme.colorScheme.surfaceContainerLow,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(12, 20, 16, 8),
                child: SizedBox(
                  height: 180,
                  child: _HistoryChart(days: history),
                ),
              ),
            ),
            const SizedBox(height: 12),
            _PerAppBreakdown(days: history),
          ],
        ],
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({required this.days});

  final List<DayCount> days;

  @override
  Widget build(BuildContext context) {
    final total = days.fold<int>(0, (sum, day) => sum + day.total);
    final average = days.isEmpty ? 0.0 : total / days.length;
    final busiest = days.fold<DayCount>(
      days.isNotEmpty ? days.first : DayCount(date: '', total: 0, perApp: const {}),
      (max, day) => day.total > max.total ? day : max,
    );

    return Row(
      children: [
        Expanded(
          child: _SummaryTile(
            label: 'Total',
            value: '$total',
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _SummaryTile(label: 'Daily average', value: average.toStringAsFixed(1)),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _SummaryTile(label: 'Busiest day', value: '${busiest.total}'),
        ),
      ],
    );
  }
}

class _SummaryTile extends StatelessWidget {
  const _SummaryTile({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      elevation: 0,
      color: theme.colorScheme.surfaceContainerLow,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 14),
        child: Column(
          children: [
            Text(
              value,
              style: theme.textTheme.titleLarge
                  ?.copyWith(fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 2),
            Text(
              label,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _HistoryChart extends StatelessWidget {
  const _HistoryChart({required this.days});

  final List<DayCount> days;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final maxY = days.fold<int>(10, (max, day) => day.total > max ? day.total : max);
    final step = rangeLabelStep(days.length);

    return BarChart(
      BarChartData(
        alignment: BarChartAlignment.spaceBetween,
        maxY: (maxY * 1.2).ceilToDouble(),
        barTouchData: BarTouchData(
          touchTooltipData: BarTouchTooltipData(
            getTooltipItem: (group, groupIndex, rod, rodIndex) =>
                BarTooltipItem(
                  '${rod.toY.round()}',
                  TextStyle(
                    color: theme.colorScheme.onInverseSurface,
                    fontWeight: FontWeight.w600,
                  ),
                ),
          ),
        ),
        titlesData: FlTitlesData(
          leftTitles: const AxisTitles(),
          rightTitles: const AxisTitles(),
          topTitles: const AxisTitles(),
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              reservedSize: 28,
              getTitlesWidget: (value, meta) {
                final index = value.toInt();
                if (index < 0 || index >= days.length) {
                  return const SizedBox.shrink();
                }
                if (index % step != 0 && index != days.length - 1) {
                  return const SizedBox.shrink();
                }
                final date = DateTime.tryParse(days[index].date);
                if (date == null) return const SizedBox.shrink();
                final label = days.length <= 7
                    ? DateFormatE.weekdayShort(date)
                    : '${date.day}/${date.month}';
                return Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text(
                    label,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                      fontSize: 10,
                    ),
                  ),
                );
              },
            ),
          ),
        ),
        gridData: const FlGridData(show: false),
        borderData: FlBorderData(show: false),
        barGroups: [
          for (var i = 0; i < days.length; i++)
            BarChartGroupData(
              x: i,
              barRods: [
                BarChartRodData(
                  toY: days[i].total.toDouble(),
                  width: days.length <= 7 ? 22 : 8,
                  borderRadius: BorderRadius.circular(3),
                  color: theme.colorScheme.primary.withValues(alpha: 0.85),
                ),
              ],
            ),
        ],
      ),
    );
  }

  static int rangeLabelStep(int count) => count <= 7 ? 1 : 5;
}

class DateFormatE {
  static const weekdayNames = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  static String weekdayShort(DateTime date) =>
      weekdayNames[date.weekday - 1];
}

class _PerAppBreakdown extends StatelessWidget {
  const _PerAppBreakdown({required this.days});

  final List<DayCount> days;

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
              () {
                final count = days.fold<int>(
                  0,
                  (sum, day) => sum + (day.perApp[app.packageName] ?? 0),
                );
                return ListTile(
                  leading: appAvatar(app, radius: 16),
                  title: Text(app.name),
                  trailing: Text(
                    '$count',
                    style: theme.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.w600),
                  ),
                );
              }(),
          ],
        ),
      ),
    );
  }
}
