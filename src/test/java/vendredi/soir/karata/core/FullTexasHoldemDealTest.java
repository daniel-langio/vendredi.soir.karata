package vendredi.soir.karata.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import vendredi.soir.karata.core.action.*;
import vendredi.soir.karata.core.entity.*;
import vendredi.soir.karata.core.rules.*;

class FullTexasHoldemDealTest {

  @Test
  void testFullTexasHoldemDealWithAutomaticShowdown() {
    // 1. Setup Game and Players
    Player alice = new Player("Alice");
    Player bob = new Player("Bob");
    Rules rules = new TexasHoldemRules();
    Game game = new Game(List.of(alice, bob), rules);
    Dealer dealer = game.getDealer();

    // 2. Initialize stacks
    dealer.execute(game, new InitializePlayerChips(alice, 1000));
    dealer.execute(game, new InitializePlayerChips(bob, 1000));

    // 3. Start Deal
    Deal deal = game.startNewDeal(Deck.CLASSIC);
    dealer.execute(game, deal, new ShuffleDeck());

    // 4. Blinds
    dealer.execute(game, deal, new SmallBlind(alice, 10));
    dealer.execute(game, deal, new BigBlind(bob, 20));

    // 5. Pre-flop Deal
    dealer.execute(game, deal, new DealHoleCard(alice, Card.CLUB_ACE));
    dealer.execute(game, deal, new DealHoleCard(alice, Card.SPADE_ACE));
    dealer.execute(game, deal, new DealHoleCard(bob, Card.HEART_TWO));
    dealer.execute(game, deal, new DealHoleCard(bob, Card.DIAMOND_SEVEN));

    // 6. Pre-flop Betting
    dealer.execute(game, deal, new Call(alice, 10)); // Alice calls big blind (total 20)
    dealer.execute(game, deal, new Check(bob)); // Bob checks

    // 7. Flop
    dealer.execute(
        game, deal, new RevealCards(List.of(Card.CLUB_TWO, Card.CLUB_THREE, Card.CLUB_FOUR)));

    // 8. Flop Betting
    dealer.execute(game, deal, new Check(alice));
    dealer.execute(game, deal, new Check(bob));

    // 9. Turn
    dealer.execute(game, deal, new RevealCards(List.of(Card.DIAMOND_ACE))); // Alice hits trips

    // 10. Turn Betting
    dealer.execute(game, deal, new Bet(alice, 50));
    dealer.execute(game, deal, new Call(bob, 50));

    // 11. River
    dealer.execute(game, deal, new RevealCards(List.of(Card.HEART_ACE))); // Alice hits quads!

    // 12. River Betting
    dealer.execute(game, deal, new Bet(alice, 100));
    dealer.execute(game, deal, new Call(bob, 100));

    assertEquals(340, deal.getTotalPot());

    // 13. Automatic Showdown
    dealer.execute(game, deal, new Showdown());

    // 14. Final Verification
    // Alice wins with Quads.
    // Chips replayed:
    // Alice: 1000 (init) - 10 (sb) - 10 (call) - 50 (bet) - 100 (bet) + 340 (pot) = 1170
    // Bob: 1000 (init) - 20 (bb) - 0 (check) - 50 (call) - 100 (call) = 830
    assertEquals(1170, game.getChips(alice), "Alice should have 1170 chips");
    assertEquals(830, game.getChips(bob), "Bob should have 830 chips");
    assertEquals(2000, game.getChips(alice) + game.getChips(bob));

    assertTrue(
        deal.getHistory().stream().anyMatch(a -> a instanceof AwardPot),
        "AwardPot action should be present");
  }
}
