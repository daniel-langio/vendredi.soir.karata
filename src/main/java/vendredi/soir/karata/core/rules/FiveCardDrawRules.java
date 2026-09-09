package vendredi.soir.karata.core.rules;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import vendredi.soir.karata.core.action.Action;
import vendredi.soir.karata.core.action.AdvancePhase;
import vendredi.soir.karata.core.action.Draw;
import vendredi.soir.karata.core.action.Showdown;
import vendredi.soir.karata.core.entity.Deal;
import vendredi.soir.karata.core.entity.Game;
import vendredi.soir.karata.core.entity.Player;

/**
 * No-Limit Five-Card Draw: same blinds/betting engine as Hold'em, but each player gets 5 private
 * hole cards and no shared board at all (so hand evaluation is naturally just "best hand from your
 * 5 cards" - TexasHoldemRules.evaluateShowdownHands already reduces to exactly this once the board
 * is always empty, no override needed). One betting round, a single draw phase where every active
 * player discards 0-5 cards and gets that many replacements, then a second betting round.
 */
public class FiveCardDrawRules extends TexasHoldemRules {

  @Override
  public int holeCardCount() {
    return 5;
  }

  @Override
  public String currentPhase(Deal deal, List<Player> dealtInPlayers) {
    if (deal.getHistory().stream().anyMatch(a -> a instanceof Showdown)) return "SHOWDOWN";
    long advances = deal.getHistory().stream().filter(a -> a instanceof AdvancePhase).count();
    if (advances == 0) return "PRE_DRAW";
    if (advances == 1) return "DRAW";
    return "POST_DRAW";
  }

  @Override
  public boolean isCurrentPhaseComplete(Deal deal, List<Player> dealtInPlayers) {
    if ("DRAW".equals(currentPhase(deal, dealtInPlayers))) {
      Set<Player> active =
          dealtInPlayers.stream().filter(p -> !deal.hasFolded(p)).collect(Collectors.toSet());
      Set<Player> drawn =
          deal.getActionsInCurrentPhase().stream()
              .filter(a -> a instanceof Draw)
              .map(a -> ((Draw) a).getPlayer())
              .collect(Collectors.toSet());
      return drawn.containsAll(active);
    }
    return isBettingRoundComplete(deal, dealtInPlayers);
  }

  @Override
  public Action nextPhaseAction(
      Game game, Deal deal, List<Player> dealtInPlayers, boolean foldedOut) {
    if (foldedOut) return new Showdown();
    return switch (currentPhase(deal, dealtInPlayers)) {
      case "PRE_DRAW", "DRAW" -> new AdvancePhase();
      default -> new Showdown(); // POST_DRAW betting closed
    };
  }
}
