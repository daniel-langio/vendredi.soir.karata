package vendredi.soir.karata.core.action;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vendredi.soir.karata.core.entity.Card;
import vendredi.soir.karata.core.entity.Player;

@Getter
@RequiredArgsConstructor
public final class DealHoleCard implements DealerAction {
  private final Player player;
  private final Card card;
}
