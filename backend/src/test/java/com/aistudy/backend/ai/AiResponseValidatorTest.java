package com.aistudy.backend.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiResponseValidatorTest {
    private final AiResponseValidator validator = new AiResponseValidator();

    private static List<AiClient.EvidenceChunk> evidence() {
        return List.of(new AiClient.EvidenceChunk("notes.pdf", 1, "gradient descent content"));
    }

    @Test
    void hallucinatedCitationsAreDroppedAndUngrounded() {
        AiClient.TutorAnswer raw = new AiClient.TutorAnswer("Answer",
                true, List.of(new AiClient.Citation("invented.pdf", 3)));
        AiClient.TutorAnswer out = validator.validateTutor(raw, evidence());
        assertThat(out.citations()).isEmpty();
        assertThat(out.grounded()).isFalse();
    }

    @Test
    void validCitationsAreKept() {
        AiClient.TutorAnswer raw = new AiClient.TutorAnswer("Answer",
                true, List.of(new AiClient.Citation("notes.pdf", 1)));
        AiClient.TutorAnswer out = validator.validateTutor(raw, evidence());
        assertThat(out.grounded()).isTrue();
        assertThat(out.citations()).hasSize(1);
    }

    @Test
    void duplicateCitationsAreDeduplicated() {
        AiClient.TutorAnswer raw = new AiClient.TutorAnswer("Answer",
                true, List.of(new AiClient.Citation("notes.pdf", 1),
                        new AiClient.Citation("notes.pdf", 1),
                        new AiClient.Citation("NOTES.PDF", 1),
                        new AiClient.Citation("notes.pdf", 2)));
        AiClient.TutorAnswer out = validator.validateTutor(raw, evidence());
        assertThat(out.citations()).hasSize(2);
        assertThat(out.grounded()).isTrue();
    }

    @Test
    void blankAnswerDegradesGracefully() {
        AiClient.TutorAnswer out = validator.validateTutor(
                new AiClient.TutorAnswer("  ", true, List.of()), evidence());
        assertThat(out.grounded()).isFalse();
        assertThat(out.answer()).isNotBlank();
    }

    @Test
    void nullTutorResponseDegradesGracefully() {
        AiClient.TutorAnswer out = validator.validateTutor(null, evidence());
        assertThat(out.answer()).isNotBlank();
        assertThat(out.grounded()).isFalse();
    }

    @Test
    void assessmentScoreIsClamped() {
        assertThat(validator.validateAssessment(
                new com.aistudy.backend.ai.AssessmentResult(150, List.of("x"), List.of(), "good")).score())
                .isEqualTo(100);
        assertThat(validator.validateAssessment(
                new com.aistudy.backend.ai.AssessmentResult(-5, List.of(), List.of("y"), "bad")).score())
                .isEqualTo(0);
    }

    @Test
    void nullAssessmentDegradesGracefully() {
        assertThat(validator.validateAssessment(null).feedback()).isNotBlank();
    }

    @Test
    void mcqWithoutOptionsIsRejected() {
        List<AiClient.GeneratedQuestion> out = validator.validateQuestions(List.of(
                new AiClient.GeneratedQuestion("MCQ", "What?", List.of("only-one"), "only-one", "C", "EASY")));
        assertThat(out).isEmpty();
    }
}
