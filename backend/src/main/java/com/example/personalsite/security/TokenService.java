package com.example.personalsite.security;

import com.example.personalsite.config.AppProperties;
import com.example.personalsite.exception.AppException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

  private final AppProperties properties;

  public TokenService(AppProperties properties) {
    this.properties = properties;
  }

  public String issue(Long userId) {
    long expiresAt = Instant.now().plus(properties.auth().tokenTtlHours(), ChronoUnit.HOURS).getEpochSecond();
    String payload = encode(userId + ":" + expiresAt);
    return payload + "." + sign(payload);
  }

  public Long verify(String token) {
    String[] parts = token == null ? new String[0] : token.split("\\.", 2);
    if (parts.length != 2 || !sign(parts[0]).equals(parts[1])) {
      throw AppException.unauthorized("登录状态无效，请重新登录");
    }

    String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
    String[] values = payload.split(":", 2);
    if (values.length != 2) {
      throw AppException.unauthorized("登录状态无效，请重新登录");
    }

    long expiresAt;
    try {
      expiresAt = Long.parseLong(values[1]);
    } catch (NumberFormatException exception) {
      throw AppException.unauthorized("登录状态无效，请重新登录");
    }
    if (Instant.now().getEpochSecond() > expiresAt) {
      throw AppException.unauthorized("登录已过期，请重新登录");
    }

    try {
      return Long.parseLong(values[0]);
    } catch (NumberFormatException exception) {
      throw AppException.unauthorized("登录状态无效，请重新登录");
    }
  }

  public String issueCaptcha(String answer) {
    long expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES).getEpochSecond();
    String payload = encode(answer.trim().toLowerCase() + ":" + expiresAt);
    return payload + "." + sign(payload);
  }

  public boolean verifyCaptcha(String token, String answer) {
    if (token == null || answer == null || token.isBlank() || answer.isBlank()) {
      return false;
    }
    String[] parts = token.split("\\.", 2);
    if (parts.length != 2 || !sign(parts[0]).equals(parts[1])) {
      return false;
    }
    String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
    String[] values = payload.split(":", 2);
    if (values.length != 2) {
      return false;
    }
    long expiresAt;
    try {
      expiresAt = Long.parseLong(values[1]);
    } catch (NumberFormatException exception) {
      return false;
    }
    return Instant.now().getEpochSecond() <= expiresAt && values[0].equals(answer.trim().toLowerCase());
  }

  private String encode(String value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private String sign(String payload) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(properties.auth().tokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to sign token", exception);
    }
  }
}
