package vendredi.soir.karata.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vendredi.soir.karata.core.action.*;
import vendredi.soir.karata.core.entity.*;
import vendredi.soir.karata.core.rules.*;

class FullTexasHoldemDealTest {

  @Test
  void testFullTexasHoldemDeal() {
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

    assertEquals(10, deal.getPlayerRoundContribution(alice));
    assertEquals(20, deal.getPlayerRoundContribution(bob));
    assertEquals(30, deal.getTotalPot());

    // 5. Pre-flop Deal
    dealer.execute(game, deal, new DealHoleCard(alice, Card.CLUB_ACE));
    dealer.execute(game, deal, new DealHoleCard(alice, Card.SPADE_ACE));
    dealer.execute(game, deal, new DealHoleCard(bob, Card.HEART_TWO));
    dealer.execute(game, deal, new DealHoleCard(bob, Card.DIAMOND_SEVEN));

    // 6. Pre-flop Betting
    dealer.execute(game, deal, new Call(alice, 10)); // Alice calls big blind (adds 10 more to reach 20)
    dealer.execute(game, deal, new Check(bob));    // Bob checks

    assertEquals(40, deal.getTotalPot());

    // 7. Flop
    dealer.execute(game, deal, new RevealCards(List.of(Card.CLUB_TWO, Card.CLUB_THREE, Card.CLUB_FOUR)));

    // 8. Flop Betting
    dealer.execute(game, deal, new Check(alice));
    dealer.execute(game, deal, new Check(bob));

    // 9. Turn
    dealer.execute(game, deal, new RevealCards(List.of(Card.DIAMOND_ACE))); // Alice hits trips

    // 10. Turn Betting
    dealer.execute(game, deal, new Bet(alice, 50));
    dealer.execute(game, deal, new Call(bob, 50));

    assertEquals(140, deal.getTotalPot());

    // 11. River
    dealer.execute(game, deal, new RevealCards(List.of(Card.HEART_ACE))); // Alice hits quads!

    // 12. River Betting
    dealer.execute(game, deal, new Bet(alice, 100));
    dealer.execute(game, deal, new Call(bob, 100));

    assertEquals(340, deal.getTotalPot());

    // 13. Showdown and Winner
    Map<Player, Hand> winners = rules.evaluateWinners(deal, game.getPlayers());
    assertEquals(1, winners.size());
    assertTrue(winners.containsKey(alice));
    assertEquals(HandCategory.FOUR_OF_A_KIND, winners.get(alice).getType());

    // 14. Award Pot
    long pot = deal.getTotalPot();
    dealer.execute(game, deal, new AwardPot(alice, pot));

    // 15. Final Verification
    System.out.println("Alice chips: " + game.getChips(alice));
    System.out.println("Bob chips: " + game.getChips(bob));

    assertEquals(2000, game.getChips(alice) + game.getChips(bob));
  }
}
