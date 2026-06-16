package com.example.personalsite.controller;

import com.example.personalsite.dto.PostDtos.CreatePostRequest;
import com.example.personalsite.dto.PostDtos.LikeResponse;
import com.example.personalsite.dto.PostDtos.PostResponse;
import com.example.personalsite.service.PostService;
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
@RequestMapping("/api/posts")
public class PostController {

  private final PostService postService;

  public PostController(PostService postService) {
    this.postService = postService;
  }

  @GetMapping
  public List<PostResponse> list() {
    return postService.listFeed();
  }

  @GetMapping("/{id}")
  public PostResponse get(@PathVariable Long id) {
    return postService.getPost(id);
  }

  @PostMapping
  public PostResponse create(@Valid @RequestBody CreatePostRequest request) {
    return postService.create(request);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    postService.delete(id);
  }

  @PostMapping("/{id}/like")
  public LikeResponse like(@PathVariable Long id) {
    return postService.like(id);
  }

  @DeleteMapping("/{id}/like")
  public LikeResponse unlike(@PathVariable Long id) {
    return postService.unlike(id);
  }
}
