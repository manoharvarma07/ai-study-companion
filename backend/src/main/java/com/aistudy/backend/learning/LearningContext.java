package com.aistudy.backend.learning;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "learning_context")
@Getter
@Setter
@NoArgsConstructor
public class LearningContext {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "project_id", nullable = false, unique = true)
    private UUID projectId;

    @Column(columnDefinition = "TEXT")
    private String goals;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(name = "repeated_mistakes", columnDefinition = "TEXT")
    private String repeatedMistakes;

    @Column(name = "assessment_summary", columnDefinition = "TEXT")
    private String assessmentSummary;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { updatedAt = Instant.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
