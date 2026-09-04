package vendredi.soir.karata.core.action;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vendredi.soir.karata.core.entity.Player;

@Getter
@RequiredArgsConstructor
public final class Fold implements PlayerAction {
  private final Player player;
}
