package com.example.personalsite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public class PostDtos {

  public record CreatePostRequest(@NotBlank @Size(max = 2000) String content, List<String> imageUrls) {}

  public record PostResponse(
      Long id,
      UserDto author,
      String content,
      List<String> imageUrls,
      long likeCount,
      long commentCount,
      boolean likedByMe,
      boolean canDelete,
      Instant createdAt,
      Instant updatedAt) {}

  public record LikeResponse(boolean liked, long likeCount) {}
}
