package com.example.backend.repository;

import com.example.backend.model.VideoLike;
import com.example.backend.model.Video;
import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VideoLikeRepository extends JpaRepository<VideoLike, Long> {
    Optional<VideoLike> findByVideoAndUser(Video video, User user);
    Long countByVideo(Video video);
    void deleteAllByVideo(Video video);
}
