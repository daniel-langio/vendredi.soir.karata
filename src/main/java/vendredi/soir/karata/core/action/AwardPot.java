package vendredi.soir.karata.core.action;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vendredi.soir.karata.core.entity.Player;

@Getter
@RequiredArgsConstructor
public final class AwardPot implements DealerAction {
  private final Player winner;
  private final long amount;
}
