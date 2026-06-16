package com.example.personalsite.repository;

import com.example.personalsite.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
  List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

  List<Comment> findByPostIdAndParentIsNullOrderByCreatedAtAsc(Long postId);

  List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

  long countByPostId(Long postId);
}
