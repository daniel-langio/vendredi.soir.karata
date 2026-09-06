import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:poker_client/screens/register_screen.dart';
import 'test_helpers.dart';

void main() {
  group('RegisterScreen validation', () {
    testWidgets('renders the form', (WidgetTester tester) async {
      await tester.pumpWidget(
        wrapForTest(const RegisterScreen(serverUrl: 'https://test.poker/poker')),
      );
      await tester.pump();

      expect(find.text('Create account'), findsWidgets);
      expect(find.widgetWithText(TextFormField, 'Username'), findsOneWidget);
      expect(find.widgetWithText(TextFormField, 'Password'), findsOneWidget);
    });

    testWidgets('rejects a too-short username without making a network call',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        wrapForTest(const RegisterScreen(serverUrl: 'https://test.poker/poker')),
      );
      await tester.pump();

      await tester.enterText(find.widgetWithText(TextFormField, 'Username'), 'ab');
      await tester.enterText(find.widgetWithText(TextFormField, 'Password'), 'longenough');
      await tester.tap(find.widgetWithText(ElevatedButton, 'Create account'));
      await tester.pumpAndSettle();

      expect(find.text('At least 3 characters'), findsOneWidget);
    });

    testWidgets('rejects a too-short password without making a network call',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        wrapForTest(const RegisterScreen(serverUrl: 'https://test.poker/poker')),
      );
      await tester.pump();

      await tester.enterText(find.widgetWithText(TextFormField, 'Username'), 'poker_champ');
      await tester.enterText(find.widgetWithText(TextFormField, 'Password'), '123');
      await tester.tap(find.widgetWithText(ElevatedButton, 'Create account'));
      await tester.pumpAndSettle();

      expect(find.text('At least 6 characters'), findsOneWidget);
    });
  });
}
