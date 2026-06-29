package vendredi.soir.karata.core.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vendredi.soir.karata.core.action.Action;
import vendredi.soir.karata.core.action.PlayerAction;
import vendredi.soir.karata.core.action.Bet;
import vendredi.soir.karata.core.action.Raise;
import vendredi.soir.karata.core.action.Call;
import vendredi.soir.karata.core.entity.Card;
import vendredi.soir.karata.core.entity.Deal;
import vendredi.soir.karata.core.entity.Game;
import vendredi.soir.karata.core.entity.Hand;
import vendredi.soir.karata.core.entity.Player;
import vendredi.soir.karata.core.factory.HandFactory;

public class TexasHoldemRules implements Rules {

  @Override
  public boolean isActionLegal(Game game, Deal deal, Action action) {
    if (action instanceof PlayerAction pa) {
      if (pa instanceof Bet bet) {
        return game.getChips(bet.player()) >= bet.amount();
      }
      if (pa instanceof Raise raise) {
        return game.getChips(raise.player()) >= raise.amount();
      }
      if (pa instanceof Call call) {
        return game.getChips(call.player()) >= call.amount();
      }
    }
    return true;
  }

  @Override
  public Player determineNextPlayer(Deal deal, List<Player> players) {
    List<Player> active =
        players.stream()
            .filter(p -> !deal.hasFolded(p))
            .toList();
    return active.isEmpty() ? null : active.get(0);
  }

  @Override
  public Map<Player, Hand> evaluateWinners(Deal deal, List<Player> players) {
    Map<Player, Hand> bestHands = new HashMap<>();
    for (Player p : players) {
      if (!deal.hasFolded(p)) {
        List<Card> allCards = new ArrayList<>(deal.getHoleCards(p));
        allCards.addAll(deal.getBoard());
        if (allCards.size() >= 5) {
          bestHands.put(p, HandFactory.evaluateBestHand(allCards));
        }
      }
    }
    return bestHands;
  }

  @Override
  public long getMinimumRaise(Deal deal) {
    return 20;
  }

  @Override
  public List<Player> getBettingOrder(List<Player> players) {
    return players;
  }
}
