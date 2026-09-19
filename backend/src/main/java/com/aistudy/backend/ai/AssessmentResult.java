package com.aistudy.backend.ai;

import java.util.List;

/**
 * Structured assessment of an open-ended (or MCQ) answer.
 * Score is always 0-100; enforced by {@link AiResponseValidator}.
 */
public record AssessmentResult(
        int score,
        List<String> understood,
        List<String> missing,
        String feedback) {
    public AssessmentResult {
        understood = understood == null ? List.of() : List.copyOf(understood);
        missing = missing == null ? List.of() : List.copyOf(missing);
    }
}
