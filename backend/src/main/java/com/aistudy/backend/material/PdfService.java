package com.aistudy.backend.material;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF text extraction with per-page granularity (PDFBox).
 * Uploaded documents are treated as untrusted DATA, never as instructions.
 */
@Service
public class PdfService {
    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    public record PageText(int pageNumber, String text) {}
    public record ExtractedPdf(List<PageText> pages, int pageCount) {
        public String fullText() {
            StringBuilder sb = new StringBuilder();
            for (PageText p : pages) {
                if (!p.text().isBlank()) {
                    sb.append(p.text().strip()).append("\n\n");
                }
            }
            return sb.toString().strip();
        }
    }

    public ExtractedPdf extract(InputStream data, long maxBytes) throws IOException {
        byte[] bytes = data.readAllBytes();
        if (bytes.length == 0) {
            throw new IOException("Empty PDF file");
        }
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            int pageCount = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            List<PageText> pages = new ArrayList<>(pageCount);
            for (int i = 1; i <= pageCount; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(doc);
                pages.add(new PageText(i, text == null ? "" : text.strip()));
            }
            log.info("Extracted {} pages from PDF", pageCount);
            return new ExtractedPdf(pages, pageCount);
        } catch (IOException e) {
            throw new IOException("Failed to parse PDF: " + e.getMessage(), e);
        }
    }
}
