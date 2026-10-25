import 'package:flutter/material.dart';

import 'plant_painter.dart';

/// Animated dashboard plant: tweens from the previous severity to the new one
/// whenever the count changes, so the plant visibly wilts as scrolls
/// accumulate — never resetting to healthy in between.
class PlantAvatar extends StatefulWidget {
  const PlantAvatar({
    super.key,
    required this.count,
    required this.limit,
    this.size = 150,
  });

  final int count;
  final int limit;
  final double size;

  @override
  State<PlantAvatar> createState() => _PlantAvatarState();
}

class _PlantAvatarState extends State<PlantAvatar>
    with SingleTickerProviderStateMixin {
  late final AnimationController controller;
  late double _from;
  late double _to;

  @override
  void initState() {
    super.initState();
    _from = _to = plantSeverity(widget.count, widget.limit);
    controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 900),
      value: 1,
    );
  }

  @override
  void didUpdateWidget(PlantAvatar old) {
    super.didUpdateWidget(old);
    if (widget.count != old.count || widget.limit != old.limit) {
      final target = plantSeverity(widget.count, widget.limit);
      if (target != _to) {
        _from = _to;
        _to = target;
        controller.forward(from: 0);
      }
    }
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controller,
      builder: (context, _) {
        final severity =
            _from + (_to - _from) * Curves.easeOutCubic.transform(controller.value);
        return SizedBox(
          width: widget.size,
          height: widget.size * 140 / 120,
          child: CustomPaint(
            painter: PlantPainter(severity: severity),
            size: Size(widget.size, widget.size * 140 / 120),
          ),
        );
      },
    );
  }
}
