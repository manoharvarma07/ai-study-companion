package com.aistudy.backend.quiz;

import com.aistudy.backend.knowledge.Concept;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id")
    private Concept concept;

    /** MCQ or OPEN */
    @Column(nullable = false, length = 20)
    private String type = "MCQ";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    /** JSON array of options (MCQ). */
    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(length = 20)
    private String difficulty;

    @Column(nullable = false)
    private int position;
}
