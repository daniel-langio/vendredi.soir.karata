package vendredi.soir.karata.core.rules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vendredi.soir.karata.core.entity.Card;
import vendredi.soir.karata.core.entity.Deal;
import vendredi.soir.karata.core.entity.Hand;
import vendredi.soir.karata.core.entity.Player;
import vendredi.soir.karata.core.factory.HandFactory;

/**
 * No-Limit Omaha: identical to {@link TexasHoldemRules} in every way except how many hole cards are
 * dealt (4, not 2) and how a hand is evaluated (best 5-card hand using exactly 2 hole cards and
 * exactly 3 board cards, not "any 5 of the 7 available" the way Hold'em allows). Traditional Omaha
 * cash games are pot-limit, not no-limit - this engine only has one no-limit betting engine (see
 * {@link TexasHoldemRules#isActionLegal}), so this variant is deliberately No-Limit Omaha rather
 * than claiming a pot-limit cap it doesn't actually enforce.
 */
public class OmahaRules extends TexasHoldemRules {

  @Override
  public int holeCardCount() {
    return 4;
  }

  @Override
  public Map<Player, Hand> evaluateShowdownHands(Deal deal, List<Player> players) {
    Map<Player, Hand> bestHands = new HashMap<>();
    for (Player p : players) {
      if (!deal.hasFolded(p)) {
        List<Card> holeCards = deal.getHoleCards(p);
        List<Card> board = deal.getBoard();
        if (holeCards.size() >= 2 && board.size() >= 3) {
          bestHands.put(p, HandFactory.evaluateBestOmahaHand(holeCards, board));
        }
      }
    }
    return bestHands;
  }
}
