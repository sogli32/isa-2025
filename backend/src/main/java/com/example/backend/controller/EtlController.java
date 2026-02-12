package com.example.backend.controller;

import com.example.backend.dto.EtlPopularVideoResponse;
import com.example.backend.dto.EtlPopularVideoResponse.EtlVideoEntry;
import com.example.backend.model.EtlPipelineResult;
import com.example.backend.model.Video;
import com.example.backend.repository.EtlPipelineResultRepository;
import com.example.backend.services.EtlPipelineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/etl")
//@CrossOrigin(origins = "http://localhost:4200")
public class EtlController {

    private final EtlPipelineResultRepository etlPipelineResultRepository;
    private final EtlPipelineService etlPipelineService;

    public EtlController(EtlPipelineResultRepository etlPipelineResultRepository,
                         EtlPipelineService etlPipelineService) {
        this.etlPipelineResultRepository = etlPipelineResultRepository;
        this.etlPipelineService = etlPipelineService;
    }

    /**
     * Vraca top 3 najpopularnija videa iz poslednjeg ETL izvrsavanja.
     * Dostupno samo ulogovanim korisnicima.
     */
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularVideos() {
        Optional<EtlPipelineResult> latestResult = etlPipelineResultRepository.findTopByOrderByExecutedAtDesc();

        if (latestResult.isEmpty()) {
            // Vrati prazan rezultat umesto 404 da frontend moze da prikaze poruku
            EtlPopularVideoResponse emptyResponse = new EtlPopularVideoResponse(null, new ArrayList<>());
            return ResponseEntity.ok(emptyResponse);
        }

        EtlPipelineResult result = latestResult.get();
        List<EtlVideoEntry> entries = new ArrayList<>();

        if (result.getVideo1() != null) {
            Video v = result.getVideo1();
            entries.add(new EtlVideoEntry(v.getId(), v.getTitle(),
                    v.getUser().getUsername(), result.getScore1(), v.getViewCount()));
        }
        if (result.getVideo2() != null) {
            Video v = result.getVideo2();
            entries.add(new EtlVideoEntry(v.getId(), v.getTitle(),
                    v.getUser().getUsername(), result.getScore2(), v.getViewCount()));
        }
        if (result.getVideo3() != null) {
            Video v = result.getVideo3();
            entries.add(new EtlVideoEntry(v.getId(), v.getTitle(),
                    v.getUser().getUsername(), result.getScore3(), v.getViewCount()));
        }

        EtlPopularVideoResponse response = new EtlPopularVideoResponse(result.getExecutedAt(), entries);
        return ResponseEntity.ok(response);
    }

    /**
     * Rucno pokretanje ETL pipeline-a (za testiranje).
     */
    @PostMapping("/run")
    public ResponseEntity<String> runPipeline() {
        etlPipelineService.runEtlPipeline();
        return ResponseEntity.ok("ETL pipeline uspesno pokrenut.");
    }
}
