package com.example.personalsite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public class CommentDtos {

  public record CreateCommentRequest(@NotBlank @Size(max = 1000) String content, Long parentId) {}

  public record CommentResponse(
      Long id,
      Long postId,
      Long parentId,
      UserDto author,
      String content,
      boolean canDelete,
      Instant createdAt,
      Instant updatedAt,
      List<CommentResponse> replies) {}
}
