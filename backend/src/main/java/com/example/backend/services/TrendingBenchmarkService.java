package com.example.backend.services;

import com.example.backend.dto.TrendingBenchmarkReport;
import com.example.backend.dto.TrendingBenchmarkResult;
import com.example.backend.dto.VideoResponse;
import com.example.backend.model.Video;
import com.example.backend.repository.CommentRepository;
import com.example.backend.repository.VideoLikeRepository;
import com.example.backend.repository.VideoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servis za benchmark testiranje različitih strategija računanja trendinga.
 * 
 * [S2] Potrebno je pronaći i dokazati optimalnu meru između performansi 
 * i trendinga u realnom vremenu. Meru prikazati tabelarno ili grafički 
 * poredeći brzinu odziva u proizvoljnom periodu (response time ili latency).
 * 
 * Testirane strategije:
 * 1. REAL_TIME - računanje popularnosti u realnom vremenu za svaki zahtev
 * 2. CACHED - korišćenje pre-izračunatog popularityScore iz baze
 * 3. HYBRID - keširani rezultati sa periodičnim osvežavanjem
 */
@Service
public class TrendingBenchmarkService {
    
    private final VideoRepository videoRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final CommentRepository commentRepository;
    
    // Cache za hybrid strategiju
    private List<Video> cachedTrendingVideos = new ArrayList<>();
    private LocalDateTime cacheTimestamp = LocalDateTime.MIN;
    private static final long CACHE_TTL_SECONDS = 60; // 1 minut
    
    // Weights (isti kao u PopularityCalculationService)
    private static final double VIEW_WEIGHT = 1.0;
    private static final double LIKE_WEIGHT = 5.0;
    private static final double COMMENT_WEIGHT = 10.0;
    
    public TrendingBenchmarkService(VideoRepository videoRepository,
                                     VideoLikeRepository videoLikeRepository,
                                     CommentRepository commentRepository) {
        this.videoRepository = videoRepository;
        this.videoLikeRepository = videoLikeRepository;
        this.commentRepository = commentRepository;
    }
    
    /**
     * Izvršava kompletan benchmark test svih strategija.
     */
    public TrendingBenchmarkReport runBenchmark(int iterations, int limit) {
        TrendingBenchmarkReport report = new TrendingBenchmarkReport();
        report.setTotalVideosInDatabase((int) videoRepository.count());
        report.setIterationsPerStrategy(iterations);
        
        List<TrendingBenchmarkResult> results = new ArrayList<>();
        
        // Warm-up faza (da se izbegne cold start)
        warmUp(limit);
        
        // Test svake strategije
        results.add(benchmarkRealTimeStrategy(iterations, limit));
        results.add(benchmarkCachedStrategy(iterations, limit));
        results.add(benchmarkHybridStrategy(iterations, limit));
        
        report.setResults(results);
        
        // Odredi optimalnu strategiju
        determineOptimalStrategy(report);
        
        return report;
    }
    
    /**
     * Warm-up faza za JVM i database connection pool.
     */
    private void warmUp(int limit) {
        for (int i = 0; i < 5; i++) {
            getTrendingRealTime(limit);
            getTrendingCached(limit);
            getTrendingHybrid(limit);
        }
    }
    
    // ==================== STRATEGIJA 1: REAL-TIME ====================
    
    /**
     * REAL-TIME strategija: računa popularnost za svaki video pri svakom zahtevu.
     * Prednosti: Uvek ažurni podaci
     * Mane: Sporo za velike količine videa
     */
    public List<Video> getTrendingRealTime(int limit) {
        List<Video> allVideos = videoRepository.findAll();
        
        // Računa popularnost za svaki video
        Map<Video, Double> scores = new HashMap<>();
        for (Video video : allVideos) {
            double score = calculatePopularityScoreRealTime(video);
            scores.put(video, score);
        }
        
        // Sortira po score-u i vraća top N
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Video, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    private double calculatePopularityScoreRealTime(Video video) {
        long viewCount = video.getViewCount();
        long likeCount = videoLikeRepository.countByVideo(video);
        long commentCount = commentRepository.countByVideo(video);
        
        double engagementScore = (viewCount * VIEW_WEIGHT) +
                (likeCount * LIKE_WEIGHT) +
                (commentCount * COMMENT_WEIGHT);
        
        double timeDecay = calculateTimeDecay(video.getCreatedAt());
        
        return engagementScore * timeDecay;
    }
    
    private double calculateTimeDecay(LocalDateTime createdAt) {
        long hoursOld = Duration.between(createdAt, LocalDateTime.now()).toHours();
        double daysOld = hoursOld / 24.0;
        double halfLifeDays = 7.0;
        return Math.pow(2, -daysOld / halfLifeDays);
    }
    
    private TrendingBenchmarkResult benchmarkRealTimeStrategy(int iterations, int limit) {
        TrendingBenchmarkResult result = new TrendingBenchmarkResult(
            "REAL_TIME",
            "Računa popularnost za svaki video pri svakom zahtevu. Uvek ažurni podaci."
        );
        result.setRealTime(true);
        
        List<Double> times = new ArrayList<>();
        
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            List<Video> trending = getTrendingRealTime(limit);
            long end = System.nanoTime();
            
            double timeMs = (end - start) / 1_000_000.0;
            times.add(timeMs);
            result.setVideoCount(trending.size());
        }
        
        calculateStatistics(result, times, iterations);
        return result;
    }
    
