import 'package:flutter/material.dart';

/// Karata's dark palette, matching the design mockups.
class KarataColors {
  static const bg = Color(0xFF0A090C);
  static const page = Color(0xFF151318);
  static const ink = Color(0xFFF3F1F5);
  static const dim = Color(0xFF6D6A74);
  static const red = Color(0xFFE4485F);
  static const card = Color(0xFFF7F6F3);
  static const cardInk = Color(0xFF141317);
  static const pill = Color(0xFF232127);
  static const pillLine = Color(0xFF2B2930);
  static const field = Color(0xFF141319);
  static const chipBg = Color(0xFF2F3312);
  static const chipInk = Color(0xFFC8D66D);
  static const live = Color(0xFF8FA544);
  static const stale = Color(0xFF5A5860);
  static const allInBg = Color(0xFF3A2028);
  static const allInInk = Color(0xFFF0A4B1);
}

ThemeData karataTheme() {
  return ThemeData(
    useMaterial3: true,
    scaffoldBackgroundColor: KarataColors.page,
    colorScheme: const ColorScheme.dark(
      surface: KarataColors.page,
      primary: KarataColors.ink,
      secondary: KarataColors.chipInk,
      error: KarataColors.red,
    ),
    fontFamily: 'SF Pro Text',
    appBarTheme: const AppBarTheme(
      backgroundColor: KarataColors.page,
      foregroundColor: KarataColors.ink,
      elevation: 0,
      centerTitle: false,
    ),
    textTheme: const TextTheme(
      bodyMedium: TextStyle(color: KarataColors.ink),
      bodyLarge: TextStyle(color: KarataColors.ink),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: KarataColors.field,
      contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: BorderSide.none,
      ),
      labelStyle: const TextStyle(color: KarataColors.dim),
      hintStyle: const TextStyle(color: Color(0xFF3A383F)),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: KarataColors.pill,
        foregroundColor: KarataColors.ink,
        minimumSize: const Size.fromHeight(56),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
        textStyle: const TextStyle(fontSize: 16),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: KarataColors.ink,
        side: const BorderSide(color: KarataColors.pillLine),
        minimumSize: const Size.fromHeight(56),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
        textStyle: const TextStyle(fontSize: 16),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(foregroundColor: KarataColors.dim),
    ),
    dividerColor: const Color(0xFF1A181E),
  );
}
