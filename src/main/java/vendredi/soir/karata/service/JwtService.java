package vendredi.soir.karata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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

  public String validateAndExtractUsername(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new UnauthorizedException("Missing or invalid Authorization header");
    }
    String token = authHeader.substring(7).trim();
    if (token.isEmpty()) {
      throw new UnauthorizedException("Bearer token is empty");
    }

    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      // Robust fallback for simple development/testing mock tokens when no secret is configured
      if (secret == null || secret.trim().isEmpty()) {
        return token;
      }
      throw new UnauthorizedException("Invalid JWT format");
    }

    // Verify signature if secret is configured
    if (secret != null && !secret.trim().isEmpty()) {
      try {
        String data = parts[0] + "." + parts[1];
        String signature = parts[2];
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey =
            new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] expectedSignatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        String expectedSignature =
            Base64.getUrlEncoder().withoutPadding().encodeToString(expectedSignatureBytes);

        // Remove padding and normalize received signature for comparison
        String sanitizedReceivedSignature =
            signature.replace("=", "").replace("+", "-").replace("/", "_");
        if (!expectedSignature.equals(sanitizedReceivedSignature)) {
          throw new UnauthorizedException("Invalid JWT signature");
        }
      } catch (NoSuchAlgorithmException | InvalidKeyException e) {
        throw new UnauthorizedException("Error validating JWT signature");
      }
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
    } catch (Exception e) {
      throw new UnauthorizedException("Invalid JWT payload");
    }
  }
}
