package vendredi.soir.karata.endpoint.rest.mapper;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import vendredi.soir.karata.core.entity.Deal;
import vendredi.soir.karata.core.entity.Player;
import vendredi.soir.karata.endpoint.rest.model.*;
import vendredi.soir.karata.repository.model.poker.GameEntity;

@Component
public class RestMapper {
  public PlayerInfo toRest(Player player, vendredi.soir.karata.core.entity.Game game) {
    return new PlayerInfo(
        UUID.nameUUIDFromBytes(player.getName().getBytes()),
        player.getName(),
        game.getChips(player));
  }

  public DealState toRest(Deal deal, UUID dealId, vendredi.soir.karata.core.entity.Game game) {
    if (deal == null) return null;
    List<String> communityCards = new ArrayList<>(5);
    for (int i = 0; i < 5; i++)
      communityCards.add(i < deal.getBoard().size() ? deal.getBoard().get(i).toString() : null);
    Player activePlayer = game.getRules().determineNextPlayer(deal, game.getPlayers());
    return new DealState(
        dealId,
        communityCards,
        deal.getTotalPot(),
        Phase.valueOf(deal.getCurrentPhase()),
        activePlayer != null ? UUID.nameUUIDFromBytes(activePlayer.getName().getBytes()) : null);
  }

  public Game toRest(vendredi.soir.karata.core.entity.Game game, GameEntity entity) {
    UUID currentDealId = game.getCurrentDealId();
    return new Game(
        entity.getId(),
        entity.getName(),
        new Blinds(entity.getSmallBlind(), entity.getBigBlind()),
        game.getPlayers().stream().map(p -> toRest(p, game)).collect(Collectors.toList()),
        new ArrayList<>(),
        currentDealId,
        toRest(game.getCurrentDeal(), currentDealId, game));
  }
}
