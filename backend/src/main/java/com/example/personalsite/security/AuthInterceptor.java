package com.example.personalsite.security;

import com.example.personalsite.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

  private final TokenService tokenService;
  private final UserRepository userRepository;

  public AuthInterceptor(TokenService tokenService, UserRepository userRepository) {
    this.tokenService = tokenService;
    this.userRepository = userRepository;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.startsWith("Bearer ")) {
      try {
        Long userId = tokenService.verify(authorization.substring("Bearer ".length()).trim());
        userRepository.findById(userId).ifPresent(user -> CurrentUser.set(user.getId()));
      } catch (RuntimeException ignored) {
        CurrentUser.clear();
      }
    }
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    CurrentUser.clear();
  }
}
