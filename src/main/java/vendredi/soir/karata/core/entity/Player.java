package vendredi.soir.karata.core.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode(of = "name")
@RequiredArgsConstructor
public class Player {
  private final String name;

  public void receiveCard(Card card) {}

  public void clearHand() {}
}
