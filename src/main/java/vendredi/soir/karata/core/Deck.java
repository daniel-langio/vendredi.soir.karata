package vendredi.soir.karata.core;

import static vendredi.soir.karata.core.Card.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public class Deck {
  public static final Deck ALL_CLUBS =
      new Deck(
          List.of(
              CLUB_TWO,
              CLUB_THREE,
              CLUB_FOUR,
              CLUB_FIVE,
              CLUB_SIX,
              CLUB_SEVEN,
              CLUB_EIGHT,
              CLUB_NINE,
              CLUB_TEN,
              CLUB_JACK,
              CLUB_QUEEN,
              CLUB_KING,
              CLUB_ACE));
  public static final Deck ALL_DIAMONDS =
      new Deck(
          List.of(
              DIAMOND_TWO,
              DIAMOND_THREE,
              DIAMOND_FOUR,
              DIAMOND_FIVE,
              DIAMOND_SEVEN,
              DIAMOND_EIGHT,
              DIAMOND_NINE,
              DIAMOND_TEN,
              DIAMOND_JACK,
              DIAMOND_QUEEN,
              DIAMOND_KING,
              DIAMOND_ACE));
  public static final Deck ALL_HEARTS =
      new Deck(
          List.of(
              HEART_TWO,
              HEART_THREE,
              HEART_FOUR,
              HEART_FIVE,
              HEART_SIX,
              HEART_SEVEN,
              HEART_EIGHT,
              HEART_NINE,
              HEART_TEN,
              HEART_JACK,
              HEART_QUEEN,
              HEART_KING,
              HEART_ACE));
  public static final Deck ALL_SPADES =
      new Deck(
          List.of(
              SPADE_TWO,
              SPADE_THREE,
              SPADE_FOUR,
              SPADE_FIVE,
              SPADE_SIX,
              SPADE_SEVEN,
              SPADE_EIGHT,
              SPADE_NINE,
              SPADE_TEN,
              SPADE_JACK,
              SPADE_QUEEN,
              SPADE_KING,
              SPADE_ACE));

  public static final Deck CLASSIC = Deck.of(ALL_CLUBS, ALL_DIAMONDS, ALL_HEARTS, ALL_SPADES);

  private final List<Card> cards;

  public Deck() {
    this.cards = new ArrayList<>();
  }

  public Deck(List<Card> cards) {
    this.cards = new ArrayList<>(cards);
  }

  public static Deck of(Deck... decksToAdd) {
    var cards = Stream.of(decksToAdd).map(Deck::getCards).flatMap(Collection::stream).toList();

    return new Deck(cards);
  }

  public void shuffle() {
    Collections.shuffle(cards);
  }

  public Card draw() {
    if (cards.isEmpty()) {
      throw new IllegalStateException("Deck is empty");
    }
    return cards.remove(0);
  }

  @Override
  public String toString() {
    return cards.stream().map(Card::toString).reduce("", (a, b) -> a + " " + b);
  }
}
