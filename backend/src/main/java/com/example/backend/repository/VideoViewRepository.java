package com.example.backend.repository;

import com.example.backend.model.VideoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VideoViewRepository extends JpaRepository<VideoView, Long> {

    @Query("SELECT vv.video.id, CAST(vv.viewedAt AS LocalDate), COUNT(vv) " +
           "FROM VideoView vv " +
           "WHERE vv.viewedAt >= :since " +
           "GROUP BY vv.video.id, CAST(vv.viewedAt AS LocalDate)")
    List<Object[]> countViewsPerVideoPerDay(@Param("since") LocalDateTime since);
}
