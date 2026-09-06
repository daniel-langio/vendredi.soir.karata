import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// The user's explicitly-chosen app language, overriding the system locale once set. Null means
/// "follow the system locale" (MaterialApp falls back to the first supported one - English - if
/// the system's isn't in AppLocalizations.supportedLocales).
class LocaleController extends ValueNotifier<Locale?> {
  LocaleController._() : super(null);
  static final LocaleController instance = LocaleController._();

  static const _prefsKey = 'app_locale';

  /// Loads any previously-saved choice. Fire-and-forget from main() - MyApp is wrapped in a
  /// ValueListenableBuilder, so the UI just updates once this resolves; there's nothing worth
  /// blocking app startup on here.
  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final code = prefs.getString(_prefsKey);
    if (code != null) value = Locale(code);
  }

  Future<void> setLocale(Locale? locale) async {
    value = locale;
    final prefs = await SharedPreferences.getInstance();
    if (locale == null) {
      await prefs.remove(_prefsKey);
    } else {
      await prefs.setString(_prefsKey, locale.languageCode);
    }
  }
}
