package vendredi.soir.karata.core.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vendredi.soir.karata.core.entity.Card.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import vendredi.soir.karata.core.entity.HandCategory;

class HandFactoryTest {

  @Test
  void omaha_does_not_award_a_flush_from_4_suited_hole_cards_with_only_1_matching_board_card() {
    // The classic Omaha beginner mistake: 4 clubs in hand looks like a flush draw, but Omaha
    // requires exactly 2 hole + exactly 3 board cards - with only 1 club on the board, at most
    // 2 hole clubs + 1 board club (3 total) can ever be used together, never a 5-card flush.
    // None of the 9 ranks below repeat and no 5 of them are consecutive, so the honest best
    // Omaha hand here is plain high card.
    var hole = List.of(CLUB_TWO, CLUB_FIVE, CLUB_NINE, CLUB_KING);
    var board = List.of(CLUB_ACE, HEART_SEVEN, DIAMOND_JACK, SPADE_THREE, HEART_TEN);

    assertEquals(HandCategory.HIGH_CARD, HandFactory.evaluateBestOmahaHand(hole, board).getType());

    // Sanity check that this scenario really is the trap it claims to be: naively treating all
    // 9 cards as one pool (the Hold'em rule) DOES find a flush (5 clubs are present overall) -
    // proving evaluateBestOmahaHand's constraint is actually doing something, not a no-op.
    var allNine = new java.util.ArrayList<>(hole);
    allNine.addAll(board);
    assertEquals(HandCategory.FLUSH, HandFactory.evaluateBestHand(allNine).getType());
  }
}
