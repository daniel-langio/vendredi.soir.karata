package vendredi.soir.karata.core.entity;

import static vendredi.soir.karata.core.entity.Rank.*;
import static vendredi.soir.karata.core.entity.Suit.*;

public record Card(Suit suit, Rank rank) {
  public static final Card CLUB_TWO = new Card(CLUBS, TWO);
  public static final Card CLUB_THREE = new Card(CLUBS, THREE);
  public static final Card CLUB_FOUR = new Card(CLUBS, FOUR);
  public static final Card CLUB_FIVE = new Card(CLUBS, FIVE);
  public static final Card CLUB_SIX = new Card(CLUBS, SIX);
  public static final Card CLUB_SEVEN = new Card(CLUBS, SEVEN);
  public static final Card CLUB_EIGHT = new Card(CLUBS, EIGHT);
  public static final Card CLUB_NINE = new Card(CLUBS, NINE);
  public static final Card CLUB_TEN = new Card(CLUBS, TEN);
  public static final Card CLUB_JACK = new Card(CLUBS, JACK);
  public static final Card CLUB_QUEEN = new Card(CLUBS, QUEEN);
  public static final Card CLUB_KING = new Card(CLUBS, KING);
  public static final Card CLUB_ACE = new Card(CLUBS, ACE);

  public static final Card DIAMOND_TWO = new Card(DIAMONDS, TWO);
  public static final Card DIAMOND_THREE = new Card(DIAMONDS, THREE);
  public static final Card DIAMOND_FOUR = new Card(DIAMONDS, FOUR);
  public static final Card DIAMOND_FIVE = new Card(DIAMONDS, FIVE);
  public static final Card DIAMOND_SIX = new Card(DIAMONDS, SIX);
  public static final Card DIAMOND_SEVEN = new Card(DIAMONDS, SEVEN);
  public static final Card DIAMOND_EIGHT = new Card(DIAMONDS, EIGHT);
  public static final Card DIAMOND_NINE = new Card(DIAMONDS, NINE);
  public static final Card DIAMOND_TEN = new Card(DIAMONDS, TEN);
  public static final Card DIAMOND_JACK = new Card(DIAMONDS, JACK);
  public static final Card DIAMOND_QUEEN = new Card(DIAMONDS, QUEEN);
  public static final Card DIAMOND_KING = new Card(DIAMONDS, KING);
  public static final Card DIAMOND_ACE = new Card(DIAMONDS, ACE);

  public static final Card HEART_TWO = new Card(HEARTS, TWO);
  public static final Card HEART_THREE = new Card(HEARTS, THREE);
  public static final Card HEART_FOUR = new Card(HEARTS, FOUR);
  public static final Card HEART_FIVE = new Card(HEARTS, FIVE);
  public static final Card HEART_SIX = new Card(HEARTS, SIX);
  public static final Card HEART_SEVEN = new Card(HEARTS, SEVEN);
  public static final Card HEART_EIGHT = new Card(HEARTS, EIGHT);
  public static final Card HEART_NINE = new Card(HEARTS, NINE);
  public static final Card HEART_TEN = new Card(HEARTS, TEN);
  public static final Card HEART_JACK = new Card(HEARTS, JACK);
  public static final Card HEART_QUEEN = new Card(HEARTS, QUEEN);
  public static final Card HEART_KING = new Card(HEARTS, KING);
  public static final Card HEART_ACE = new Card(HEARTS, ACE);

  public static final Card SPADE_TWO = new Card(SPADES, TWO);
  public static final Card SPADE_THREE = new Card(SPADES, THREE);
  public static final Card SPADE_FOUR = new Card(SPADES, FOUR);
  public static final Card SPADE_FIVE = new Card(SPADES, FIVE);
  public static final Card SPADE_SIX = new Card(SPADES, SIX);
  public static final Card SPADE_SEVEN = new Card(SPADES, SEVEN);
  public static final Card SPADE_EIGHT = new Card(SPADES, EIGHT);
  public static final Card SPADE_NINE = new Card(SPADES, NINE);
  public static final Card SPADE_TEN = new Card(SPADES, TEN);
  public static final Card SPADE_JACK = new Card(SPADES, JACK);
  public static final Card SPADE_QUEEN = new Card(SPADES, QUEEN);
  public static final Card SPADE_KING = new Card(SPADES, KING);
  public static final Card SPADE_ACE = new Card(SPADES, ACE);

  @Override
  public String toString() {
    return rank + "" + suit;
  }
}
