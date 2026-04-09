package com.behrainwala.mcp.tools;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * MCP tool for extracting text and metadata from PDF files.
 * Avoids the need for vision tokens by returning structured plain text.
 */
@Service
public class PdfTool {

    private static final Logger log = LoggerFactory.getLogger(PdfTool.class);
    private static final int MAX_TEXT_LENGTH = 50_000;

    @Tool(name = "pdf_to_text",
          description = "Extract all text content from a PDF file at a local file path or a URL. "
                  + "Returns the full text of the document so that LLMs do not need to spend vision tokens on PDF images. "
                  + "Optionally restrict extraction to a specific page range.")
    public String pdfToText(
            @ToolParam(description = "Absolute local file path (e.g. 'C:/docs/report.pdf') or a URL pointing to a PDF.") String source,
            @ToolParam(description = "First page to extract (1-based, inclusive). Omit or set 0 for the beginning.", required = false) Integer startPage,
            @ToolParam(description = "Last page to extract (1-based, inclusive). Omit or set 0 for the last page.", required = false) Integer endPage) {

        try (PDDocument doc = loadDocument(source)) {
            int totalPages = doc.getNumberOfPages();

            int start = (startPage != null && startPage > 0) ? startPage : 1;
            int end   = (endPage   != null && endPage   > 0) ? Math.min(endPage, totalPages) : totalPages;

            if (start > totalPages) {
                return "Error: startPage (" + start + ") exceeds total pages (" + totalPages + ").";
            }
            if (start > end) {
                return "Error: startPage (" + start + ") is greater than endPage (" + end + ").";
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(start);
            stripper.setEndPage(end);
            stripper.setSortByPosition(true);

            String text = stripper.getText(doc);

            StringBuilder sb = new StringBuilder();
            sb.append("PDF Text Extraction\n");
            sb.append("───────────────────\n");
            sb.append("Source : ").append(source).append("\n");
            sb.append("Pages  : ").append(start).append("–").append(end)
              .append(" of ").append(totalPages).append("\n\n");

            if (text.isBlank()) {
                sb.append("[No extractable text found — the PDF may be image-based or encrypted.]");
            } else if (text.length() > MAX_TEXT_LENGTH) {
                sb.append(text, 0, MAX_TEXT_LENGTH);
                sb.append("\n\n... [truncated at ").append(MAX_TEXT_LENGTH).append(" chars — use startPage/endPage to extract specific sections]");
            } else {
                sb.append(text);
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("pdf_to_text failed for '{}': {}", source, e.getMessage());
            return "Error extracting PDF text: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    @Tool(name = "pdf_metadata",
          description = "Return metadata and structural information for a PDF file: title, author, subject, keywords, "
                  + "creator application, PDF producer, creation/modification dates, number of pages, page size, "
                  + "PDF version, encryption status, and file size.")
    public String pdfMetadata(
            @ToolParam(description = "Absolute local file path (e.g. 'C:/docs/report.pdf') or a URL pointing to a PDF.") String source) {

        try (PDDocument doc = loadDocument(source)) {
            PDDocumentInformation info = doc.getDocumentInformation();
            int totalPages = doc.getNumberOfPages();

            StringBuilder sb = new StringBuilder();
            sb.append("PDF Metadata\n");
            sb.append("────────────\n");
            sb.append("Source           : ").append(source).append("\n");

            // Document information dictionary
            appendMeta(sb, "Title",            info.getTitle());
            appendMeta(sb, "Author",           info.getAuthor());
            appendMeta(sb, "Subject",          info.getSubject());
            appendMeta(sb, "Keywords",         info.getKeywords());
            appendMeta(sb, "Creator",          info.getCreator());
            appendMeta(sb, "Producer",         info.getProducer());
            appendMeta(sb, "Creation Date",    formatCalendar(info.getCreationDate()));
            appendMeta(sb, "Modification Date",formatCalendar(info.getModificationDate()));

            // Structure
            sb.append("\nStructure\n");
            sb.append("─────────\n");
            sb.append("Pages            : ").append(totalPages).append("\n");
            sb.append("PDF Version      : ").append(doc.getVersion()).append("\n");
            sb.append("Encrypted        : ").append(doc.isEncrypted()).append("\n");

            // Page sizes (report first page and flag if mixed)
            if (totalPages > 0) {
                PDPage firstPage = doc.getPage(0);
                PDRectangle mediaBox = firstPage.getMediaBox();
                sb.append("Page Size (p.1)  : ")
                  .append(String.format("%.0f x %.0f pt", mediaBox.getWidth(), mediaBox.getHeight()))
                  .append(" (")
                  .append(String.format("%.1f x %.1f in", mediaBox.getWidth() / 72, mediaBox.getHeight() / 72))
                  .append(")\n");
                sb.append("Rotation (p.1)   : ").append(firstPage.getRotation()).append("°\n");
            }

            // File size if local
            try {
                File f = new File(source);
                if (f.exists()) {
                    long bytes = f.length();
                    sb.append("File Size        : ").append(formatSize(bytes)).append("\n");
                }
            } catch (Exception ignored) { /* URL sources — size not available */ }

            return sb.toString();

        } catch (Exception e) {
            log.error("pdf_metadata failed for '{}': {}", source, e.getMessage());
            return "Error reading PDF metadata: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private PDDocument loadDocument(String source) throws Exception {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        String trimmed = source.strip();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            URL url = URI.create(trimmed).toURL();
            try (InputStream is = url.openStream()) {
                return Loader.loadPDF(is.readAllBytes());
            }
        } else {
            return Loader.loadPDF(new File(trimmed));
        }
    }

    private void appendMeta(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(String.format("%-17s: %s%n", label, value));
        }
    }

    private String formatCalendar(Calendar cal) {
        if (cal == null) return null;
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(cal.getTime());
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
