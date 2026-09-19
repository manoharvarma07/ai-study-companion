package com.aistudy.backend.learning;

import com.aistudy.backend.analytics.ActivityEventService;
import com.aistudy.backend.knowledge.Concept;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasteryServiceTest {

    @Mock
    private ConceptMasteryRepository masteryRepo;
    @Mock
    private MasteryHistoryRepository historyRepo;
    @Mock
    private ActivityEventService events;
    @Test
    void emaFormula() {
        // newMastery = 0.7 * old + 0.3 * score
        assertThat(MasteryService.nextMastery(50, 100)).isCloseTo(65.0, within(0.001));
        assertThat(MasteryService.nextMastery(0, 0)).isCloseTo(0.0, within(0.001));
        assertThat(MasteryService.nextMastery(80, 60)).isCloseTo(74.0, within(0.001));
    }

    @Test
    void scoresAreClampedTo0And100() {
        assertThat(MasteryService.nextMastery(-20, 150)).isCloseTo(30.0, within(0.001));
        assertThat(MasteryService.nextMastery(200, 200)).isCloseTo(100.0, within(0.001));
    }

    @Test
    void oldMasteryDominatesSingleAssessment() {
        double after = MasteryService.nextMastery(90, 0);
        assertThat(after).isGreaterThan(50.0); // 0.7*90 = 63, estimate not absolute truth
    }

    @Test
    void firstAssessmentCreatesMasteryRecord() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Concept concept = new Concept();
        concept.setId(UUID.randomUUID());
        concept.setName("Validation Data");
        when(masteryRepo.findByConceptIdAndUserId(concept.getId(), userId))
                .thenReturn(Optional.empty());
        when(masteryRepo.save(any(ConceptMastery.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(historyRepo.save(any(MasteryHistory.class)))
                .thenAnswer(inv -> {
                    MasteryHistory h = inv.getArgument(0);
                    h.setId(UUID.randomUUID());
                    return h;
                });

        ConceptMastery out = new MasteryService(masteryRepo, historyRepo, events)
                .update(userId, projectId, concept, 100);

        // EMA from 0: 0.7*0 + 0.3*100 = 30 — a real record must exist after one quiz answer.
        assertThat(out.getMasteryScore()).isCloseTo(30.0, within(0.001));
        assertThat(out.getProjectId()).isEqualTo(projectId);
        assertThat(out.getUserId()).isEqualTo(userId);
        ArgumentCaptor<ConceptMastery> saved = ArgumentCaptor.forClass(ConceptMastery.class);
        verify(masteryRepo).save(saved.capture());
        assertThat(saved.getValue().getConcept()).isSameAs(concept);
        verify(historyRepo).save(any(MasteryHistory.class));
    }

    @Test
    void zeroScoreStillCreatesMasteryRecord() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Concept concept = new Concept();
        concept.setId(UUID.randomUUID());
        concept.setName("General");
        when(masteryRepo.findByConceptIdAndUserId(concept.getId(), userId))
                .thenReturn(Optional.empty());
        when(masteryRepo.save(any(ConceptMastery.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(historyRepo.save(any(MasteryHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ConceptMastery out = new MasteryService(masteryRepo, historyRepo, events)
                .update(userId, projectId, concept, 0);

        // Even a 0 score must leave a visible record (Weak), never "No mastery data".
        assertThat(out.getMasteryScore()).isCloseTo(0.0, within(0.001));
        verify(masteryRepo).save(any(ConceptMastery.class));
    }
}
