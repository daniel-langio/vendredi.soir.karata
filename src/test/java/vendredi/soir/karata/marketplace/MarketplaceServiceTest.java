package vendredi.soir.karata.marketplace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vendredi.soir.karata.banking.BankingService;
import vendredi.soir.karata.endpoint.rest.exception.BadRequestException;
import vendredi.soir.karata.endpoint.rest.exception.ForbiddenException;

class MarketplaceServiceTest {
  private ChipListingRepository chipListingRepository;
  private BankingService bankingService;
  private IfayClient ifayClient;
  private MarketplaceService service;

  @BeforeEach
  void setUp() {
    chipListingRepository = mock(ChipListingRepository.class);
    bankingService = mock(BankingService.class);
    ifayClient = mock(IfayClient.class);
    when(chipListingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    service = new MarketplaceService(chipListingRepository, bankingService, ifayClient, "dev");
  }

  @Test
  void only_allowlisted_sellers_can_create_a_listing() {
    assertThrows(
        ForbiddenException.class,
        () -> service.createListing("alice", 1000, 2000, "+261340000001", PaymentProvider.MVOLA));
    verifyNoInteractions(bankingService);
  }

  @Test
  void creating_a_listing_escrows_chips_out_of_the_sellers_wallet() {
    ChipListing listing =
        service.createListing("dev", 1000, 2000, "+261340000001", PaymentProvider.MVOLA);

    verify(bankingService).debit("dev", 1000);
    assertEquals(ListingStatus.ACTIVE, listing.getStatus());
    assertEquals(1000, listing.getChipsAmount());
    assertEquals(2000, listing.getPriceAr());
  }

  @Test
  void insufficient_funds_propagates_and_no_listing_is_created() {
    doThrow(new BadRequestException("Insufficient chips balance"))
        .when(bankingService)
        .debit(eq("dev"), anyLong());

    assertThrows(
        BadRequestException.class,
        () -> service.createListing("dev", 999_999, 2000, "+261340000001", PaymentProvider.MVOLA));
    verify(chipListingRepository, never()).save(any());
  }

  @Test
  void cancelling_refunds_the_seller_and_only_works_on_active_listings() {
    ChipListing listing = activeListing();
    when(chipListingRepository.findByIdForUpdate(listing.getId())).thenReturn(Optional.of(listing));

    service.cancelListing("dev", listing.getId());

    verify(bankingService).credit("dev", listing.getChipsAmount());
    assertEquals(ListingStatus.CANCELLED, listing.getStatus());
  }

  @Test
  void cancelling_someone_elses_listing_is_forbidden() {
    ChipListing listing = activeListing();
    when(chipListingRepository.findByIdForUpdate(listing.getId())).thenReturn(Optional.of(listing));

    assertThrows(ForbiddenException.class, () -> service.cancelListing("mallory", listing.getId()));
    verifyNoInteractions(bankingService);
  }

  @Test
  void initiating_a_purchase_calls_ifay_with_the_listings_own_receiving_number() {
    ChipListing listing = activeListing();
    when(chipListingRepository.findByIdForUpdate(listing.getId())).thenReturn(Optional.of(listing));
    when(ifayClient.submitClaim(any(), any(), anyLong(), any(), any()))
        .thenReturn(new IfayClient.ClaimResponse("payment-1", "PENDING", null));

    ChipListing result =
        service.initiatePurchase("bob", listing.getId(), "+261340000099", "SOME-REF");

    ArgumentCaptor<String> receiverCaptor = ArgumentCaptor.forClass(String.class);
    verify(ifayClient)
        .submitClaim(
            eq("+261340000099"),
            receiverCaptor.capture(),
            eq(listing.getPriceAr()),
            eq(PaymentProvider.MVOLA),
            eq("SOME-REF"));
    assertEquals(listing.getReceivingPhoneNumber(), receiverCaptor.getValue());
    assertEquals(ListingStatus.PENDING_PAYMENT, result.getStatus());
    assertEquals("bob", result.getBuyerUsername());
    assertEquals("payment-1", result.getIfayPaymentId());
  }

  @Test
  void cannot_buy_your_own_listing() {
    ChipListing listing = activeListing();
    when(chipListingRepository.findByIdForUpdate(listing.getId())).thenReturn(Optional.of(listing));

    assertThrows(
        BadRequestException.class,
        () -> service.initiatePurchase("dev", listing.getId(), "+261340000099", "SOME-REF"));
    verifyNoInteractions(ifayClient);
  }

  @Test
  void checkAndComplete_credits_the_buyer_once_ifay_reports_verified() {
    ChipListing listing = activeListing();
    listing.setStatus(ListingStatus.PENDING_PAYMENT);
    listing.setBuyerUsername("bob");
    listing.setIfayPaymentId("payment-1");
    when(chipListingRepository.findByIdForUpdate(listing.getId())).thenReturn(Optional.of(listing));
    when(ifayClient.getClaim("payment-1"))
        .thenReturn(new IfayClient.ClaimResponse("payment-1", "VERIFIED", listing.getPriceAr()));

    ChipListing result = service.checkAndComplete(listing.getId());

    verify(bankingService).credit("bob", listing.getChipsAmount());
    assertEquals(ListingStatus.SOLD, result.getStatus());
  }

  @Test
  void checkAndComplete_does_nothing_while_still_pending() {
    ChipListing listing = activeListing();
    listing.setStatus(ListingStatus.PENDING_PAYMENT);
    listing.setBuyerUsername("bob");
    listing.setIfayPaymentId("payment-1");
    when(chipListingRepository.findByIdForUpdate(listing.getId())).thenReturn(Optional.of(listing));
    when(ifayClient.getClaim("payment-1"))
        .thenReturn(new IfayClient.ClaimResponse("payment-1", "PENDING", null));

    service.checkAndComplete(listing.getId());

    verifyNoInteractions(bankingService);
    assertEquals(ListingStatus.PENDING_PAYMENT, listing.getStatus());
  }

  private static ChipListing activeListing() {
    return ChipListing.builder()
        .id(UUID.randomUUID())
        .sellerUsername("dev")
        .chipsAmount(1000)
        .priceAr(2000)
        .receivingPhoneNumber("+261340000001")
        .provider(PaymentProvider.MVOLA)
        .status(ListingStatus.ACTIVE)
        .build();
  }
}
