package vendredi.soir.karata.core.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import vendredi.soir.karata.core.entity.Player;

@Getter
public final class Call implements PlayerAction {
  private final Player player;
  private final long amount;

  @JsonCreator
  public Call(@JsonProperty("player") Player player, @JsonProperty("amount") long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Call amount must be positive");
    }
    this.player = player;
    this.amount = amount;
  }
}
