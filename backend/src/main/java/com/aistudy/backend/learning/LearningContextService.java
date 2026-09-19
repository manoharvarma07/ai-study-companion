package com.aistudy.backend.learning;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Persistent per-project learner memory. Services push compact summaries here
 * (assessment outcomes, mistakes) and consumers pull only a short digest,
 * so we never send full history to the AI on every request.
 */
@Service
public class LearningContextService {
    private final LearningContextRepository repo;

    public LearningContextService(LearningContextRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public LearningContext getOrCreate(UUID userId, UUID projectId) {
        return repo.findByProjectIdAndUserId(projectId, userId).orElseGet(() -> {
            LearningContext c = new LearningContext();
            c.setUserId(userId);
            c.setProjectId(projectId);
            return repo.save(c);
        });
    }

    /** Short digest for AI prompts — only relevant, recent context. */
    @Transactional(readOnly = true)
    public String digest(UUID userId, UUID projectId) {
        return repo.findByProjectIdAndUserId(projectId, userId).map(c -> {
            StringBuilder sb = new StringBuilder();
            append(sb, "Strengths", c.getStrengths());
            append(sb, "Weaknesses", c.getWeaknesses());
            append(sb, "Repeated mistakes", c.getRepeatedMistakes());
            append(sb, "Recent assessment summary", c.getAssessmentSummary());
            return sb.toString().strip();
        }).orElse("");
    }

    @Transactional
    public void noteAssessment(UUID userId, UUID projectId, String conceptName,
                               int score, String missingSummary) {
        LearningContext c = getOrCreate(userId, projectId);
        if (score >= 70) {
            c.setStrengths(merge(c.getStrengths(), conceptName + " (" + score + ")"));
        } else {
            c.setWeaknesses(merge(c.getWeaknesses(), conceptName + " (" + score + ")"));
            if (missingSummary != null && !missingSummary.isBlank()) {
                c.setRepeatedMistakes(merge(c.getRepeatedMistakes(),
                        conceptName + ": " + truncate(missingSummary, 200)));
            }
        }
        c.setAssessmentSummary(truncate(
                "Latest: " + conceptName + " scored " + score + ". " + nullStr(c.getAssessmentSummary()), 1000));
        repo.save(c);
    }

    private static void append(StringBuilder sb, String label, String v) {
        if (v != null && !v.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(label).append(": ").append(truncate(v, 400));
        }
    }

    private static String merge(String existing, String addition) {
        String base = existing == null ? "" : existing;
        String merged = base.isBlank() ? addition : base + "; " + addition;
        return truncate(merged, 1500);
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(s.length() - max) : s;
    }

    private static String nullStr(String s) {
        return s == null ? "" : s;
    }
}
