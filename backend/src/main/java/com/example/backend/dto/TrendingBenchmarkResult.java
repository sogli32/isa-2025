package com.example.backend.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO za rezultate benchmark testiranja trending strategija.
 * 
 * [S2] Potrebno je pronaći i dokazati optimalnu meru između performansi 
 * i trendinga u realnom vremenu.
 */
public class TrendingBenchmarkResult {
    
    private String strategyName;
    private String description;
    private int iterations;
    private double avgResponseTimeMs;
    private double minResponseTimeMs;
    private double maxResponseTimeMs;
    private double medianResponseTimeMs;
    private double p95ResponseTimeMs; // 95th percentile
    private double p99ResponseTimeMs; // 99th percentile
    private double standardDeviation;
    private boolean isRealTime;
    private int videoCount;
    private List<Double> allResponseTimes; // za grafički prikaz
    
    public TrendingBenchmarkResult() {}
    
    public TrendingBenchmarkResult(String strategyName, String description) {
        this.strategyName = strategyName;
        this.description = description;
    }
    
    // Getters and Setters
    public String getStrategyName() {
        return strategyName;
    }
    
    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getIterations() {
        return iterations;
    }
    
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }
    
    public double getAvgResponseTimeMs() {
        return avgResponseTimeMs;
    }
    
    public void setAvgResponseTimeMs(double avgResponseTimeMs) {
        this.avgResponseTimeMs = avgResponseTimeMs;
    }
    
    public double getMinResponseTimeMs() {
        return minResponseTimeMs;
    }
    
    public void setMinResponseTimeMs(double minResponseTimeMs) {
        this.minResponseTimeMs = minResponseTimeMs;
    }
    
    public double getMaxResponseTimeMs() {
        return maxResponseTimeMs;
    }
    
    public void setMaxResponseTimeMs(double maxResponseTimeMs) {
        this.maxResponseTimeMs = maxResponseTimeMs;
    }
    
    public double getMedianResponseTimeMs() {
        return medianResponseTimeMs;
    }
    
    public void setMedianResponseTimeMs(double medianResponseTimeMs) {
        this.medianResponseTimeMs = medianResponseTimeMs;
    }
    
    public double getP95ResponseTimeMs() {
        return p95ResponseTimeMs;
    }
    
    public void setP95ResponseTimeMs(double p95ResponseTimeMs) {
        this.p95ResponseTimeMs = p95ResponseTimeMs;
    }
    
    public double getP99ResponseTimeMs() {
        return p99ResponseTimeMs;
    }
    
    public void setP99ResponseTimeMs(double p99ResponseTimeMs) {
        this.p99ResponseTimeMs = p99ResponseTimeMs;
    }
    
    public double getStandardDeviation() {
        return standardDeviation;
    }
    
    public void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }
    
    public boolean isRealTime() {
        return isRealTime;
    }
    
    public void setRealTime(boolean realTime) {
        isRealTime = realTime;
    }
    
    public int getVideoCount() {
        return videoCount;
    }
    
    public void setVideoCount(int videoCount) {
        this.videoCount = videoCount;
    }
    
    public List<Double> getAllResponseTimes() {
        return allResponseTimes;
    }
    
    public void setAllResponseTimes(List<Double> allResponseTimes) {
        this.allResponseTimes = allResponseTimes;
    }
}
