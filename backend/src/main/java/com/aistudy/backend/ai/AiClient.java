package com.aistudy.backend.ai;

import java.util.List;
import java.util.UUID;

/**
 * Single abstraction for all AI calls. Application code must never call
 * the OpenAI API (or any model API) directly — everything goes through here.
 */
public interface AiClient {

    /** Which model backs this client (e.g. "gpt-4o-mini" or "offline-heuristic"). */
    String modelName();

    /** True when talking to a real model API vs. the offline heuristic fallback. */
    boolean isLive();

    TutorAnswer generateTutorResponse(TutorPrompt prompt);

    List<GeneratedQuestion> generateQuiz(QuizPrompt prompt);

    AssessmentResult evaluateAnswer(AssessmentPrompt prompt);

    List<ExtractedConcept> extractConcepts(ConceptPrompt prompt);

    GeneratedRecommendation generateRecommendation(RecommendationPrompt prompt);

    // ---------- context records (application-controlled input only) ----------

    record AiContext(UUID userId, UUID projectId, String feature) {}

    record EvidenceChunk(String materialName, Integer pageNumber, String content) {}

    record TutorPrompt(AiContext ctx, String projectName, String projectGoal,
                       String learnerContext, List<EvidenceChunk> evidence,
                       List<ChatTurn> history, String question) {}
    record ChatTurn(String role, String content) {}
    record Citation(String material, Integer page) {}
    record TutorAnswer(String answer, boolean grounded, List<Citation> citations) {}

    record QuizPrompt(AiContext ctx, String projectName, String projectGoal,
                      List<ConceptFocus> focusConcepts, List<EvidenceChunk> evidence,
                      int mcqCount, int openCount) {}
    record ConceptFocus(String name, double masteryScore, int mistakes) {}
    record GeneratedQuestion(String type, String prompt, List<String> options,
                             String correctAnswer, String conceptName, String difficulty) {}

    record AssessmentPrompt(AiContext ctx, String questionPrompt, String questionType,
                            String correctAnswer, String studentAnswer, String conceptName) {}

    record ConceptPrompt(AiContext ctx, String materialName, String sampleText) {}
    record ExtractedConcept(String name, String description) {}

    record RecommendationPrompt(AiContext ctx, String projectName, String projectGoal,
                                List<WeakConcept> weakConcepts, String recentActivity,
                                String learnerContext) {}
    record WeakConcept(String name, double masteryScore, String trend) {}
    record GeneratedRecommendation(String text, String reason) {}
}
