package vendredi.soir.karata.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

public sealed interface PlayerAction
    permits PlayerAction.Fold,
        PlayerAction.Check,
        PlayerAction.Call,
        PlayerAction.Bet,
        PlayerAction.Raise {

  Player player();

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
