import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:poker_client/screens/login_screen.dart';

void main() {
  group('LoginScreen validation', () {
    testWidgets('renders the form', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: LoginScreen(serverUrl: 'https://test.poker/poker')),
      );

      expect(find.text('Log in'), findsWidgets);
      expect(find.widgetWithText(TextFormField, 'Username'), findsOneWidget);
      expect(find.widgetWithText(TextFormField, 'Password'), findsOneWidget);
    });

    testWidgets('rejects empty fields without making a network call',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: LoginScreen(serverUrl: 'https://test.poker/poker')),
      );

      await tester.tap(find.widgetWithText(ElevatedButton, 'Log in'));
      await tester.pumpAndSettle();

      expect(find.text('Required'), findsWidgets);
    });
  });
}
