package vendredi.soir.karata.core.entity;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import vendredi.soir.karata.core.action.Action;
import vendredi.soir.karata.core.action.AwardPot;
import vendredi.soir.karata.core.action.Showdown;
import vendredi.soir.karata.core.rules.Rules;

@RequiredArgsConstructor
public class Dealer {
  private final Rules rules;

  public void execute(Game game, Deal deal, Action action) {
    if (!rules.isActionLegal(game, deal, action)) {
      throw new IllegalArgumentException("Illegal action");
    }

    deal.apply(action);

    if (action instanceof Showdown) {
      handleShowdown(game, deal);
    }
  }

  public void execute(Game game, Action action) {
    game.addAction(action);
  }

  private void handleShowdown(Game game, Deal deal) {
    Map<Player, Hand> winners = rules.evaluateWinners(deal, game.getPlayers());
    if (winners.isEmpty()) {
      return;
    }

    long totalPot = deal.getTotalPot();
    long share = totalPot / winners.size();
    long remainder = totalPot % winners.size();

    int i = 0;
    for (Player winner : winners.keySet()) {
      long amount = share + (i == 0 ? remainder : 0); // Give remainder to first winner
      deal.apply(new AwardPot(winner, amount));
      i++;
    }
  }
}
