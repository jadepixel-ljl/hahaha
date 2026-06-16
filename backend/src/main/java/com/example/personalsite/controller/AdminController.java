package com.example.personalsite.controller;

import com.example.personalsite.dto.AdminDtos.UpdateUserStatusRequest;
import com.example.personalsite.dto.CommentDtos.CommentResponse;
import com.example.personalsite.dto.PostDtos.PostResponse;
import com.example.personalsite.dto.UserDto;
import com.example.personalsite.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final AdminService adminService;

  public AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  @GetMapping("/users")
  public List<UserDto> users() {
    return adminService.users();
  }

  @PatchMapping("/users/{id}/status")
  public UserDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
    return adminService.updateStatus(id, request);
  }

  @DeleteMapping("/users/{id}")
  public void deleteUser(@PathVariable Long id) {
    adminService.deleteUser(id);
  }

  @GetMapping("/posts")
  public List<PostResponse> posts() {
    return adminService.posts();
  }

  @GetMapping("/comments")
  public List<CommentResponse> comments() {
    return adminService.comments();
  }

  @DeleteMapping("/posts/{id}")
  public void deletePost(@PathVariable Long id) {
    adminService.deletePost(id);
  }

  @DeleteMapping("/comments/{id}")
  public void deleteComment(@PathVariable Long id) {
    adminService.deleteComment(id);
  }
}
