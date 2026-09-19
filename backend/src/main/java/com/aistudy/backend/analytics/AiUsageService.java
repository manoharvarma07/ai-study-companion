package com.aistudy.backend.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AiUsageService {
    private static final Logger log = LoggerFactory.getLogger(AiUsageService.class);
    private final AiUsageRepository repo;

    public AiUsageService(AiUsageRepository repo) {
        this.repo = repo;
    }

    public void log(UUID userId, UUID projectId, String feature, String model,
                    long latencyMs, int inputTokens, int outputTokens,
                    boolean success, String errorMessage) {
        try {
            AiUsage u = new AiUsage();
            u.setUserId(userId);
            u.setProjectId(projectId);
            u.setFeature(feature);
            u.setModel(model);
            u.setLatencyMs(latencyMs);
            u.setInputTokens(inputTokens);
            u.setOutputTokens(outputTokens);
            u.setEstimatedCost(estimateCost(model, inputTokens, outputTokens));
            u.setSuccess(success);
            u.setErrorMessage(errorMessage);
            repo.save(u);
        } catch (Exception e) {
            log.warn("Failed to record AI usage: {}", e.getMessage());
        }
    }

    /** Rough cost estimate (USD per 1K tokens); unknown/offline models cost 0. */
    static double estimateCost(String model, int in, int out) {
        if (model == null || model.startsWith("offline-") || model.startsWith("heuristic")) {
            return 0.0;
        }
        double inRate = 0.00015, outRate = 0.0006; // gpt-4o-mini class pricing
        if (model.contains("gpt-4o") && !model.contains("mini")) {
            inRate = 0.0025;
            outRate = 0.01;
        }
        return (in / 1000.0) * inRate + (out / 1000.0) * outRate;
    }
}
