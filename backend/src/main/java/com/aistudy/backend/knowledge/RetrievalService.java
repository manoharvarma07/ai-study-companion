package com.aistudy.backend.knowledge;

import com.aistudy.backend.ai.OpenAiClient;
import com.aistudy.backend.ai.QueryIntent;
import com.aistudy.backend.common.config.AppProperties;
import com.aistudy.backend.material.MaterialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Project-scoped semantic retrieval. Every query is filtered by
 * (project_id, user_id) — cross-project leakage is impossible by construction.
 */
@Service
public class RetrievalService {
    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);
    private final DocumentChunkRepository chunks;
    private final MaterialRepository materials;
    private final OpenAiClient ai;
    private final int defaultTopK;

    public RetrievalService(DocumentChunkRepository chunks, MaterialRepository materials,
                            OpenAiClient ai, AppProperties props) {
        this.chunks = chunks;
        this.materials = materials;
        this.ai = ai;
        this.defaultTopK = props.rag().topK();
    }

    public record RetrievedChunk(UUID id, String materialName, Integer pageNumber,
                                 String content, double distance) {}

    public List<RetrievedChunk> retrieve(UUID projectId, UUID userId, String question) {
        return retrieve(projectId, userId, question, defaultTopK);
    }

    public List<RetrievedChunk> retrieve(UUID projectId, UUID userId, String question, int topK) {
        String literal = OpenAiClient.toVectorLiteral(ai.embed(question));
        List<RetrievedChunk> hits = new ArrayList<>();
        try {
            for (DocumentChunkRepository.ChunkHit h
                    : chunks.searchByProject(projectId, userId, literal, topK)) {
                hits.add(new RetrievedChunk(h.getId(), h.getMaterialName(),
                        h.getPageNumber(), h.getContent(),
                        h.getDistance() == null ? 2.0 : h.getDistance()));
            }
        } catch (Exception e) {
            log.warn("Vector search failed, falling back to keyword search: {}", e.getMessage());
        }
        if (hits.isEmpty()) {
            hits = keywordFallback(projectId, userId, question, topK);
        }
        return hits;
    }

    /**
     * Tutor retrieval with intention-aware breadth. Focused questions use semantic
     * top-k; document-wide questions (enumeration, summary) and applied
     * computation questions (which need full data tables) use page-level
     * document evidence so answers never depend on 5-6 semantic chunks alone.
     * Always scoped to (project_id, user_id).
     */
    public List<RetrievedChunk> retrieveForTutor(UUID projectId, UUID userId, String question,
                                                 QueryIntent intent) {
        if (intent == QueryIntent.ENUMERATION || intent == QueryIntent.SUMMARY
                || intent == QueryIntent.APPLIED) {
            List<RetrievedChunk> broad = documentEvidence(projectId, userId);
            if (!broad.isEmpty()) {
                log.info("[TUTOR] document-level retrieval: {} pages for intent {}",
                        broad.size(), intent);
                return broad;
            }
        }
        return retrieve(projectId, userId, question, defaultTopK);
    }

    /**
     * Page-level evidence across the whole project: chunks grouped by
     * (material, page) in document order, capped to bound prompt size.
     */
    List<RetrievedChunk> documentEvidence(UUID projectId, UUID userId) {
        Map<UUID, String> names = new HashMap<>();
        materials.findByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, userId)
                .forEach(m -> names.put(m.getId(), m.getFilename()));
        Map<String, List<DocumentChunk>> byPage = new java.util.LinkedHashMap<>();
        List<DocumentChunk> all = new ArrayList<>(chunks.findByProjectIdAndUserId(projectId, userId));
        all.sort(Comparator.comparing((DocumentChunk c) ->
                        names.getOrDefault(c.getMaterialId(), ""))
                .thenComparing((DocumentChunk c) ->
                        c.getPageNumber() == null ? Integer.MAX_VALUE : c.getPageNumber())
                .thenComparingInt(DocumentChunk::getChunkIndex));
        for (DocumentChunk c : all) {
            String key = c.getMaterialId() + "|" + c.getPageNumber();
            byPage.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }
        List<RetrievedChunk> out = new ArrayList<>();
        for (List<DocumentChunk> page : byPage.values()) {
            if (out.size() >= 20) {
                break;
            }
            DocumentChunk first = page.get(0);
            StringBuilder content = new StringBuilder();
            for (DocumentChunk c : page) {
                if (content.length() >= 900) {
                    break;
                }
                if (content.length() > 0) {
                    content.append("\n");
                }
                String part = c.getContent() == null ? "" : c.getContent();
                content.append(part, 0, Math.min(part.length(), 900 - content.length()));
            }
            out.add(new RetrievedChunk(first.getId(), names.get(first.getMaterialId()),
                    first.getPageNumber(), content.toString(), 1.0));
        }
        return out;
    }

    /** Keyword fallback scoped to the same project (used when vectors are missing). */
    List<RetrievedChunk> keywordFallback(UUID projectId, UUID userId, String question, int topK) {
        Set<String> keys = OpenAiClient.keywords(question);
        Map<UUID, String> names = new HashMap<>();
        materials.findByProjectIdAndUserIdOrderByCreatedAtDesc(projectId, userId)
                .forEach(m -> names.put(m.getId(), m.getFilename()));
        List<RetrievedChunk> out = new ArrayList<>();
        for (DocumentChunk c : chunks.findByProjectIdAndUserId(projectId, userId)) {
            String body = c.getContent() == null ? "" : c.getContent().toLowerCase();
            long score = keys.stream().filter(k -> OpenAiClient.matchesKeyword(body, k)).count();
            if (score > 0) {
                out.add(new RetrievedChunk(c.getId(), names.get(c.getMaterialId()),
                        c.getPageNumber(), c.getContent(), 1.0 - score * 0.1));
            }
        }
        out.sort((a, b) -> Double.compare(a.distance(), b.distance()));
        return out.stream().limit(topK).toList();
    }
}
