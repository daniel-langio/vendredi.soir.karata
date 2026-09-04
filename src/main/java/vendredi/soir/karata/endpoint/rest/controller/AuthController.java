package vendredi.soir.karata.endpoint.rest.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vendredi.soir.karata.endpoint.rest.exception.BadRequestException;
import vendredi.soir.karata.service.AuthService;

@RestController
@RequestMapping("/poker/auth")
@AllArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public TokenResponse register(@RequestBody AuthRequest r) {
    validate(r);
    return new TokenResponse(authService.register(r.username().trim(), r.password()));
  }

  @PostMapping("/login")
  public TokenResponse login(@RequestBody AuthRequest r) {
    validate(r);
    return new TokenResponse(authService.login(r.username().trim(), r.password()));
  }

  private void validate(AuthRequest r) {
    if (r == null) {
      throw new BadRequestException("Request body cannot be null");
    }
    if (r.username() == null || r.username().trim().length() < 3) {
      throw new BadRequestException("Username must be at least 3 characters");
    }
    if (r.password() == null || r.password().length() < 6) {
      throw new BadRequestException("Password must be at least 6 characters");
    }
  }

  public record AuthRequest(String username, String password) {}

  public record TokenResponse(String token) {}
}
