package com.example.personalsite.service;

import com.example.personalsite.security.TokenService;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {

  private final TokenService tokenService;

  public CaptchaService(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  public CaptchaChallenge create() {
    int a = ThreadLocalRandom.current().nextInt(10, 50);
    int b = ThreadLocalRandom.current().nextInt(1, 10);
    String question = a + " + " + b + " = ?";
    String answer = String.valueOf(a + b);
    return new CaptchaChallenge(question, tokenService.issueCaptcha(answer));
  }

  public record CaptchaChallenge(String question, String token) {}
}
