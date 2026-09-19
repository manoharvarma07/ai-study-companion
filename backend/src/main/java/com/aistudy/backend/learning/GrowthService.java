package com.aistudy.backend.learning;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Growth analysis per concept: IMPROVING / STABLE / REQUIRING_ATTENTION,
 * derived from mastery history + current estimate. Prototype heuristic.
 */
@Service
public class GrowthService {
    private final ConceptMasteryRepository mastery;
    private final MasteryHistoryRepository history;

    public GrowthService(ConceptMasteryRepository mastery, MasteryHistoryRepository history) {
        this.mastery = mastery;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public List<MasteryDto.GrowthResponse> analyze(UUID userId, UUID projectId) {
        List<MasteryDto.GrowthResponse> out = new ArrayList<>();
        for (ConceptMastery m : mastery.findByProjectIdAndUserId(projectId, userId)) {
            List<MasteryHistory> h = history
                    .findTop20ByConceptIdAndUserIdOrderByCreatedAtDesc(m.getConcept().getId(), userId);
            h.sort(Comparator.comparing(MasteryHistory::getCreatedAt));
            out.add(new MasteryDto.GrowthResponse(
                    m.getConcept().getId().toString(),
                    m.getConcept().getName(),
                    m.getMasteryScore(),
                    classify(m.getMasteryScore(), h),
                    describe(h)));
        }
        return out;
    }

    static String classify(double current, List<MasteryHistory> history) {
        if (history.size() >= 2) {
            double first = history.get(0).getScore();
            double last = history.get(history.size() - 1).getScore();
            if (last - first >= 10) {
                return "IMPROVING";
            }
            if (first - last >= 10) {
                return "REQUIRING_ATTENTION";
            }
        }
        if (current < 40) {
            return "REQUIRING_ATTENTION";
        }
        if (current >= 70) {
            return "STABLE";
        }
        return history.size() >= 2 ? "STABLE" : "REQUIRING_ATTENTION";
    }

    private static String describe(List<MasteryHistory> h) {
        if (h.isEmpty()) {
            return "No attempts yet — estimated mastery only.";
        }
        return "Based on " + h.size() + " attempt(s); latest score " + ((int) h.get(h.size() - 1).getScore()) + ".";
    }
}
