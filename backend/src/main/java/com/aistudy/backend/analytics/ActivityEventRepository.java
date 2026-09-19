package com.aistudy.backend.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {
    Optional<ActivityEvent> findByIdempotencyKey(String key);
    List<ActivityEvent> findTop50ByUserIdAndProjectIdOrderByCreatedAtDesc(UUID userId, UUID projectId);
    List<ActivityEvent> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByUserIdAndProjectId(UUID userId, UUID projectId);
    long countByUserIdAndProjectIdAndEventType(UUID userId, UUID projectId, String eventType);
    Optional<ActivityEvent> findTop1ByUserIdOrderByCreatedAtDesc(UUID userId);
    List<ActivityEvent> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);
}
