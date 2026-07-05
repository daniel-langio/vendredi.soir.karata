package vendredi.soir.karata.core.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Suit {
  CLUBS("C"),
  DIAMONDS("D"),
  HEARTS("H"),
  SPADES("S");

  private final String label;
  public static final Suit[] ALL = values();

  @Override
  public String toString() {
    return label;
  }
}
