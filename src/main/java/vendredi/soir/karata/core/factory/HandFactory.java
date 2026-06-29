package vendredi.soir.karata.core.factory;

import static vendredi.soir.karata.core.HandCategory.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import vendredi.soir.karata.core.Card;
import vendredi.soir.karata.core.Deck;
import vendredi.soir.karata.core.Hand;
import vendredi.soir.karata.core.HandCategory;

public class HandFactory {
  public static Hand from(Deck deck) {
    var cards = deck.getCards();
    if (cards.size() > 5) {
      throw new IllegalStateException("Hand size has to be 5 at most");
    }

    boolean flush = isFlush(cards);
    boolean straight = isStraight(cards);

    var occurrences =
        cards.stream()
            .collect(Collectors.groupingBy(Card::rank, Collectors.counting()))
            .values()
            .stream()
            .sorted(Comparator.reverseOrder())
            .toList();

    HandCategory type =
        straight && flush
            ? STRAIGHT_FLUSH
            : occurrences.equals(List.of(5L))
                ? FIVE_OF_A_KIND
                : occurrences.equals(List.of(4L, 1L))
                    ? FOUR_OF_A_KIND
                    : occurrences.equals(List.of(3L, 2L))
                        ? FULL_HOUSE
                        : flush
                            ? FLUSH
                            : straight
                                ? STRAIGHT
                                : occurrences.equals(List.of(3L, 1L, 1L))
                                    ? THREE_OF_A_KIND
                                    : occurrences.equals(List.of(2L, 2L, 1L))
                                        ? TWO_PAIR
                                        : occurrences.equals(List.of(2L, 1L, 1L, 1L))
                                            ? ONE_PAIR
                                            : HIGH_CARD;

    return Hand.of(type, cards);
  }

  public static Hand evaluateBestHand(List<Card> cards) {
    if (cards.size() <= 5) {
      return from(new Deck(cards));
    }

    // Simplified: find best 5-card combination (combinations of 7 choose 5 = 21)
    List<List<Card>> combinations = new ArrayList<>();
    generateCombinations(cards, 5, 0, new ArrayList<>(), combinations);

    Hand bestHand = null;
    for (List<Card> combo : combinations) {
      Hand currentHand = from(new Deck(combo));
      if (bestHand == null || currentHand.compareTo(bestHand) > 0) {
        bestHand = currentHand;
      }
    }
    return bestHand;
  }

  private static void generateCombinations(
      List<Card> cards, int k, int start, List<Card> current, List<List<Card>> result) {
    if (current.size() == k) {
      result.add(new ArrayList<>(current));
      return;
    }
    for (int i = start; i < cards.size(); i++) {
      current.add(cards.get(i));
      generateCombinations(cards, k, i + 1, current, result);
      current.remove(current.size() - 1);
    }
  }

  private static boolean isFlush(List<Card> cards) {
    return cards.stream().map(Card::suit).distinct().count() == 1;
  }

  private static boolean isStraight(List<Card> cards) {
    var ranks =
        cards.stream()
            .map(card -> card.rank().getChips()) // 2..14 (Ace = 14)
            .sorted()
            .toList();

    // Duplicate ranks cannot form a straight.
    if (ranks.stream().distinct().count() != 5) {
      return false;
    }

    // Wheel straight: A-2-3-4-5
    if (ranks.equals(List.of(2, 3, 4, 5, 14))) {
      return true;
    }

    for (int i = 1; i < ranks.size(); i++) {
      if (ranks.get(i) != ranks.get(i - 1) + 1) {
        return false;
      }
    }

    return true;
  }
}
