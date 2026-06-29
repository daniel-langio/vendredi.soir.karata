package vendredi.soir.karata.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vendredi.soir.karata.core.factory.HandFactory;

public class TexasHoldemRules implements Rules {

  @Override
  public boolean isActionLegal(Game game, Action action) {
    // Basic validation
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
  public Player determineNextPlayer(Game game) {
    // Simplified: find first player who hasn't folded in this round
    List<Player> active =
        game.getPlayers().stream()
            .filter(
                p ->
                    game.getHistory().stream()
                        .noneMatch(
                            a -> a instanceof PlayerAction.Fold f && f.player().equals(p)))
            .toList();
    return active.isEmpty() ? null : active.get(0);
  }

  @Override
  public Map<Player, Hand> evaluateWinners(Game game) {
    Map<Player, Hand> bestHands = new HashMap<>();
    for (Player p : game.getPlayers()) {
      if (game.getHistory().stream()
          .noneMatch(a -> a instanceof PlayerAction.Fold f && f.player().equals(p))) {
        List<Card> allCards = new ArrayList<>(p.getHoleCards());
        allCards.addAll(game.getCommunityCards());
        bestHands.put(p, HandFactory.evaluateBestHand(allCards));
      }
    }
    return bestHands;
  }

  @Override
  public long getMinimumRaise(Game game) {
    return 20; // Default
  }

  @Override
  public List<Player> getBettingOrder(Game game) {
    return game.getPlayers();
  }
}
