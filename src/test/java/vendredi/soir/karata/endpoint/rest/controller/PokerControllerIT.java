package vendredi.soir.karata.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vendredi.soir.karata.endpoint.rest.controller.PokerController.CreateGameRequest;
import vendredi.soir.karata.endpoint.rest.controller.PokerController.JoinRequest;
import vendredi.soir.karata.endpoint.rest.mapper.RestMapper;
import vendredi.soir.karata.endpoint.rest.model.Blinds;
import vendredi.soir.karata.repository.poker.GameRepository;
import vendredi.soir.karata.service.DealService;
import vendredi.soir.karata.service.GameService;
import vendredi.soir.karata.service.JwtService;

@WebMvcTest(PokerController.class)
class PokerControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private GameService gs;
  @MockBean private DealService ds;
  @MockBean private GameRepository gr;
  @MockBean private RestMapper rm;
  @MockBean private JwtService jwtService;

  @Test
  void test_create_game_validation() throws Exception {
    // Missing name
    CreateGameRequest invalid1 = new CreateGameRequest("", new Blinds(10L, 20L));
    mockMvc
        .perform(
            post("/poker/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid1)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("Game name is required"));

    // Invalid blinds
    CreateGameRequest invalid2 = new CreateGameRequest("Table", new Blinds(-10L, 20L));
    mockMvc
        .perform(
            post("/poker/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid2)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  void test_unauthorized_endpoints() throws Exception {
    UUID dummyId = UUID.randomUUID();
    // Simulate jwtService throwing UnauthorizedException
    when(jwtService.validateAndExtractUsername(any()))
        .thenThrow(
            new vendredi.soir.karata.endpoint.rest.exception.UnauthorizedException(
                "Missing or invalid Authorization header"));

    mockMvc
        .perform(
            post("/poker/games/" + dummyId + "/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinRequest(1000L))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.message").value("Missing or invalid Authorization header"));
  }
}
