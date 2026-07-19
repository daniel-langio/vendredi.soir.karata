package vendredi.soir.karata.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import vendredi.soir.karata.core.entity.Game;
import vendredi.soir.karata.endpoint.rest.model.ActionRequest;
import vendredi.soir.karata.repository.model.poker.ActionEntity;
import vendredi.soir.karata.repository.model.poker.ActionMapper;
import vendredi.soir.karata.repository.model.poker.GameEntity;
import vendredi.soir.karata.repository.model.poker.PlayerEntity;
import vendredi.soir.karata.repository.poker.ActionRepository;
import vendredi.soir.karata.repository.poker.GameRepository;
import vendredi.soir.karata.repository.poker.PlayerRepository;
import vendredi.soir.karata.service.DealService;
import vendredi.soir.karata.service.GameService;

class PokerGameTest {

  private GameService gameService;
  private DealService dealService;
  @Mock private GameRepository gameRepository;
  @Mock private PlayerRepository playerRepository;
  @Mock private ActionRepository actionRepository;
  private ActionMapper actionMapper;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    ObjectMapper om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    actionMapper = new ActionMapper(om);
    gameService = new GameService(gameRepository, playerRepository, actionRepository, actionMapper);
    dealService = new DealService(gameService);
  }

  @Test
  void simulate_complete_game() {
    UUID gameId = UUID.randomUUID();
    GameEntity gameEntity =
        GameEntity.builder().id(gameId).name("High Stakes").smallBlind(10L).bigBlind(20L).build();
    when(gameRepository.save(any())).thenReturn(gameEntity);
    when(gameRepository.findById(gameId)).thenReturn(Optional.of(gameEntity));
    when(gameRepository.findByIdForUpdate(gameId)).thenReturn(Optional.of(gameEntity));

    gameService.createGame("High Stakes", 10L, 20L);

    List<PlayerEntity> players = new ArrayList<>();
    when(playerRepository.findByGameId(gameId)).thenReturn(players);
    when(playerRepository.save(any()))
        .thenAnswer(
            invocation -> {
              PlayerEntity pe = invocation.getArgument(0);
              players.add(pe);
              return pe;
            });

    gameService.joinGame(gameId, "Alice", 1000L);
    gameService.joinGame(gameId, "Bob", 1000L);

    List<ActionEntity> history = new ArrayList<>();
    when(actionRepository.findByGameIdOrderByActionOrderAsc(gameId)).thenReturn(history);
    when(actionRepository.save(any()))
        .thenAnswer(
            invocation -> {
              ActionEntity ae = invocation.getArgument(0);
              history.add(ae);
              return ae;
            });

    for (int i = 0; i < 4; i++) {
      dealService.startDeal(gameId);
      Game game = gameService.getGame(gameId);
      UUID dealId = history.get(history.size() - 1).getDealId();

      when(actionRepository.findByDealIdOrderByActionOrderAsc(dealId))
          .thenReturn(history.stream().filter(a -> dealId.equals(a.getDealId())).toList());

      // Simulate actions: Alice Checks, Bob Checks
      dealService.takeAction(dealId, new ActionRequest("CHECK", 0L, Instant.now().plusSeconds(60)));
      dealService.takeAction(dealId, new ActionRequest("CHECK", 0L, Instant.now().plusSeconds(60)));

      assertNotNull(game.getCurrentDeal());
    }

    Game finalGame = gameService.getGame(gameId);
    assertEquals(2, finalGame.getPlayers().size());
    assertTrue(history.size() > 10);
  }
}
