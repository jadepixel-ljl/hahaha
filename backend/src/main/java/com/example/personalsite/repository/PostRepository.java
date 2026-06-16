package com.example.personalsite.repository;

import com.example.personalsite.entity.Post;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
  List<Post> findByAuthorId(Long authorId);
}
