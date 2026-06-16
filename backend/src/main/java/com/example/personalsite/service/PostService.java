package com.example.personalsite.service;

import com.example.personalsite.dto.PostDtos.CreatePostRequest;
import com.example.personalsite.dto.PostDtos.LikeResponse;
import com.example.personalsite.dto.PostDtos.PostResponse;
import com.example.personalsite.dto.UserDto;
import com.example.personalsite.entity.Comment;
import com.example.personalsite.entity.Post;
import com.example.personalsite.entity.PostLike;
import com.example.personalsite.entity.User;
import com.example.personalsite.exception.AppException;
import com.example.personalsite.repository.CommentRepository;
import com.example.personalsite.repository.PostLikeRepository;
import com.example.personalsite.repository.PostRepository;
import com.example.personalsite.security.AuthService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

  private final PostRepository postRepository;
  private final PostLikeRepository postLikeRepository;
  private final CommentRepository commentRepository;
  private final JsonSupport jsonSupport;
  private final AuthService authService;

  public PostService(
      PostRepository postRepository,
      PostLikeRepository postLikeRepository,
      CommentRepository commentRepository,
      JsonSupport jsonSupport,
      AuthService authService) {
    this.postRepository = postRepository;
    this.postLikeRepository = postLikeRepository;
    this.commentRepository = commentRepository;
    this.jsonSupport = jsonSupport;
    this.authService = authService;
  }

  @Transactional(readOnly = true)
  public List<PostResponse> listFeed() {
    User currentUser = authService.requireUser();
    List<Post> posts = postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    List<PostResponse> responses = new ArrayList<>();
    for (Post post : posts) {
      responses.add(toResponse(post, currentUser));
    }
    return responses;
  }

  @Transactional(readOnly = true)
  public PostResponse getPost(Long id) {
    User currentUser = authService.requireUser();
    Post post = postRepository.findById(id).orElseThrow(() -> AppException.notFound("动态不存在"));
    return toResponse(post, currentUser);
  }

  @Transactional
  public PostResponse create(CreatePostRequest request) {
    User currentUser = authService.requireUser();
    Post post = new Post();
    post.setAuthor(currentUser);
    post.setContent(request.content().trim());
    post.setImageUrlsJson(jsonSupport.toJson(request.imageUrls()));
    return toResponse(postRepository.save(post), currentUser);
  }

  @Transactional
  public void delete(Long id) {
    User currentUser = authService.requireUser();
    Post post = postRepository.findById(id).orElseThrow(() -> AppException.notFound("动态不存在"));
    if (!currentUser.isAdmin() && !post.getAuthor().getId().equals(currentUser.getId())) {
      throw AppException.forbidden("只能删除自己的动态");
    }
    deleteDependencies(id);
    postRepository.delete(post);
  }

  @Transactional
  public void deleteFeedByAuthor(Long authorId) {
    List<Post> posts = postRepository.findByAuthorId(authorId);
    for (Post post : posts) {
      deleteDependencies(post.getId());
      postRepository.delete(post);
    }
  }

  @Transactional
  public LikeResponse like(Long postId) {
    User currentUser = authService.requireUser();
    Post post = postRepository.findById(postId).orElseThrow(() -> AppException.notFound("动态不存在"));
    if (postLikeRepository.existsByUserIdAndPostId(currentUser.getId(), postId)) {
      return new LikeResponse(true, postLikeRepository.countByPostId(postId));
    }
    PostLike like = new PostLike();
    like.setUser(currentUser);
    like.setPost(post);
    postLikeRepository.save(like);
    return new LikeResponse(true, postLikeRepository.countByPostId(postId));
  }

  @Transactional
  public LikeResponse unlike(Long postId) {
    User currentUser = authService.requireUser();
    if (!postRepository.existsById(postId)) {
      throw AppException.notFound("动态不存在");
    }
    postLikeRepository.deleteByUserIdAndPostId(currentUser.getId(), postId);
    return new LikeResponse(false, postLikeRepository.countByPostId(postId));
  }

  @Transactional(readOnly = true)
  public long likeCount(Long postId) {
    return postLikeRepository.countByPostId(postId);
  }

  @Transactional(readOnly = true)
  public boolean likedByMe(Long postId) {
    User currentUser = authService.requireUser();
    return postLikeRepository.existsByUserIdAndPostId(currentUser.getId(), postId);
  }

  @Transactional(readOnly = true)
  public boolean canDelete(Post post, User currentUser) {
    return currentUser.isAdmin() || post.getAuthor().getId().equals(currentUser.getId());
  }

  private PostResponse toResponse(Post post, User currentUser) {
    return new PostResponse(
        post.getId(),
        UserDto.from(post.getAuthor()),
        post.getContent(),
        jsonSupport.fromJson(post.getImageUrlsJson()),
        postLikeRepository.countByPostId(post.getId()),
        commentRepository.countByPostId(post.getId()),
        postLikeRepository.existsByUserIdAndPostId(currentUser.getId(), post.getId()),
        canDelete(post, currentUser),
        post.getCreatedAt(),
        post.getUpdatedAt());
  }

  private void deleteDependencies(Long postId) {
    commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtAsc(postId)
        .forEach(comment -> deleteCommentTree(comment, new java.util.HashSet<>()));
    postLikeRepository.deleteByPostId(postId);
  }

  private void deleteCommentTree(Comment comment, java.util.Set<Long> deleted) {
    if (!deleted.add(comment.getId())) {
      return;
    }
    commentRepository.findByParentIdOrderByCreatedAtAsc(comment.getId())
        .forEach(child -> deleteCommentTree(child, deleted));
    commentRepository.delete(comment);
  }
}
