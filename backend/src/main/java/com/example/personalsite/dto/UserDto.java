package com.example.personalsite.dto;

import com.example.personalsite.entity.User;
import com.example.personalsite.entity.UserRole;
import com.example.personalsite.entity.UserStatus;
import java.time.Instant;

public record UserDto(
    Long id,
    String username,
    String nickname,
    String avatarUrl,
    UserRole role,
    UserStatus status,
    Instant createdAt) {

  public static UserDto from(User user) {
    return new UserDto(
        user.getId(),
        user.getUsername(),
        user.getNickname(),
        user.getAvatarUrl(),
        user.getRole(),
        user.getStatus(),
        user.getCreatedAt());
  }
}
