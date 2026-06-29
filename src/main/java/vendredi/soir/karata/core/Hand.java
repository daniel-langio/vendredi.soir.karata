package vendredi.soir.karata.core;

import java.util.List;
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
    // Better tie-breaker: compare ranks of sorted cards (descending)
    List<Integer> thisRanks = this.cards.stream()
        .map(c -> c.rank().getChips())
        .sorted(java.util.Comparator.reverseOrder())
        .toList();
    List<Integer> oRanks = o.cards.stream()
        .map(c -> c.rank().getChips())
        .sorted(java.util.Comparator.reverseOrder())
        .toList();

    for (int i = 0; i < Math.min(thisRanks.size(), oRanks.size()); i++) {
      if (!thisRanks.get(i).equals(oRanks.get(i))) {
        return thisRanks.get(i) - oRanks.get(i);
      }
    }
    return 0;
  }
}
