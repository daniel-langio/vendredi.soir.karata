package vendredi.soir.karata.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import vendredi.soir.karata.core.factory.HandFactory;

class PokerGameIntegrationTest {

  @Test
  void testPokerRoundFlow() {
    // 1. Setup Table and Players
    Table table = new Table(6);
    Player player1 = new Player("Alice", 1000);
    Player player2 = new Player("Bob", 1000);
    table.join(player1);
    table.join(player2);

    // 2. Start Session and Round
    Session session = new Session(table);
    Deck deck = new Deck(Deck.CLASSIC.getCards());
    deck.shuffle();
    session.startNewRound(deck);
    Round round = session.getCurrentRound();

    // 3. Deal Hole Cards
    player1.receiveCard(deck.draw());
    player1.receiveCard(deck.draw());
    player2.receiveCard(deck.draw());
    player2.receiveCard(deck.draw());

    assertEquals(2, player1.getHoleCards().size());
    assertEquals(2, player2.getHoleCards().size());

    // 4. Betting (Simplified)
    player1.removeChips(100);
    round.addToPot(100);
    player2.removeChips(100);
    round.addToPot(100);

    assertEquals(200, round.getPot());
    assertEquals(900, player1.getChips());
    assertEquals(900, player2.getChips());

    // 5. Deal Community Cards (Flop, Turn, River)
    for (int i = 0; i < 5; i++) {
      round.addCommunityCard(deck.draw());
    }
    assertEquals(5, round.getCommunityCards().size());

    // 6. Determine Winner (Simplified logic)
    List<Card> p1FullHand = new ArrayList<>(player1.getHoleCards());
    p1FullHand.addAll(round.getCommunityCards());
    Hand hand1 = HandFactory.from(new Deck(p1FullHand.subList(0, 5))); // Just pick first 5 for simplicity

    List<Card> p2FullHand = new ArrayList<>(player2.getHoleCards());
    p2FullHand.addAll(round.getCommunityCards());
    Hand hand2 = HandFactory.from(new Deck(p2FullHand.subList(0, 5)));

    Player winner = (hand1.compareTo(hand2) >= 0) ? player1 : player2;
    winner.addChips(round.getPot());

    assertTrue(player1.getChips() > 900 || player2.getChips() > 900);
    assertEquals(2000, player1.getChips() + player2.getChips());
  }
}
