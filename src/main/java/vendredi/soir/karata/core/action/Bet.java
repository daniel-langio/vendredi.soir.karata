package vendredi.soir.karata.core.action;

import lombok.Getter;
import vendredi.soir.karata.core.entity.Player;

@Getter
public final class Bet implements PlayerAction {
  private final Player player;
  private final long amount;

  public Bet(Player player, long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Bet amount must be positive");
    }
    this.player = player;
    this.amount = amount;
  }
}
