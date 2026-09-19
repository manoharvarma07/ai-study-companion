package com.aistudy.backend.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {
    List<Recommendation> findTop20ByProjectIdAndUserIdOrderByCreatedAtDesc(UUID projectId, UUID userId);
}
