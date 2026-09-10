package vendredi.soir.karata.marketplace;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vendredi.soir.karata.banking.BankingService;
import vendredi.soir.karata.endpoint.rest.exception.BadRequestException;
import vendredi.soir.karata.endpoint.rest.exception.ForbiddenException;
import vendredi.soir.karata.endpoint.rest.exception.NotFoundException;

/**
 * A listing escrows its full chip count out of the seller's wallet immediately on creation ({@link
 * BankingService#debit}, which already rejects insufficient funds); {@code chipsAmount} then tracks
 * what's still unreserved as buyers make purchases against it - each purchase is its own {@link
 * ListingPurchase} row (a listing isn't a single atomic sale, buyers pick a quantity), decrementing
 * the listing's remaining stock the moment it's initiated so two buyers can never oversell the same
 * chips. Cancelling refunds whatever's still unreserved; already-initiated purchases resolve
 * independently regardless. Only usernames in {@code marketplace.seller-allowlist} can create a
 * listing for now (config, not code, so opening this up to real users later is a config change).
 */
@Service
public class MarketplaceService {
  private final ChipListingRepository chipListingRepository;
  private final ListingPurchaseRepository listingPurchaseRepository;
  private final BankingService bankingService;
  private final IfayClient ifayClient;
  private final Set<String> sellerAllowlist;

  public MarketplaceService(
      ChipListingRepository chipListingRepository,
      ListingPurchaseRepository listingPurchaseRepository,
      BankingService bankingService,
      IfayClient ifayClient,
      @Value("${marketplace.seller-allowlist:dev}") String sellerAllowlistCsv) {
    this.chipListingRepository = chipListingRepository;
    this.listingPurchaseRepository = listingPurchaseRepository;
    this.bankingService = bankingService;
    this.ifayClient = ifayClient;
    this.sellerAllowlist =
        Arrays.stream(sellerAllowlistCsv.split(",")).map(String::trim).collect(Collectors.toSet());
  }

  @Transactional
  public ChipListing createListing(
      String sellerUsername,
      long chipsAmount,
      long unitPriceAr,
      String receivingPhoneNumber,
      PaymentProvider provider) {
    if (!sellerAllowlist.contains(sellerUsername)) {
      throw new ForbiddenException("Only approved sellers can create a listing");
    }
    if (chipsAmount <= 0) {
      throw new BadRequestException("chipsAmount must be strictly positive");
    }
    if (unitPriceAr <= 0) {
      throw new BadRequestException("unitPriceAr must be strictly positive");
    }

    bankingService.debit(sellerUsername, chipsAmount);

    ChipListing listing =
        ChipListing.builder()
            .id(UUID.randomUUID())
            .sellerUsername(sellerUsername)
            .chipsAmount(chipsAmount)
            .unitPriceAr(unitPriceAr)
            .receivingPhoneNumber(receivingPhoneNumber)
            .provider(provider)
            .status(ListingStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();
    return chipListingRepository.save(listing);
  }

  @Transactional(readOnly = true)
  public List<ChipListing> listActive() {
    return chipListingRepository.findByStatus(ListingStatus.ACTIVE).stream()
        .filter(l -> l.getChipsAmount() > 0)
        .toList();
  }

  @Transactional
  public ChipListing cancelListing(String sellerUsername, UUID listingId) {
    ChipListing listing = findListingForUpdate(listingId);
    if (!listing.getSellerUsername().equals(sellerUsername)) {
      throw new ForbiddenException("Not your listing");
    }
    if (listing.getStatus() != ListingStatus.ACTIVE) {
      throw new BadRequestException("Only an active listing can be cancelled");
    }
    bankingService.credit(sellerUsername, listing.getChipsAmount());
    listing.setStatus(ListingStatus.CANCELLED);
    return chipListingRepository.save(listing);
  }

  @Transactional
  public ListingPurchase initiatePurchase(
      String buyerUsername, UUID listingId, long quantity, String buyerPhoneNumber, String pspRef) {
    if (quantity <= 0) {
      throw new BadRequestException("quantity must be strictly positive");
    }

    ChipListing listing = findListingForUpdate(listingId);
    if (listing.getStatus() != ListingStatus.ACTIVE) {
      throw new BadRequestException("Listing is no longer available");
    }
    if (listing.getSellerUsername().equals(buyerUsername)) {
      throw new BadRequestException("Cannot buy your own listing");
    }
    if (quantity > listing.getChipsAmount()) {
      throw new BadRequestException("Not enough chips left in this listing");
    }

    long totalPriceAr = quantity * listing.getUnitPriceAr();
    IfayClient.ClaimResponse claim =
        ifayClient.submitClaim(
            buyerPhoneNumber,
            listing.getReceivingPhoneNumber(),
            totalPriceAr,
            listing.getProvider(),
            pspRef);

    listing.setChipsAmount(listing.getChipsAmount() - quantity);
    chipListingRepository.save(listing);

    ListingPurchase purchase =
        ListingPurchase.builder()
            .id(UUID.randomUUID())
            .listingId(listingId)
            .buyerUsername(buyerUsername)
            .quantity(quantity)
            .totalPriceAr(totalPriceAr)
            .status(PurchaseStatus.PENDING_PAYMENT)
            .ifayPaymentId(claim.id())
            .createdAt(Instant.now())
            .build();
    return listingPurchaseRepository.save(purchase);
  }

  /** Re-checks a pending purchase against ifay; credits the buyer once verified. */
  @Transactional
  public ListingPurchase checkAndComplete(UUID purchaseId) {
    ListingPurchase purchase = findPurchaseForUpdate(purchaseId);
    if (purchase.getStatus() != PurchaseStatus.PENDING_PAYMENT) {
      return purchase;
    }
    IfayClient.ClaimResponse claim = ifayClient.getClaim(purchase.getIfayPaymentId());
    if ("VERIFIED".equals(claim.status())) {
      bankingService.credit(purchase.getBuyerUsername(), purchase.getQuantity());
      purchase.setStatus(PurchaseStatus.COMPLETED);
      purchase.setCompletedAt(Instant.now());
      listingPurchaseRepository.save(purchase);
    }
    return purchase;
  }

  @Transactional(readOnly = true)
  public ChipListing getListing(UUID listingId) {
    return chipListingRepository
        .findById(listingId)
        .orElseThrow(() -> new NotFoundException("No listing " + listingId));
  }

  List<ListingPurchase> findAllPendingPurchases() {
    return listingPurchaseRepository.findByStatus(PurchaseStatus.PENDING_PAYMENT);
  }

  private ChipListing findListingForUpdate(UUID listingId) {
    return chipListingRepository
        .findByIdForUpdate(listingId)
        .orElseThrow(() -> new NotFoundException("No listing " + listingId));
  }

  private ListingPurchase findPurchaseForUpdate(UUID purchaseId) {
    return listingPurchaseRepository
        .findByIdForUpdate(purchaseId)
        .orElseThrow(() -> new NotFoundException("No purchase " + purchaseId));
  }
}
