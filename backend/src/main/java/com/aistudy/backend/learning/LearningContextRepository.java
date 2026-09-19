package com.aistudy.backend.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LearningContextRepository extends JpaRepository<LearningContext, UUID> {
    Optional<LearningContext> findByProjectIdAndUserId(UUID projectId, UUID userId);
}
