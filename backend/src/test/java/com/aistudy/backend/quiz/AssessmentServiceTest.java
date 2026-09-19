package com.aistudy.backend.quiz;

import com.aistudy.backend.ai.AiClient;
import com.aistudy.backend.analytics.ActivityEventService;
import com.aistudy.backend.knowledge.Concept;
import com.aistudy.backend.knowledge.ConceptRepository;
import com.aistudy.backend.learning.LearningContextService;
import com.aistudy.backend.learning.MasteryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock
    private QuestionRepository questions;
    @Mock
    private AnswerRepository answers;
    @Mock
    private ConceptRepository concepts;
    @Mock
    private AiClient ai;
    @Mock
    private MasteryService mastery;
    @Mock
    private LearningContextService context;
    @Mock
    private ActivityEventService events;

    private AssessmentService service;
    private final ObjectMapper mapper = new ObjectMapper();

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID quizId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        service = new AssessmentService(questions, answers, concepts, ai, mastery, context, events, mapper);
        org.mockito.Mockito.lenient().when(answers.save(any(Answer.class))).thenAnswer(inv -> {
            Answer a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
    }

    private Concept concept(String name) {
        Concept c = new Concept();
        c.setId(UUID.randomUUID());
        c.setName(name);
        return c;
    }

    private Question mcq(String optionsJson, String correctAnswer, Concept c) {
        Quiz quiz = new Quiz();
        quiz.setId(quizId);
        Question q = new Question();
        q.setId(questionId);
        q.setQuiz(quiz);
        q.setProjectId(projectId);
        q.setUserId(userId);
        q.setConcept(c);
        q.setType("MCQ");
        q.setPrompt("Q?");
        q.setOptions(optionsJson);
        q.setCorrectAnswer(correctAnswer);
        return q;
    }

    private void stubQuestion(Question q) {
        when(questions.findByIdAndUserId(questionId, userId)).thenReturn(Optional.of(q));
        when(answers.findByQuestionIdAndUserId(questionId, userId)).thenReturn(Optional.empty());
    }

    @Test
    void correctLetterScores100AndUpdatesMastery() {
        Concept c = concept("Geography");
        stubQuestion(mcq("[\"Paris\",\"London\",\"Rome\",\"Madrid\"]", "Paris", c));

        QuizDto.AnswerResponse res = service.assess(userId, quizId, questionId,
                new QuizDto.SubmitAnswerRequest(null, "A"));

        assertThat(res.score()).isEqualTo(100);
        ArgumentCaptor<Answer> saved = ArgumentCaptor.forClass(Answer.class);
        verify(answers).save(saved.capture());
        assertThat(saved.getValue().getScore()).isEqualTo(100.0);
        assertThat(saved.getValue().getSelectedOption()).isEqualTo("A");
        verify(mastery).update(eq(userId), eq(projectId), eq(c), eq(100.0));
    }

    @Test
    void wrongLetterScoresZeroButStillUpdatesMastery() {
        Concept c = concept("Geography");
        stubQuestion(mcq("[\"Paris\",\"London\",\"Rome\",\"Madrid\"]", "Paris", c));

        QuizDto.AnswerResponse res = service.assess(userId, quizId, questionId,
                new QuizDto.SubmitAnswerRequest(null, "B"));

        assertThat(res.score()).isEqualTo(0);
        // A wrong answer must still create/update mastery (score 0), never skip it.
        verify(mastery).update(eq(userId), eq(projectId), eq(c), eq(0.0));
    }

    @Test
    void letterMatchesPrefixedOptions() {
        Concept c = concept("Geography");
        stubQuestion(mcq("[\"A) Paris\",\"B) London\",\"C) Rome\",\"D) Madrid\"]", "Paris", c));

        QuizDto.AnswerResponse res = service.assess(userId, quizId, questionId,
                new QuizDto.SubmitAnswerRequest(null, "a"));

        assertThat(res.score()).isEqualTo(100);
    }

    @Test
    void letterMatchesWhenCorrectAnswerCarriesPrefix() {
        Concept c = concept("Geography");
        stubQuestion(mcq("[\"Paris\",\"London\",\"Rome\",\"Madrid\"]", "B) London", c));

        QuizDto.AnswerResponse correct = service.assess(userId, quizId, questionId,
                new QuizDto.SubmitAnswerRequest(null, "B"));
        assertThat(correct.score()).isEqualTo(100);
    }

    @Test
    void bareLetterCorrectAnswer() {
        Concept c = concept("Geography");
        stubQuestion(mcq("[\"Paris\",\"London\",\"Rome\",\"Madrid\"]", "C", c));

        // fresh stubs per assess call (Mockito stubbing is per-invocation setup)
        QuizDto.AnswerResponse correct = service.assess(userId, quizId, questionId,
                new QuizDto.SubmitAnswerRequest(null, "c"));
        assertThat(correct.score()).isEqualTo(100);
    }

    @Test
    void answerTextFallbackForMcq() {
        Concept c = concept("Geography");
        stubQuestion(mcq("[\"Paris\",\"London\",\"Rome\",\"Madrid\"]", "Paris", c));

        QuizDto.AnswerResponse res = service.assess(userId, quizId, questionId,
                new QuizDto.SubmitAnswerRequest("Paris", null));

        assertThat(res.score()).isEqualTo(100);
    }

    @Test
    void optionTextStartingWithLetterIsNotStripped() {
        // "A feature..." must not be treated as an "A" option marker.
        assertThat(AssessmentService.stripOptionPrefix("A feature is an input."))
                .isEqualTo("A feature is an input.");
        assertThat(AssessmentService.stripOptionPrefix("A) Paris")).isEqualTo("Paris");
        assertThat(AssessmentService.stripOptionPrefix("(B) London")).isEqualTo("London");
        assertThat(AssessmentService.stripOptionPrefix("C. Rome")).isEqualTo("Rome");
        assertThat(AssessmentService.stripOptionPrefix("D: Madrid")).isEqualTo("Madrid");
    }

    @Test
    void resolveConceptCreatesMissingConcept() {
        when(concepts.findByProjectIdAndUserIdAndNameIgnoreCase(projectId, userId, "Validation Data"))
                .thenReturn(Optional.empty());
        Concept created = concept("Validation Data");
        when(concepts.save(any(Concept.class))).thenReturn(created);

        Concept out = service.resolveConcept(userId, projectId, "Validation Data");

        assertThat(out).isSameAs(created);
        ArgumentCaptor<Concept> saved = ArgumentCaptor.forClass(Concept.class);
        verify(concepts).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Validation Data");
        assertThat(saved.getValue().getProject().getId()).isEqualTo(projectId);
        assertThat(saved.getValue().getUser().getId()).isEqualTo(userId);
    }

    @Test
    void resolveConceptReturnsExistingWithoutSaving() {
        Concept existing = concept("Gradient Descent");
        when(concepts.findByProjectIdAndUserIdAndNameIgnoreCase(projectId, userId, "Gradient Descent"))
                .thenReturn(Optional.of(existing));

        assertThat(service.resolveConcept(userId, projectId, "Gradient Descent")).isSameAs(existing);
        verify(concepts, never()).save(any());
    }

    @Test
    void resolveConceptBlankYieldsNull() {
        assertThat(service.resolveConcept(userId, projectId, "  ")).isNull();
        assertThat(service.resolveConcept(userId, projectId, null)).isNull();
        verify(concepts, never()).save(any());
    }

    @Test
    void fullTextAnswerMatchesPrefixedOption() {
        Concept c = concept("Geography");
        stubQuestion(mcq("[\"A) Paris\",\"B) London\",\"C) Rome\",\"D) Madrid\"]", "A) Paris", c));

        QuizDto.AnswerResponse res = service.assess(userId, quizId, questionId,
                new QuizDto.SubmitAnswerRequest("London", null));

        assertThat(res.score()).isEqualTo(0);
        verify(mastery).update(eq(userId), eq(projectId), eq(c), eq(0.0));
    }

    @Test
    void nullConceptSkipsMasteryButStillSavesAnswer() {
        stubQuestion(mcq("[\"Paris\",\"London\"]", "Paris", null));

        QuizDto.AnswerResponse res = service.assess(userId, quizId, questionId,
                new QuizDto.SubmitAnswerRequest(null, "A"));

        assertThat(res.score()).isEqualTo(100);
        verify(answers).save(any(Answer.class));
        verify(mastery, never()).update(any(), any(), any(), any(Double.class));
    }
}
