package vendredi.soir.karata.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vendredi.soir.karata.core.factory.HandFactory;

public class TexasHoldemRules implements Rules {

  @Override
  public boolean isActionLegal(Deal deal, Action action) {
    if (action instanceof PlayerAction pa) {
      if (pa instanceof PlayerAction.Bet bet) {
        return bet.player().getChips() >= bet.amount();
      }
      if (pa instanceof PlayerAction.Raise raise) {
        return raise.player().getChips() >= raise.amount();
      }
      if (pa instanceof PlayerAction.Call call) {
        return call.player().getChips() >= call.amount();
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
