package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "etl_pipeline_results")
public class EtlPipelineResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime executedAt;

    // Top 3 videa sa popularity score-ovima
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video1_id")
    private Video video1;

    @Column
    private Double score1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video2_id")
    private Video video2;

    @Column
    private Double score2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video3_id")
    private Video video3;

    @Column
    private Double score3;

    public EtlPipelineResult() {}

    public EtlPipelineResult(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public Video getVideo1() {
        return video1;
    }

    public void setVideo1(Video video1) {
        this.video1 = video1;
    }

    public Double getScore1() {
        return score1;
    }

    public void setScore1(Double score1) {
        this.score1 = score1;
    }

    public Video getVideo2() {
        return video2;
    }

    public void setVideo2(Video video2) {
        this.video2 = video2;
    }

    public Double getScore2() {
        return score2;
    }

    public void setScore2(Double score2) {
        this.score2 = score2;
    }

    public Video getVideo3() {
        return video3;
    }

    public void setVideo3(Video video3) {
        this.video3 = video3;
    }

    public Double getScore3() {
        return score3;
    }

    public void setScore3(Double score3) {
        this.score3 = score3;
    }
}
