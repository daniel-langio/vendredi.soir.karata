package vendredi.soir.karata.core.action;

import lombok.Getter;
import lombok.NoArgsConstructor;
import vendredi.soir.karata.core.entity.Player;

@Getter
@NoArgsConstructor(force = true)
public final class Call implements PlayerAction {
  private final Player player;
  private final long amount;

  public Call(Player player, long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Call amount must be positive");
    }
    this.player = player;
    this.amount = amount;
  }
}
