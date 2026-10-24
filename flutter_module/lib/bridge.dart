import 'dart:async';

import 'package:flutter/services.dart';

/// Typed client for the native side of Scroll Aware. The native layer owns all
/// storage and Android integrations; the UI only reads and invokes.
class Bridge {
  Bridge() {
    _events
        .receiveBroadcastStream()
        .listen(_onCounterEvent, onError: (_) {});
  }

  static const _methods = MethodChannel('scrollaware/bridge');
  static const _events = EventChannel('scrollaware/counter');

  final _counterController = StreamController<TodayCounts>.broadcast();

  Stream<TodayCounts> get counterStream => _counterController.stream;

  void _onCounterEvent(Object? event) {
    final map = (event as Map?)?.cast<String, Object?>();
    if (map == null) return;
    final perApp = (map['perApp'] as Map?)?.cast<String, int>() ?? {};
    final today = TodayCounts(
      total: (map['total'] as num?)?.toInt() ?? 0,
      perApp: perApp,
    );
    _counterController.add(today);
  }

  Future<Status> getStatus() async =>
      Status.fromMap(await _methods.invokeMethod('getStatus'));

  Future<TodayCounts> getToday() async {
    final map =
        (await _methods.invokeMethod('getToday') as Map?)?.cast<String, Object?>();
    final perApp = (map?['perApp'] as Map?)?.cast<String, int>() ?? {};
    return TodayCounts(
      total: (map?['total'] as num?)?.toInt() ?? 0,
      perApp: perApp,
    );
  }

  Future<List<DayCount>> getHistory(int days) async {
    final list = await _methods.invokeMethod('getHistory', days) as List?;
    return (list ?? [])
        .map((e) => DayCount.fromMap((e as Map).cast<String, Object?>()))
        .toList();
  }

  Future<void> setLimit(int limit) => _methods.invokeMethod('setLimit', limit);

  Future<void> setTrackEnabled(String packageName, bool enabled) =>
      _methods.invokeMethod('setTrackEnabled', {
        'package': packageName,
        'value': enabled,
      });

  // The native handler reads a bare boolean argument.
  Future<void> _setBool(String method, bool value) =>
      _methods.invokeMethod(method, value);

  Future<void> setOverlayEnabled(bool value) => _setBool('setOverlayEnabled', value);

  Future<void> setNotificationsEnabled(bool value) =>
      _setBool('setNotificationsEnabled', value);

  Future<void> setDiagnosticsEnabled(bool value) =>
      _setBool('setDiagnosticsEnabled', value);

  Future<void> completeOnboarding() => _methods.invokeMethod('setOnboarded');

  Future<void> resetCounters() => _methods.invokeMethod('resetCounters');

  Future<void> deleteAllData() => _methods.invokeMethod('deleteAllData');

  Future<void> openAccessibilitySettings() =>
      _methods.invokeMethod('openAccessibilitySettings');

  Future<void> openOverlaySettings() =>
      _methods.invokeMethod('openOverlaySettings');

  Future<void> requestBatteryExemption() =>
      _methods.invokeMethod('requestBatteryExemption');

  Future<void> requestNotificationPermission() =>
      _methods.invokeMethod('requestNotificationPermission');

  Future<String> exportDiagnostics() async =>
      await _methods.invokeMethod('exportDiagnostics') as String? ?? '';
}

class Status {
  const Status({
    required this.serviceEnabled,
    required this.overlayPermission,
    required this.batteryExempt,
    required this.notificationsEnabled,
    required this.limit,
    required this.overlayEnabled,
    required this.notificationsPreference,
    required this.diagnosticsEnabled,
    required this.onboarded,
    required this.limitReachedToday,
    required this.trackApps,
    required this.classificationState,
    required this.classificationReason,
    required this.appVersion,
  });

  factory Status.fromMap(Map<Object?, Object?> map) {
    final track = (map['trackApps'] as Map?)?.cast<String, bool>() ?? {};
    final classification = map['currentClassification'] as Map?;
    return Status(
      serviceEnabled: map['serviceEnabled'] as bool? ?? false,
      overlayPermission: map['overlayPermission'] as bool? ?? false,
      batteryExempt: map['batteryExempt'] as bool? ?? false,
      notificationsEnabled: map['notificationsEnabled'] as bool? ?? false,
      limit: (map['limit'] as num?)?.toInt() ?? 0,
      overlayEnabled: map['overlayEnabled'] as bool? ?? true,
      notificationsPreference: map['notificationsPreference'] as bool? ?? true,
      diagnosticsEnabled: map['diagnosticsEnabled'] as bool? ?? false,
      onboarded: map['onboarded'] as bool? ?? false,
      limitReachedToday: map['limitReachedToday'] as bool? ?? false,
      trackApps: track,
      classificationState: classification?['state'] as String?,
      classificationReason: classification?['reason'] as String?,
      appVersion: map['appVersion'] as String? ?? '',
    );
  }

  final bool serviceEnabled;
  final bool overlayPermission;
  final bool batteryExempt;
  final bool notificationsEnabled;
  final int limit;
  final bool overlayEnabled;
  final bool notificationsPreference;
  final bool diagnosticsEnabled;
  final bool onboarded;
  final bool limitReachedToday;
  final Map<String, bool> trackApps;
  final String? classificationState;
  final String? classificationReason;
  final String appVersion;
}

class TodayCounts {
  const TodayCounts({required this.total, required this.perApp});

  final int total;
  final Map<String, int> perApp;
}

class DayCount {
  const DayCount({required this.date, required this.total, required this.perApp});

  factory DayCount.fromMap(Map<String, Object?> map) {
    final perApp = (map['perApp'] as Map?)?.cast<String, int>() ?? {};
    return DayCount(
      date: map['date'] as String? ?? '',
      total: (map['total'] as num?)?.toInt() ?? 0,
      perApp: perApp,
    );
  }

  final String date;
  final int total;
  final Map<String, int> perApp;
}
