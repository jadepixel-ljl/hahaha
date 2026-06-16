package com.example.personalsite.controller;

import com.example.personalsite.dto.CommentDtos.CommentResponse;
import com.example.personalsite.dto.CommentDtos.CreateCommentRequest;
import com.example.personalsite.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

  private final CommentService commentService;

  public CommentController(CommentService commentService) {
    this.commentService = commentService;
  }

  @GetMapping
  public List<CommentResponse> list(@PathVariable Long postId) {
    return commentService.listByPost(postId);
  }

  @PostMapping
  public CommentResponse create(@PathVariable Long postId, @Valid @RequestBody CreateCommentRequest request) {
    return commentService.create(postId, request);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    commentService.delete(id);
  }
}
