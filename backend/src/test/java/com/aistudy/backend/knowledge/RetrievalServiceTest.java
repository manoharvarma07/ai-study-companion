package com.aistudy.backend.knowledge;

import com.aistudy.backend.ai.OpenAiClient;
import com.aistudy.backend.ai.QueryIntent;
import com.aistudy.backend.common.config.AppProperties;
import com.aistudy.backend.material.Material;
import com.aistudy.backend.material.MaterialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private DocumentChunkRepository chunks;
    @Mock
    private MaterialRepository materials;
    @Mock
    private OpenAiClient ai;

    private RetrievalService service() {
        AppProperties props = new AppProperties(
                new AppProperties.Jwt("test-secret-that-is-at-least-32-bytes-long!", 3600000),
                new AppProperties.Ai("", "https://openrouter.ai/api/v1", "m", "e", 1536, 60000, true),
                new AppProperties.Rag(5),
                new AppProperties.Storage("./uploads"));
        return new RetrievalService(chunks, materials, ai, props);
    }

    private Material material(UUID id, String filename) {
        Material m = new Material();
        m.setId(id);
        m.setFilename(filename);
        return m;
    }

    private DocumentChunk chunk(UUID id, UUID materialId, int page, int index, String content) {
        DocumentChunk c = new DocumentChunk();
        c.setId(id);
        c.setMaterialId(materialId);
        c.setProjectId(UUID.randomUUID());
        c.setUserId(UUID.randomUUID());
        c.setPageNumber(page);
        c.setChunkIndex(index);
        c.setContent(content);
        return c;
    }

    @Test
    void enumerationUsesDocumentLevelEvidence() {
        UUID user = UUID.randomUUID(), project = UUID.randomUUID(), mat = UUID.randomUUID();
        when(materials.findByProjectIdAndUserIdOrderByCreatedAtDesc(project, user))
                .thenReturn(List.of(material(mat, "doc.pdf")));
        when(chunks.findByProjectIdAndUserId(project, user)).thenReturn(List.of(
                chunk(UUID.randomUUID(), mat, 2, 1, "KNN content"),
                chunk(UUID.randomUUID(), mat, 1, 0, "intro"),
                chunk(UUID.randomUUID(), mat, 12, 0, "Naive Bayes content")));

        List<RetrievalService.RetrievedChunk> out =
                service().retrieveForTutor(project, user, "List all algorithms.", QueryIntent.ENUMERATION);

        // Page-level, document order, spanning the whole document — not top-k fragments.
        assertThat(out).hasSize(3);
        assertThat(out.stream().map(RetrievalService.RetrievedChunk::pageNumber).toList())
                .containsExactly(1, 2, 12);
        assertThat(out.stream().map(RetrievalService.RetrievedChunk::materialName).distinct().toList())
                .containsExactly("doc.pdf");
        verify(chunks, never()).searchByProject(any(), any(), any(), eq(5));
    }

    @Test
    void appliedComputationUsesDocumentLevelEvidence() {
        UUID user = UUID.randomUUID(), project = UUID.randomUUID(), mat = UUID.randomUUID();
        when(materials.findByProjectIdAndUserIdOrderByCreatedAtDesc(project, user))
                .thenReturn(List.of(material(mat, "doc.pdf")));
        when(chunks.findByProjectIdAndUserId(project, user)).thenReturn(List.of(
                chunk(UUID.randomUUID(), mat, 3, 0, "student table"),
                chunk(UUID.randomUUID(), mat, 1, 0, "intro")));

        List<RetrievalService.RetrievedChunk> out = service().retrieveForTutor(
                project, user, "Which class does KNN predict for the new student?", QueryIntent.APPLIED);

        // Full data tables must reach the model so it can compute, not refuse.
        assertThat(out).hasSize(2);
        assertThat(out.stream().map(RetrievalService.RetrievedChunk::pageNumber).toList())
                .containsExactly(1, 3);
        verify(chunks, never()).searchByProject(any(), any(), any(), eq(5));
    }

    @Test
    void definitionUsesSemanticTopK() {
        UUID user = UUID.randomUUID(), project = UUID.randomUUID();
        DocumentChunkRepository.ChunkHit hit = org.mockito.Mockito.mock(
                DocumentChunkRepository.ChunkHit.class);
        when(hit.getId()).thenReturn(UUID.randomUUID());
        when(hit.getContent()).thenReturn("Classification content");
        when(hit.getPageNumber()).thenReturn(1);
        when(hit.getMaterialName()).thenReturn("doc.pdf");
        when(chunks.searchByProject(eq(project), eq(user), any(), eq(5))).thenReturn(List.of(hit));
        when(ai.embed("What is classification?")).thenReturn(List.of(0.1, 0.2));

        List<RetrievalService.RetrievedChunk> out = service()
                .retrieveForTutor(project, user, "What is classification?", QueryIntent.DEFINITION);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).content()).isEqualTo("Classification content");
        verify(chunks, never()).findByProjectIdAndUserId(any(), any());
    }
}
