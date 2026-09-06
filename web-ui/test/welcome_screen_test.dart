import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:poker_client/screens/welcome_screen.dart';
import 'test_helpers.dart';

void main() {
  testWidgets('WelcomeScreen renders the identity and both entry points',
      (WidgetTester tester) async {
    await tester.pumpWidget(wrapForTest(const WelcomeScreen()));
    await tester.pump();

    expect(find.text('Karata'), findsOneWidget);
    expect(find.widgetWithText(ElevatedButton, 'Create account'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, 'Log in'), findsOneWidget);

    // The server URL field is hidden by default.
    expect(find.text('Server base URL'), findsNothing);
    await tester.tap(find.text('Server settings'));
    await tester.pumpAndSettle();
    expect(find.text('Server base URL'), findsOneWidget);
  });
}
