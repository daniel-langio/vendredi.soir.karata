package vendredi.soir.karata.banking;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.karata.service.JwtService;

@RestController
@RequestMapping("/poker/wallet")
@AllArgsConstructor
public class BankingController {
  private final BankingService bankingService;
  private final JwtService jwtService;

  @GetMapping
  public WalletResponse get(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String username = jwtService.validateAndExtractUsername(authHeader);
    return new WalletResponse(bankingService.getBalance(username));
  }

  public record WalletResponse(long chips) {}
}
