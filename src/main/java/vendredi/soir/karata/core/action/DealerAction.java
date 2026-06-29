package vendredi.soir.karata.core.action;

public sealed interface DealerAction extends Action
    permits ShuffleDeck, DealHoleCard, RevealCards, AwardPot {
}
