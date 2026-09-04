package vendredi.soir.karata.core.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import vendredi.soir.karata.core.entity.Player;

@Getter
public final class Check implements PlayerAction {
  private final Player player;

  @JsonCreator
  public Check(@JsonProperty("player") Player player) {
    this.player = player;
  }
}
