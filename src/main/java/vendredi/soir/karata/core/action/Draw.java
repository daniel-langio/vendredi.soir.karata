package vendredi.soir.karata.core.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import vendredi.soir.karata.core.entity.Card;
import vendredi.soir.karata.core.entity.Player;

/**
 * Five-Card Draw: a player discards 0-5 of their current hand and is dealt that many replacement
 * cards (see DealService.takeAction, which deals the replacements as ordinary DealHoleCard actions
 * right after this one). {@link vendredi.soir.karata.core.entity.Deal#getHoleCards} subtracts the
 * discarded cards from a player's dealt cards, so the replacements are the only ones left in that
 * slot.
 */
@Getter
public final class Draw implements PlayerAction {
  private final Player player;
  private final List<Card> discarded;

  @JsonCreator
  public Draw(
      @JsonProperty("player") Player player, @JsonProperty("discarded") List<Card> discarded) {
    this.player = player;
    this.discarded = discarded;
  }
}
