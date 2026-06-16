package com.example.personalsite.service;

import com.example.personalsite.config.AppProperties;
import com.example.personalsite.dto.AuthDtos.AuthResponse;
import com.example.personalsite.dto.AuthDtos.LoginRequest;
import com.example.personalsite.dto.AuthDtos.RegisterRequest;
import com.example.personalsite.dto.AuthDtos.UpdateProfileRequest;
import com.example.personalsite.dto.UserDto;
import com.example.personalsite.entity.User;
import com.example.personalsite.entity.UserRole;
import com.example.personalsite.entity.UserStatus;
import com.example.personalsite.exception.AppException;
import com.example.personalsite.repository.UserRepository;
import com.example.personalsite.security.AuthService;
import com.example.personalsite.security.TokenService;
import java.util.Locale;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final TokenService tokenService;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final CaptchaService captchaService;
  private final AppProperties properties;
  private final AuthService authService;

  public UserService(
      UserRepository userRepository,
      TokenService tokenService,
      CaptchaService captchaService,
      AppProperties properties,
      AuthService authService) {
    this.userRepository = userRepository;
    this.tokenService = tokenService;
    this.captchaService = captchaService;
    this.properties = properties;
    this.authService = authService;
  }

  public record CaptchaData(String question, String token) {}

  public CaptchaData captcha() {
    var challenge = captchaService.create();
    return new CaptchaData(challenge.question(), challenge.token());
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String username = normalizeUsername(request.username());
    if (userRepository.existsByUsernameIgnoreCase(username)) {
      throw AppException.badRequest("账号已存在");
    }
    if (!tokenService.verifyCaptcha(request.captchaToken(), request.captchaAnswer())) {
      throw AppException.badRequest("验证码不正确");
    }
    User user = new User();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setNickname(resolveNickname(request.nickname(), username));
    if (request.avatarUrl() != null && !request.avatarUrl().trim().isEmpty()) {
      user.setAvatarUrl(request.avatarUrl().trim());
    }
    user.setRole(UserRole.USER);
    user.setStatus(UserStatus.ACTIVE);
    userRepository.save(user);
    return new AuthResponse(tokenService.issue(user.getId()), UserDto.from(user));
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    String username = normalizeUsername(request.username());
    User user = userRepository
        .findByUsernameIgnoreCase(username)
        .orElseThrow(() -> AppException.badRequest("账号或密码错误"));
    if (user.getStatus() == UserStatus.BANNED) {
      throw AppException.forbidden("账号已被封禁");
    }
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw AppException.badRequest("账号或密码错误");
    }
    return new AuthResponse(tokenService.issue(user.getId()), UserDto.from(user));
  }

  @Transactional(readOnly = true)
  public UserDto me() {
    return UserDto.from(authService.requireUser());
  }

  @Transactional
  public UserDto updateProfile(UpdateProfileRequest request) {
    User user = authService.requireUser();
    user.setNickname(request.nickname().trim());
    if (request.avatarUrl() != null) {
      user.setAvatarUrl(request.avatarUrl().trim().isEmpty() ? null : request.avatarUrl().trim());
    }
    return UserDto.from(user);
  }

  @Transactional
  public User ensureAdminAccount() {
    String adminUsername = normalizeUsername(properties.admin().username());
    return userRepository.findByUsernameIgnoreCase(adminUsername).orElseGet(() -> {
      User admin = new User();
      admin.setUsername(adminUsername);
      admin.setPasswordHash(passwordEncoder.encode(properties.admin().password()));
      admin.setNickname(resolveNickname(properties.admin().nickname(), adminUsername));
      admin.setRole(UserRole.ADMIN);
      admin.setStatus(UserStatus.ACTIVE);
      return userRepository.save(admin);
    });
  }

  private String normalizeUsername(String username) {
    if (username == null) {
      throw AppException.badRequest("账号不能为空");
    }
    return username.trim().toLowerCase(Locale.ROOT);
  }

  private String resolveNickname(String nickname, String fallback) {
    String value = nickname == null ? "" : nickname.trim();
    return value.isEmpty() ? fallback : value;
  }
}
