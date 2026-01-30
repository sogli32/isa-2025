package com.example.backend.controller;

import com.example.backend.dto.TrendingBenchmarkReport;
import com.example.backend.services.TrendingBenchmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Kontroler za benchmark testiranje trending strategija.
 * 
 * [S2] Potrebno je pronaći i dokazati optimalnu meru između performansi 
 * i trendinga u realnom vremenu. Meru prikazati tabelarno ili grafički 
 * poredeći brzinu odziva u proizvoljnom periodu (response time ili latency).
 */
@RestController
@RequestMapping("/api/benchmark")
@CrossOrigin(origins = "http://localhost:4200")
public class BenchmarkController {
    
    private final TrendingBenchmarkService benchmarkService;
    
    public BenchmarkController(TrendingBenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }
    
    /**
     * Pokreće benchmark test svih trending strategija.
     * 
     * @param iterations Broj iteracija za svaku strategiju (default: 100)
     * @param limit Broj videa za vraćanje u trending listi (default: 20)
     * @return Kompletan izveštaj sa statistikama i preporukom
     * 
     * Primer poziva: GET /api/benchmark/trending?iterations=100&limit=20
     */
    @GetMapping("/trending")
    public ResponseEntity<TrendingBenchmarkReport> runTrendingBenchmark(
            @RequestParam(defaultValue = "100") int iterations,
            @RequestParam(defaultValue = "20") int limit
    ) {
        // Validacija
        if (iterations < 10) iterations = 10;
        if (iterations > 1000) iterations = 1000;
        if (limit < 1) limit = 1;
        if (limit > 100) limit = 100;
        
        TrendingBenchmarkReport report = benchmarkService.runBenchmark(iterations, limit);
        
        return ResponseEntity.ok(report);
    }
    
    /**
     * Brzi benchmark sa manjim brojem iteracija.
     * Koristan za brzu proveru performansi.
     */
    @GetMapping("/trending/quick")
    public ResponseEntity<TrendingBenchmarkReport> runQuickBenchmark() {
        TrendingBenchmarkReport report = benchmarkService.runBenchmark(20, 20);
        return ResponseEntity.ok(report);
    }
}
