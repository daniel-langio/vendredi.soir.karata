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

  /**
   * Every non-folded player's best hand at a real (card-based) showdown, winners and losers alike -
   * unlike {@link #evaluateWinners}, which narrows this down to only the tied-for-best hand(s) used
   * to award the pot.
   */
  Map<Player, Hand> evaluateShowdownHands(Deal deal, List<Player> players);

  long getMinimumRaise(Deal deal);

  List<Player> getBettingOrder(List<Player> players);

  boolean isBettingRoundComplete(Deal deal, List<Player> players);

  /** How many hole cards each player is dealt at the start of a hand - 2 for Texas Hold'em. */
  default int holeCardCount() {
    return 2;
  }

  /**
   * The current named phase of this deal (e.g. "PRE_FLOP", "SHOWDOWN") - each variant defines its
   * own street sequence, so this replaces what used to be a single hardcoded Deal method.
   */
  String currentPhase(Deal deal, List<Player> dealtInPlayers);

  /**
   * Whether every player still needs to do in the current phase has been done - a betting round for
   * Hold'em/Omaha, but e.g. "has everyone drawn yet" during Five-Card Draw's draw phase.
   */
  boolean isCurrentPhaseComplete(Deal deal, List<Player> dealtInPlayers);

  /**
   * What the Dealer should do once {@link #isCurrentPhaseComplete} is true (or everyone but one
   * player has folded) to advance the deal - reveal board cards, insert a phase-boundary marker, or
   * trigger a showdown.
   */
  Action nextPhaseAction(Game game, Deal deal, List<Player> dealtInPlayers, boolean foldedOut);
}
