package com.aistudy.backend.analytics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_usage")
@Getter
@Setter
@NoArgsConstructor
public class AiUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(nullable = false, length = 40)
    private String feature;

    @Column(nullable = false, length = 120)
    private String model;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "estimated_cost", nullable = false)
    private double estimatedCost;

    @Column(nullable = false)
    private boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }
}
