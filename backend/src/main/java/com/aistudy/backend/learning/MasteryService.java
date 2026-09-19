package com.aistudy.backend.learning;

import com.aistudy.backend.analytics.ActivityEventService;
import com.aistudy.backend.knowledge.Concept;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Estimated mastery tracking. newMastery = 0.7 * old + 0.3 * score.
 * Always presented as an estimate, never as exact measurement.
 */
@Service
public class MasteryService {
    private final ConceptMasteryRepository mastery;
    private final MasteryHistoryRepository history;
    private final ActivityEventService events;

    public MasteryService(ConceptMasteryRepository mastery, MasteryHistoryRepository history,
                          ActivityEventService events) {
        this.mastery = mastery;
        this.history = history;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<MasteryDto.MasteryResponse> list(UUID userId, UUID projectId) {
        return mastery.findByProjectIdAndUserId(projectId, userId).stream()
                .map(m -> new MasteryDto.MasteryResponse(
                        m.getConcept().getId().toString(),
                        m.getConcept().getName(),
                        round(m.getMasteryScore()), round(m.getConfidence()),
                        m.getUpdatedAt() == null ? null : m.getUpdatedAt().toString()))
                .toList();
    }

    @Transactional
    public ConceptMastery update(UUID userId, UUID projectId, Concept concept, double assessmentScore) {
        double score = clamp(assessmentScore);
        ConceptMastery m = mastery.findByConceptIdAndUserId(concept.getId(), userId)
                .orElseGet(() -> {
                    ConceptMastery nm = new ConceptMastery();
                    nm.setConcept(concept);
                    nm.setUserId(userId);
                    nm.setProjectId(projectId);
                    nm.setMasteryScore(0);
                    nm.setConfidence(0);
                    return nm;
                });
        // Exponential moving average: estimate, not absolute truth.
        double updated = 0.7 * m.getMasteryScore() + 0.3 * score;
        m.setMasteryScore(round(updated));
        m.setConfidence(round(Math.min(1.0, m.getConfidence() + 0.1)));
        ConceptMastery saved = mastery.save(m);

        MasteryHistory h = new MasteryHistory();
        h.setConcept(concept);
        h.setUserId(userId);
        h.setProjectId(projectId);
        h.setScore(score);
        history.save(h);

        events.record(userId, projectId, "MASTERY_UPDATED",
                Map.of("concept", concept.getName(), "mastery", String.valueOf(saved.getMasteryScore())),
                "mastery-" + concept.getId() + "-" + h.getId());
        return saved;
    }

    /** Pure function used by tests and the service above. */
    public static double nextMastery(double oldMastery, double assessmentScore) {
        return 0.7 * clamp(oldMastery) + 0.3 * clamp(assessmentScore);
    }

    static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
