package com.aistudy.backend.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByQuizIdAndUserIdOrderByPositionAsc(UUID quizId, UUID userId);
    Optional<Question> findByIdAndUserId(UUID id, UUID userId);
    long countByProjectIdAndUserId(UUID projectId, UUID userId);
}
