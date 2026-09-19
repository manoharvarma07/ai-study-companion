package com.aistudy.backend.learning;

import java.util.List;

public class MasteryDto {
    public record MasteryResponse(String conceptId, String conceptName, double masteryScore,
                                  double confidence, String updatedAt) {}

    public record GrowthResponse(String conceptId, String conceptName, double masteryScore,
                                 String trend, String detail) {}

    public record RecommendationResponse(String id, String text, String reason, String createdAt) {}

    public record LearningContextResponse(String goals, String strengths, String weaknesses,
                                          String repeatedMistakes, String assessmentSummary) {}
}
