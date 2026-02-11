package com.example.backend.repository;

import com.example.backend.model.EtlPipelineResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EtlPipelineResultRepository extends JpaRepository<EtlPipelineResult, Long> {

    Optional<EtlPipelineResult> findTopByOrderByExecutedAtDesc();
}
