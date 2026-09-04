package vendredi.soir.karata.core.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import vendredi.soir.karata.core.action.Action;
import vendredi.soir.karata.core.action.AwardPot;
import vendredi.soir.karata.core.action.Showdown;
import vendredi.soir.karata.core.rules.Rules;

@RequiredArgsConstructor
public class Dealer {
  private final Rules rules;

  /**
   * Applies the given action to the deal and returns every action actually applied as a result, in
   * order: the given action itself, followed by any actions generated as a side effect (e.g.
   * AwardPot entries triggered by a Showdown). Callers must persist every returned action.
   */
  public List<Action> execute(Game game, Deal deal, Action action) {
    if (!rules.isActionLegal(game, deal, action)) {
      throw new IllegalArgumentException("Illegal action");
    }

    deal.apply(action);

    List<Action> applied = new ArrayList<>();
    applied.add(action);
    if (action instanceof Showdown) {
      applied.addAll(handleShowdown(game, deal));
    }
    return applied;
  }

  public void execute(Game game, Action action) {
    game.addAction(action);
  }

  private List<Action> handleShowdown(Game game, Deal deal) {
    Map<Player, Hand> winners = rules.evaluateWinners(deal, game.getPlayers());
    if (winners.isEmpty()) {
      return List.of();
    }

    long totalPot = deal.getTotalPot();
    long share = totalPot / winners.size();
    long remainder = totalPot % winners.size();

    List<Action> awards = new ArrayList<>();
    int i = 0;
    for (Player winner : winners.keySet()) {
      long amount = share + (i == 0 ? remainder : 0); // Give remainder to first winner
      AwardPot awardPot = new AwardPot(winner, amount);
      deal.apply(awardPot);
      awards.add(awardPot);
      i++;
    }
    return awards;
  }
}
