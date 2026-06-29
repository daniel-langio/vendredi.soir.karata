package vendredi.soir.karata.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class PokerArchitectureTest {

  @Test
  void testNewPokerArchitecture() {
    // 1. Setup
    Player player1 = new Player("Alice", 1000);
    Player player2 = new Player("Bob", 1000);
    Deck deck = new Deck(Deck.CLASSIC.getCards());
    Game game = new Game(List.of(player1, player2), deck);
    Rules rules = new TexasHoldemRules();
    Dealer dealer = new Dealer(game, rules);

    // 2. Initial actions
    dealer.shuffle();
    dealer.deal(player1);
    dealer.deal(player1);
    dealer.deal(player2);
    dealer.deal(player2);

    assertEquals(5, game.getHistory().size());
    assertTrue(game.getHistory().get(0) instanceof DealerAction.Shuffle);

    // 3. Player actions
    dealer.executeAction(new PlayerAction.Bet(player1, 100));
    dealer.executeAction(new PlayerAction.Call(player2, 100));

    assertEquals(200, game.getMainPot());
    assertEquals(900, player1.getChips());
    assertEquals(900, player2.getChips());

    // 4. More dealer actions
    dealer.reveal(3); // Flop
    dealer.reveal(1); // Turn
    dealer.reveal(1); // River

    assertEquals(5, game.getCommunityCards().size());
    assertEquals(10, game.getHistory().size());

    // 5. End of game
    dealer.awardPot();

    assertTrue(player1.getChips() > 900 || player2.getChips() > 900);
    assertEquals(2000, player1.getChips() + player2.getChips());
    assertEquals(0, game.getMainPot());
  }
}
