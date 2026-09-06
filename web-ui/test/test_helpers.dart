import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:poker_client/l10n/app_localizations.dart';

/// Wraps a screen the same way MyApp's MaterialApp does, so AppLocalizations.of(context) has
/// something to find - without this, any screen using it throws a null-check failure in tests.
Widget wrapForTest(Widget child) {
  return MaterialApp(
    supportedLocales: AppLocalizations.supportedLocales,
    localizationsDelegates: const [
      AppLocalizations.delegate,
      GlobalMaterialLocalizations.delegate,
      GlobalWidgetsLocalizations.delegate,
    ],
    home: child,
  );
}
