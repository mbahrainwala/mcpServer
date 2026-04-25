package com.behrainwala.mcp.tools;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;

/**
 * MCP tool for extracting text and metadata from PDF files.
 * Avoids the need for vision tokens by returning structured plain text.
 */
@Service
public class PdfTool {

    private static final Logger log = LoggerFactory.getLogger(PdfTool.class);
    private static final int MAX_TEXT_LENGTH = 50_000;
    private static final int DEFAULT_DPI = 150;
    private static final int MAX_DPI = 300;
    private static final int MAX_IMAGE_PAGES = 5;
    private static final int MAX_OCR_PAGES = 10;
    private static final float JPEG_QUALITY = 0.8f;

    @Autowired(required = false)
    private OcrService ocrService;

    void setOcrService(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @Tool(name = "pdf_to_text",
          description = "Extract all text content from a PDF. Accepts a local file path, a URL, or base64-encoded PDF content. "
                  + "For text-based PDFs, uses fast embedded-text extraction. "
                  + "For image-based or scanned PDFs (where no embedded text exists), automatically falls back to "
                  + "Tesseract OCR so that LLMs do not need to spend vision tokens. "
                  + "Optionally restrict extraction to a specific page range.")
    public String pdfToText(
            @ToolParam(description = "Absolute local file path (e.g. 'C:/docs/report.pdf'), a URL pointing to a PDF, "
                    + "or base64-encoded PDF content (prefixed with 'base64:').") String source,
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
            sb.append("Source : ").append(displaySource(source)).append("\n");
            sb.append("Pages  : ").append(start).append("–").append(end)
              .append(" of ").append(totalPages).append("\n\n");

            if (text.isBlank()) {
                if (ocrService != null && ocrService.isAvailable()) {
                    String ocrText = ocrPages(doc, start, end);
                    if (!ocrText.isBlank()) {
                        sb.append("[OCR via Tesseract — pages ").append(start).append("–").append(end).append("]\n\n");
                        if (ocrText.length() > MAX_TEXT_LENGTH) {
                            sb.append(ocrText, 0, MAX_TEXT_LENGTH);
                            sb.append("\n\n... [truncated at ").append(MAX_TEXT_LENGTH)
                              .append(" chars — use startPage/endPage to extract specific sections]");
                        } else {
                            sb.append(ocrText);
                        }
                    } else {
                        sb.append("[No extractable text found. OCR was attempted but returned no text — ")
                          .append("this PDF appears to be image-only (scanned or non-text content) or encrypted. ")
                          .append("No further text extraction is possible.]");
                    }
                } else {
                    sb.append("[No extractable text found — this PDF is image-only (scanned or graphical content) or encrypted. ")
                      .append("Use the pdf_to_images tool to render pages as images for vision-based reading.]");
                }
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
          description = "Return metadata and structural information for a PDF: title, author, subject, keywords, "
                  + "creator application, PDF producer, creation/modification dates, number of pages, page size, "
                  + "PDF version, encryption status, and file size.")
    public String pdfMetadata(
            @ToolParam(description = "Absolute local file path (e.g. 'C:/docs/report.pdf'), a URL pointing to a PDF, "
                    + "or base64-encoded PDF content (prefixed with 'base64:').") String source) {

        try (PDDocument doc = loadDocument(source)) {
            PDDocumentInformation info = doc.getDocumentInformation();
            int totalPages = doc.getNumberOfPages();

            StringBuilder sb = new StringBuilder();
            sb.append("PDF Metadata\n");
            sb.append("────────────\n");
            sb.append("Source           : ").append(displaySource(source)).append("\n");

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

    /**
     * Renders PDF pages to base64-encoded JPEG images for vision-based reading.
     * Called by the MCP tool registration in McpServerConfig.
     */
    public record PdfImageResult(List<String> base64Images, int startPage, int endPage, int totalPages, String source) {}

    /**
     * Renders PDF pages to JPEG files on disk.  Returns file paths instead of base64 data.
     * Use this when the consuming LLM does not support ImageContent in tool results.
     */
    public record PdfFileResult(List<String> filePaths, int startPage, int endPage, int totalPages, String source) {}

    public PdfImageResult renderPagesToBase64Jpeg(String source, Integer startPage, Integer endPage, Integer dpi) throws Exception {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }

        int renderDpi = (dpi != null && dpi > 0) ? Math.min(dpi, MAX_DPI) : DEFAULT_DPI;

        try (PDDocument doc = loadDocument(source)) {
            int totalPages = doc.getNumberOfPages();

            int start = (startPage != null && startPage > 0) ? startPage : 1;
            int end   = (endPage   != null && endPage   > 0) ? Math.min(endPage, totalPages) : totalPages;

            if (start > totalPages) {
                throw new IllegalArgumentException("startPage (" + start + ") exceeds total pages (" + totalPages + ")");
            }
            if (start > end) {
                throw new IllegalArgumentException("startPage (" + start + ") is greater than endPage (" + end + ")");
            }

            // Cap the number of pages to avoid excessive memory/token usage
            if (end - start + 1 > MAX_IMAGE_PAGES) {
                end = start + MAX_IMAGE_PAGES - 1;
                log.info("Capping pdf_to_images to {} pages ({}–{} of {})", MAX_IMAGE_PAGES, start, end, totalPages);
            }

            PDFRenderer renderer = new PDFRenderer(doc);
            List<String> images = new ArrayList<>();

            for (int pageIdx = start - 1; pageIdx < end; pageIdx++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIdx, renderDpi);
                String base64 = encodeToBase64Jpeg(image);
                images.add(base64);
            }

            return new PdfImageResult(images, start, end, totalPages, displaySource(source));
        }
    }

    /**
     * Renders PDF pages to JPEG files on disk.  Suitable for LLMs that do not support
     * image content blocks in tool results (e.g. many local LLM APIs).
     */
    public PdfFileResult renderPagesToFiles(String source, Integer startPage, Integer endPage, Integer dpi) throws Exception {
        PdfImageResult imgResult = renderPagesToBase64Jpeg(source, startPage, endPage, dpi);

        String dirPrefix = "pdf_images_" + sanitizeForFilename(source) + "_";
        Path outputDir = Files.createTempDirectory(dirPrefix);
        List<String> filePaths = new ArrayList<>();

        for (int i = 0; i < imgResult.base64Images().size(); i++) {
            int pageNum = imgResult.startPage() + i;
            byte[] imageBytes = Base64.getDecoder().decode(imgResult.base64Images().get(i));
            Path filePath = outputDir.resolve("page_" + pageNum + ".jpg");
            Files.write(filePath, imageBytes);
            filePaths.add(filePath.toAbsolutePath().toString());
        }

        return new PdfFileResult(filePaths, imgResult.startPage(), imgResult.endPage(), imgResult.totalPages(), imgResult.source());
    }

    private String ocrPages(PDDocument doc, int start, int end) {
        int ocrEnd = Math.min(end, start + MAX_OCR_PAGES - 1);
        if (ocrEnd < end) {
            log.info("Capping OCR to {} pages ({}–{} of requested {})", MAX_OCR_PAGES, start, ocrEnd, end);
        }
        PDFRenderer renderer = new PDFRenderer(doc);
        StringBuilder sb = new StringBuilder();
        for (int pageIdx = start - 1; pageIdx < ocrEnd; pageIdx++) {
            try {
                BufferedImage img = renderer.renderImageWithDPI(pageIdx, DEFAULT_DPI);
                String pageText = ocrService.ocr(img);
                if (!pageText.isBlank()) {
                    sb.append(pageText).append("\n");
                }
            } catch (Exception e) {
                log.warn("OCR failed for page {}: {}", pageIdx + 1, e.getMessage());
            }
        }
        return sb.toString().strip();
    }

    private String encodeToBase64Jpeg(BufferedImage image) throws Exception {
        // Convert to RGB if the image has an alpha channel (JPEG doesn't support alpha)
        BufferedImage rgbImage = image;
        if (image.getType() == BufferedImage.TYPE_INT_ARGB || image.getTransparency() != BufferedImage.OPAQUE) {
            rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgbImage.createGraphics().drawImage(image, 0, 0, java.awt.Color.WHITE, null);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgbImage, null, null), param);
        } finally {
            writer.dispose();
        }

        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static final String BASE64_PREFIX = "base64:";

    private PDDocument loadDocument(String source) throws Exception {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        String trimmed = source.strip();
        if (trimmed.startsWith(BASE64_PREFIX)) {
            byte[] bytes = Base64.getDecoder().decode(trimmed.substring(BASE64_PREFIX.length()));
            return Loader.loadPDF(bytes);
        } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
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

    private String displaySource(String source) {
        if (source != null && source.strip().startsWith(BASE64_PREFIX)) {
            int dataLen = source.strip().length() - BASE64_PREFIX.length();
            return "[base64-encoded PDF, ~" + formatSize((long) (dataLen * 0.75)) + "]";
        }
        return source;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    /**
     * Extracts the filename (without extension) from a source path or URL and strips
     * characters that are unsafe in directory names.  Returns a short, stable prefix
     * so that temp folders are identifiable per-PDF (e.g. "pdf_images_report_&lt;random&gt;").
     */
    private static String sanitizeForFilename(String source) {
        if (source == null || source.isBlank()) return "unknown";
        String trimmed = source.strip();

        // For base64 content there is no meaningful name
        if (trimmed.startsWith(BASE64_PREFIX)) return "base64";

        // Grab the last path/URL segment
        String name = trimmed;
        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < name.length() - 1) {
            name = name.substring(lastSlash + 1);
        }
        // Strip query strings for URLs
        int query = name.indexOf('?');
        if (query > 0) name = name.substring(0, query);

        // Remove extension
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);

        // Replace non-alphanumeric chars with underscores and trim length
        name = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (name.length() > 40) name = name.substring(0, 40);
        return name.isEmpty() ? "unknown" : name;
    }
}
