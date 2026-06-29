package vendredi.soir.karata.core;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Game {
  private List<Player> players;
  private Deck deck;
  private List<Long> pots;
  private List<Card> communityCards;
  private List<Action> history;

  public Game(List<Player> players, Deck deck) {
    this.players = new ArrayList<>(players);
    this.deck = deck;
    this.pots = new ArrayList<>(List.of(0L));
    this.communityCards = new ArrayList<>();
    this.history = new ArrayList<>();
  }

  public void addAction(Action action) {
    this.history.add(action);
  }

  public long getMainPot() {
    return pots.get(0);
  }

  public void addToMainPot(long amount) {
    pots.set(0, pots.get(0) + amount);
  }
}
