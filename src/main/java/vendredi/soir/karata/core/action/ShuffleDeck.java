package vendredi.soir.karata.core.action;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vendredi.soir.karata.core.entity.Card;

@Getter
@RequiredArgsConstructor
public final class ShuffleDeck implements DealerAction {
  private final List<Card> cards;
}
