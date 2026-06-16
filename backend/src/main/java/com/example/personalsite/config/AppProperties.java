package com.example.personalsite.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Auth auth, Admin admin, Upload upload) {

  public record Auth(String tokenSecret, long tokenTtlHours) {}

  public record Admin(String username, String password, String nickname) {}

  public record Upload(String location) {}
}
