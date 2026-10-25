import 'dart:math' as math;

import 'package:flutter/material.dart';

/// Converts today's count into the plant's continuous severity (0 = thriving,
/// 1 = dead). Mirrors the native PlantState thresholds: the reference is the
/// daily limit, or 100 scrolls when no limit is set; 200% of the reference
/// reads as fully dead.
double plantSeverity(int count, int limit) {
  final reference = limit > 0 ? limit : 100;
  if (reference <= 0) return 0;
  return (count / (2.0 * reference)).clamp(0.0, 1.0);
}

/// Severity anchor colors, matching the floating-counter vector states.
Color plantAccent(double severity) => _lerpChain(severity, const [
      Color(0xFF6CE284),
      Color(0xFFB9C64C),
      Color(0xFFFFB129),
      Color(0xFFEF5350),
      Color(0xFFE53935),
    ]);

Color plantLeafColor(double severity) => _lerpChain(severity, const [
      Color(0xFF3FA24B),
      Color(0xFF9EB33C),
      Color(0xFFFBC02D),
      Color(0xFF8D6E63),
      Color(0xFF4E342E),
    ]);

Color _lerpChain(double t, List<Color> colors) {
  final clamped = t.clamp(0.0, 1.0);
  final scaled = clamped * (colors.length - 1);
  final index = scaled.floor().clamp(0, colors.length - 2);
  return Color.lerp(colors[index], colors[index + 1], scaled - index)!;
}

/// The dashboard plant. One shared coordinate system with the overlay's vector
/// drawables (120×140): jar with a face, soil, stem and drooping leaf pairs,
/// floating reel icons, sparkles when thriving, debris and smoke when dying.
/// Everything is parameterized by `severity` so the plant wilts continuously
/// as the count grows — no image swaps.
class PlantPainter extends CustomPainter {
  PlantPainter({required this.severity}) : accent = plantAccent(severity);

  final double severity;
  final Color accent;

  @override
  void paint(Canvas canvas, Size size) {
    final s = size.width / 120;
    final leafColor = plantLeafColor(severity);
    final stemColor = Color.lerp(const Color(0xFF2E7D32), const Color(0xFF3E2723), severity)!;
    final faceColor = const Color(0xFF2B2620);
    final wilt = severity * 62; // degrees of droop at the leaf pairs

    // Glow behind everything, tinted by the accent.
    for (final (radius, alpha) in [(46.0, 0.06), (34.0, 0.09), (23.0, 0.13)]) {
      canvas.drawCircle(
        Offset(60 * s, 72 * s),
        radius * s,
        Paint()..color = accent.withValues(alpha: alpha),
      );
    }

    // Floating reel icons crowd in as severity rises.
    _drawReels(canvas, s);

    if (severity < 0.15) _drawSparkles(canvas, s);

    // Stem bends sideways as it dies.
    final stemPaint = Paint()
      ..color = stemColor
      ..strokeWidth = 3.5 * s
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke;
    final stemTip = Offset((60 + 8 * severity) * s, (78 - 26 * severity) * s);
    final stem = Path()
      ..moveTo(60 * s, 104 * s)
      ..cubicTo(
        60 * s, 94 * s,
        (60 + 4 * severity) * s, (86 - 10 * severity) * s,
        stemTip.dx, stemTip.dy,
      );
    canvas.drawPath(stem, stemPaint);

    // Leaf pairs hang progressively lower; the top one drops off when dead.
    for (var i = 0; i < 2; i++) {
      _drawLeafPair(canvas, (88 - i * 14).toDouble(), wilt, s, leafColor);
    }
    if (severity < 0.7) _drawTopLeaf(canvas, stemTip, wilt, s, leafColor);

    if (severity > 0.6) {
      _drawDebris(canvas, ((severity - 0.6) / 0.4).clamp(0.0, 1.0), s);
    }
    if (severity > 0.78) {
      _drawSmoke(canvas, ((severity - 0.78) / 0.22).clamp(0.0, 1.0), s);
    }

    // Glass jar, soil, and the face — drawn over the plant base so the stem
    // appears rooted inside.
    _drawJar(canvas, s, faceColor);
  }

