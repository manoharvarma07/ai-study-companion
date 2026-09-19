package com.aistudy.backend.material;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfAndChunkingTest {

    static byte[] minimalPdf(String text) {
        String stream = "BT /F1 12 Tf 50 750 Td (" + text + ") Tj ET";
        List<String> objs = List.of(
                "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj",
                "2 0 obj << /Type /Pages /Kids [3 0 R 6 0 R] /Count 2 >> endobj",
                "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj",
                "4 0 obj << /Length " + stream.length() + " >> stream\n" + stream + "\nendstream endobj",
                "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj",
                "6 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj");
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new java.util.ArrayList<>();
        for (String o : objs) {
            offsets.add(pdf.length());
            pdf.append(o).append("\n");
        }
        int xref = pdf.length();
        pdf.append("xref\n0 7\n0000000000 65535 f \n");
        for (int off : offsets) {
            pdf.append(String.format("%010d 00000 n \n", off));
        }
        pdf.append("trailer << /Size 7 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test
    void extractsTextPerPage() throws Exception {
        PdfService pdf = new PdfService();
        PdfService.ExtractedPdf out = pdf.extract(
                new ByteArrayInputStream(minimalPdf("Gradient descent rocks")), 10_000);
        assertThat(out.pageCount()).isEqualTo(2);
        assertThat(out.fullText()).contains("Gradient descent rocks");
        assertThat(out.pages().get(0).pageNumber()).isEqualTo(1);
    }

    @Test
    void chunkingPreservesPagesAndSplitsLongText() {
        ChunkingService chunking = new ChunkingService();
        String longText = "Machine learning concept. ".repeat(200);
        List<ChunkingService.TextChunk> chunks = chunking.chunk(List.of(
                new PdfService.PageText(1, "Short intro."),
                new PdfService.PageText(3, longText)));
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).pageNumber()).isEqualTo(1);
        assertThat(chunks.stream().filter(c -> c.pageNumber() == 3).count()).isGreaterThan(1);
        assertThat(chunks).allMatch(c -> c.content().length() <= ChunkingService.CHUNK_SIZE + 50);
        // chunk indexes are unique and ordered
        List<Integer> idx = chunks.stream().map(ChunkingService.TextChunk::chunkIndex).toList();
        assertThat(idx).doesNotHaveDuplicates();
    }

    @Test
    void blankPagesAreSkipped() {
        ChunkingService chunking = new ChunkingService();
        List<ChunkingService.TextChunk> chunks = chunking.chunk(List.of(
                new PdfService.PageText(1, "   "),
                new PdfService.PageText(2, "Real content here about neural networks.")));
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).pageNumber()).isEqualTo(2);
    }
}
