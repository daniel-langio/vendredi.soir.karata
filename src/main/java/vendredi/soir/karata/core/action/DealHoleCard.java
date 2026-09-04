package vendredi.soir.karata.core.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import vendredi.soir.karata.core.entity.Card;
import vendredi.soir.karata.core.entity.Player;

@Getter
public final class DealHoleCard implements DealerAction {
  private final Player player;
  private final Card card;

  @JsonCreator
  public DealHoleCard(@JsonProperty("player") Player player, @JsonProperty("card") Card card) {
    this.player = player;
    this.card = card;
  }
}
