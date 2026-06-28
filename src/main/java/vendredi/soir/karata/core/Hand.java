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
    return 0;
  }
}
