package com.aistudy.backend.analytics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_events")
@Getter
@Setter
@NoArgsConstructor
public class ActivityEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "event_metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "idempotency_key", unique = true, length = 200)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }
}