  void _drawLeafPair(Canvas canvas, double attachY, double wiltDeg, double s, Color color) {
    final radians = wiltDeg * math.pi / 180;
    final paint = Paint()..color = color;
    for (final dir in const [-1.0, 1.0]) {
      canvas.save();
      canvas.translate(60 * s, attachY * s);
      canvas.rotate(dir * -radians); // left leaf turns CCW, right leaf CW: both sag
      final leaf = Path()
        ..moveTo(-dir * 1 * s, 0)
        ..cubicTo(
          -dir * 3 * s, -8 * s,
          -dir * 11 * s, -12 * s,
          -dir * 17 * s, -8 * s,
        )
        ..cubicTo(
          -dir * 13 * s, -2 * s,
          -dir * 5 * s, 2 * s,
          -dir * 1 * s, 0,
        );
      canvas.drawPath(leaf, paint);
      canvas.restore();
    }
  }

  void _drawTopLeaf(Canvas canvas, Offset tip, double wiltDeg, double s, Color color) {
    canvas.save();
    canvas.translate(tip.dx, tip.dy);
    canvas.rotate(wiltDeg * math.pi / 180 * 0.6);
    final leaf = Path()
      ..moveTo(0, 2 * s)
      ..cubicTo(-4 * s, -4 * s, -2 * s, -14 * s, 0, -20 * s)
      ..cubicTo(2 * s, -14 * s, 4 * s, -4 * s, 0, 2 * s);
    canvas.drawPath(leaf, Paint()..color = color);
    canvas.restore();
  }

  void _drawDebris(Canvas canvas, double t, double s) {
    final color = const Color(0xFF4E342E).withValues(alpha: 0.55 + 0.45 * t);
    final paint = Paint()..color = color;
    for (final (dx, dy) in const [(-34.0, 26.0), (26.0, 27.0), (14.0, 23.0)]) {
      final leaf = Path()
        ..moveTo(dx * s, dy * s)
        ..cubicTo(
          (dx + 1) * s, (dy - 4) * s,
          (dx + 7) * s, (dy - 5) * s,
          (dx + 10) * s, (dy - 2) * s,
        )
        ..cubicTo(
          (dx + 7) * s, (dy + 1) * s,
          (dx + 1) * s, (dy + 1) * s,
          dx * s, dy * s,
        );
      canvas.drawPath(leaf, paint);
    }
  }

  void _drawSmoke(Canvas canvas, double t, double s) {
    final paint = Paint()
      ..color = const Color(0xFFB0BEC5).withValues(alpha: 0.55 * t)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.5 * s
      ..strokeCap = StrokeCap.round;
    for (final dx in const [50.0, 68.0, 58.0]) {
      final wisp = Path()
        ..moveTo(dx * s, 24 * s)
        ..cubicTo(
          (dx - 4) * s, 18 * s,
          (dx + 4) * s, 16 * s,
          dx * s, 10 * s,
        );
      canvas.drawPath(wisp, paint);
    }
  }

