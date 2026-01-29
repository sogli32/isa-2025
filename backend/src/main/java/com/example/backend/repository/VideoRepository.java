package com.example.backend.repository;

import com.example.backend.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {


    List<Video> findAllByOrderByCreatedAtDesc();

    // U VideoRepository.java:
    @Query("SELECT v FROM Video v ORDER BY v.popularityScore DESC")
    List<Video> findTrendingVideos(Pageable pageable);
    List<Video> findByUserId(Long userId);



    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :videoId")
    int incrementViewCount(@Param("videoId") Long videoId);
}
