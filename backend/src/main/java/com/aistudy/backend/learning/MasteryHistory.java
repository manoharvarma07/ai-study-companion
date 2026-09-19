package com.aistudy.backend.learning;

import com.aistudy.backend.knowledge.Concept;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mastery_history")
@Getter
@Setter
@NoArgsConstructor
public class MasteryHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private double score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }
}
