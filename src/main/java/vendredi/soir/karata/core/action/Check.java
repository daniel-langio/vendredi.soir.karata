package vendredi.soir.karata.core.action;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import vendredi.soir.karata.core.entity.Player;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public final class Check implements PlayerAction {
  private final Player player;
}
