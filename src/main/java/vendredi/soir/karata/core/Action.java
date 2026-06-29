package vendredi.soir.karata.core;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

public sealed interface Action permits PlayerAction, DealerAction {

  @Getter
  @Accessors(fluent = true)
  @RequiredArgsConstructor
  final class Fold implements PlayerAction {
    private final Player player;
  }

  @Getter
  @Accessors(fluent = true)
  @RequiredArgsConstructor
  final class Check implements PlayerAction {
    private final Player player;
  }

  @Getter
  @Accessors(fluent = true)
  final class Call implements PlayerAction {
    private final Player player;
    private final long amount;

    public Call(Player player, long amount) {
      if (amount <= 0) {
        throw new IllegalArgumentException("Call amount must be positive");
      }
      this.player = player;
      this.amount = amount;
    }
  }

  @Getter
  @Accessors(fluent = true)
  final class Bet implements PlayerAction {
    private final Player player;
    private final long amount;

    public Bet(Player player, long amount) {
      if (amount <= 0) {
        throw new IllegalArgumentException("Bet amount must be positive");
      }
      this.player = player;
      this.amount = amount;
    }
  }

  @Getter
  @Accessors(fluent = true)
  final class Raise implements PlayerAction {
    private final Player player;
    private final long amount;

    public Raise(Player player, long amount) {
      if (amount <= 0) {
        throw new IllegalArgumentException("Raise amount must be positive");
      }
      this.player = player;
      this.amount = amount;
    }
  }
}

sealed interface PlayerAction extends Action
    permits Action.Fold, Action.Check, Action.Call, Action.Bet, Action.Raise {
  Player player();
}

sealed interface DealerAction extends Action
    permits DealerAction.Shuffle, DealerAction.Deal, DealerAction.Reveal {

  @Getter
  @Accessors(fluent = true)
  @RequiredArgsConstructor
  final class Shuffle implements DealerAction {}

  @Getter
  @Accessors(fluent = true)
  @RequiredArgsConstructor
  final class Deal implements DealerAction {
    private final Player player;
    private final Card card;
  }

  @Getter
  @Accessors(fluent = true)
  @RequiredArgsConstructor
  final class Reveal implements DealerAction {
    private final List<Card> cards;
  }
}
