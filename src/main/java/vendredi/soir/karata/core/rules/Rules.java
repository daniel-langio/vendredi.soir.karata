package vendredi.soir.karata.core.rules;

import java.util.List;
import java.util.Map;
import vendredi.soir.karata.core.action.Action;
import vendredi.soir.karata.core.entity.Deal;
import vendredi.soir.karata.core.entity.Game;
import vendredi.soir.karata.core.entity.Hand;
import vendredi.soir.karata.core.entity.Player;

public interface Rules {
  boolean isActionLegal(Game game, Deal deal, Action action);

  Player determineNextPlayer(Deal deal, List<Player> players);

  Map<Player, Hand> evaluateWinners(Deal deal, List<Player> players);

  long getMinimumRaise(Deal deal);

  List<Player> getBettingOrder(List<Player> players);
}
