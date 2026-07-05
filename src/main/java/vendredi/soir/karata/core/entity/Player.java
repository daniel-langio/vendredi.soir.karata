package vendredi.soir.karata.core.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Player {
  private final String name;

  public void receiveCard(Card card) {
  }

  public void clearHand() {
  }
}
