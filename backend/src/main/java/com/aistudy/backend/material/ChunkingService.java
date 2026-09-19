package com.aistudy.backend.material;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits extracted page text into overlapping chunks, preserving page numbers.
 */
@Service
public class ChunkingService {
    public static final int CHUNK_SIZE = 900;
    public static final int OVERLAP = 120;

    public record TextChunk(String content, Integer pageNumber, int chunkIndex) {}

    public List<TextChunk> chunk(List<PdfService.PageText> pages) {
        List<TextChunk> out = new ArrayList<>();
        int index = 0;
        for (PdfService.PageText page : pages) {
            String text = page.text();
            if (text == null || text.isBlank()) {
                continue;
            }
            String normalized = text.replaceAll("\\s+", " ").strip();
            int start = 0;
            boolean first = true;
            while (start < normalized.length()) {
                int end = Math.min(start + CHUNK_SIZE, normalized.length());
                // prefer to break on sentence/word boundary
                if (end < normalized.length()) {
                    int dot = normalized.lastIndexOf(". ", end);
                    if (dot > start + CHUNK_SIZE / 2) {
                        end = dot + 1;
                    } else {
                        int space = normalized.lastIndexOf(' ', end);
                        if (space > start + CHUNK_SIZE / 2) {
                            end = space;
                        }
                    }
                }
                String piece = normalized.substring(start, end).strip();
                if (!piece.isBlank()) {
                    out.add(new TextChunk(piece, page.pageNumber(), index++));
                }
                if (end >= normalized.length()) {
                    break;
                }
                start = first ? end : Math.max(end - OVERLAP, start + 1);
                first = false;
                if (out.size() > 5000) {
                    break; // safety cap per document
                }
            }
        }
        return out;
    }
}
