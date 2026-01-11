package com.example.backend.repository;

import com.example.backend.model.Comment;
import com.example.backend.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByVideoOrderByCreatedAtAsc(Video video);
}
