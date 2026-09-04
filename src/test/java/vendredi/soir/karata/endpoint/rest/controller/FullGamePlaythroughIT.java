package vendredi.soir.karata.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vendredi.soir.karata.conf.FacadeIT;
import vendredi.soir.karata.endpoint.rest.model.ActionRequest;
import vendredi.soir.karata.endpoint.rest.model.Game;
import vendredi.soir.karata.endpoint.rest.model.Hand;
import vendredi.soir.karata.endpoint.rest.model.Phase;
import vendredi.soir.karata.endpoint.rest.model.PlayerInfo;

/**
 * Drives a complete heads-up hand through the real HTTP API (real controllers, services,
 * persistence, and a real Postgres via Testcontainers) from table creation to showdown. This exists
 * because unit tests mocking the repositories or hand-driving the core engine directly missed real
 * bugs (chip ledger silently zeroed by a broken Player.equals(), deal-start crashing on a
 * too-narrow DB column, no automatic blind/hole-card/street orchestration at all) that only showed
 * up when the whole stack — including a real JSON/DB round trip — was exercised together.
 */
class FullGamePlaythroughIT extends FacadeIT {

  @Autowired private TestRestTemplate rest;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void plays_a_full_heads_up_hand_end_to_end() throws Exception {
    UUID gameId = createGame("Full Game IT", 10, 20);

    join(gameId, "alice", 1000);
    join(gameId, "bob", 1000);

    // Starting the deal must post blinds and deal hole cards automatically.
    Game started = startDeal(gameId);
    UUID dealId = started.currentDealId();
    assertNotNull(dealId, "starting a deal should produce a deal id");
    assertEquals(Phase.PRE_FLOP, started.currentDeal().phase());
    assertEquals(990L, chipsOf(started, "alice"), "alice should have posted the small blind");
    assertEquals(980L, chipsOf(started, "bob"), "bob should have posted the big blind");

    assertEquals(2, hand(dealId, "alice").cards().size(), "alice should have 2 hole cards");
    assertEquals(2, hand(dealId, "bob").cards().size(), "bob should have 2 hole cards");

    // Pre-flop: alice (small blind) must call the extra 10 to match the big blind. That closes
    // the pre-flop betting round, which should automatically deal the flop.
    action(dealId, "alice", "CALL", 10L);

    Game afterPreFlop = getGame(gameId);
    assertEquals(Phase.FLOP, afterPreFlop.currentDeal().phase(), "flop should auto-deal");
    assertEquals(3, communityCardCount(afterPreFlop), "flop should reveal exactly 3 cards");

    // Flop: bob acts first heads-up (next after the big blind wraps to... last actor was alice's
    // call, so bob is next), both check, closing the round and auto-dealing the turn.
    action(dealId, "bob", "CHECK", null);
    action(dealId, "alice", "CHECK", null);

    Game afterFlop = getGame(gameId);
    assertEquals(Phase.TURN, afterFlop.currentDeal().phase(), "turn should auto-deal");
    assertEquals(4, communityCardCount(afterFlop), "turn should reveal a 4th card");

    // Turn: same pattern.
    action(dealId, "bob", "CHECK", null);
    action(dealId, "alice", "CHECK", null);

    Game afterTurn = getGame(gameId);
    assertEquals(Phase.RIVER, afterTurn.currentDeal().phase(), "river should auto-deal");
    assertEquals(5, communityCardCount(afterTurn), "river should reveal a 5th card");

    // River: closing this round should automatically trigger the showdown and award the pot.
    action(dealId, "bob", "CHECK", null);
    action(dealId, "alice", "CHECK", null);

    Game finalGame = getGame(gameId);
    assertEquals(Phase.SHOWDOWN, finalGame.currentDeal().phase(), "hand should auto-conclude");
    assertEquals(5, communityCardCount(finalGame));

    long totalChips = finalGame.players().stream().mapToLong(PlayerInfo::chips).sum();
    assertEquals(2000L, totalChips, "no chips should be created or destroyed by the hand");

    // Once the hand is over, no further action should be accepted.
    HttpEntity<ActionRequest> lateAction =
        authorized("alice", new ActionRequest("CHECK", null, Instant.now().plusSeconds(60)));
    ResponseEntity<Void> rejected =
        rest.postForEntity("/poker/deals/" + dealId + "/actions", lateAction, Void.class);
    assertEquals(
        HttpStatus.BAD_REQUEST,
        rejected.getStatusCode(),
        "no more actions should be accepted once the deal has reached showdown");
  }

  private UUID createGame(String name, long smallBlind, long bigBlind) {
    Map<String, Object> body =
        Map.of("name", name, "blinds", Map.of("small", smallBlind, "big", bigBlind));
    ResponseEntity<Game> resp = rest.postForEntity("/poker/games", body, Game.class);
    assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    return resp.getBody().gameId();
  }

  private void join(UUID gameId, String username, long buyIn) {
    HttpEntity<Map<String, Object>> req = authorized(username, Map.of("buyInAmount", buyIn));
    ResponseEntity<Void> resp =
        rest.postForEntity("/poker/games/" + gameId + "/players", req, Void.class);
    assertEquals(
        HttpStatus.NO_CONTENT, resp.getStatusCode(), username + " should be able to join/buy-in");
  }

  private Game startDeal(UUID gameId) throws Exception {
    ResponseEntity<String> resp =
        rest.postForEntity("/poker/games/" + gameId + "/deals", null, String.class);
    assertEquals(
        HttpStatus.CREATED, resp.getStatusCode(), "starting deal failed, body: " + resp.getBody());
    return objectMapper.readValue(resp.getBody(), Game.class);
  }

  private Game getGame(UUID gameId) {
    ResponseEntity<Game> resp = rest.getForEntity("/poker/games/" + gameId, Game.class);
    assertEquals(HttpStatus.OK, resp.getStatusCode());
    return resp.getBody();
  }

  private void action(UUID dealId, String username, String actionType, Long amount) {
    HttpEntity<ActionRequest> req =
        authorized(username, new ActionRequest(actionType, amount, Instant.now().plusSeconds(60)));
    ResponseEntity<String> resp =
        rest.postForEntity("/poker/deals/" + dealId + "/actions", req, String.class);
    assertEquals(
        HttpStatus.NO_CONTENT,
        resp.getStatusCode(),
        actionType + " by " + username + " should be accepted, got body: " + resp.getBody());
  }

  private Hand hand(UUID dealId, String username) {
    ResponseEntity<Hand> resp =
        rest.exchange(
            "/poker/deals/" + dealId + "/hand/me",
            HttpMethod.GET,
            authorized(username, null),
            Hand.class);
    assertEquals(HttpStatus.OK, resp.getStatusCode());
    return resp.getBody();
  }

  private <T> HttpEntity<T> authorized(String username, T body) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + username);
    return new HttpEntity<>(body, headers);
  }

  private static long chipsOf(Game game, String username) {
    return game.players().stream()
        .filter(p -> p.username().equals(username))
        .findFirst()
        .orElseThrow()
        .chips();
  }

  private static int communityCardCount(Game game) {
    return (int) game.currentDeal().communityCards().stream().filter(c -> c != null).count();
  }
}
