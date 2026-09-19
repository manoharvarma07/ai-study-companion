package com.aistudy.backend.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {
    List<AiUsage> findTop100ByUserIdOrderByCreatedAtDesc(UUID userId);
    List<AiUsage> findTop100ByOrderByCreatedAtDesc();
    List<AiUsage> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);
    java.util.Optional<AiUsage> findTop1ByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("select u.feature as feature, count(u) as calls, sum(u.inputTokens + u.outputTokens) as tokens, sum(u.estimatedCost) as cost "
            + "from AiUsage u group by u.feature")
    List<FeatureUsage> usageByFeature();

    interface FeatureUsage {
        String getFeature();
        long getCalls();
        long getTokens();
        double getCost();
    }
}
