package vendredi.soir.karata.core;

import static org.junit.jupiter.api.Assertions.*;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class DeckTest {

  @Test
  void classic_deck_to_string() {
    var actual = Deck.CLASSIC.toString();

    assertNotNull(actual);
    log.info(actual);
  }
}
