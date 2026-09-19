package com.aistudy.backend.knowledge;

import com.aistudy.backend.ai.QueryIntent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryIntentTest {

    @Test
    void definition() {
        assertThat(QueryIntent.classify("What is classification?")).isEqualTo(QueryIntent.DEFINITION);
        assertThat(QueryIntent.classify("What are predefined classes?")).isEqualTo(QueryIntent.DEFINITION);
        assertThat(QueryIntent.classify("Define overfitting.")).isEqualTo(QueryIntent.DEFINITION);
    }

    @Test
    void explanationAndProcess() {
        assertThat(QueryIntent.classify("Explain gradient descent.")).isEqualTo(QueryIntent.EXPLANATION);
        assertThat(QueryIntent.classify("How does KNN work?")).isEqualTo(QueryIntent.PROCESS);
        assertThat(QueryIntent.classify("How does Naive Bayes work?")).isEqualTo(QueryIntent.PROCESS);
    }

    @Test
    void comparisonBeatsDefinitionPrefix() {
        assertThat(QueryIntent.classify("What is the difference between Naive Bayes and KNN?"))
                .isEqualTo(QueryIntent.COMPARISON);
        assertThat(QueryIntent.classify("KNN vs Naive Bayes?")).isEqualTo(QueryIntent.COMPARISON);
    }

    @Test
    void enumeration() {
        assertThat(QueryIntent.classify("How many algorithms are there in the PDF?"))
                .isEqualTo(QueryIntent.ENUMERATION);
        assertThat(QueryIntent.classify("List all algorithms in the PDF.")).isEqualTo(QueryIntent.ENUMERATION);
        assertThat(QueryIntent.classify("Which algorithms are discussed?")).isEqualTo(QueryIntent.ENUMERATION);
        assertThat(QueryIntent.classify("What topics are covered?")).isEqualTo(QueryIntent.ENUMERATION);
    }

    @Test
    void navigation() {
        assertThat(QueryIntent.classify("Which page discusses Naive Bayes?")).isEqualTo(QueryIntent.NAVIGATION);
        assertThat(QueryIntent.classify("Where is KNN explained?")).isEqualTo(QueryIntent.NAVIGATION);
    }

    @Test
    void summary() {
        assertThat(QueryIntent.classify("Summarize the document.")).isEqualTo(QueryIntent.SUMMARY);
        assertThat(QueryIntent.classify("Give me a brief overview.")).isEqualTo(QueryIntent.SUMMARY);
    }

    @Test
    void applied() {
        assertThat(QueryIntent.classify("Consider a new student with study hours 4.5 and attendance 8.5. Which class does KNN predict?"))
                .isEqualTo(QueryIntent.APPLIED);
        assertThat(QueryIntent.classify("Classify this new data point with Naive Bayes."))
                .isEqualTo(QueryIntent.APPLIED);
        assertThat(QueryIntent.classify("What is the difference between Naive Bayes and KNN?"))
                .isEqualTo(QueryIntent.COMPARISON);
    }

    @Test
    void generalAndEdgeCases() {
        assertThat(QueryIntent.classify("Can you explain that more simply?")).isEqualTo(QueryIntent.EXPLANATION);
        assertThat(QueryIntent.classify("Tell me about regularization.")).isEqualTo(QueryIntent.GENERAL);
        assertThat(QueryIntent.classify(null)).isEqualTo(QueryIntent.GENERAL);
        assertThat(QueryIntent.classify("  ")).isEqualTo(QueryIntent.GENERAL);
    }
}
