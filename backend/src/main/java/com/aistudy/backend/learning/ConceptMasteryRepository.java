package com.aistudy.backend.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConceptMasteryRepository extends JpaRepository<ConceptMastery, UUID> {
    Optional<ConceptMastery> findByConceptIdAndUserId(UUID conceptId, UUID userId);
    List<ConceptMastery> findByProjectIdAndUserIdOrderByMasteryScoreAsc(UUID projectId, UUID userId);
    List<ConceptMastery> findByProjectIdAndUserId(UUID projectId, UUID userId);
}
