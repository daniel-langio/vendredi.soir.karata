package vendredi.soir.karata.core.entity;

import static org.junit.jupiter.api.Assertions.*;
import static vendredi.soir.karata.core.entity.Card.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import vendredi.soir.karata.core.action.AdvancePhase;
import vendredi.soir.karata.core.action.DealHoleCard;
import vendredi.soir.karata.core.action.Draw;
import vendredi.soir.karata.core.action.Showdown;
import vendredi.soir.karata.core.rules.FiveCardDrawRules;

class DealDrawTest {

  @Test
  void discarding_and_redrawing_replaces_exactly_the_discarded_cards() {
    Deal deal = new Deal(Deck.CLASSIC);
    Player alice = new Player("alice");

    List<Card> original = List.of(CLUB_TWO, CLUB_FIVE, CLUB_NINE, CLUB_KING, HEART_TWO);
    original.forEach(c -> deal.apply(new DealHoleCard(alice, c)));
    assertEquals(original, deal.getHoleCards(alice), "should have exactly the 5 dealt cards");

    // Discard 2, get 2 replacements - same mechanism DealService.takeAction uses.
    deal.apply(new Draw(alice, List.of(CLUB_TWO, HEART_TWO)));
    deal.apply(new DealHoleCard(alice, DIAMOND_ACE));
    deal.apply(new DealHoleCard(alice, SPADE_ACE));

    List<Card> afterDraw = deal.getHoleCards(alice);
    assertEquals(5, afterDraw.size(), "hand size must stay exactly 5 after a draw");
    assertTrue(
        afterDraw.containsAll(List.of(CLUB_FIVE, CLUB_NINE, CLUB_KING, DIAMOND_ACE, SPADE_ACE)));
    assertFalse(afterDraw.contains(CLUB_TWO), "discarded card must be gone");
    assertFalse(afterDraw.contains(HEART_TWO), "discarded card must be gone");
  }

  @Test
  void five_card_draw_phase_advances_through_pre_draw_draw_post_draw_showdown() {
    Deal deal = new Deal(Deck.CLASSIC);
    Player alice = new Player("alice");
    Player bob = new Player("bob");
    List<Player> players = List.of(alice, bob);
    FiveCardDrawRules rules = new FiveCardDrawRules();

    assertEquals("PRE_DRAW", rules.currentPhase(deal, players));

    deal.apply(new AdvancePhase());
    assertEquals("DRAW", rules.currentPhase(deal, players));

    deal.apply(new AdvancePhase());
    assertEquals("POST_DRAW", rules.currentPhase(deal, players));

    deal.apply(new Showdown());
    assertEquals("SHOWDOWN", rules.currentPhase(deal, players));
  }
}
