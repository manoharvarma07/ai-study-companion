package com.aistudy.backend.material;

import com.aistudy.backend.analytics.ActivityEventService;
import com.aistudy.backend.knowledge.ConceptService;
import com.aistudy.backend.knowledge.DocumentChunk;
import com.aistudy.backend.knowledge.DocumentChunkRepository;
import com.aistudy.backend.knowledge.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Async pipeline: QUEUED → PROCESSING → extract → chunk → embed → concepts → READY.
 * Runs off the request thread; failures land in FAILED with attempts + error recorded.
 */
@Service
public class MaterialProcessor {
    private static final Logger log = LoggerFactory.getLogger(MaterialProcessor.class);
    static final int MAX_ATTEMPTS = 3;

    private final MaterialRepository materials;
    private final DocumentChunkRepository chunks;
    private final StorageService storage;
    private final PdfService pdf;
    private final ChunkingService chunking;
    private final EmbeddingService embeddings;
    private final ConceptService concepts;
    private final ActivityEventService events;

    public MaterialProcessor(MaterialRepository materials, DocumentChunkRepository chunks,
                             StorageService storage, PdfService pdf, ChunkingService chunking,
                             EmbeddingService embeddings, ConceptService concepts,
                             ActivityEventService events) {
        this.materials = materials;
        this.chunks = chunks;
        this.storage = storage;
        this.pdf = pdf;
        this.chunking = chunking;
        this.embeddings = embeddings;
        this.concepts = concepts;
        this.events = events;
    }

    @Async("materialProcessorExecutor")
    @Transactional
    public void processAsync(UUID materialId) {
        Material m = materials.findById(materialId).orElse(null);
        if (m == null) {
            log.warn("Material {} gone, skipping processing", materialId);
            return;
        }
        // Idempotency guard: never process twice concurrently or re-process READY work.
        if (m.getStatus() == Material.MaterialStatus.PROCESSING
                || m.getStatus() == Material.MaterialStatus.READY) {
            log.info("Material {} already {}, skipping", materialId, m.getStatus());
            return;
        }
        if (m.getProcessingAttempts() >= MAX_ATTEMPTS) {
            log.warn("Material {} exceeded max attempts, leaving FAILED", materialId);
            return;
        }

        m.setStatus(Material.MaterialStatus.PROCESSING);
        m.setProcessingAttempts(m.getProcessingAttempts() + 1);
        m.setErrorMessage(null);
        materials.save(m);
        events.record(m.getUser().getId(), m.getProject().getId(), "MATERIAL_PROCESSING_STARTED",
                Map.of("materialId", materialId.toString(), "attempt", m.getProcessingAttempts()),
                "material-processing-" + materialId + "-" + m.getProcessingAttempts());

        try {
            process(m);
            m.setStatus(Material.MaterialStatus.READY);
            m.setErrorMessage(null);
            materials.save(m);
            events.record(m.getUser().getId(), m.getProject().getId(), "MATERIAL_PROCESSED",
                    Map.of("materialId", materialId.toString(), "pages", String.valueOf(m.getPageCount())),
                    "material-processed-" + materialId);
            log.info("Material {} READY ({} pages)", materialId, m.getPageCount());
        } catch (Exception e) {
            log.error("Processing failed for material {}", materialId, e);
            m.setStatus(Material.MaterialStatus.FAILED);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            m.setErrorMessage(msg.length() > 2000 ? msg.substring(0, 2000) : msg);
            materials.save(m);
            events.record(m.getUser().getId(), m.getProject().getId(), "MATERIAL_PROCESSING_FAILED",
                    Map.of("materialId", materialId.toString(), "error", m.getErrorMessage()),
                    "material-failed-" + materialId + "-" + m.getProcessingAttempts());
        }
    }

    private void process(Material m) throws Exception {
        PdfService.ExtractedPdf extracted;
        try (InputStream in = storage.load(m.getStoragePath())) {
            extracted = pdf.extract(in, m.getFileSize());
        }
        m.setPageCount(extracted.pageCount());

        List<ChunkingService.TextChunk> pieces = chunking.chunk(extracted.pages());
        if (pieces.isEmpty()) {
            throw new IllegalStateException("No extractable text found in PDF");
        }
        // Replace any previous chunks for a clean re-process (retry safety).
        chunks.deleteByMaterialIdAndUserId(m.getId(), m.getUser().getId());

        List<DocumentChunk> entities = new ArrayList<>(pieces.size());
        for (ChunkingService.TextChunk tc : pieces) {
            DocumentChunk c = new DocumentChunk();
            c.setMaterialId(m.getId());
            c.setProjectId(m.getProject().getId());
            c.setUserId(m.getUser().getId());
            c.setContent(tc.content());
            c.setPageNumber(tc.pageNumber());
            c.setChunkIndex(tc.chunkIndex());
            c.setEmbedding(embeddings.embedToLiteral(tc.content()));
            c.setMetadata("{\"material\":\"" + escape(m.getFilename()) + "\"}");
            entities.add(c);
        }
        chunks.saveAll(entities);

        String sample = extracted.fullText();
        concepts.extractAndStore(m.getUser().getId(), m.getProject().getId(),
                m.getFilename(), sample.length() > 12000 ? sample.substring(0, 12000) : sample);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
