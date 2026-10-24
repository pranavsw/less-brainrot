// Display metadata for the supported tracking targets. Package names must match
// the native SupportedPackages constants.
import 'package:flutter/material.dart';

class TrackedAppInfo {
  const TrackedAppInfo({
    required this.packageName,
    required this.name,
    required this.section,
    required this.initials,
    required this.color,
  });

  final String packageName;
  final String name;
  final String section;
  final String initials;
  final Color color;

  static const instagram = TrackedAppInfo(
    packageName: 'com.instagram.android',
    name: 'Instagram',
    section: 'Reels',
    initials: 'IG',
    color: Color(0xFFB76E79),
  );

  static const youtube = TrackedAppInfo(
    packageName: 'com.google.android.youtube',
    name: 'YouTube',
    section: 'Shorts',
    initials: 'YT',
    color: Color(0xFFC25B4E),
  );

  static const all = [instagram, youtube];
}

CircleAvatar appAvatar(TrackedAppInfo app, {double radius = 20}) {
  return CircleAvatar(
    radius: radius,
    backgroundColor: app.color.withValues(alpha: 0.18),
    child: Text(
      app.initials,
      style: TextStyle(
        color: app.color,
        fontWeight: FontWeight.w700,
        fontSize: radius * 0.7,
      ),
    ),
  );
}
