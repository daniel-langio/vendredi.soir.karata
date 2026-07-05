package vendredi.soir.karata.core.action;

import vendredi.soir.karata.core.entity.Player;

public sealed interface PlayerAction extends Action
    permits Fold, Check, Call, Bet, Raise, SmallBlind, BigBlind {
  Player getPlayer();
}
