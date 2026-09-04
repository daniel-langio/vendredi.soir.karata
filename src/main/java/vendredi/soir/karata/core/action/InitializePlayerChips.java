package vendredi.soir.karata.core.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import vendredi.soir.karata.core.entity.Player;

@Getter
public final class InitializePlayerChips implements DealerAction {
  private final Player player;
  private final long amount;

  @JsonCreator
  public InitializePlayerChips(
      @JsonProperty("player") Player player, @JsonProperty("amount") long amount) {
    this.player = player;
    this.amount = amount;
  }
}
