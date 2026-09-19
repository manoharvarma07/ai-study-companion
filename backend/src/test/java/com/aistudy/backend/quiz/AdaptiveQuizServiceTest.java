package com.aistudy.backend.quiz;

import com.aistudy.backend.knowledge.Concept;
import com.aistudy.backend.knowledge.ConceptRepository;
import com.aistudy.backend.learning.ConceptMastery;
import com.aistudy.backend.learning.ConceptMasteryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdaptiveQuizServiceTest {

    private Concept concept(String name) {
        Concept c = new Concept();
        c.setId(UUID.randomUUID());
        c.setName(name);
        return c;
    }

    private ConceptMastery mastery(Concept c, double score) {
        ConceptMastery m = new ConceptMastery();
        m.setConcept(c);
        m.setMasteryScore(score);
        return m;
    }

    @Test
    void weakestConceptComesFirst() {
        Concept weak = concept("Overfitting");
        Concept strong = concept("Basics");
        ConceptRepository concepts = mock(ConceptRepository.class);
        ConceptMasteryRepository masteryRepo = mock(ConceptMasteryRepository.class);
        AnswerRepository answers = mock(AnswerRepository.class);
        UUID user = UUID.randomUUID(), project = UUID.randomUUID();
        when(concepts.findByProjectIdAndUserIdOrderByName(project, user))
                .thenReturn(List.of(weak, strong));
        when(masteryRepo.findByProjectIdAndUserId(project, user))
                .thenReturn(List.of(mastery(weak, 20), mastery(strong, 90)));
        when(answers.mistakeCounts(project, user)).thenReturn(List.of());

        var focus = new AdaptiveQuizService(concepts, masteryRepo, answers)
                .selectFocus(user, project, 5);
        assertThat(focus).hasSize(2);
        assertThat(focus.get(0).concept().getName()).isEqualTo("Overfitting");
    }

    @Test
    void mistakesBoostPriority() {
        Concept a = concept("A");
        Concept b = concept("B");
        ConceptRepository concepts = mock(ConceptRepository.class);
        ConceptMasteryRepository masteryRepo = mock(ConceptMasteryRepository.class);
        AnswerRepository answers = mock(AnswerRepository.class);
        UUID user = UUID.randomUUID(), project = UUID.randomUUID();
        when(concepts.findByProjectIdAndUserIdOrderByName(project, user))
                .thenReturn(List.of(a, b));
        when(masteryRepo.findByProjectIdAndUserId(project, user))
                .thenReturn(List.of(mastery(a, 50), mastery(b, 50)));
        AnswerRepository.ConceptMistakes m = mock(AnswerRepository.ConceptMistakes.class);
        when(m.getConcept()).thenReturn("B");
        when(m.getMistakes()).thenReturn(4L);
        when(answers.mistakeCounts(project, user)).thenReturn(List.of(m));

        var focus = new AdaptiveQuizService(concepts, masteryRepo, answers)
                .selectFocus(user, project, 5);
        assertThat(focus.get(0).concept().getName()).isEqualTo("B");
    }

    @Test
    void emptyConceptsYieldsEmptyFocus() {
        ConceptRepository concepts = mock(ConceptRepository.class);
        ConceptMasteryRepository masteryRepo = mock(ConceptMasteryRepository.class);
        AnswerRepository answers = mock(AnswerRepository.class);
        UUID user = UUID.randomUUID(), project = UUID.randomUUID();
        when(concepts.findByProjectIdAndUserIdOrderByName(project, user)).thenReturn(List.of());
        var focus = new AdaptiveQuizService(concepts, masteryRepo, answers)
                .selectFocus(user, project, 5);
        assertThat(focus).isEmpty();
    }
}
