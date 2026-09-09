package vendredi.soir.karata.endpoint.rest.mapper;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import vendredi.soir.karata.core.action.AwardPot;
import vendredi.soir.karata.core.entity.Card;
import vendredi.soir.karata.core.entity.Deal;
import vendredi.soir.karata.core.entity.Hand;
import vendredi.soir.karata.core.entity.Player;
import vendredi.soir.karata.endpoint.rest.model.*;
import vendredi.soir.karata.repository.model.poker.GameEntity;

@Component
public class RestMapper {
  public PlayerInfo toRest(Player player, vendredi.soir.karata.core.entity.Game game) {
    Deal deal = game.getCurrentDeal();
    PlayerStatus status = PlayerStatus.ACTIVE;
    long contribution = 0L;
    BlindRole blind = null;
    String lastAction = null;
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
      lastAction = deal.getLastActionDescription(player);
    }
    return new PlayerInfo(
        UUID.nameUUIDFromBytes(player.getName().getBytes()),
        player.getName(),
        game.getChips(player),
        status,
        contribution,
        blind,
        lastAction);
  }

  public DealState toRest(
      Deal deal, UUID dealId, vendredi.soir.karata.core.entity.Game game, Instant turnDeadline) {
    if (deal == null) return null;
    List<String> communityCards = new ArrayList<>(5);
    for (int i = 0; i < 5; i++)
      communityCards.add(i < deal.getBoard().size() ? deal.getBoard().get(i).toString() : null);
    List<Player> dealtIn = deal.filterDealtIn(game.getPlayers());
    Player activePlayer = game.getRules().determineNextPlayer(deal, dealtIn);
    return new DealState(
        dealId,
        communityCards,
        deal.getTotalPot(),
        Phase.valueOf(game.getRules().currentPhase(deal, dealtIn)),
        activePlayer != null ? UUID.nameUUIDFromBytes(activePlayer.getName().getBytes()) : null,
        deal.getCurrentRoundBet(),
        outcome(deal, game),
        activePlayer != null ? turnDeadline : null);
  }

  public Game toRest(
      vendredi.soir.karata.core.entity.Game game,
      GameEntity entity,
      String requestingUsername,
      Instant turnDeadline,
      Set<String> activeUsernames) {
    UUID currentDealId = game.getCurrentDealId();
    Deal deal = game.getCurrentDeal();
    boolean handInProgress =
        deal != null
            && !"SHOWDOWN"
                .equals(game.getRules().currentPhase(deal, deal.filterDealtIn(game.getPlayers())));

    List<PlayerInfo> players =
        game.getPlayers().stream()
            // A player who has left stays visible only while still contesting a hand they were
            // already dealt into (e.g. all-in when they left, so leaving didn't fold them out) -
            // once that hand ends, or if there's no hand in progress, they disappear for good.
            .filter(
                p ->
                    activeUsernames.contains(p.getName())
                        || (handInProgress
                            && !deal.getHoleCards(p).isEmpty()
                            && !deal.hasFolded(p)))
            .map(p -> toRest(p, game))
            .collect(Collectors.toList());

    return new Game(
        entity.getId(),
        entity.getName(),
        new Blinds(entity.getSmallBlind(), entity.getBigBlind()),
        players,
        new ArrayList<>(),
        currentDealId,
        toRest(deal, currentDealId, game, turnDeadline),
        you(game, requestingUsername),
        entity.getClosed(),
        entity.getDefaultBuyIn(),
        entity.getVariant());
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
    boolean realShowdown =
        deal.filterDealtIn(game.getPlayers()).stream().filter(p -> !deal.hasFolded(p)).count() > 1;

    // Always go through Rules rather than combining hole+board cards directly here - Hold'em
    // treats all 7 cards as one pool ("best 5 of 7"), but Omaha requires exactly 2 hole + 3 board,
    // so a variant-blind HandFactory.evaluateBestHand call would describe the wrong hand there.
    Map<Player, Hand> showdownHands =
        realShowdown
            ? game.getRules().evaluateShowdownHands(deal, deal.filterDealtIn(game.getPlayers()))
            : Map.of();

    List<WinnerInfo> winners =
        awards.stream()
            .map(
                ap -> {
                  Player winner = ap.getWinner();
                  Hand hand = showdownHands.get(winner);
                  return new WinnerInfo(
                      UUID.nameUUIDFromBytes(winner.getName().getBytes()),
                      winner.getName(),
                      ap.getAmount(),
                      hand != null ? hand.describe() : null);
                })
            .toList();

    // Stable, deterministic order (the map itself has none) - same order the players are listed
    // in everywhere else on this game.
    List<RevealedHand> revealedHands =
        game.getPlayers().stream()
            .filter(showdownHands::containsKey)
            .map(
                p ->
                    new RevealedHand(
                        UUID.nameUUIDFromBytes(p.getName().getBytes()),
                        p.getName(),
                        deal.getHoleCards(p).stream().map(Card::toString).toList(),
                        showdownHands.get(p).describe()))
            .toList();

    return new DealOutcome(winners, revealedHands);
  }
}
