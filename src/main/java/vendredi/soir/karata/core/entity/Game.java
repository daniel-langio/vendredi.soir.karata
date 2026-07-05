package vendredi.soir.karata.core.entity;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import vendredi.soir.karata.core.action.Action;
import vendredi.soir.karata.core.action.AwardPot;
import vendredi.soir.karata.core.action.Bet;
import vendredi.soir.karata.core.action.BigBlind;
import vendredi.soir.karata.core.action.Call;
import vendredi.soir.karata.core.action.InitializePlayerChips;
import vendredi.soir.karata.core.action.PlayerAction;
import vendredi.soir.karata.core.action.Raise;
import vendredi.soir.karata.core.action.SmallBlind;
import vendredi.soir.karata.core.rules.Rules;

@Getter
public class Game {
  private final List<Player> players;
  private final Dealer dealer;
  private final Rules rules;
  private final List<Deal> deals;
  private final List<Action> history;

  public Game(List<Player> players, Rules rules) {
    this.players = new ArrayList<>(players);
    this.rules = rules;
    this.dealer = new Dealer(rules);
    this.deals = new ArrayList<>();
    this.history = new ArrayList<>();
  }

  public void addAction(Action action) {
    this.history.add(action);
  }

  public Deal startNewDeal(Deck deck) {
    Deal deal = new Deal(deck);
    deals.add(deal);
    return deal;
  }

  public Deal getCurrentDeal() {
    if (deals.isEmpty()) {
      return null;
    }
    return deals.get(deals.size() - 1);
  }

  public long getChips(Player player) {
    long chips = 0;

    // 1. Initial chips from game history
    chips +=
        history.stream()
            .filter(a -> a instanceof InitializePlayerChips ipc && ipc.player().equals(player))
            .mapToLong(a -> ((InitializePlayerChips) a).amount())
            .sum();

    // 2. Adjustments from all deals
    for (Deal deal : deals) {
      chips +=
          deal.getHistory().stream()
              .mapToLong(
                  a -> {
                    if (a instanceof PlayerAction pa && pa.player().equals(player)) {
                      if (a instanceof Bet bet) return -bet.amount();
                      if (a instanceof Raise raise) return -raise.amount();
                      if (a instanceof Call call) return -call.amount();
                      if (a instanceof SmallBlind sb) return -sb.amount();
                      if (a instanceof BigBlind bb) return -bb.amount();
                    }
                    if (a instanceof AwardPot ap && ap.winner().equals(player)) {
                      return ap.amount();
                    }
                    return 0L;
                  })
              .sum();
    }

    return chips;
  }
}
