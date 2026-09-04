package vendredi.soir.karata.endpoint.rest.mapper;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import vendredi.soir.karata.core.action.AwardPot;
import vendredi.soir.karata.core.entity.Card;
import vendredi.soir.karata.core.entity.Deal;
import vendredi.soir.karata.core.entity.Player;
import vendredi.soir.karata.core.factory.HandFactory;
import vendredi.soir.karata.endpoint.rest.model.*;
import vendredi.soir.karata.repository.model.poker.GameEntity;

@Component
public class RestMapper {
  public PlayerInfo toRest(Player player, vendredi.soir.karata.core.entity.Game game) {
    Deal deal = game.getCurrentDeal();
    PlayerStatus status = PlayerStatus.ACTIVE;
    long contribution = 0L;
    BlindRole blind = null;
    if (deal != null) {
      if (deal.hasFolded(player)) {
        status = PlayerStatus.FOLDED;
      } else if (deal.isAllIn(player, game)) {
        status = PlayerStatus.ALL_IN;
      }
      contribution = deal.getPlayerRoundContribution(player);
      if (deal.isSmallBlind(player)) {
        blind = BlindRole.SMALL;
      } else if (deal.isBigBlind(player)) {
        blind = BlindRole.BIG;
      }
    }
    return new PlayerInfo(
        UUID.nameUUIDFromBytes(player.getName().getBytes()),
        player.getName(),
        game.getChips(player),
        status,
        contribution,
        blind);
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
        activePlayer != null ? UUID.nameUUIDFromBytes(activePlayer.getName().getBytes()) : null,
        deal.getCurrentRoundBet(),
        outcome(deal, game));
  }

  public Game toRest(
      vendredi.soir.karata.core.entity.Game game, GameEntity entity, String requestingUsername) {
    UUID currentDealId = game.getCurrentDealId();
    return new Game(
        entity.getId(),
        entity.getName(),
        new Blinds(entity.getSmallBlind(), entity.getBigBlind()),
        game.getPlayers().stream().map(p -> toRest(p, game)).collect(Collectors.toList()),
        new ArrayList<>(),
        currentDealId,
        toRest(game.getCurrentDeal(), currentDealId, game),
        you(game, requestingUsername));
  }

  private YouState you(vendredi.soir.karata.core.entity.Game game, String requestingUsername) {
    if (requestingUsername == null) return null;
    Deal deal = game.getCurrentDeal();
    if (deal == null) return null;
    Player caller =
        game.getPlayers().stream()
            .filter(p -> p.getName().equals(requestingUsername))
            .findFirst()
            .orElse(null);
    if (caller == null) return null;

    long currentRoundBet = deal.getCurrentRoundBet();
    long callerContribution = deal.getPlayerRoundContribution(caller);
    long callAmount = Math.max(0, currentRoundBet - callerContribution);
    long minRaise = game.getRules().getMinimumRaise(deal);
    long maxRaise = game.getChips(caller);
    return new YouState(callAmount, minRaise, maxRaise);
  }

  private DealOutcome outcome(Deal deal, vendredi.soir.karata.core.entity.Game game) {
    List<AwardPot> awards =
        deal.getHistory().stream()
            .filter(a -> a instanceof AwardPot)
            .map(a -> (AwardPot) a)
            .toList();
    if (awards.isEmpty()) return null;

    // A real (card-based) showdown only happens when more than one player is still active;
    // otherwise the pot was won uncontested by everyone else folding, and no hand is shown.
    boolean realShowdown = game.getPlayers().stream().filter(p -> !deal.hasFolded(p)).count() > 1;

    List<WinnerInfo> winners =
        awards.stream()
            .map(
                ap -> {
                  Player winner = ap.getWinner();
                  String handRank = null;
                  if (realShowdown) {
                    List<Card> allCards = new ArrayList<>(deal.getHoleCards(winner));
                    allCards.addAll(deal.getBoard());
                    handRank = HandFactory.evaluateBestHand(allCards).describe();
                  }
                  return new WinnerInfo(
                      UUID.nameUUIDFromBytes(winner.getName().getBytes()),
                      winner.getName(),
                      ap.getAmount(),
                      handRank);
                })
            .toList();
    return new DealOutcome(winners);
  }
}