  void _drawJar(Canvas canvas, double s, Color faceColor) {
    // Glass jar
    final jar = Path()
      ..moveTo(46 * s, 74 * s)
      ..lineTo(74 * s, 74 * s)
      ..lineTo(74 * s, 82 * s)
      ..cubicTo(82 * s, 86 * s, 86 * s, 92 * s, 86 * s, 102 * s)
      ..cubicTo(86 * s, 114 * s, 78 * s, 122 * s, 60 * s, 122 * s)
      ..cubicTo(42 * s, 122 * s, 34 * s, 114 * s, 34 * s, 102 * s)
      ..cubicTo(34 * s, 92 * s, 38 * s, 86 * s, 46 * s, 82 * s)
      ..close();
    canvas.drawPath(jar, Paint()..color = const Color(0xFFF1EBDF));

    // Rim
    final rim = RRect.fromRectAndRadius(
      Rect.fromLTWH(39 * s, 68 * s, 42 * s, 8 * s),
      Radius.circular(4 * s),
    );
    canvas.drawRRect(rim, Paint()..color = const Color(0xFFE4DCCB));

    // Glass shine
    final shine = Paint()
      ..color = Colors.white.withValues(alpha: 0.45)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2 * s
      ..strokeCap = StrokeCap.round;
    final shinePath = Path()
      ..moveTo(40 * s, 92 * s)
      ..cubicTo(38 * s, 97 * s, 38 * s, 102 * s, 40 * s, 107 * s);
    canvas.drawPath(shinePath, shine);

    // Soil
    final soil = Path()
      ..moveTo(38 * s, 110 * s)
      ..cubicTo(40 * s, 104 * s, 48 * s, 102 * s, 60 * s, 102 * s)
      ..cubicTo(72 * s, 102 * s, 80 * s, 104 * s, 82 * s, 110 * s)
      ..cubicTo(82 * s, 117 * s, 74 * s, 121 * s, 60 * s, 121 * s)
      ..cubicTo(46 * s, 121 * s, 38 * s, 117 * s, 38 * s, 110 * s)
      ..close();
    canvas.drawPath(soil, Paint()..color = const Color(0xFF5D4037));
    canvas.drawCircle(Offset(48 * s, 114 * s), 1.8 * s, Paint()..color = const Color(0xFF795548));
    canvas.drawCircle(Offset(68 * s, 113 * s), 1.5 * s, Paint()..color = const Color(0xFF6D4C41));

    // Face: dot eyes below the X-eyes threshold, then crossed eyes.
    final line = Paint()
      ..color = faceColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2 * s
      ..strokeCap = StrokeCap.round;
    if (severity > 0.6) {
      for (final ex in const [53.0, 67.0]) {
        canvas.drawLine(
          Offset((ex - 2.5) * s, 85.5 * s),
          Offset((ex + 2.5) * s, 90.5 * s),
          line,
        );
        canvas.drawLine(
          Offset((ex + 2.5) * s, 85.5 * s),
          Offset((ex - 2.5) * s, 90.5 * s),
          line,
        );
      }
    } else {
      canvas.drawCircle(Offset(53 * s, 88 * s), 2.3 * s, Paint()..color = faceColor);
      canvas.drawCircle(Offset(67 * s, 88 * s), 2.3 * s, Paint()..color = faceColor);
    }
    // Mouth morphs smile → flat → frown.
    final dip = 6.0 - 10.0 * severity.clamp(0.0, 1.0);
    final mouth = Path()
      ..moveTo(53 * s, 96 * s)
      ..quadraticBezierTo(60 * s, (96 + dip) * s, 67 * s, 96 * s);
    canvas.drawPath(mouth, line);
  }

  void _drawReels(Canvas canvas, double s) {
    final positions = <Offset>[
      const Offset(18, 24),
      const Offset(88, 20),
      const Offset(92, 46),
      if (severity > 0.45) const Offset(12, 48),
      if (severity > 0.8) const Offset(20, 94),
    ];
    for (final pos in positions) {
      final rect = Rect.fromLTWH(pos.dx * s, pos.dy * s, 17 * s, 17 * s);
      canvas.drawRRect(
        RRect.fromRectAndRadius(rect, Radius.circular(3.5 * s)),
        Paint()..color = accent.withValues(alpha: 0.9),
      );
      final play = Path()
        ..moveTo((pos.dx + 6) * s, (pos.dy + 5) * s)
        ..lineTo((pos.dx + 12) * s, (pos.dy + 8.5) * s)
        ..lineTo((pos.dx + 6) * s, (pos.dy + 12) * s)
        ..close();
      canvas.drawPath(play, Paint()..color = const Color(0xFF171A21));
    }
  }

  void _drawSparkles(Canvas canvas, double s) {
    final paint = Paint()..color = const Color(0xFFA5D6A7).withValues(alpha: 0.9);
    for (final (cx, cy, r) in const [(28.0, 54.0, 7.0), (94.0, 60.0, 5.0)]) {
      final star = Path()
        ..moveTo(cx * s, (cy - r) * s)
        ..quadraticBezierTo(cx * s, cy * s, (cx + r) * s, cy * s)
        ..quadraticBezierTo(cx * s, cy * s, cx * s, (cy + r) * s)
        ..quadraticBezierTo(cx * s, cy * s, (cx - r) * s, cy * s)
        ..quadraticBezierTo(cx * s, cy * s, cx * s, (cy - r) * s);
      canvas.drawPath(star, paint);
    }
  }

  @override
  bool shouldRepaint(PlantPainter old) =>
      old.severity != severity || old.accent != accent;
}
