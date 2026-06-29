package vendredi.soir.karata.core.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import vendredi.soir.karata.core.action.Action;
import vendredi.soir.karata.core.action.DealerAction;
import vendredi.soir.karata.core.action.PlayerAction;
import vendredi.soir.karata.core.action.Bet;
import vendredi.soir.karata.core.action.Raise;
import vendredi.soir.karata.core.action.Call;
import vendredi.soir.karata.core.action.Fold;
import vendredi.soir.karata.core.action.DealHoleCard;
import vendredi.soir.karata.core.action.RevealCards;

@Getter
public class Deal {
  private final Deck deck;
  private final List<Action> history;

  public Deal(Deck deck) {
    this.deck = new Deck(new ArrayList<>(deck.getCards()));
    this.history = new ArrayList<>();
  }

  public void apply(Action action) {
    history.add(action);
  }

  // Projections
  public List<Card> getHoleCards(Player player) {
    return history.stream()
        .filter(a -> a instanceof DealHoleCard dhc && dhc.player().equals(player))
        .map(a -> ((DealHoleCard) a).card())
        .collect(Collectors.toList());
  }

  public boolean hasFolded(Player player) {
    return history.stream()
        .anyMatch(a -> a instanceof Fold f && f.player().equals(player));
  }

  public long getContribution(Player player) {
    return history.stream()
        .filter(a -> a instanceof PlayerAction pa && pa.player().equals(player))
        .mapToLong(
            a -> {
              if (a instanceof Bet bet) return bet.amount();
              if (a instanceof Raise raise) return raise.amount();
              if (a instanceof Call call) return call.amount();
              return 0L;
            })
        .sum();
  }

  public long getTotalPot() {
    return history.stream()
        .mapToLong(
            a -> {
              if (a instanceof Bet bet) return bet.amount();
              if (a instanceof Raise raise) return raise.amount();
              if (a instanceof Call call) return call.amount();
              return 0L;
            })
        .sum();
  }

  public List<Card> getBoard() {
    return history.stream()
        .filter(a -> a instanceof RevealCards)
        .flatMap(a -> ((RevealCards) a).cards().stream())
        .collect(Collectors.toList());
  }
}
