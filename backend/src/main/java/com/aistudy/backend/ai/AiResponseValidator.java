package com.aistudy.backend.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validates structured AI output so malformed model responses degrade
 * gracefully instead of crashing the API or leaking bad data.
 */
@Component
public class AiResponseValidator {
    private static final Logger log = LoggerFactory.getLogger(AiResponseValidator.class);

    public AiClient.TutorAnswer validateTutor(AiClient.TutorAnswer r,
                                              List<AiClient.EvidenceChunk> evidence) {
        if (r == null || r.answer() == null || r.answer().isBlank()) {
            log.warn("Tutor validation failed: blank/malformed AI answer — returning safe fallback");
            return new AiClient.TutorAnswer(
                    "I couldn't generate a response for that. Please try again.",
                    false, List.of());
        }
        List<AiClient.Citation> valid = new ArrayList<>();
        if (r.citations() != null) {
            for (AiClient.Citation c : r.citations()) {
                if (c == null || c.material() == null || c.material().isBlank()) {
                    continue;
                }
                // A citation is only valid if it refers to material actually retrieved.
                boolean known = evidence != null && evidence.stream()
                        .anyMatch(e -> e.materialName() != null
                                && e.materialName().equalsIgnoreCase(c.material().strip()));
                if (known) {
                    valid.add(new AiClient.Citation(c.material().strip(), c.page()));
                } else {
                    log.warn("Dropping hallucinated citation: {}", c.material());
                }
            }
        }
        // Deduplicate identical citations so the UI renders each source once.
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        valid = valid.stream().filter(c -> seen.add(
                        c.material().toLowerCase() + "|" + c.page())).toList();
        boolean grounded = r.grounded() && !valid.isEmpty();
        return new AiClient.TutorAnswer(r.answer().strip(), grounded, List.copyOf(valid));
    }

    public AssessmentResult validateAssessment(AssessmentResult r) {
        if (r == null) {
            return new AssessmentResult(0, List.of(), List.of("No assessment produced"),
                    "The answer could not be assessed. Please try again.");
        }
        int score = Math.max(0, Math.min(100, r.score()));
        String feedback = r.feedback() == null || r.feedback().isBlank()
                ? "No feedback available." : r.feedback().strip();
        return new AssessmentResult(score,
                r.understood().stream().filter(Objects::nonNull).toList(),
                r.missing().stream().filter(Objects::nonNull).toList(),
                feedback);
    }

    public List<AiClient.GeneratedQuestion> validateQuestions(List<AiClient.GeneratedQuestion> qs) {
        if (qs == null) {
            return List.of();
        }
        List<AiClient.GeneratedQuestion> out = new ArrayList<>();
        for (AiClient.GeneratedQuestion q : qs) {
            if (q == null || q.prompt() == null || q.prompt().isBlank()) {
                continue;
            }
            String type = "OPEN".equalsIgnoreCase(q.type()) ? "OPEN" : "MCQ";
            List<String> options = q.options() == null ? List.of() : q.options().stream()
                    .filter(Objects::nonNull).map(String::strip)
                    .filter(s -> !s.isEmpty()).limit(6).toList();
            if (type.equals("MCQ") && options.size() < 2) {
                continue; // MCQ without choices is unusable
            }
            String correct = q.correctAnswer() == null ? null : q.correctAnswer().strip();
            out.add(new AiClient.GeneratedQuestion(type, q.prompt().strip(), options, correct,
                    q.conceptName() == null ? null : q.conceptName().strip(),
                    q.difficulty() == null ? "MEDIUM" : q.difficulty().toUpperCase()));
        }
        return out;
    }
}
