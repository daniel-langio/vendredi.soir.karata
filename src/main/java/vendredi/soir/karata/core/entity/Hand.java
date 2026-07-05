package vendredi.soir.karata.core.entity;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Hand implements Comparable<Hand> {
  private final HandCategory type;
  private final List<Card> cards;

  public static Hand of(HandCategory type, List<Card> cards) {
    return new Hand(type, cards);
  }

  @Override
  public int compareTo(Hand o) {
    if (this.type != o.type) {
      return this.type.ordinal() - o.type.ordinal();
    }

    List<Integer> thisTieBreakers = getTieBreakers();
    List<Integer> oTieBreakers = o.getTieBreakers();

    for (int i = 0; i < Math.min(thisTieBreakers.size(), oTieBreakers.size()); i++) {
      if (!thisTieBreakers.get(i).equals(oTieBreakers.get(i))) {
        return thisTieBreakers.get(i) - oTieBreakers.get(i);
      }
    }
    return 0;
  }

  private List<Integer> getTieBreakers() {
    // Specialized tie-breaking for Straights and Straight Flushes
    if (type == HandCategory.STRAIGHT || type == HandCategory.STRAIGHT_FLUSH) {
      var ranks = cards.stream().map(Card::rank).map(Rank::getChips).sorted().toList();
      // Wheel: A-2-3-4-5 -> the high card is 5
      if (ranks.equals(List.of(2, 3, 4, 5, 14))) {
        return List.of(5);
      }
      // Otherwise, the high card determines the strength
      return List.of(ranks.get(ranks.size() - 1));
    }

    Map<Rank, Long> counts = cards.stream()
        .collect(Collectors.groupingBy(Card::rank, Collectors.counting()));

    return cards.stream()
        .map(Card::rank)
        .sorted(Comparator.<Rank, Long>comparing(counts::get).reversed()
            .thenComparing(Comparator.comparing(Rank::getChips).reversed()))
        .map(Rank::getChips)
        .toList();
  }
}
