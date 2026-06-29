package vendredi.soir.karata.core;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class Round {
  private final List<Player> participants;
  private final Deck deck;
  private final List<Card> communityCards;
  private final List<PlayerAction> actions;
  private long pot;

  public Round(List<Player> participants, Deck deck) {
    this.participants = new ArrayList<>(participants);
    this.deck = deck;
    this.communityCards = new ArrayList<>();
    this.actions = new ArrayList<>();
    this.pot = 0;
  }

  public void addToPot(long amount) {
    this.pot += amount;
  }

  public void addCommunityCard(Card card) {
    this.communityCards.add(card);
  }

  public void recordAction(PlayerAction action) {
    this.actions.add(action);
  }

  public List<Player> getActivePlayers() {
    return participants.stream().filter(p -> !hasFolded(p)).toList();
  }

  public boolean hasFolded(Player player) {
    return actions.stream()
        .anyMatch(a -> a.player().equals(player) && a instanceof PlayerAction.Fold);
  }
}
