package com.example.analytics.controller;

import com.example.analytics.dto.BenchmarkResult;
import com.example.analytics.service.MessageConsumerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final MessageConsumerService consumerService;

    public AnalyticsController(MessageConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    /**
     * Endpoint za dobijanje benchmark rezultata
     */
    @GetMapping("/benchmark")
    public ResponseEntity<Map<String, Object>> getBenchmarkResults() {
        List<BenchmarkResult> jsonResults = consumerService.getJsonResults();
        List<BenchmarkResult> protobufResults = consumerService.getProtobufResults();

        // Izračunaj prosečne vrednosti
        Map<String, Object> response = new HashMap<>();
        response.put("jsonResults", jsonResults);
        response.put("protobufResults", protobufResults);
        response.put("jsonStats", calculateStats(jsonResults));
        response.put("protobufStats", calculateStats(protobufResults));

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint za resetovanje benchmark rezultata
     */
    @PostMapping("/benchmark/reset")
    public ResponseEntity<String> resetBenchmark() {
        consumerService.clearResults();
        return ResponseEntity.ok("Benchmark results cleared");
    }

    /**
     * Helper metoda za računanje statistike
     */
    private Map<String, Object> calculateStats(List<BenchmarkResult> results) {
        if (results.isEmpty()) {
            return Map.of(
                    "count", 0,
                    "avgMessageSize", 0.0,
                    "avgDeserializationTimeNs", 0.0,
                    "avgDeserializationTimeMs", 0.0
            );
        }

        double avgSize = results.stream()
                .mapToInt(BenchmarkResult::getMessageSize)
                .average()
                .orElse(0.0);

        double avgDeserializationNs = results.stream()
                .mapToLong(BenchmarkResult::getDeserializationTimeNs)
                .average()
                .orElse(0.0);

        double avgDeserializationMs = avgDeserializationNs / 1_000_000.0;

        return Map.of(
                "count", results.size(),
                "avgMessageSize", avgSize,
                "avgDeserializationTimeNs", avgDeserializationNs,
                "avgDeserializationTimeMs", avgDeserializationMs
        );
    }
}
