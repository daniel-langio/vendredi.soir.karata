package vendredi.soir.karata.core.action;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vendredi.soir.karata.core.entity.Player;

@Getter
@RequiredArgsConstructor
public final class Check implements PlayerAction {
  private final Player player;
}
