package vendredi.soir.karata.core.action;

import lombok.Getter;
import lombok.NoArgsConstructor;
import vendredi.soir.karata.core.entity.Player;

@Getter
@NoArgsConstructor(force = true)
public final class Raise implements PlayerAction {
  private final Player player;
  private final long amount;

  public Raise(Player player, long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Raise amount must be positive");
    }
    this.player = player;
    this.amount = amount;
  }
}
