package vendredi.soir.karata.endpoint.rest.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import vendredi.soir.karata.core.entity.Card;
import vendredi.soir.karata.core.entity.Deal;
import vendredi.soir.karata.core.entity.Game;
import vendredi.soir.karata.core.entity.Player;
import vendredi.soir.karata.endpoint.rest.model.Blinds;
import vendredi.soir.karata.endpoint.rest.model.DealState;
import vendredi.soir.karata.endpoint.rest.model.Phase;
import vendredi.soir.karata.endpoint.rest.model.PlayerInfo;

@Component
public class RestMapper {

  public PlayerInfo toRest(Player player, Game game) {
    return new PlayerInfo(
        UUID.nameUUIDFromBytes(player.getName().getBytes()), // Mock UUID if not in entity
        player.getName(),
        game.getChips(player));
  }

  public DealState toRest(Deal deal, UUID dealId) {
    if (deal == null) return null;

    List<Card> board = deal.getBoard();
    List<String> communityCards = new ArrayList<>(5);
    for (int i = 0; i < 5; i++) {
      if (i < board.size()) {
        communityCards.add(board.get(i).toString());
      } else {
        communityCards.add(null);
      }
    }

    return new DealState(
        dealId,
        communityCards,
        deal.getTotalPot(),
        Phase.valueOf(deal.getCurrentPhase()),
        null // TODO: implement active player detection in Deal core
        );
  }

  public vendredi.soir.karata.endpoint.rest.model.Game toRest(Game game, UUID gameId) {
    List<UUID> dealHistory = new ArrayList<>(); // TODO: persistent IDs

    Deal currentDeal = game.getCurrentDeal();
    UUID currentDealId = currentDeal != null ? UUID.randomUUID() : null; // Mock for now

    return new vendredi.soir.karata.endpoint.rest.model.Game(
        gameId,
        "Texas Hold'em", // TODO: name in Game entity
        new Blinds(10L, 20L), // TODO: blinds in Game entity
        game.getPlayers().stream().map(p -> toRest(p, game)).collect(Collectors.toList()),
        dealHistory,
        currentDealId,
        toRest(currentDeal, currentDealId));
  }
}
