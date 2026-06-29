package vendredi.soir.karata.core;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Dealer {
  private final Game game;
  private final Rules rules;

  public void shuffle() {
    game.getDeck().shuffle();
    recordAction(new DealerAction.Shuffle());
  }

  public void deal(Player player) {
    Card card = game.getDeck().draw();
    player.receiveCard(card);
    recordAction(new DealerAction.Deal(player, card));
  }

  public void reveal(int count) {
    List<Card> cards = new java.util.ArrayList<>();
    for (int i = 0; i < count; i++) {
      cards.add(game.getDeck().draw());
    }
    game.getCommunityCards().addAll(cards);
    recordAction(new DealerAction.Reveal(cards));
  }

  public void executeAction(Action action) {
    if (!rules.isActionLegal(game, action)) {
      throw new IllegalArgumentException("Illegal action");
    }
    if (action instanceof PlayerAction pa) {
      handlePlayerAction(pa);
    }
    recordAction(action);
  }

  private void handlePlayerAction(PlayerAction pa) {
    if (pa instanceof PlayerAction.Bet bet) {
      bet.player().removeChips(bet.amount());
      game.addToMainPot(bet.amount());
    } else if (pa instanceof PlayerAction.Raise raise) {
      raise.player().removeChips(raise.amount());
      game.addToMainPot(raise.amount());
    } else if (pa instanceof PlayerAction.Call call) {
      call.player().removeChips(call.amount());
      game.addToMainPot(call.amount());
    }
  }

  private void recordAction(Action action) {
    game.addAction(action);
  }

  public void awardPot() {
    var winners = rules.evaluateWinners(game);
    // Simplified: award main pot to first winner for now
    if (!winners.isEmpty()) {
      Player winner = winners.keySet().iterator().next();
      winner.addChips(game.getMainPot());
      game.getPots().set(0, 0L);
    }
  }
}
