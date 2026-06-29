package vendredi.soir.karata.core;

import java.util.List;
import java.util.Map;

public interface Rules {
  boolean isActionLegal(Game game, Action action);

  Player determineNextPlayer(Game game);

  Map<Player, Hand> evaluateWinners(Game game);

  long getMinimumRaise(Game game);

  List<Player> getBettingOrder(Game game);
}
