package com.aistudy.backend.learning;

import com.aistudy.backend.knowledge.Concept;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "concept_mastery")
@Getter
@Setter
@NoArgsConstructor
public class ConceptMastery {
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

    /** Estimated mastery 0-100. A prototype learning estimate, NOT exact science. */
    @Column(name = "mastery_score", nullable = false)
    private double masteryScore;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { updatedAt = Instant.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
