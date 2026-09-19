package com.aistudy.backend.quiz;

import com.aistudy.backend.ai.AiClient;
import com.aistudy.backend.analytics.ActivityEventService;
import com.aistudy.backend.common.exception.ResourceNotFoundException;
import com.aistudy.backend.knowledge.RetrievalService;
import com.aistudy.backend.project.Project;
import com.aistudy.backend.project.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QuizService {
    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final QuizRepository quizzes;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final ProjectService projects;
    private final AdaptiveQuizService adaptive;
    private final RetrievalService retrieval;
    private final AiClient ai;
    private final AssessmentService assessment;
    private final ActivityEventService events;
    private final ObjectMapper mapper;

    public QuizService(QuizRepository quizzes, QuestionRepository questions, AnswerRepository answers,
                       ProjectService projects, AdaptiveQuizService adaptive,
                       RetrievalService retrieval, AiClient ai, AssessmentService assessment,
                       ActivityEventService events, ObjectMapper mapper) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.answers = answers;
        this.projects = projects;
        this.adaptive = adaptive;
        this.retrieval = retrieval;
        this.ai = ai;
        this.assessment = assessment;
        this.events = events;
        this.mapper = mapper;
    }

    @Transactional
    public QuizDto.QuizDetailResponse create(UUID userId, UUID projectId, QuizDto.CreateQuizRequest req) {
        Project project = projects.requireOwned(userId, projectId);
        int mcq = req.mcqCount() <= 0 ? 3 : Math.min(20, req.mcqCount());
        int open = Math.max(0, Math.min(10, req.openCount()));

        List<AdaptiveQuizService.FocusConcept> focus = adaptive.selectFocus(userId, projectId, 5);
        List<AiClient.ConceptFocus> aiFocus = focus.stream()
                .map(f -> new AiClient.ConceptFocus(f.concept().getName(), f.masteryScore(), f.mistakes()))
                .toList();

        List<RetrievalService.RetrievedChunk> hits =
                retrieval.retrieve(projectId, userId, project.getName() + " " + nullStr(project.getGoal()), 6);
        List<AiClient.EvidenceChunk> evidence = hits.stream()
                .map(h -> new AiClient.EvidenceChunk(h.materialName(), h.pageNumber(), h.content()))
                .toList();

        List<AiClient.GeneratedQuestion> generated;
        try {
            generated = ai.generateQuiz(new AiClient.QuizPrompt(
                    new AiClient.AiContext(userId, projectId, "QUIZ_GENERATION"),
                    project.getName(), project.getGoal(), aiFocus, evidence, mcq, open));
        } catch (Exception e) {
            log.warn("Quiz generation failed: {}", e.getMessage());
            generated = List.of();
        }
        if (generated.isEmpty()) {
            throw new IllegalStateException("Could not generate quiz questions. Add material or concepts first.");
        }

        Quiz quiz = new Quiz();
        quiz.setUser(project.getUser());
        quiz.setProject(project);
        String title = req.title() == null || req.title().isBlank()
                ? "Quiz on " + project.getName() : req.title().strip();
        quiz.setTitle(title);
        quiz.setStatus("IN_PROGRESS");
        quizzes.save(quiz);

        int pos = 0;
        List<Question> entities = new ArrayList<>();
        for (AiClient.GeneratedQuestion g : generated) {
            Question q = new Question();
            q.setQuiz(quiz);
            q.setProjectId(projectId);
            q.setUserId(userId);
            q.setConcept(assessment.resolveConcept(userId, projectId, g.conceptName()));
            q.setType(g.type());
            q.setPrompt(g.prompt());
            try {
                q.setOptions(mapper.writeValueAsString(g.options() == null ? List.of() : g.options()));
            } catch (Exception e) {
                q.setOptions("[]");
            }
            q.setCorrectAnswer(g.correctAnswer());
            q.setDifficulty(g.difficulty());
            q.setPosition(pos++);
            entities.add(q);
        }
        questions.saveAll(entities);

        events.record(userId, projectId, "QUIZ_STARTED",
                Map.of("quizId", quiz.getId().toString(), "questions", String.valueOf(entities.size())),
                "quiz-started-" + quiz.getId());
        return detail(userId, quiz.getId());
    }

    @Transactional(readOnly = true)
    public QuizDto.QuizDetailResponse get(UUID userId, UUID quizId) {
        return detail(userId, quizId);
    }

    @Transactional(readOnly = true)
    public List<QuizDto.QuizSummaryResponse> list(UUID userId, UUID projectId) {
        projects.requireOwned(userId, projectId);
        return quizzes.findByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, userId).stream()
                .map(q -> new QuizDto.QuizSummaryResponse(q.getId().toString(), q.getTitle(),
                        q.getStatus(), q.getScore(),
                        (int) questions.countByProjectIdAndUserId(projectId, userId),
                        q.getCreatedAt() == null ? null : q.getCreatedAt().toString()))
                .toList();
    }

    @Transactional
    public QuizDto.AnswerResponse answer(UUID userId, UUID quizId, UUID questionId,
                                        QuizDto.SubmitAnswerRequest req) {
        Quiz quiz = quizzes.findByIdAndUserId(quizId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        if ("COMPLETED".equals(quiz.getStatus())) {
            throw new IllegalStateException("Quiz already completed");
        }
        return assessment.assess(userId, quizId, questionId, req);
    }

    @Transactional
    public QuizDto.CompleteQuizResponse complete(UUID userId, UUID quizId) {
        Quiz quiz = quizzes.findByIdAndUserId(quizId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        List<Answer> given = answers.findByQuizIdAndUserId(quizId, userId);
        double avg = given.stream().filter(a -> a.getScore() != null)
                .mapToDouble(Answer::getScore).average().orElse(0.0);
        quiz.setScore(Math.round(avg * 10.0) / 10.0);
        quiz.setStatus("COMPLETED");
        quiz.setCompletedAt(Instant.now());
        quizzes.save(quiz);
        events.record(userId, quiz.getProject().getId(), "QUIZ_COMPLETED",
                Map.of("quizId", quizId.toString(), "score", String.valueOf(quiz.getScore())),
                "quiz-completed-" + quizId);
        return new QuizDto.CompleteQuizResponse(quiz.getId().toString(), quiz.getStatus(),
                quiz.getScore(), given.size());
    }

    private QuizDto.QuizDetailResponse detail(UUID userId, UUID quizId) {
        Quiz quiz = quizzes.findByIdAndUserId(quizId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        List<QuizDto.QuestionResponse> qs = questions.findByQuizIdAndUserIdOrderByPositionAsc(quizId, userId)
                .stream().map(q -> {
                    List<String> opts = List.of();
                    try {
                        opts = q.getOptions() == null ? List.of() : mapper.readValue(q.getOptions(),
                                mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                    } catch (Exception ignored) {
                    }
                    return new QuizDto.QuestionResponse(q.getId().toString(), q.getType(), q.getPrompt(),
                            opts, q.getConcept() == null ? null : q.getConcept().getName(),
                            q.getDifficulty(), q.getPosition());
                }).toList();
        return new QuizDto.QuizDetailResponse(quiz.getId().toString(), quiz.getTitle(),
                quiz.getStatus(), quiz.getScore(), qs,
                quiz.getCreatedAt() == null ? null : quiz.getCreatedAt().toString(),
                quiz.getCompletedAt() == null ? null : quiz.getCompletedAt().toString());
    }

    private static String nullStr(String s) {
        return s == null ? "" : s;
    }
}
