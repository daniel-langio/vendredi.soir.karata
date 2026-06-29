package vendredi.soir.karata.core;

import java.util.List;
import java.util.Map;

public interface Rules {
  boolean isActionLegal(Deal deal, Action action);

  Player determineNextPlayer(Deal deal, List<Player> players);

  Map<Player, Hand> evaluateWinners(Deal deal, List<Player> players);

  long getMinimumRaise(Deal deal);

  List<Player> getBettingOrder(List<Player> players);
}
