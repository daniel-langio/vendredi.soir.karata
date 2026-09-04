package vendredi.soir.karata.core.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import vendredi.soir.karata.core.entity.Player;

@Getter
public final class AwardPot implements DealerAction {
  private final Player winner;
  private final long amount;

  @JsonCreator
  public AwardPot(@JsonProperty("winner") Player winner, @JsonProperty("amount") long amount) {
    this.winner = winner;
    this.amount = amount;
  }
}
