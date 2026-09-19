package com.aistudy.backend.learning;

import com.aistudy.backend.ai.AiClient;
import com.aistudy.backend.analytics.ActivityEventService;
import com.aistudy.backend.project.Project;
import com.aistudy.backend.project.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "What should I do next?" — weakest estimated concept first, informed by
 * mastery, trends, goal, recent activity, and learner context.
 */
@Service
public class RecommendationService {
    private final RecommendationRepository repo;
    private final ConceptMasteryRepository mastery;
    private final MasteryHistoryRepository history;
    private final ProjectService projects;
    private final LearningContextService context;
    private final GrowthService growth;
    private final AiClient ai;
    private final ActivityEventService events;

    public RecommendationService(RecommendationRepository repo, ConceptMasteryRepository mastery,
                                 MasteryHistoryRepository history, ProjectService projects,
                                 LearningContextService context, GrowthService growth,
                                 AiClient ai, ActivityEventService events) {
        this.repo = repo;
        this.mastery = mastery;
        this.history = history;
        this.projects = projects;
        this.context = context;
        this.growth = growth;
        this.ai = ai;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<MasteryDto.RecommendationResponse> list(UUID userId, UUID projectId) {
        projects.requireOwned(userId, projectId);
        return repo.findTop20ByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, userId)
                .stream().map(r -> new MasteryDto.RecommendationResponse(
                        r.getId().toString(), r.getText(), r.getReason(),
                        r.getCreatedAt() == null ? null : r.getCreatedAt().toString()))
                .toList();
    }

    @Transactional
    public MasteryDto.RecommendationResponse generate(UUID userId, UUID projectId) {
        Project project = projects.requireOwned(userId, projectId);

        List<MasteryDto.GrowthResponse> trends = growth.analyze(userId, projectId);
        List<AiClient.WeakConcept> weak = mastery.findByProjectIdAndUserIdOrderByMasteryScoreAsc(projectId, userId)
                .stream().limit(5)
                .map(m -> new AiClient.WeakConcept(
                        m.getConcept().getName(), m.getMasteryScore(),
                        trendOf(trends, m.getConcept().getId())))
                .toList();

        List<MasteryHistory> recent = history.findByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, userId)
                .stream().limit(5).toList();
        StringBuilder activity = new StringBuilder();
        recent.forEach(h -> activity.append(h.getConcept().getName())
                .append("=").append((int) h.getScore()).append("; "));

        AiClient.GeneratedRecommendation gen = ai.generateRecommendation(
                new AiClient.RecommendationPrompt(
                        new AiClient.AiContext(userId, projectId, "RECOMMENDATION"),
                        project.getName(), project.getGoal(), weak,
                        activity.toString(), context.digest(userId, projectId)));

        Recommendation r = new Recommendation();
        r.setUserId(userId);
        r.setProjectId(projectId);
        r.setText(gen.text());
        r.setReason(gen.reason());
        Recommendation saved = repo.save(r);
        events.record(userId, projectId, "RECOMMENDATION_CREATED",
                Map.of("recommendationId", saved.getId().toString()),
                "recommendation-" + saved.getId());
        return new MasteryDto.RecommendationResponse(saved.getId().toString(),
                saved.getText(), saved.getReason(), saved.getCreatedAt().toString());
    }

    private static String trendOf(List<MasteryDto.GrowthResponse> trends, UUID conceptId) {
        return trends.stream()
                .filter(t -> t.conceptId().equals(conceptId.toString()))
                .map(MasteryDto.GrowthResponse::trend)
                .findFirst().orElse("UNKNOWN");
    }
}
