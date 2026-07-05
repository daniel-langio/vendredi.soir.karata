package vendredi.soir.karata.endpoint.rest.model;

import java.util.List;
import java.util.UUID;

public record Game(
    UUID gameId,
    String name,
    Blinds blinds,
    List<PlayerInfo> players,
    List<UUID> dealHistory,
    UUID currentDealId,
    DealState currentDeal) {}
