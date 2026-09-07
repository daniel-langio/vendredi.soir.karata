import 'package:flutter_test/flutter_test.dart';
import 'package:poker_client/table_name_generator.dart';

void main() {
  test('generateTableName returns two capitalized words', () {
    for (var i = 0; i < 50; i++) {
      final name = generateTableName();
      final parts = name.split(' ');
      expect(parts.length, 2, reason: '"$name" should be exactly two words');
      for (final word in parts) {
        expect(word.isNotEmpty, isTrue);
        expect(word[0], word[0].toUpperCase(), reason: '"$word" should start with a capital');
      }
    }
  });

  test('generateTableName varies across calls', () {
    final names = {for (var i = 0; i < 30; i++) generateTableName()};
    expect(names.length, greaterThan(1), reason: 'should not always produce the same name');
  });
}
