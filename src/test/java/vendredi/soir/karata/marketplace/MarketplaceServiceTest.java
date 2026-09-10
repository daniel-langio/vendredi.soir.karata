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
  private ListingPurchaseRepository listingPurchaseRepository;
  private BankingService bankingService;
  private IfayClient ifayClient;
  private MarketplaceService service;

  @BeforeEach
  void setUp() {
    chipListingRepository = mock(ChipListingRepository.class);
    listingPurchaseRepository = mock(ListingPurchaseRepository.class);
    bankingService = mock(BankingService.class);
    ifayClient = mock(IfayClient.class);
    when(chipListingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(listingPurchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    service =
        new MarketplaceService(
            chipListingRepository, listingPurchaseRepository, bankingService, ifayClient, "dev");
  }

  @Test
  void only_allowlisted_sellers_can_create_a_listing() {
    assertThrows(
        ForbiddenException.class,
        () -> service.createListing("alice", 1000, 2, "+261340000001", PaymentProvider.MVOLA));
    verifyNoInteractions(bankingService);
  }

  @Test
  void creating_a_listing_escrows_chips_out_of_the_sellers_wallet() {
    ChipListing listing =
        service.createListing("dev", 1000, 2, "+261340000001", PaymentProvider.MVOLA);

    verify(bankingService).debit("dev", 1000);
    assertEquals(ListingStatus.ACTIVE, listing.getStatus());
    assertEquals(1000, listing.getChipsAmount());
    assertEquals(2, listing.getUnitPriceAr());
  }

  @Test
  void insufficient_funds_propagates_and_no_listing_is_created() {
    doThrow(new BadRequestException("Insufficient chips balance"))
        .when(bankingService)
        .debit(eq("dev"), anyLong());

    assertThrows(
        BadRequestException.class,
        () -> service.createListing("dev", 999_999, 2, "+261340000001", PaymentProvider.MVOLA));
    verify(chipListingRepository, never()).save(any());
  }

  @Test
  void cancelling_refunds_the_seller_the_remaining_chips_and_only_works_on_active_listings() {
    ChipListing listing = activeListing();
    when(chipListingRepository.findByIdForUpdate(listing.getId())).thenReturn(Optional.of(listing));

    service.cancelListing("dev", listing.getId());

    verify(bankingService).credit("dev", 1000);
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
  void buying_a_partial_quantity_charges_quantity_times_unit_price_and_reserves_the_stock() {
    ChipListing listing = activeListing();
    when(chipListingRepository.findByIdForUpdate(listing.getId())).thenReturn(Optional.of(listing));
    when(ifayClient.submitClaim(any(), any(), anyLong(), any(), any()))
        .thenReturn(new IfayClient.ClaimResponse("payment-1", "PENDING", null));

    ListingPurchase purchase =
        service.initiatePurchase("bob", listing.getId(), 300, "+261340000099", "SOME-REF");

    ArgumentCaptor<String> receiverCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
    verify(ifayClient)
        .submitClaim(
            eq("+261340000099"),
            receiverCaptor.capture(),
            amountCaptor.capture(),
            eq(PaymentProvider.MVOLA),
            eq("SOME-REF"));
    assertEquals(listing.getReceivingPhoneNumber(), receiverCaptor.getValue());
    assertEquals(300L * listing.getUnitPriceAr(), amountCaptor.getValue());
    assertEquals(300L * listing.getUnitPriceAr(), purchase.getTotalPriceAr());
    assertEquals(300, purchase.getQuantity());
    assertEquals(PurchaseStatus.PENDING_PAYMENT, purchase.getStatus());
    assertEquals("bob", purchase.getBuyerUsername());
    assertEquals("payment-1", purchase.getIfayPaymentId());
    // Reserved out of the listing's remaining stock immediately, before payment verifies.
    assertEquals(700, listing.getChipsAmount());
  }

  @Test
  void cannot_buy_more_than_the_listings_remaining_stock() {
    ChipListing listing = activeListing();
    when(chipListingRepository.findByIdForUpdate(listing.getId())).thenReturn(Optional.of(listing));

    assertThrows(
        BadRequestException.class,
        () -> service.initiatePurchase("bob", listing.getId(), 1001, "+261340000099", "SOME-REF"));
    verifyNoInteractions(ifayClient);
  }

  @Test
  void cannot_buy_your_own_listing() {
    ChipListing listing = activeListing();
    when(chipListingRepository.findByIdForUpdate(listing.getId())).thenReturn(Optional.of(listing));

    assertThrows(
        BadRequestException.class,
        () -> service.initiatePurchase("dev", listing.getId(), 100, "+261340000099", "SOME-REF"));
    verifyNoInteractions(ifayClient);
  }

  @Test
  void checkAndComplete_credits_the_buyer_once_ifay_reports_verified() {
    ListingPurchase purchase = pendingPurchase();
    when(listingPurchaseRepository.findByIdForUpdate(purchase.getId()))
        .thenReturn(Optional.of(purchase));
    when(ifayClient.getClaim("payment-1"))
        .thenReturn(
            new IfayClient.ClaimResponse("payment-1", "VERIFIED", purchase.getTotalPriceAr()));

    ListingPurchase result = service.checkAndComplete(purchase.getId());

    verify(bankingService).credit("bob", purchase.getQuantity());
    assertEquals(PurchaseStatus.COMPLETED, result.getStatus());
  }

  @Test
  void checkAndComplete_does_nothing_while_still_pending() {
    ListingPurchase purchase = pendingPurchase();
    when(listingPurchaseRepository.findByIdForUpdate(purchase.getId()))
        .thenReturn(Optional.of(purchase));
    when(ifayClient.getClaim("payment-1"))
        .thenReturn(new IfayClient.ClaimResponse("payment-1", "PENDING", null));

    service.checkAndComplete(purchase.getId());

    verifyNoInteractions(bankingService);
    assertEquals(PurchaseStatus.PENDING_PAYMENT, purchase.getStatus());
  }

  private static ChipListing activeListing() {
    return ChipListing.builder()
        .id(UUID.randomUUID())
        .sellerUsername("dev")
        .chipsAmount(1000)
        .unitPriceAr(2)
        .receivingPhoneNumber("+261340000001")
        .provider(PaymentProvider.MVOLA)
        .status(ListingStatus.ACTIVE)
        .build();
  }

  private static ListingPurchase pendingPurchase() {
    return ListingPurchase.builder()
        .id(UUID.randomUUID())
        .listingId(UUID.randomUUID())
        .buyerUsername("bob")
        .quantity(300)
        .totalPriceAr(600)
        .status(PurchaseStatus.PENDING_PAYMENT)
        .ifayPaymentId("payment-1")
        .build();
  }
}
