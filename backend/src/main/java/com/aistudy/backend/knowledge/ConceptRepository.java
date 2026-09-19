package com.aistudy.backend.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConceptRepository extends JpaRepository<Concept, UUID> {
    List<Concept> findByProjectIdAndUserIdOrderByName(UUID projectId, UUID userId);
    Optional<Concept> findByProjectIdAndUserIdAndNameIgnoreCase(UUID projectId, UUID userId, String name);
    Optional<Concept> findByIdAndUserId(UUID id, UUID userId);
    long countByProjectIdAndUserId(UUID projectId, UUID userId);
}
