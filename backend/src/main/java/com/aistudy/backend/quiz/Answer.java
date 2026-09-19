package com.aistudy.backend.quiz;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "answers")
@Getter
@Setter
@NoArgsConstructor
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "selected_option", length = 10)
    private String selectedOption;

    @Column
    private Double score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(columnDefinition = "TEXT")
    private String understood;

    @Column(columnDefinition = "TEXT")
    private String missing;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }
}
