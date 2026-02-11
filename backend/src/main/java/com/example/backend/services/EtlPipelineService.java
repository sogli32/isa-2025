package com.example.backend.services;

import com.example.backend.model.EtlPipelineResult;
import com.example.backend.model.Video;
import com.example.backend.repository.EtlPipelineResultRepository;
import com.example.backend.repository.VideoRepository;
import com.example.backend.repository.VideoViewRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EtlPipelineService {

    private final VideoViewRepository videoViewRepository;
    private final VideoRepository videoRepository;
    private final EtlPipelineResultRepository etlPipelineResultRepository;

    public EtlPipelineService(VideoViewRepository videoViewRepository,
                              VideoRepository videoRepository,
                              EtlPipelineResultRepository etlPipelineResultRepository) {
        this.videoViewRepository = videoViewRepository;
        this.videoRepository = videoRepository;
        this.etlPipelineResultRepository = etlPipelineResultRepository;
    }

    /**
     * ETL Pipeline - pokrece se jednom dnevno u ponoc.
     *
     * Extract: Cita preglede iz video_views tabele za poslednjih 7 dana.
     * Transform: Grupise po videu, racuna popularity score sa tezinskim faktorima.
     *   - Pregledi od pre x dana se mnoze sa tezinom (7 - x + 1).
     *   - Pregledi od pre 7 dana: tezina 1, od pre 6 dana: tezina 2, ..., od jucerasnjeg dana: tezina 7.
     * Load: Upisuje top 3 videa u etl_pipeline_results tabelu.
     */
    @Scheduled(cron = "0 0 0 * * *") // svaki dan u ponoc
    @Transactional
    public void runEtlPipeline() {
        System.out.println("=== ETL Pipeline started at " + LocalDateTime.now() + " ===");

        // ===== EXTRACT =====
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Object[]> viewData = videoViewRepository.countViewsPerVideoPerDay(sevenDaysAgo);

        // ===== TRANSFORM =====
        // Grupisanje: videoId -> { datum -> broj_pregleda }
        Map<Long, Map<LocalDate, Long>> viewsByVideoAndDate = new HashMap<>();

        for (Object[] row : viewData) {
            Long videoId = (Long) row[0];
            LocalDate date = (LocalDate) row[1];
            Long count = (Long) row[2];

            viewsByVideoAndDate
                    .computeIfAbsent(videoId, k -> new HashMap<>())
                    .put(date, count);
        }

        // Racunanje popularity score-a za svaki video
        LocalDate today = LocalDate.now();
        Map<Long, Double> popularityScores = new HashMap<>();

        for (Map.Entry<Long, Map<LocalDate, Long>> entry : viewsByVideoAndDate.entrySet()) {
            Long videoId = entry.getKey();
            Map<LocalDate, Long> dailyViews = entry.getValue();

            double score = 0.0;
            for (Map.Entry<LocalDate, Long> dayEntry : dailyViews.entrySet()) {
                LocalDate viewDate = dayEntry.getKey();
                Long viewCount = dayEntry.getValue();

                long daysAgo = ChronoUnit.DAYS.between(viewDate, today);
                if (daysAgo >= 0 && daysAgo <= 6) {
                    // Tezina: pregledi od pre x dana se mnoze sa (7 - x)
                    // x=0 (danas) -> tezina 7, x=1 (juce) -> tezina 6, ..., x=6 (pre 6 dana) -> tezina 1
                    // Ali po specifikaciji: pregledi od prethodnog dana sa tezinom 7, pre 7 dana sa tezinom 1
                    // "pregledi od pre x dana mnoze sa tezinom 7-x+1"
                    // x=1 (juce) -> 7-1+1=7, x=7 (pre 7 dana) -> 7-7+1=1
                    // Ali daysAgo=0 je danas, pa: daysAgo=0 -> nije "pre x dana" vec danas
                    // Tretiramo danas kao x=0, tezina = 7-0+1=8? Ne, bolje:
                    // daysAgo=1 (juce) -> weight=7, daysAgo=7 -> weight=1
                    // daysAgo=0 (danas) -> weight=7 (isto kao juce, ili 8 - ali drzimo se specifikacije)

                    double weight;
                    if (daysAgo == 0) {
                        // Danas - najvisa tezina (isto kao "prethodni dan" - 7)
                        weight = 7.0;
                    } else {
                        // pre x dana -> tezina = 7 - x + 1 = 8 - x
                        weight = 8.0 - daysAgo;
                    }
                    score += viewCount * weight;
                }
            }
            popularityScores.put(videoId, score);
        }

        // Sortiraj po score-u i uzmi top 3
        List<Map.Entry<Long, Double>> sortedEntries = popularityScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        // ===== LOAD =====
        EtlPipelineResult result = new EtlPipelineResult(LocalDateTime.now());

        if (sortedEntries.size() >= 1) {
            Video v1 = videoRepository.findById(sortedEntries.get(0).getKey()).orElse(null);
            result.setVideo1(v1);
            result.setScore1(sortedEntries.get(0).getValue());
        }
        if (sortedEntries.size() >= 2) {
            Video v2 = videoRepository.findById(sortedEntries.get(1).getKey()).orElse(null);
            result.setVideo2(v2);
            result.setScore2(sortedEntries.get(1).getValue());
        }
        if (sortedEntries.size() >= 3) {
            Video v3 = videoRepository.findById(sortedEntries.get(2).getKey()).orElse(null);
            result.setVideo3(v3);
            result.setScore3(sortedEntries.get(2).getValue());
        }

        etlPipelineResultRepository.save(result);

        System.out.println("=== ETL Pipeline completed. Top 3 videos: " +
                sortedEntries.stream()
                        .map(e -> "videoId=" + e.getKey() + " score=" + String.format("%.2f", e.getValue()))
                        .collect(Collectors.joining(", ")) + " ===");
    }
}
