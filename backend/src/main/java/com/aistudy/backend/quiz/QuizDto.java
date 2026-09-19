package com.aistudy.backend.quiz;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public class QuizDto {
    public record CreateQuizRequest(
            @Size(max = 300) String title,
            @Min(1) @Max(20) int mcqCount,
            @Min(0) @Max(10) int openCount) {
        public CreateQuizRequest {
            // defaults when Jackson passes 0 for missing ints
        }
    }

    public record SubmitAnswerRequest(
            @Size(max = 5000) String answerText,
            @Size(max = 10) String selectedOption) {}

    public record QuestionResponse(String id, String type, String prompt, List<String> options,
                                   String conceptName, String difficulty, int position) {}

    public record QuizDetailResponse(String id, String title, String status, Double score,
                                     List<QuestionResponse> questions,
                                     String createdAt, String completedAt) {}

    public record QuizSummaryResponse(String id, String title, String status, Double score,
                                      int questionCount, String createdAt) {}

    public record AnswerResponse(String answerId, int score, String feedback,
                                 List<String> understood, List<String> missing) {}

    public record CompleteQuizResponse(String quizId, String status, Double score, int answered) {}
}
