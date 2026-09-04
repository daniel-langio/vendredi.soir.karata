import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:poker_client/screens/menu_screen.dart';

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  testWidgets('MenuScreen renders identity, entry points, and an empty tables list',
      (WidgetTester tester) async {
    await tester.pumpWidget(const MaterialApp(
      home: MenuScreen(
        serverUrl: 'https://test.poker/poker',
        token: 'mock-token',
        username: 'eli',
      ),
    ));
    await tester.pumpAndSettle();

    expect(find.text('eli'), findsOneWidget);
    expect(find.widgetWithText(ElevatedButton, 'Create a table'), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, 'Join with a link'), findsOneWidget);
    expect(find.text('No tables yet. Create or join one to see it here.'), findsOneWidget);
  });
}
