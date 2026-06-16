package com.example.personalsite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

  public record CaptchaResponse(String question, String token) {}

  public record RegisterRequest(
      @NotBlank @Size(min = 3, max = 40) String username,
      @NotBlank @Size(min = 6, max = 72) String password,
      @Size(max = 80) String nickname,
      String avatarUrl,
      @NotBlank String captchaToken,
      @NotBlank String captchaAnswer) {}

  public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

  public record AuthResponse(String token, UserDto user) {}

  public record UpdateProfileRequest(@NotBlank @Size(max = 80) String nickname, String avatarUrl) {}
}
