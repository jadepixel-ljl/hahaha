package com.example.personalsite.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class BootstrapService {

  private final UserService userService;

  public BootstrapService(UserService userService) {
    this.userService = userService;
  }

  @PostConstruct
  public void init() {
    userService.ensureAdminAccount();
  }
}
