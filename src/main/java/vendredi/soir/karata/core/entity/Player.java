package vendredi.soir.karata.core.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(of = "name")
public class Player {
  private final String name;

  @JsonCreator
  public Player(@JsonProperty("name") String name) {
    this.name = name;
  }

  public void receiveCard(Card card) {}

  public void clearHand() {}
}
