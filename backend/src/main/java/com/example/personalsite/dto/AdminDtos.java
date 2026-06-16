package com.example.personalsite.dto;

import com.example.personalsite.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public class AdminDtos {

  public record UpdateUserStatusRequest(@NotNull UserStatus status) {}
}
