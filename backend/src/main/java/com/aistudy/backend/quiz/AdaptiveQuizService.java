package com.aistudy.backend.quiz;

import com.aistudy.backend.knowledge.Concept;
import com.aistudy.backend.knowledge.ConceptRepository;
import com.aistudy.backend.learning.ConceptMasteryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Adaptive selection: weakest estimated mastery first, boosted by past
 * mistakes. Never random — every quiz targets what the learner needs most.
 */
@Service
public class AdaptiveQuizService {
    private final ConceptRepository concepts;
    private final ConceptMasteryRepository mastery;
    private final AnswerRepository answers;

    public AdaptiveQuizService(ConceptRepository concepts, ConceptMasteryRepository mastery,
                               AnswerRepository answers) {
        this.concepts = concepts;
        this.mastery = mastery;
        this.answers = answers;
    }

    public record FocusConcept(Concept concept, double masteryScore, int mistakes) {}

    @Transactional(readOnly = true)
    public List<FocusConcept> selectFocus(UUID userId, UUID projectId, int limit) {
        Map<String, Long> mistakes = new HashMap<>();
        try {
            answers.mistakeCounts(projectId, userId)
                    .forEach(m -> mistakes.put(m.getConcept(), m.getMistakes()));
        } catch (Exception ignored) {
        }
        Map<UUID, Double> masteryByConcept = new HashMap<>();
        mastery.findByProjectIdAndUserId(projectId, userId)
                .forEach(m -> masteryByConcept.put(m.getConcept().getId(), m.getMasteryScore()));

        List<FocusConcept> all = new ArrayList<>();
        for (Concept c : concepts.findByProjectIdAndUserIdOrderByName(projectId, userId)) {
            double ms = masteryByConcept.getOrDefault(c.getId(), 50.0);
            int mk = mistakes.getOrDefault(c.getName(), 0L).intValue();
            all.add(new FocusConcept(c, ms, mk));
        }
        // Priority: low mastery first; mistakes break ties (higher mistakes first).
        all.sort(Comparator.comparingDouble((FocusConcept f) -> f.masteryScore() - f.mistakes() * 5.0));
        if (all.isEmpty()) {
            return List.of();
        }
        return all.stream().limit(Math.max(1, limit)).toList();
    }
}
