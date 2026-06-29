package vendredi.soir.karata.core;

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
    // Hole cards are now managed by the Deal aggregate history.
    // This method can be kept if we want to track something else, or removed.
    // For now, let's keep it empty to fulfill the contract if needed, or remove it.
  }

  public void clearHand() {
    // State is now derived from history.
  }
}
