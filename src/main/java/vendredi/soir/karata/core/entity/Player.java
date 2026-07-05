package vendredi.soir.karata.core.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class Player {
  private final String name;

  public void receiveCard(Card card) {}

  public void clearHand() {}
}
