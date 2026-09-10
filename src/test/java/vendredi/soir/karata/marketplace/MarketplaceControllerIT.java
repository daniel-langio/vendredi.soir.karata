package vendredi.soir.karata.marketplace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import vendredi.soir.karata.endpoint.rest.exception.ForbiddenException;
import vendredi.soir.karata.endpoint.rest.exception.UnauthorizedException;
import vendredi.soir.karata.marketplace.MarketplaceController.CreateListingRequest;
import vendredi.soir.karata.marketplace.MarketplaceController.InitiatePurchaseRequest;
import vendredi.soir.karata.service.JwtService;

@WebMvcTest(MarketplaceController.class)
class MarketplaceControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private MarketplaceService marketplaceService;
  @MockBean private JwtService jwtService;

  @Test
  void create_listing_validation() throws Exception {
    when(jwtService.validateAndExtractUsername(any())).thenReturn("dev");

    CreateListingRequest missingAmount =
        new CreateListingRequest(null, 2000L, "+261340000001", PaymentProvider.MVOLA);
    mockMvc
        .perform(
            post("/marketplace/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingAmount)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

    verifyNoInteractions(marketplaceService);
  }

  @Test
  void create_listing_rejects_non_allowlisted_seller() throws Exception {
    when(jwtService.validateAndExtractUsername(any())).thenReturn("alice");
    when(marketplaceService.createListing(eq("alice"), anyLong(), anyLong(), any(), any()))
        .thenThrow(new ForbiddenException("Only approved sellers can create a listing"));

    CreateListingRequest r =
        new CreateListingRequest(1000L, 2000L, "+261340000001", PaymentProvider.MVOLA);
    mockMvc
        .perform(
            post("/marketplace/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void unauthorized_when_no_valid_token() throws Exception {
    when(jwtService.validateAndExtractUsername(any()))
        .thenThrow(new UnauthorizedException("Missing or invalid Authorization header"));

    mockMvc.perform(get("/marketplace/listings")).andExpect(status().isUnauthorized());
  }

  @Test
  void buy_validation_requires_psp_ref_and_phone() throws Exception {
    when(jwtService.validateAndExtractUsername(any())).thenReturn("bob");

    InitiatePurchaseRequest missingRef = new InitiatePurchaseRequest("+261340000099", null);
    mockMvc
        .perform(
            post("/marketplace/listings/" + UUID.randomUUID() + "/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingRef)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

    verifyNoInteractions(marketplaceService);
  }
}
