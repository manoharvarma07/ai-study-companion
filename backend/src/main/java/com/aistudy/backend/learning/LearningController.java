package com.aistudy.backend.learning;

import com.aistudy.backend.common.security.CurrentUser;
import com.aistudy.backend.project.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class LearningController {
    private final MasteryService mastery;
    private final GrowthService growth;
    private final RecommendationService recommendations;
    private final LearningContextService context;
    private final ProjectService projects;

    public LearningController(MasteryService mastery, GrowthService growth,
                              RecommendationService recommendations,
                              LearningContextService context, ProjectService projects) {
        this.mastery = mastery;
        this.growth = growth;
        this.recommendations = recommendations;
        this.context = context;
        this.projects = projects;
    }

    @GetMapping("/api/projects/{projectId}/mastery")
    public List<MasteryDto.MasteryResponse> mastery(@PathVariable UUID projectId) {
        UUID userId = CurrentUser.id();
        projects.requireOwned(userId, projectId);
        return mastery.list(userId, projectId);
    }

    @GetMapping("/api/projects/{projectId}/growth")
    public List<MasteryDto.GrowthResponse> growth(@PathVariable UUID projectId) {
        UUID userId = CurrentUser.id();
        projects.requireOwned(userId, projectId);
        return growth.analyze(userId, projectId);
    }

    @GetMapping("/api/projects/{projectId}/recommendations")
    public List<MasteryDto.RecommendationResponse> recommendations(@PathVariable UUID projectId) {
        return recommendations.list(CurrentUser.id(), projectId);
    }

    @PostMapping("/api/projects/{projectId}/recommendations")
    public MasteryDto.RecommendationResponse generateRecommendation(@PathVariable UUID projectId) {
        return recommendations.generate(CurrentUser.id(), projectId);
    }

    @GetMapping("/api/projects/{projectId}/context")
    public MasteryDto.LearningContextResponse context(@PathVariable UUID projectId) {
        UUID userId = CurrentUser.id();
        projects.requireOwned(userId, projectId);
        LearningContext c = context.getOrCreate(userId, projectId);
        return new MasteryDto.LearningContextResponse(c.getGoals(), c.getStrengths(),
                c.getWeaknesses(), c.getRepeatedMistakes(), c.getAssessmentSummary());
    }
}
