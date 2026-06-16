package com.example.personalsite.controller;

import com.example.personalsite.dto.AuthDtos.AuthResponse;
import com.example.personalsite.dto.AuthDtos.CaptchaResponse;
import com.example.personalsite.dto.AuthDtos.LoginRequest;
import com.example.personalsite.dto.AuthDtos.RegisterRequest;
import com.example.personalsite.dto.AuthDtos.UpdateProfileRequest;
import com.example.personalsite.dto.UserDto;
import com.example.personalsite.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserService userService;

  public AuthController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/captcha")
  public CaptchaResponse captcha() {
    var captcha = userService.captcha();
    return new CaptchaResponse(captcha.question(), captcha.token());
  }

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return userService.register(request);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return userService.login(request);
  }

  @GetMapping("/me")
  public UserDto me() {
    return userService.me();
  }

  @PatchMapping("/me")
  public UserDto updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
    return userService.updateProfile(request);
  }
}
