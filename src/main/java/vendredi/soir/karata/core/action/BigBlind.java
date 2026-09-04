package vendredi.soir.karata.core.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import vendredi.soir.karata.core.entity.Player;

@Getter
public final class BigBlind implements PlayerAction {
  private final Player player;
  private final long amount;

  @JsonCreator
  public BigBlind(@JsonProperty("player") Player player, @JsonProperty("amount") long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Big blind amount must be positive");
    }
    this.player = player;
    this.amount = amount;
  }
}
