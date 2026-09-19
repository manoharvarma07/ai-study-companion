package com.aistudy.backend.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MasteryHistoryRepository extends JpaRepository<MasteryHistory, UUID> {
    List<MasteryHistory> findTop20ByConceptIdAndUserIdOrderByCreatedAtDesc(UUID conceptId, UUID userId);
    List<MasteryHistory> findByProjectIdAndUserIdOrderByCreatedAtDesc(UUID projectId, UUID userId);
}
