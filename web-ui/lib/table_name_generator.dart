import 'dart:math';

// Two short poker-themed word lists, combined as "<Adjective> <Noun>" (e.g. "Reckless Flush").
// Adding/removing words here is the whole extension point for this feature - nothing else
// needs to change to grow the pool of generated names.
const _adjectives = [
  'Silent',
  'Reckless',
  'Midnight',
  'Golden',
  'Lucky',
  'Bluffing',
  'Wild',
  'Crimson',
  'Iron',
  'Velvet',
  'Roaring',
  'Sneaky',
  'Electric',
  'Frosty',
  'Neon',
  'Rowdy',
  'Salty',
  'Shadow',
  'Rusty',
  'Sly',
];

const _nouns = [
  'Bluff',
  'Flush',
  'River',
  'Showdown',
  'Ante',
  'Raise',
  'Pot',
  'Kicker',
  'Nuts',
  'Rounder',
  'Grinder',
  'Deal',
  'Ace',
  'Royal',
  'Turn',
  'Straddle',
  'Ringer',
  'Table',
  'Chip',
  'Bankroll',
];

final _random = Random();

/// A short, random `adjective noun` poker table name (e.g. "Reckless Flush") - used to prefill
/// NewTableScreen's name field so creating a table never requires typing one, and can be
/// re-rolled on demand via the shuffle button next to it.
String generateTableName() {
  final adjective = _adjectives[_random.nextInt(_adjectives.length)];
  final noun = _nouns[_random.nextInt(_nouns.length)];
  return '$adjective $noun';
}
