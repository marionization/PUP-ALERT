import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:device_preview/device_preview.dart';

import 'firebase_options.dart';
import 'screens/auth/login_screen.dart';
import 'screens/dashboard/dashboard_screen.dart';
import 'theme/app_theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Firebase across Web, iOS, and Android
  try {
    await Firebase.initializeApp(
      options: DefaultFirebaseOptions.currentPlatform,
    );
  } catch (e) {
    debugPrint('Firebase initialization notice: $e');
  }

  // Check saved session
  final prefs = await SharedPreferences.getInstance();
  final isLoggedIn = prefs.getBool('is_logged_in') ?? false;
  final role = prefs.getString('role') ?? 'student';
  final name = prefs.getString('user_name') ?? 'Student';
  final firstName =
      prefs.getString('student_first_name') ?? (name.split(' ').first);

  runApp(
    DevicePreview(
      enabled: !kReleaseMode,
      builder: (context) => PupAlertApp(
        isLoggedIn: isLoggedIn,
        role: role,
        name: name,
        firstName: firstName,
      ),
    ),
  );
}

class PupAlertApp extends StatelessWidget {
  final bool isLoggedIn;
  final String role;
  final String name;
  final String firstName;

  const PupAlertApp({
    super.key,
    required this.isLoggedIn,
    required this.role,
    required this.name,
    required this.firstName,
  });

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'PUP ALERT',
      debugShowCheckedModeBanner: false,
      locale: DevicePreview.locale(context),
      builder: DevicePreview.appBuilder,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ThemeMode.light,
      home: isLoggedIn
          ? DashboardScreen(
              userRole: role,
              userName: name,
              firstName: firstName,
            )
          : const LoginScreen(),
    );
  }
}
