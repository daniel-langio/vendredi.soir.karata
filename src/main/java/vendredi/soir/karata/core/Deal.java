package vendredi.soir.karata.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

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
    applyActionEffects(action);
  }

  private void applyActionEffects(Action action) {
    if (action instanceof PlayerAction pa) {
      if (pa instanceof PlayerAction.Bet bet) {
        bet.player().removeChips(bet.amount());
      } else if (pa instanceof PlayerAction.Raise raise) {
        raise.player().removeChips(raise.amount());
      } else if (pa instanceof PlayerAction.Call call) {
        call.player().removeChips(call.amount());
      }
    } else if (action instanceof DealerAction da) {
      if (da instanceof DealerAction.AwardPot ap) {
        ap.winner().addChips(ap.amount());
      }
    }
  }

  // Projections
  public List<Card> getHoleCards(Player player) {
    return history.stream()
        .filter(a -> a instanceof DealerAction.DealHoleCard dhc && dhc.player().equals(player))
        .map(a -> ((DealerAction.DealHoleCard) a).card())
        .collect(Collectors.toList());
  }

  public boolean hasFolded(Player player) {
    return history.stream()
        .anyMatch(a -> a instanceof PlayerAction.Fold f && f.player().equals(player));
  }

  public long getContribution(Player player) {
    return history.stream()
        .filter(a -> a instanceof PlayerAction pa && pa.player().equals(player))
        .mapToLong(
            a -> {
              if (a instanceof PlayerAction.Bet bet) return bet.amount();
              if (a instanceof PlayerAction.Raise raise) return raise.amount();
              if (a instanceof PlayerAction.Call call) return call.amount();
              return 0L;
            })
        .sum();
  }

  public long getTotalPot() {
    return history.stream()
        .mapToLong(
            a -> {
              if (a instanceof PlayerAction.Bet bet) return bet.amount();
              if (a instanceof PlayerAction.Raise raise) return raise.amount();
              if (a instanceof PlayerAction.Call call) return call.amount();
              return 0L;
            })
        .sum();
  }

  public List<Card> getBoard() {
    return history.stream()
        .filter(a -> a instanceof DealerAction.RevealCards)
        .flatMap(a -> ((DealerAction.RevealCards) a).cards().stream())
        .collect(Collectors.toList());
  }
}
