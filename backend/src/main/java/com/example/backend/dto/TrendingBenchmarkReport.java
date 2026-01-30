package com.example.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kompletan izveštaj benchmark testiranja svih trending strategija.
 */
public class TrendingBenchmarkReport {
    
    private LocalDateTime timestamp;
    private int totalVideosInDatabase;
    private int iterationsPerStrategy;
    private List<TrendingBenchmarkResult> results;
    private String recommendation;
    private String optimalStrategy;
    
    public TrendingBenchmarkReport() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getTotalVideosInDatabase() {
        return totalVideosInDatabase;
    }
    
    public void setTotalVideosInDatabase(int totalVideosInDatabase) {
        this.totalVideosInDatabase = totalVideosInDatabase;
    }
    
    public int getIterationsPerStrategy() {
        return iterationsPerStrategy;
    }
    
    public void setIterationsPerStrategy(int iterationsPerStrategy) {
        this.iterationsPerStrategy = iterationsPerStrategy;
    }
    
    public List<TrendingBenchmarkResult> getResults() {
        return results;
    }
    
    public void setResults(List<TrendingBenchmarkResult> results) {
        this.results = results;
    }
    
    public String getRecommendation() {
        return recommendation;
    }
    
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
    
    public String getOptimalStrategy() {
        return optimalStrategy;
    }
    
    public void setOptimalStrategy(String optimalStrategy) {
        this.optimalStrategy = optimalStrategy;
    }
}
