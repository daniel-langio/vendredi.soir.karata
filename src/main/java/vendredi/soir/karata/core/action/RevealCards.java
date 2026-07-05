package vendredi.soir.karata.core.action;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import vendredi.soir.karata.core.entity.Card;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public final class RevealCards implements DealerAction {
  private final List<Card> cards;
}
