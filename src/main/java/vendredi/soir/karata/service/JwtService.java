package vendredi.soir.karata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vendredi.soir.karata.endpoint.rest.exception.UnauthorizedException;

@Service
public class JwtService {
  private final String secret;
  private final ObjectMapper objectMapper;

  public JwtService(@Value("${jwt.secret:}") String secret, ObjectMapper objectMapper) {
    this.secret = secret;
    this.objectMapper = objectMapper;
  }

  /** Mints a signed JWT carrying the given username as both "sub" and "username". */
  public String generateToken(String username) {
    if (secret == null || secret.trim().isEmpty()) {
      throw new IllegalStateException("jwt.secret must be configured to issue tokens");
    }
    try {
      String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
      String payload =
          base64UrlEncode(
              objectMapper.writeValueAsString(
                  Map.of(
                      "sub",
                      username,
                      "username",
                      username,
                      "iat",
                      Instant.now().getEpochSecond())));
      String data = header + "." + payload;
      String signature = sign(data);
      return data + "." + signature;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to generate JWT", e);
    }
  }

  public String validateAndExtractUsername(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new UnauthorizedException("Missing or invalid Authorization header");
    }
    String token = authHeader.substring(7).trim();
    if (token.isEmpty()) {
      throw new UnauthorizedException("Bearer token is empty");
    }
    if (secret == null || secret.trim().isEmpty()) {
      throw new UnauthorizedException("Server authentication is not configured");
    }

    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new UnauthorizedException("Invalid JWT format");
    }

    try {
      String data = parts[0] + "." + parts[1];
      String expectedSignature = sign(data);
      String sanitizedReceivedSignature =
          parts[2].replace("=", "").replace("+", "-").replace("/", "_");
      if (!expectedSignature.equals(sanitizedReceivedSignature)) {
        throw new UnauthorizedException("Invalid JWT signature");
      }
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new UnauthorizedException("Error validating JWT signature");
    }

    try {
      byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
      Map<String, Object> claims = objectMapper.readValue(payloadBytes, Map.class);
      if (claims.containsKey("username")) {
        return claims.get("username").toString();
      } else if (claims.containsKey("sub")) {
        return claims.get("sub").toString();
      } else {
        throw new UnauthorizedException("JWT missing username/sub claim");
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new UnauthorizedException("Invalid JWT payload");
    }
  }

  private String sign(String data) throws NoSuchAlgorithmException, InvalidKeyException {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey =
        new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(secretKey);
    byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
  }

  private static String base64UrlEncode(String s) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(s.getBytes(StandardCharsets.UTF_8));
  }
}
