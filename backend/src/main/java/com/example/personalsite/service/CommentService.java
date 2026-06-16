package com.example.personalsite.service;

import com.example.personalsite.dto.CommentDtos.CommentResponse;
import com.example.personalsite.dto.CommentDtos.CreateCommentRequest;
import com.example.personalsite.dto.UserDto;
import com.example.personalsite.entity.Comment;
import com.example.personalsite.entity.Post;
import com.example.personalsite.entity.User;
import com.example.personalsite.exception.AppException;
import com.example.personalsite.repository.CommentRepository;
import com.example.personalsite.repository.PostRepository;
import com.example.personalsite.security.AuthService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

  private final CommentRepository commentRepository;
  private final PostRepository postRepository;
  private final AuthService authService;

  public CommentService(CommentRepository commentRepository, PostRepository postRepository, AuthService authService) {
    this.commentRepository = commentRepository;
    this.postRepository = postRepository;
    this.authService = authService;
  }

  @Transactional(readOnly = true)
  public List<CommentResponse> listByPost(Long postId) {
    authService.requireUser();
    List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    Map<Long, CommentResponse> nodes = new HashMap<>();
    List<CommentResponse> roots = new ArrayList<>();
    User currentUser = authService.requireUser();
    comments.forEach(comment -> {
      CommentResponse response = toResponse(comment, currentUser, List.of());
      nodes.put(comment.getId(), response);
    });
    comments.forEach(comment -> {
      CommentResponse current = nodes.get(comment.getId());
      Long parentId = comment.getParent() == null ? null : comment.getParent().getId();
      if (parentId == null) {
        roots.add(current);
      } else {
        CommentResponse parent = nodes.get(parentId);
        if (parent != null) {
          parent.replies().add(current);
        } else {
          roots.add(current);
        }
      }
    });
    roots.sort(Comparator.comparing(CommentResponse::createdAt));
    return roots;
  }

  @Transactional
  public CommentResponse create(Long postId, CreateCommentRequest request) {
    User currentUser = authService.requireUser();
    Post post = postRepository.findById(postId).orElseThrow(() -> AppException.notFound("动态不存在"));
    Comment comment = new Comment();
    comment.setPost(post);
    comment.setAuthor(currentUser);
    comment.setContent(request.content().trim());
    if (request.parentId() != null) {
      Comment parent = commentRepository.findById(request.parentId()).orElseThrow(() -> AppException.notFound("楼层不存在"));
      if (!parent.getPost().getId().equals(postId)) {
        throw AppException.badRequest("回复目标不属于当前动态");
      }
      comment.setParent(parent);
    }
    return toResponse(commentRepository.save(comment), currentUser, List.of());
  }

  @Transactional
  public void delete(Long id) {
    User currentUser = authService.requireUser();
    Comment comment = commentRepository.findById(id).orElseThrow(() -> AppException.notFound("评论不存在"));
    if (!currentUser.isAdmin() && !comment.getAuthor().getId().equals(currentUser.getId())) {
      throw AppException.forbidden("只能删除自己的评论");
    }
    deleteSubtree(comment);
  }

  @Transactional
  public void adminDelete(Long id) {
    authService.requireAdmin();
    Comment comment = commentRepository.findById(id).orElseThrow(() -> AppException.notFound("评论不存在"));
    deleteSubtree(comment);
  }

  @Transactional
  public void deleteByAuthor(Long authorId) {
    List<Comment> comments = commentRepository.findAll().stream()
        .filter(comment -> comment.getAuthor().getId().equals(authorId))
        .toList();
    Set<Long> deleted = new HashSet<>();
    for (Comment comment : comments) {
      deleteSubtree(comment, deleted);
    }
  }

  @Transactional(readOnly = true)
  public List<CommentResponse> listAllForAdmin() {
    authService.requireAdmin();
    User currentUser = authService.requireUser();
    return commentRepository.findAll().stream()
        .sorted(Comparator.comparing(Comment::getCreatedAt).reversed())
        .map(comment -> toResponse(comment, currentUser, List.of()))
        .toList();
  }

  private void deleteSubtree(Comment comment) {
    deleteSubtree(comment, new HashSet<>());
  }

  private void deleteSubtree(Comment comment, Set<Long> deleted) {
    if (!deleted.add(comment.getId())) {
      return;
    }
    commentRepository.findByParentIdOrderByCreatedAtAsc(comment.getId()).forEach(child -> deleteSubtree(child, deleted));
    commentRepository.delete(comment);
  }

  private CommentResponse toResponse(Comment comment, User currentUser, List<CommentResponse> replies) {
    boolean canDelete = currentUser.isAdmin() || comment.getAuthor().getId().equals(currentUser.getId());
    return new CommentResponse(
        comment.getId(),
        comment.getPost().getId(),
        comment.getParent() == null ? null : comment.getParent().getId(),
        UserDto.from(comment.getAuthor()),
        comment.getContent(),
        canDelete,
        comment.getCreatedAt(),
        comment.getUpdatedAt(),
        new ArrayList<>(replies));
  }
}
