package vendredi.soir.karata.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

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
    dealer.execute(deal, new DealerAction.ShuffleDeck());

    Card aliceCard1 = deal.getDeck().draw();
    dealer.execute(deal, new DealerAction.DealHoleCard(alice, aliceCard1));
    Card aliceCard2 = deal.getDeck().draw();
    dealer.execute(deal, new DealerAction.DealHoleCard(alice, aliceCard2));

    Card bobCard1 = deal.getDeck().draw();
    dealer.execute(deal, new DealerAction.DealHoleCard(bob, bobCard1));
    Card bobCard2 = deal.getDeck().draw();
    dealer.execute(deal, new DealerAction.DealHoleCard(bob, bobCard2));

    assertEquals(2, deal.getHoleCards(alice).size());
    assertEquals(2, deal.getHoleCards(bob).size());

    // 4. Betting round
    dealer.execute(deal, new PlayerAction.Bet(alice, 100));
    dealer.execute(deal, new PlayerAction.Call(bob, 100));

    assertEquals(200, deal.getTotalPot());
    assertEquals(900, alice.getChips());
    assertEquals(900, bob.getChips());
    assertEquals(100, deal.getContribution(alice));

    // 5. Flop
    List<Card> flop = List.of(deal.getDeck().draw(), deal.getDeck().draw(), deal.getDeck().draw());
    dealer.execute(deal, new DealerAction.RevealCards(flop));
    assertEquals(3, deal.getBoard().size());

    // 6. Finalize and Award Pot
    Player winner = alice; // Mocked winner
    dealer.execute(deal, new DealerAction.AwardPot(winner, deal.getTotalPot()));

    assertEquals(1100, alice.getChips());
    assertEquals(900, bob.getChips());
    assertTrue(deal.getHistory().get(deal.getHistory().size() - 1) instanceof DealerAction.AwardPot);
  }
}
