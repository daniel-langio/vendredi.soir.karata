package vendredi.soir.karata.core.action;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import vendredi.soir.karata.core.entity.Card;

@Getter
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public final class RevealCards implements DealerAction {
  private final List<Card> cards;
}
