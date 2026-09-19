package com.aistudy.backend.learning;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GrowthServiceTest {

    private static MasteryHistory hist(double score) {
        MasteryHistory h = new MasteryHistory();
        h.setScore(score);
        return h;
    }

    @Test
    void improvingOnStrongGains() {
        List<MasteryHistory> h = List.of(hist(40), hist(55), hist(70));
        // need createdAt ordering; classify only uses first/last scores
        assertThat(GrowthService.classify(65, new java.util.ArrayList<>(h))).isEqualTo("IMPROVING");
    }

    @Test
    void requiringAttentionOnDecline() {
        List<MasteryHistory> h = new java.util.ArrayList<>(List.of(hist(80), hist(60)));
        assertThat(GrowthService.classify(60, h)).isEqualTo("REQUIRING_ATTENTION");
    }

    @Test
    void lowMasteryWithoutHistoryNeedsAttention() {
        assertThat(GrowthService.classify(20, List.of())).isEqualTo("REQUIRING_ATTENTION");
    }

    @Test
    void highMasteryIsStable() {
        assertThat(GrowthService.classify(85, List.of(hist(80)))).isEqualTo("STABLE");
    }
}
