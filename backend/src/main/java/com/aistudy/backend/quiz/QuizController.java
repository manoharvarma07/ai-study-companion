package com.aistudy.backend.quiz;

import com.aistudy.backend.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class QuizController {
    private final QuizService service;

    public QuizController(QuizService service) {
        this.service = service;
    }

    @PostMapping("/api/projects/{projectId}/quizzes")
    public QuizDto.QuizDetailResponse create(@PathVariable UUID projectId,
                                            @RequestBody(required = false) QuizDto.CreateQuizRequest req) {
        QuizDto.CreateQuizRequest effective = req == null
                ? new QuizDto.CreateQuizRequest(null, 3, 1) : req;
        return service.create(CurrentUser.id(), projectId, effective);
    }

    @GetMapping("/api/projects/{projectId}/quizzes")
    public List<QuizDto.QuizSummaryResponse> list(@PathVariable UUID projectId) {
        return service.list(CurrentUser.id(), projectId);
    }

    @GetMapping("/api/quizzes/{quizId}")
    public QuizDto.QuizDetailResponse get(@PathVariable UUID quizId) {
        return service.get(CurrentUser.id(), quizId);
    }

    @PostMapping("/api/quizzes/{quizId}/questions/{questionId}/answers")
    public QuizDto.AnswerResponse answer(@PathVariable UUID quizId,
                                        @PathVariable UUID questionId,
                                        @Valid @RequestBody QuizDto.SubmitAnswerRequest req) {
        return service.answer(CurrentUser.id(), quizId, questionId, req);
    }

    /** Spec-compatible alias: POST /api/quizzes/{quizId}/answers with questionId in body. */
    @PostMapping("/api/quizzes/{quizId}/answers")
    public QuizDto.AnswerResponse answerAlias(@PathVariable UUID quizId,
                                             @Valid @RequestBody AnswerWithQuestion req) {
        return service.answer(CurrentUser.id(), quizId, req.questionId(),
                new QuizDto.SubmitAnswerRequest(req.answerText(), req.selectedOption()));
    }

    @PostMapping("/api/quizzes/{quizId}/complete")
    public QuizDto.CompleteQuizResponse complete(@PathVariable UUID quizId) {
        return service.complete(CurrentUser.id(), quizId);
    }

    public record AnswerWithQuestion(UUID questionId, String answerText, String selectedOption) {}
}
