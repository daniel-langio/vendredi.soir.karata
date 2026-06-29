package vendredi.soir.karata.core;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player {
  private final String name;
  private long chips;
  private List<Card> holeCards;

  public Player(String name, long initialChips) {
    this.name = name;
    this.chips = initialChips;
    this.holeCards = new ArrayList<>();
  }

  public void addChips(long amount) {
    this.chips += amount;
  }

  public void removeChips(long amount) {
    if (amount > chips) {
      throw new IllegalArgumentException("Not enough chips");
    }
    this.chips -= amount;
  }

  public void receiveCard(Card card) {
    this.holeCards.add(card);
  }

  public void clearHand() {
    this.holeCards.clear();
  }
}
