package vendredi.soir.karata.core.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import vendredi.soir.karata.core.entity.Card;

@Getter
public final class RevealCards implements DealerAction {
  private final List<Card> cards;

  @JsonCreator
  public RevealCards(@JsonProperty("cards") List<Card> cards) {
    this.cards = cards;
  }
}
