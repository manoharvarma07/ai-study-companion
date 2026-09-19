package com.aistudy.backend.ai;

import com.aistudy.backend.analytics.AiUsageService;
import com.aistudy.backend.common.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OfflineAiTest {
    private OpenAiClient ai;

    @BeforeEach
    void setup() {
        AppProperties props = new AppProperties(
                new AppProperties.Jwt("test-secret-that-is-at-least-32-bytes-long!", 3600000),
                new AppProperties.Ai("", "https://api.openai.com/v1", "gpt-4o-mini",
                        "text-embedding-3-small", 1536, 60000, true),
                new AppProperties.Rag(6),
                new AppProperties.Storage("./uploads"));
        ai = new OpenAiClient(props, new ObjectMapper(),
                mock(AiUsageService.class), new AiResponseValidator());
    }

    private AiClient.TutorPrompt prompt(String q, List<AiClient.EvidenceChunk> evidence) {
        return new AiClient.TutorPrompt(
                new AiClient.AiContext(UUID.randomUUID(), UUID.randomUUID(), "TUTOR"),
                "Test Project", "learn things", "", evidence, List.of(), q);
    }

    @Test
    void offlineTutorSynthesizesCompleteSentencesWithSingleCitation() {
        var evidence = List.of(
                new AiClient.EvidenceChunk("classification in machine learning-1.pdf", 1,
                        "Classification in Machine Learning. Classification is a machine learning technique "
                                + "used to put data into one of two or more predefined categories (classes). "
                                + "The model is first trained using labeled examples, where it learns the "
                                + "relationship between features and labels. Unrelated filler about sorting "
                                + "algorithms and data structures that has nothing to do with the question at hand "
                                + "and only exists to push the raw chunk well beyond any truncation budget "
                                + "so a naive substring cut would break mid-word in a very embarrassing way."),
                new AiClient.EvidenceChunk("classification in machine learning-1.pdf", 2,
                        "Types of Data Used in Classification. Classification can use numerical data, such as "
                                + "age, marks, height, temperature, etc. It can also use categorical data."));
        AiClient.TutorAnswer r = ai.generateTutorResponse(
                prompt("What are predefined categories (classes)?", evidence));
        assertThat(r.grounded()).isTrue();
        // Complete sentences only — never a mid-word cut like "...labe".
        assertThat(r.answer()).matches("(?s).*[.?!]\\s*$");
        assertThat(r.answer()).doesNotEndWith("labe");
        assertThat(r.answer().length()).isLessThanOrEqualTo(700);
        assertThat(r.answer()).containsIgnoringCase("predefined categor");
        // Only the chunk that supports the answer is cited — not every retrieved page.
        assertThat(r.citations()).hasSize(1);
        assertThat(r.citations().get(0).material())
                .isEqualTo("classification in machine learning-1.pdf");
        assertThat(r.citations().get(0).page()).isEqualTo(1);
    }

    @Test
    void liveFailureFallsBackToOfflineAnswer() {
        // Key configured (= live path attempted) but provider unreachable:
        // must degrade to a grounded offline answer, never throw or go ungrounded.
        AppProperties liveProps = new AppProperties(
                new AppProperties.Jwt("test-secret-that-is-at-least-32-bytes-long!", 3600000),
                new AppProperties.Ai("test-key", "http://127.0.0.1:9", "some-model",
                        "text-embedding-3-small", 1536, 60000, true),
                new AppProperties.Rag(6),
                new AppProperties.Storage("./uploads"));
        OpenAiClient liveClient = new OpenAiClient(liveProps, new ObjectMapper(),
                mock(AiUsageService.class), new AiResponseValidator());
        var evidence = List.of(new AiClient.EvidenceChunk("notes.pdf", 2,
                "Gradient descent uses the learning rate to update weights. "
                        + "It moves parameters opposite to the gradient direction."));
        AiClient.TutorAnswer r = liveClient.generateTutorResponse(
                prompt("How does gradient descent update weights?", evidence));
        assertThat(r.grounded()).isTrue();
        assertThat(r.answer()).isNotBlank();
        assertThat(r.citations()).anyMatch(c -> c.material().equals("notes.pdf"));
    }

    @Test
    void completeAnswerValidation() {
        assertThat(OpenAiClient.isCompleteAnswer(
                "Classification is a technique used to assign data to predefined categories.")).isTrue();
        assertThat(OpenAiClient.isCompleteAnswer("The model is first trained using labe")).isFalse();
        assertThat(OpenAiClient.isCompleteAnswer("  ")).isFalse();
        assertThat(OpenAiClient.isCompleteAnswer(null)).isFalse();
        assertThat(OpenAiClient.isCompleteAnswer("Too short")).isFalse();
        assertThat(OpenAiClient.isCompleteAnswer("Supported by the text (see figure 2).")).isTrue();
    }

    @Test
    void disabledFallbackSurfacesProviderError() {
        AppProperties noFallback = new AppProperties(
                new AppProperties.Jwt("test-secret-that-is-at-least-32-bytes-long!", 3600000),
                new AppProperties.Ai("test-key", "http://127.0.0.1:9", "some-model",
                        "text-embedding-3-small", 1536, 60000, false),
                new AppProperties.Rag(6),
                new AppProperties.Storage("./uploads"));
        OpenAiClient strict = new OpenAiClient(noFallback, new ObjectMapper(),
                mock(AiUsageService.class), new AiResponseValidator());
        var evidence = List.of(new AiClient.EvidenceChunk("notes.pdf", 1, "Some content here."));
        // No silent offline summary: the provider failure must propagate as a clear error.
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> strict.generateTutorResponse(
                        prompt("What is this about?", evidence)))
                .isInstanceOf(com.aistudy.backend.common.exception.AiException.class);
    }

    @Test
    void sanitizeRedactsCredentials() {
        assertThat(OpenAiClient.sanitize("failed with Bearer abcdef1234567890 done"))
                .isEqualTo("failed with Bearer [redacted] done");
        assertThat(OpenAiClient.sanitize("key sk-abcdef1234567890 leaked"))
                .isEqualTo("key sk-[redacted] leaked");
        assertThat(OpenAiClient.sanitize("plain error, no secrets")).isEqualTo("plain error, no secrets");
        assertThat(OpenAiClient.sanitize(null)).isEmpty();
    }

    @Test
    void enumerationAnswerUsesBulletsAcrossChunks() {
        var evidence = List.of(
                new AiClient.EvidenceChunk("doc.pdf", 2,
                        "KNN is a classification algorithm. It predicts the class by majority vote among neighbors."),
                new AiClient.EvidenceChunk("doc.pdf", 12,
                        "Naive Bayes is a classification algorithm. It applies Bayes theorem with strong independence."));
        AiClient.TutorAnswer r = ai.generateTutorResponse(prompt("List all algorithms.", evidence));
        assertThat(r.grounded()).isTrue();
        assertThat(r.answer()).contains("•");
        assertThat(r.answer()).containsIgnoringCase("KNN");
        assertThat(r.answer()).containsIgnoringCase("Naive Bayes");
        assertThat(r.citations()).hasSize(2);
        assertThat(r.citations().stream().map(AiClient.Citation::page).toList())
                .containsExactlyInAnyOrder(2, 12);
    }

    @Test
    void repeatedContentQuotedOnce() {
        var evidence = List.of(
                new AiClient.EvidenceChunk("doc.pdf", 2,
                        "KNN is a classification algorithm. It predicts the class by majority vote."),
                new AiClient.EvidenceChunk("doc.pdf", 5,
                        "KNN is a classification algorithm. It uses distance measures."));
        AiClient.TutorAnswer r = ai.generateTutorResponse(prompt("What is KNN?", evidence));
        assertThat(r.grounded()).isTrue();
        int occurrences = r.answer().split("KNN is a classification algorithm", -1).length - 1;
        assertThat(occurrences).isEqualTo(1);
    }

    @Test
    void unsupportedQuestionIsNotGrounded() {
        AiClient.TutorAnswer r = ai.generateTutorResponse(
                prompt("What is the exact exam date?", List.of()));
        assertThat(r.grounded()).isFalse();
        assertThat(r.citations()).isEmpty();
        assertThat(r.answer()).containsIgnoringCase("couldn't find");
    }

    @Test
    void supportedQuestionIsGroundedWithCitation() {
        var evidence = List.of(new AiClient.EvidenceChunk("notes.pdf", 2,
                "Gradient descent uses the learning rate to update weights."));
        AiClient.TutorAnswer r = ai.generateTutorResponse(
                prompt("How does gradient descent update weights?", evidence));
        assertThat(r.grounded()).isTrue();
        assertThat(r.citations()).anyMatch(c -> c.material().equals("notes.pdf"));
    }

    @Test
    void unrelatedQuestionWithEvidenceStaysUngrounded() {
        var evidence = List.of(new AiClient.EvidenceChunk("notes.pdf", 1,
                "Photosynthesis converts sunlight in chloroplasts."));
        AiClient.TutorAnswer r = ai.generateTutorResponse(
                prompt("What is the capital of France?", evidence));
        assertThat(r.grounded()).isFalse();
    }

    @Test
    void mcqAssessmentExactMatch() {
        AssessmentResult r = ai.evaluateAnswer(new AiClient.AssessmentPrompt(
                new AiClient.AiContext(UUID.randomUUID(), UUID.randomUUID(), "ASSESSMENT"),
                "Q?", "MCQ", "Paris", "paris", null));
        assertThat(r.score()).isEqualTo(100);
        AssessmentResult wrong = ai.evaluateAnswer(new AiClient.AssessmentPrompt(
                new AiClient.AiContext(UUID.randomUUID(), UUID.randomUUID(), "ASSESSMENT"),
                "Q?", "MCQ", "Paris", "London", null));
        assertThat(wrong.score()).isEqualTo(0);
    }

    @Test
    void openAssessmentScoresCoverage() {
        AssessmentResult r = ai.evaluateAnswer(new AiClient.AssessmentPrompt(
                new AiClient.AiContext(UUID.randomUUID(), UUID.randomUUID(), "ASSESSMENT"),
                "Explain photosynthesis", "OPEN",
                "Photosynthesis uses sunlight and chlorophyll in leaves",
                "Photosynthesis uses sunlight in leaves", null));
        assertThat(r.score()).isBetween(0, 100);
        assertThat(r.feedback()).isNotBlank();
    }

    @Test
    void blankOpenAnswerScoresZero() {
        AssessmentResult r = ai.evaluateAnswer(new AiClient.AssessmentPrompt(
                new AiClient.AiContext(UUID.randomUUID(), UUID.randomUUID(), "ASSESSMENT"),
                "Q?", "OPEN", "expected", "  ", null));
        assertThat(r.score()).isEqualTo(0);
    }

    @Test
    void offlineEmbeddingsAreDeterministicAndNormalized() {
        var a = OpenAiClient.offlineEmbed("gradient descent learning rate", 1536);
        var b = OpenAiClient.offlineEmbed("gradient descent learning rate", 1536);
        assertThat(a).hasSize(1536);
        assertThat(a).isEqualTo(b);
        double norm = Math.sqrt(a.stream().mapToDouble(x -> x * x).sum());
        assertThat(norm).isCloseTo(1.0, org.assertj.core.api.Assertions.within(0.0001));
    }

    @Test
    void quizDraftRespectsCounts() {
        var focus = List.of(new AiClient.ConceptFocus("Backprop", 30, 2));
        var out = ai.generateQuiz(new AiClient.QuizPrompt(
                new AiClient.AiContext(UUID.randomUUID(), UUID.randomUUID(), "QUIZ_GENERATION"),
                "P", "G", focus,
                List.of(new AiClient.EvidenceChunk("m.pdf", 1, "content here")), 2, 1));
        assertThat(out).hasSize(3);
        assertThat(out.stream().filter(q -> q.type().equals("MCQ")).count()).isEqualTo(2);
        assertThat(out.stream().filter(q -> q.type().equals("OPEN")).count()).isEqualTo(1);
    }

    @Test
    void recommendationTargetsWeakestConcept() {
        var weak = List.of(new AiClient.WeakConcept("Overfitting", 25, "REQUIRING_ATTENTION"));
        var r = ai.generateRecommendation(new AiClient.RecommendationPrompt(
                new AiClient.AiContext(UUID.randomUUID(), UUID.randomUUID(), "RECOMMENDATION"),
                "P", "G", weak, "", ""));
        assertThat(r.text()).contains("Overfitting");
    }
}
