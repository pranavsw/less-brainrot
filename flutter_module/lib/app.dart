import 'package:flutter/material.dart';

import 'bridge.dart';
import 'screens/apps_screen.dart';
import 'screens/dashboard.dart';
import 'screens/history_screen.dart';
import 'screens/onboarding.dart';
import 'screens/settings_screen.dart';

/// Shared app state: everything the screens need, refreshed on lifecycle changes
/// and pushed live by the native counter stream.
class AppState {
  AppState({required this.status, required this.today});

  Status status;
  TodayCounts today;
}

class ScrollAwareApp extends StatelessWidget {
  const ScrollAwareApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Scroll Aware',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorSchemeSeed: const Color(0xFF3A6B5E),
        useMaterial3: true,
      ),
      darkTheme: ThemeData(
        colorSchemeSeed: const Color(0xFF3A6B5E),
        brightness: Brightness.dark,
        useMaterial3: true,
      ),
      home: const AppRoot(),
    );
  }
}

class AppRoot extends StatefulWidget {
  const AppRoot({super.key});

  @override
  State<AppRoot> createState() => _AppRootState();
}

class _AppRootState extends State<AppRoot> with WidgetsBindingObserver {
  final bridge = Bridge();
  AppState? state;
  int tab = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    bridge.counterStream.listen((today) {
      if (state != null) {
        setState(() => state!.today = today);
      }
    });
    refresh();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState lifecycle) {
    // Returning from Android settings screens (accessibility, overlay, battery)
    // changes permissions — always re-read status on resume.
    if (lifecycle == AppLifecycleState.resumed) refresh();
  }

  Future<void> refresh() async {
    final status = await bridge.getStatus();
    final today = await bridge.getToday();
    if (mounted) setState(() => state = AppState(status: status, today: today));
  }

  @override
  Widget build(BuildContext context) {
    final appState = state;
    if (appState == null) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (!appState.status.onboarded) {
      return OnboardingFlow(bridge: bridge, onDone: refresh);
    }
    return Scaffold(
      body: IndexedStack(
        index: tab,
        children: [
          DashboardScreen(bridge: bridge, appState: appState, onRefresh: refresh),
          AppsScreen(bridge: bridge, appState: appState, onRefresh: refresh),
          HistoryScreen(bridge: bridge, appState: appState),
          SettingsScreen(bridge: bridge, appState: appState, onRefresh: refresh),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: tab,
        onDestinationSelected: (index) => setState(() {
          tab = index;
          refresh();
        }),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.today_outlined),
            selectedIcon: Icon(Icons.today),
            label: 'Today',
          ),
          NavigationDestination(
            icon: Icon(Icons.apps_outlined),
            selectedIcon: Icon(Icons.apps),
            label: 'Apps',
          ),
          NavigationDestination(
            icon: Icon(Icons.insights_outlined),
            selectedIcon: Icon(Icons.insights),
            label: 'History',
          ),
          NavigationDestination(
            icon: Icon(Icons.tune_outlined),
            selectedIcon: Icon(Icons.tune),
            label: 'Settings',
          ),
        ],
      ),
    );
  }
}
