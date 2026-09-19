package com.aistudy.backend.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    List<Quiz> findByProjectIdAndUserIdOrderByCreatedAtDesc(UUID projectId, UUID userId);
    Optional<Quiz> findByIdAndUserId(UUID id, UUID userId);
    long countByProjectIdAndUserId(UUID projectId, UUID userId);
    long countByUserId(UUID userId);
    List<Quiz> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);
}
