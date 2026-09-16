import 'package:flutter/material.dart';

class AppTheme {
  static const Color pupMaroon = Color(0xFF800000);
  static const Color pupDarkMaroon = Color(0xFF5A0000);
  static const Color pupGold = Color(0xFFFDB913);
  static const Color pupLightGold = Color(0xFFFFF2D6);
  static const Color statusInReview = Color(0xFFF57C00); // Orange
  static const Color statusInProgress = Color(0xFF1976D2); // Blue
  static const Color statusResolved = Color(0xFF388E3C); // Green

  static ThemeData lightTheme = ThemeData(
    useMaterial3: true,
    colorScheme: ColorScheme.fromSeed(
      seedColor: pupMaroon,
      primary: pupMaroon,
      secondary: pupGold,
      surface: Colors.white,
      brightness: Brightness.light,
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: pupMaroon,
      foregroundColor: Colors.white,
      elevation: 0,
      centerTitle: true,
    ),
    cardTheme: CardThemeData(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: pupMaroon,
        foregroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      ),
    ),
  );

  static ThemeData darkTheme = ThemeData(
    useMaterial3: true,
    colorScheme: ColorScheme.fromSeed(
      seedColor: pupMaroon,
      primary: pupGold,
      secondary: pupMaroon,
      surface: const Color(0xFF1E1E1E),
      brightness: Brightness.dark,
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: Color(0xFF121212),
      foregroundColor: Colors.white,
      elevation: 0,
      centerTitle: true,
    ),
    cardTheme: CardThemeData(
      color: const Color(0xFF252525),
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
    ),
  );
}
