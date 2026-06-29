package vendredi.soir.karata.core.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player {
  private final String name;
  private long chips;

  public Player(String name, long initialChips) {
    this.name = name;
    this.chips = initialChips;
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
  }

  public void clearHand() {
  }
}
