package com.aistudy.backend.analytics;

import com.aistudy.backend.common.security.CurrentUser;
import com.aistudy.backend.knowledge.ConceptRepository;
import com.aistudy.backend.knowledge.DocumentChunkRepository;
import com.aistudy.backend.learning.ConceptMasteryRepository;
import com.aistudy.backend.material.MaterialRepository;
import com.aistudy.backend.project.ProjectService;
import com.aistudy.backend.quiz.AnswerRepository;
import com.aistudy.backend.quiz.QuizRepository;
import com.aistudy.backend.tutor.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class AnalyticsController {
    private final ProjectService projects;
    private final MaterialRepository materials;
    private final DocumentChunkRepository chunks;
    private final ConceptRepository concepts;
    private final ConceptMasteryRepository mastery;
    private final MessageRepository messages;
    private final QuizRepository quizzes;
    private final AnswerRepository answers;
    private final ActivityEventRepository events;
    private final AiUsageRepository aiUsage;
    private final ObjectMapper mapper;

    public AnalyticsController(ProjectService projects, MaterialRepository materials,
                               DocumentChunkRepository chunks, ConceptRepository concepts,
                               ConceptMasteryRepository mastery, MessageRepository messages,
                               QuizRepository quizzes, AnswerRepository answers,
                               ActivityEventRepository events, AiUsageRepository aiUsage,
                               ObjectMapper mapper) {
        this.projects = projects;
        this.materials = materials;
        this.chunks = chunks;
        this.concepts = concepts;
        this.mastery = mastery;
        this.messages = messages;
        this.quizzes = quizzes;
        this.answers = answers;
        this.events = events;
        this.aiUsage = aiUsage;
        this.mapper = mapper;
    }

    @GetMapping("/api/projects/{projectId}/analytics")
    public Map<String, Object> projectAnalytics(@PathVariable UUID projectId) {
        UUID userId = CurrentUser.id();
        projects.requireOwned(userId, projectId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("materials", materials.countByProjectIdAndUserId(projectId, userId));
        out.put("materialsReady", materials.countByProjectIdAndUserIdAndStatus(
                projectId, userId, com.aistudy.backend.material.Material.MaterialStatus.READY));
        out.put("chunks", chunks.countByProjectIdAndUserId(projectId, userId));
        out.put("concepts", concepts.countByProjectIdAndUserId(projectId, userId));
        out.put("tutorMessages", messages.countByProjectIdAndUserId(projectId, userId));
        out.put("quizzes", quizzes.countByProjectIdAndUserId(projectId, userId));
        out.put("quizAttempts", answers.countAttempts(projectId, userId));
        Double avg = answers.averageScore(projectId, userId);
        out.put("averageScore", avg == null ? 0 : Math.round(avg * 10.0) / 10.0);
        Map<String, Long> dist = new LinkedHashMap<>();
        dist.put("strong", 0L);
        dist.put("developing", 0L);
        dist.put("weak", 0L);
        mastery.findByProjectIdAndUserId(projectId, userId).forEach(m -> {
            if (m.getMasteryScore() >= 70) {
                dist.compute("strong", (k, v) -> v + 1);
            } else if (m.getMasteryScore() >= 40) {
                dist.compute("developing", (k, v) -> v + 1);
            } else {
                dist.compute("weak", (k, v) -> v + 1);
            }
        });
        out.put("masteryDistribution", dist);
        out.put("recentEvents", events.findTop50ByUserIdAndProjectIdOrderByCreatedAtDesc(userId, projectId)
                .stream().limit(15).map(e -> Map.of(
                        "type", e.getEventType(),
                        "at", e.getCreatedAt().toString(),
                        "metadata", e.getMetadata() == null ? "" : e.getMetadata()))
                .toList());
        return out;
    }

    @GetMapping("/api/analytics/overview")
    public Map<String, Object> overview() {
        UUID userId = CurrentUser.id();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("events", events.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(20).map(e -> Map.of(
                        "type", e.getEventType(),
                        "at", e.getCreatedAt().toString(),
                        "projectId", e.getProjectId() == null ? "" : e.getProjectId().toString()))
                .toList());
        out.put("aiUsage", aiUsage.findTop100ByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(20).map(u -> Map.of(
                        "feature", u.getFeature(), "model", u.getModel(),
                        "success", u.isSuccess(),
                        "at", u.getCreatedAt().toString()))
                .toList());
        return out;
    }
}
