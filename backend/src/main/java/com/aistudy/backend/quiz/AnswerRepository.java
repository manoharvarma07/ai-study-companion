package com.aistudy.backend.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {
    Optional<Answer> findByQuestionIdAndUserId(UUID questionId, UUID userId);
    List<Answer> findByQuizIdAndUserId(UUID quizId, UUID userId);

    @Query("select count(a) from Answer a where a.projectId = :projectId and a.userId = :userId")
    long countAttempts(UUID projectId, UUID userId);

    @Query("select avg(a.score) from Answer a where a.projectId = :projectId and a.userId = :userId and a.score is not null")
    Double averageScore(UUID projectId, UUID userId);

    /** Mistake count per concept name for adaptive selection. */
    @Query("select c.name as concept, count(a) as mistakes from Answer a "
            + "join a.question q join q.concept c "
            + "where a.projectId = :projectId and a.userId = :userId "
            + "and (a.score is null or a.score < 60) group by c.name")
    List<ConceptMistakes> mistakeCounts(UUID projectId, UUID userId);

    interface ConceptMistakes {
        String getConcept();
        long getMistakes();
    }
}
