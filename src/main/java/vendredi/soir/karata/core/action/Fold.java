package vendredi.soir.karata.core.action;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import vendredi.soir.karata.core.entity.Player;

@Getter
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public final class Fold implements PlayerAction {
  private final Player player;
}
