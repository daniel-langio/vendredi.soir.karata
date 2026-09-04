package vendredi.soir.karata.core.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode(of = "name")
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class Player {
  private final String name;

  public void receiveCard(Card card) {}

  public void clearHand() {}
}
