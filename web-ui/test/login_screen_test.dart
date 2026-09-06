import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:poker_client/screens/login_screen.dart';
import 'test_helpers.dart';

void main() {
  group('LoginScreen validation', () {
    testWidgets('renders the form', (WidgetTester tester) async {
      await tester.pumpWidget(
        wrapForTest(const LoginScreen(serverUrl: 'https://test.poker/poker')),
      );
      await tester.pump();

      expect(find.text('Log in'), findsWidgets);
      expect(find.widgetWithText(TextFormField, 'Username'), findsOneWidget);
      expect(find.widgetWithText(TextFormField, 'Password'), findsOneWidget);
    });

    testWidgets('rejects empty fields without making a network call',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        wrapForTest(const LoginScreen(serverUrl: 'https://test.poker/poker')),
      );
      await tester.pump();

      await tester.tap(find.widgetWithText(ElevatedButton, 'Log in'));
      await tester.pumpAndSettle();

      expect(find.text('Required'), findsWidgets);
    });
  });
}
