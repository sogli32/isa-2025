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

    @Query("SELECT v FROM Video v ORDER BY v.popularityScore DESC")
    List<Video> findTrendingVideos(Pageable pageable);

    List<Video> findByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :videoId")
    int incrementViewCount(@Param("videoId") Long videoId);

    // ========== NOVI UPITI ZA PROSTORNU PRETRAGU ==========

    /**
     * Pronalazi videa u radijusu od korisnikove lokacije.
     * Koristi PostGIS earth_distance funkciju sa prostornim indeksom.
     *
     * @param userLat Geografska širina korisnika
     * @param userLng Geografska dužina korisnika
     * @param radiusMeters Radijus pretrage u metrima
     * @return Lista videa sortirana po udaljenosti
     */
    @Query(value = """
        SELECT v.*, 
               earth_distance(
                   ll_to_earth(v.latitude, v.longitude),
                   ll_to_earth(:userLat, :userLng)
               ) as distance
        FROM videos v
        WHERE v.latitude IS NOT NULL 
          AND v.longitude IS NOT NULL
          AND earth_box(ll_to_earth(:userLat, :userLng), :radiusMeters) @> ll_to_earth(v.latitude, v.longitude)
          AND earth_distance(ll_to_earth(v.latitude, v.longitude), ll_to_earth(:userLat, :userLng)) <= :radiusMeters
        ORDER BY distance ASC
        """, nativeQuery = true)
    List<Video> findVideosNearby(
            @Param("userLat") Double userLat,
            @Param("userLng") Double userLng,
            @Param("radiusMeters") Double radiusMeters
    );

    /**
     * Pronalazi popularne videa u radijusu.
     * Kombinuje prostornu pretragu sa popularity score-om.
     *
     * @param userLat Geografska širina
     * @param userLng Geografska dužina
     * @param radiusMeters Radijus pretrage
     * @param limit Maksimalan broj rezultata
     * @return Lista popularnih videa u blizini
     */
    @Query(value = """
        SELECT v.*, 
               earth_distance(
                   ll_to_earth(v.latitude, v.longitude),
                   ll_to_earth(:userLat, :userLng)
               ) as distance
        FROM videos v
        WHERE v.latitude IS NOT NULL 
          AND v.longitude IS NOT NULL
          AND earth_box(ll_to_earth(:userLat, :userLng), :radiusMeters) @> ll_to_earth(v.latitude, v.longitude)
          AND earth_distance(ll_to_earth(v.latitude, v.longitude), ll_to_earth(:userLat, :userLng)) <= :radiusMeters
        ORDER BY v.popularity_score DESC, distance ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Video> findPopularVideosNearby(
            @Param("userLat") Double userLat,
            @Param("userLng") Double userLng,
            @Param("radiusMeters") Double radiusMeters,
            @Param("limit") Integer limit
    );
}