package vendredi.soir.karata.core.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import vendredi.soir.karata.core.action.*;
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
        return game.getChips(bet.player()) >= bet.amount() && deal.getCurrentRoundBet() == 0;
      }
      if (pa instanceof Raise raise) {
        return game.getChips(raise.player()) >= raise.amount() && raise.amount() >= getMinimumRaise(deal);
      }
      if (pa instanceof Call call) {
        return game.getChips(call.player()) >= call.amount();
      }
      if (pa instanceof Check) {
        return deal.getCurrentRoundBet() == deal.getPlayerRoundContribution(pa.player());
      }
    }
    return true;
  }

  @Override
  public Player determineNextPlayer(Deal deal, List<Player> players) {
    List<Player> active = players.stream()
        .filter(p -> !deal.hasFolded(p))
        .toList();

    if (active.isEmpty()) return null;

    // Simplified turn logic: find the last player who acted and pick the next one
    Player lastActor = deal.getHistory().stream()
        .filter(a -> a instanceof PlayerAction)
        .map(a -> ((PlayerAction) a).player())
        .reduce((first, second) -> second)
        .orElse(null);

    if (lastActor == null) return active.get(0);

    int lastIndex = players.indexOf(lastActor);
    for (int i = 1; i <= players.size(); i++) {
      Player next = players.get((lastIndex + i) % players.size());
      if (active.contains(next)) return next;
    }
    return null;
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

    if (bestHands.isEmpty()) return Map.of();

    Hand winningHand = bestHands.values().stream().max(Hand::compareTo).get();
    return bestHands.entrySet().stream()
        .filter(e -> e.getValue().compareTo(winningHand) == 0)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public long getMinimumRaise(Deal deal) {
    // Simplified: double the current bet
    return Math.max(20, deal.getCurrentRoundBet() * 2);
  }

  @Override
  public List<Player> getBettingOrder(List<Player> players) {
    return players;
  }
}
