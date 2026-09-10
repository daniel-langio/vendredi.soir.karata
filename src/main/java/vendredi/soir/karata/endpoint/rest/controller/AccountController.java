package vendredi.soir.karata.endpoint.rest.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.karata.endpoint.rest.exception.BadRequestException;
import vendredi.soir.karata.service.AccountService;
import vendredi.soir.karata.service.JwtService;

@RestController
@RequestMapping("/poker/account")
@AllArgsConstructor
public class AccountController {
  private final AccountService accountService;
  private final JwtService jwtService;

  @GetMapping
  public AccountResponse get(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    String username = jwtService.validateAndExtractUsername(authHeader);
    return new AccountResponse(accountService.getPhoneNumber(username));
  }

  @PutMapping("/phone-number")
  public AccountResponse setPhoneNumber(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody SetPhoneNumberRequest r) {
    String username = jwtService.validateAndExtractUsername(authHeader);
    if (r == null || r.phoneNumber() == null || r.phoneNumber().isBlank()) {
      throw new BadRequestException("phoneNumber is required");
    }
    accountService.setPhoneNumber(username, r.phoneNumber());
    return new AccountResponse(r.phoneNumber());
  }

  public record SetPhoneNumberRequest(String phoneNumber) {}

  public record AccountResponse(String phoneNumber) {}
}