    // ==================== STRATEGIJA 2: CACHED ====================
    
    /**
     * CACHED strategija: koristi pre-izračunati popularityScore iz baze.
     * Popularnost se računa periodično (na svakih 15 min u PopularityCalculationService).
     * Prednosti: Veoma brzo
     * Mane: Podaci mogu biti zastareli do 15 minuta
     */
    public List<Video> getTrendingCached(int limit) {
        return videoRepository.findTrendingVideos(PageRequest.of(0, limit));
    }
    
    private TrendingBenchmarkResult benchmarkCachedStrategy(int iterations, int limit) {
        TrendingBenchmarkResult result = new TrendingBenchmarkResult(
            "CACHED",
            "Koristi pre-izračunati popularityScore iz baze (osvežava se svakih 15 min)."
        );
        result.setRealTime(false);
        
        List<Double> times = new ArrayList<>();
        
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            List<Video> trending = getTrendingCached(limit);
            long end = System.nanoTime();
            
            double timeMs = (end - start) / 1_000_000.0;
            times.add(timeMs);
            result.setVideoCount(trending.size());
        }
        
        calculateStatistics(result, times, iterations);
        return result;
    }
    
    // ==================== STRATEGIJA 3: HYBRID ====================
    
    /**
     * HYBRID strategija: keširani rezultati sa kraćim TTL-om.
     * Kombinuje brzinu keširanja sa relativno svežim podacima.
     * Prednosti: Brzo, podaci stari max 1 minut
     * Mane: Kompleksnija implementacija
     */
    public List<Video> getTrendingHybrid(int limit) {
        LocalDateTime now = LocalDateTime.now();
        
        // Proveri da li je cache validan
        if (cachedTrendingVideos.isEmpty() || 
            Duration.between(cacheTimestamp, now).getSeconds() > CACHE_TTL_SECONDS) {
            
            // Osvježi cache - računa real-time
            cachedTrendingVideos = getTrendingRealTime(limit);
            cacheTimestamp = now;
        }
        
        return cachedTrendingVideos;
    }
    
    private TrendingBenchmarkResult benchmarkHybridStrategy(int iterations, int limit) {
        TrendingBenchmarkResult result = new TrendingBenchmarkResult(
            "HYBRID",
            "Keširani rezultati sa 60s TTL. Balans između brzine i svežine podataka."
        );
        result.setRealTime(false);
        
        List<Double> times = new ArrayList<>();
        
        // Resetuj cache pre testa
        cachedTrendingVideos.clear();
        cacheTimestamp = LocalDateTime.MIN;
        
        for (int i = 0; i < iterations; i++) {
            // Svaki 10. zahtev simulira cache miss
            if (i % 10 == 0) {
                cacheTimestamp = LocalDateTime.MIN;
            }
            
            long start = System.nanoTime();
            List<Video> trending = getTrendingHybrid(limit);
            long end = System.nanoTime();
            
            double timeMs = (end - start) / 1_000_000.0;
            times.add(timeMs);
            result.setVideoCount(trending.size());
        }
        
        calculateStatistics(result, times, iterations);
        return result;
    }
    
    // ==================== STATISTIKA ====================
    
    private void calculateStatistics(TrendingBenchmarkResult result, List<Double> times, int iterations) {
        result.setIterations(iterations);
        result.setAllResponseTimes(new ArrayList<>(times));
        
        // Sortiraj za percentile
        Collections.sort(times);
        
        // Min, Max
        result.setMinResponseTimeMs(times.get(0));
        result.setMaxResponseTimeMs(times.get(times.size() - 1));
        
        // Average
        double sum = times.stream().mapToDouble(Double::doubleValue).sum();
        double avg = sum / times.size();
        result.setAvgResponseTimeMs(Math.round(avg * 100.0) / 100.0);
        
        // Median
        int mid = times.size() / 2;
        double median = times.size() % 2 == 0 
            ? (times.get(mid - 1) + times.get(mid)) / 2.0 
            : times.get(mid);
        result.setMedianResponseTimeMs(Math.round(median * 100.0) / 100.0);
        
        // Percentiles
        result.setP95ResponseTimeMs(getPercentile(times, 95));
        result.setP99ResponseTimeMs(getPercentile(times, 99));
        
        // Standard Deviation
        double variance = times.stream()
            .mapToDouble(t -> Math.pow(t - avg, 2))
            .sum() / times.size();
        result.setStandardDeviation(Math.round(Math.sqrt(variance) * 100.0) / 100.0);
    }
    
    private double getPercentile(List<Double> sortedList, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sortedList.size()) - 1;
        index = Math.max(0, Math.min(index, sortedList.size() - 1));
        return Math.round(sortedList.get(index) * 100.0) / 100.0;
    }
    
    /**
     * Određuje optimalnu strategiju na osnovu rezultata benchmarka.
     */
    private void determineOptimalStrategy(TrendingBenchmarkReport report) {
        List<TrendingBenchmarkResult> results = report.getResults();
        
        // Pronađi najbrži average response time
        TrendingBenchmarkResult fastest = results.stream()
            .min(Comparator.comparingDouble(TrendingBenchmarkResult::getAvgResponseTimeMs))
            .orElse(null);
        
        // Pronađi real-time strategiju
        TrendingBenchmarkResult realTime = results.stream()
            .filter(TrendingBenchmarkResult::isRealTime)
            .findFirst()
            .orElse(null);
        
        TrendingBenchmarkResult cached = results.stream()
            .filter(r -> "CACHED".equals(r.getStrategyName()))
            .findFirst()
            .orElse(null);
        
        TrendingBenchmarkResult hybrid = results.stream()
            .filter(r -> "HYBRID".equals(r.getStrategyName()))
            .findFirst()
            .orElse(null);
        
        StringBuilder recommendation = new StringBuilder();
        
        // Izračunaj speedup faktore
        double cachedSpeedup = 1.0;
        double hybridSpeedup = 1.0;
        
        if (realTime != null && cached != null && cached.getAvgResponseTimeMs() > 0) {
            cachedSpeedup = realTime.getAvgResponseTimeMs() / cached.getAvgResponseTimeMs();
            recommendation.append(String.format(
                "CACHED strategija je %.1fx brža od REAL_TIME strategije. ", cachedSpeedup));
        }
        
        if (hybrid != null && realTime != null && hybrid.getAvgResponseTimeMs() > 0) {
            hybridSpeedup = realTime.getAvgResponseTimeMs() / hybrid.getAvgResponseTimeMs();
            recommendation.append(String.format(
                "HYBRID strategija je %.1fx brža od REAL_TIME uz osvežavanje svakih 60s. ", hybridSpeedup));
        }
        
        // NOVA LOGIKA: Određivanje optimalne strategije na osnovu STVARNIH REZULTATA
        // Ako je neka strategija značajno brža (>2x), ona je optimalna
        
        String optimalStrategy;
        String reasoning;
        
        if (fastest != null) {
            optimalStrategy = fastest.getStrategyName();
        } else {
            optimalStrategy = "REAL_TIME";
        }
        
        // Ako je CACHED ili HYBRID značajno brži od REAL_TIME, izaberi ga
        if (cachedSpeedup >= 2.0 || hybridSpeedup >= 2.0) {
            // Izaberi najbrži
            if (cached != null && hybrid != null) {
                if (cached.getAvgResponseTimeMs() <= hybrid.getAvgResponseTimeMs()) {
                    optimalStrategy = "CACHED";
                    reasoning = String.format(
                        "CACHED je %.1fx brži od REAL_TIME i pruža najbolje performanse. " +
                        "Podaci se osvežavaju svakih 15 minuta što je prihvatljivo za većinu aplikacija.",
                        cachedSpeedup);
                } else {
                    optimalStrategy = "HYBRID";
                    reasoning = String.format(
                        "HYBRID je %.1fx brži od REAL_TIME uz osvežavanje svakih 60s. " +
                        "Pruža dobar balans između performansi i svežine podataka.",
                        hybridSpeedup);
                }
            } else if (cachedSpeedup >= hybridSpeedup) {
                optimalStrategy = "CACHED";
                reasoning = String.format("CACHED je %.1fx brži i pruža najbolje performanse.", cachedSpeedup);
            } else {
                optimalStrategy = "HYBRID";
                reasoning = String.format("HYBRID je %.1fx brži uz bolje osvežavanje podataka.", hybridSpeedup);
            }
        } else {
            // Ako nema značajne razlike, REAL_TIME je OK jer pruža uvek ažurne podatke
            optimalStrategy = "REAL_TIME";
            reasoning = "Razlike u performansama su minimalne (<2x), pa REAL_TIME pruža najbolji balans " +
                       "jer garantuje uvek ažurne podatke bez kašnjenja.";
        }
        
        report.setOptimalStrategy(optimalStrategy);
        
        // Dodaj obrazloženje
        recommendation.append("\n\n📊 ZAKLJUČAK BENCHMARKA:\n");
        recommendation.append("Optimalna strategija: ").append(optimalStrategy).append("\n");
        recommendation.append("Obrazloženje: ").append(reasoning).append("\n\n");
        
        // Dodaj kontekst o broju videa
        recommendation.append("📈 Kontekst: Baza trenutno sadrži ").append(report.getTotalVideosInDatabase()).append(" videa. ");
        if (report.getTotalVideosInDatabase() < 100) {
            recommendation.append("Sa većim brojem videa (100+), razlike u performansama će biti izraženije.");
        } else if (report.getTotalVideosInDatabase() < 1000) {
            recommendation.append("Ovo je srednja veličina baze gde keširane strategije pokazuju značajnu prednost.");
        } else {
            recommendation.append("Za bazu ove veličine, keširanje je praktično neophodno za prihvatljive performanse.");
        }
        
        report.setRecommendation(recommendation.toString());
    }
}