package vendredi.soir.karata.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import vendredi.soir.karata.core.action.DealerAction;
import vendredi.soir.karata.core.action.PlayerAction;
import vendredi.soir.karata.core.action.ShuffleDeck;
import vendredi.soir.karata.core.action.DealHoleCard;
import vendredi.soir.karata.core.action.RevealCards;
import vendredi.soir.karata.core.action.AwardPot;
import vendredi.soir.karata.core.action.Bet;
import vendredi.soir.karata.core.action.Call;
import vendredi.soir.karata.core.entity.Card;
import vendredi.soir.karata.core.entity.Deal;
import vendredi.soir.karata.core.entity.Dealer;
import vendredi.soir.karata.core.entity.Deck;
import vendredi.soir.karata.core.entity.Game;
import vendredi.soir.karata.core.entity.Player;
import vendredi.soir.karata.core.rules.Rules;
import vendredi.soir.karata.core.rules.TexasHoldemRules;

class EventSourcedPokerTest {

  @Test
  void testEventSourcedDealFlow() {
    // 1. Setup
    Player alice = new Player("Alice", 1000);
    Player bob = new Player("Bob", 1000);
    Rules rules = new TexasHoldemRules();
    Game game = new Game(List.of(alice, bob), rules);
    Dealer dealer = game.getDealer();

    // 2. Start a new deal
    Deal deal = game.startNewDeal(Deck.CLASSIC);

    // 3. Shuffle and Deal
    dealer.execute(deal, new ShuffleDeck());

    Card aliceCard1 = deal.getDeck().draw();
    dealer.execute(deal, new DealHoleCard(alice, aliceCard1));
    Card aliceCard2 = deal.getDeck().draw();
    dealer.execute(deal, new DealHoleCard(alice, aliceCard2));

    Card bobCard1 = deal.getDeck().draw();
    dealer.execute(deal, new DealHoleCard(bob, bobCard1));
    Card bobCard2 = deal.getDeck().draw();
    dealer.execute(deal, new DealHoleCard(bob, bobCard2));

    assertEquals(2, deal.getHoleCards(alice).size());
    assertEquals(2, deal.getHoleCards(bob).size());

    // 4. Betting round
    dealer.execute(deal, new Bet(alice, 100));
    dealer.execute(deal, new Call(bob, 100));

    assertEquals(200, deal.getTotalPot());
    assertEquals(900, alice.getChips());
    assertEquals(900, bob.getChips());
    assertEquals(100, deal.getContribution(alice));

    // 5. Flop
    List<Card> flop = List.of(deal.getDeck().draw(), deal.getDeck().draw(), deal.getDeck().draw());
    dealer.execute(deal, new RevealCards(flop));
    assertEquals(3, deal.getBoard().size());

    // 6. Finalize and Award Pot
    Player winner = alice; // Mocked winner
    dealer.execute(deal, new AwardPot(winner, deal.getTotalPot()));

    assertEquals(1100, alice.getChips());
    assertEquals(900, bob.getChips());
    assertTrue(deal.getHistory().get(deal.getHistory().size() - 1) instanceof AwardPot);
  }
}
