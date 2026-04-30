package com.behrainwala.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.Iterator;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

@Service
public class ImageTool {

    private static final Logger log = LoggerFactory.getLogger(ImageTool.class);
    private static final int MAX_TEXT_LENGTH = 50_000;
    private static final String BASE64_PREFIX = "base64:";

    @Autowired(required = false)
    private OcrService ocrService;

    void setOcrService(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @Tool(name = "image_to_text",
          description = "Extract all text from an image file using Tesseract OCR. "
                  + "Accepts a local file path, a URL, or base64-encoded image content. "
                  + "Supports common formats: JPEG, PNG, BMP, GIF, TIFF. "
                  + "Designed for local LLMs that should not spend vision tokens reading images — "
                  + "use this tool to get the text content of screenshots, scanned documents, "
                  + "photos of signs, receipts, forms, and any image containing readable text.")
    public String imageToText(
            @ToolParam(description = "Absolute local file path (e.g. 'C:/images/scan.png'), "
                    + "a URL pointing to an image, "
                    + "or base64-encoded image content (prefixed with 'base64:').") String source) {

        if (source == null || source.isBlank()) {
            return "Error: source must not be blank.";
        }

        if (ocrService == null || !ocrService.isAvailable()) {
            return "Error: OCR (Tesseract) is not available. "
                    + "Tesseract initializes on first use and downloads language data (~10 MB). "
                    + "Please retry in a moment.";
        }

        try {
            ImageLoadResult loaded = loadImage(source);
            BufferedImage image = loaded.image();
            String formatName = loaded.format();

            StringBuilder sb = new StringBuilder();
            sb.append("Image Text Extraction\n");
            sb.append("─────────────────────\n");
            sb.append("Source : ").append(displaySource(source)).append("\n");
            sb.append("Format : ").append(formatName).append("\n");
            sb.append("Size   : ").append(image.getWidth()).append(" x ").append(image.getHeight()).append(" px\n\n");

            String text = ocrService.ocr(image);

            if (text.isBlank()) {
                sb.append("[No text found — the image may contain no readable text, "
                        + "or the text may be too small, blurry, or stylized for OCR to detect.]");
            } else if (text.length() > MAX_TEXT_LENGTH) {
                sb.append(text, 0, MAX_TEXT_LENGTH);
                sb.append("\n\n... [truncated at ").append(MAX_TEXT_LENGTH).append(" chars]");
            } else {
                sb.append(text);
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("image_to_text failed for '{}': {}", source, e.getMessage());
            return "Error extracting text from image: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private record ImageLoadResult(BufferedImage image, String format) {}

    private ImageLoadResult loadImage(String source) throws Exception {
        String trimmed = source.strip();

        if (trimmed.startsWith(BASE64_PREFIX)) {
            byte[] bytes = Base64.getDecoder().decode(trimmed.substring(BASE64_PREFIX.length()));
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                return readWithFormat(bais);
            }
        } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            URL url = URI.create(trimmed).toURL();
            try (InputStream is = url.openStream()) {
                return readWithFormat(is);
            }
        } else {
            File file = new File(trimmed);
            if (!file.exists()) {
                throw new IllegalArgumentException("File not found: " + trimmed);
            }
            try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
                return readWithFormat(iis);
            }
        }
    }

    private ImageLoadResult readWithFormat(Object input) throws Exception {
        ImageInputStream iis = (input instanceof ImageInputStream existing)
                ? existing
                : ImageIO.createImageInputStream(input);

        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (!readers.hasNext()) {
            throw new IllegalArgumentException(
                    "Unsupported or unrecognisable image format. "
                    + "Supported formats: JPEG, PNG, BMP, GIF, TIFF (and any format registered with Java ImageIO).");
        }

        ImageReader reader = readers.next();
        String format = reader.getFormatName().toUpperCase();
        try {
            reader.setInput(iis);
            BufferedImage image = reader.read(0);
            return new ImageLoadResult(image, format);
        } finally {
            reader.dispose();
        }
    }

    private String displaySource(String source) {
        if (source != null && source.strip().startsWith(BASE64_PREFIX)) {
            int dataLen = source.strip().length() - BASE64_PREFIX.length();
            return "[base64-encoded image, ~" + formatSize((long) (dataLen * 0.75)) + "]";
        }
        return source;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
