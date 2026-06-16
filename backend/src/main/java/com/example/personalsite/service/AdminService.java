package com.example.personalsite.service;

import com.example.personalsite.dto.AdminDtos.UpdateUserStatusRequest;
import com.example.personalsite.dto.CommentDtos.CommentResponse;
import com.example.personalsite.dto.PostDtos.PostResponse;
import com.example.personalsite.dto.UserDto;
import com.example.personalsite.entity.User;
import com.example.personalsite.exception.AppException;
import com.example.personalsite.repository.PostLikeRepository;
import com.example.personalsite.repository.UserRepository;
import com.example.personalsite.security.AuthService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

  private final UserRepository userRepository;
  private final AuthService authService;
  private final CommentService commentService;
  private final PostService postService;
  private final PostLikeRepository postLikeRepository;

  public AdminService(
      UserRepository userRepository,
      AuthService authService,
      CommentService commentService,
      PostService postService,
      PostLikeRepository postLikeRepository) {
    this.userRepository = userRepository;
    this.authService = authService;
    this.commentService = commentService;
    this.postService = postService;
    this.postLikeRepository = postLikeRepository;
  }

  @Transactional(readOnly = true)
  public List<UserDto> users() {
    authService.requireAdmin();
    return userRepository.findAll().stream().map(UserDto::from).toList();
  }

  @Transactional
  public UserDto updateStatus(Long id, UpdateUserStatusRequest request) {
    authService.requireAdmin();
    User user = userRepository.findById(id).orElseThrow(() -> AppException.notFound("账号不存在"));
    user.setStatus(request.status());
    return UserDto.from(user);
  }

  @Transactional
  public void deleteUser(Long id) {
    authService.requireAdmin();
    User user = userRepository.findById(id).orElseThrow(() -> AppException.notFound("账号不存在"));
    if (user.isAdmin()) {
      throw AppException.forbidden("不能删除管理员账号");
    }
    postService.deleteFeedByAuthor(user.getId());
    commentService.deleteByAuthor(user.getId());
    postLikeRepository.deleteByUserId(user.getId());
    userRepository.delete(user);
  }

  @Transactional(readOnly = true)
  public List<PostResponse> posts() {
    authService.requireAdmin();
    return postService.listFeed();
  }

  @Transactional(readOnly = true)
  public List<CommentResponse> comments() {
    authService.requireAdmin();
    return commentService.listAllForAdmin();
  }

  @Transactional
  public void deletePost(Long id) {
    authService.requireAdmin();
    postService.delete(id);
  }

  @Transactional
  public void deleteComment(Long id) {
    authService.requireAdmin();
    commentService.adminDelete(id);
  }
}
