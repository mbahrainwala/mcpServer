package com.behrainwala.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.List;

/**
 * Recognises the objects present in a photo (flowers, plants, cars, animals, everyday items)
 * using the local DJL/PyTorch model in {@link VisionService}. Returns plain-text labels with
 * confidence scores so a local LLM never has to spend vision tokens just to see "what is here".
 */
@Service
public class ImageRecognitionTool {

    private static final Logger log = LoggerFactory.getLogger(ImageRecognitionTool.class);
    private static final String BASE64_PREFIX = "base64:";

    @Autowired(required = false)
    private VisionService visionService;

    void setVisionService(VisionService visionService) {
        this.visionService = visionService;
    }

    @Tool(name = "image_recognize_objects",
          description = "Detect and name the objects in a photo (flowers, plants, cars, animals, "
                  + "people, everyday items). Accepts a local file path, a URL, or base64-encoded "
                  + "image content (prefixed with 'base64:'). Runs fully locally — no internet, no API keys. "
                  + "IMPORTANT: this returns only BASIC, coarse object labels with confidence scores "
                  + "(e.g. 'flower', 'car', 'dog') — it does NOT identify fine details such as the exact "
                  + "plant/flower species, the car's make/model/year, breed, or other attributes. "
                  + "If you need that level of detail, do NOT rely on this tool's output — use your own "
                  + "vision capabilities to inspect the image directly. Use this tool for a quick, "
                  + "cheap inventory of what is in the picture.")
    public String recognizeObjects(
            @ToolParam(description = "Absolute local file path (e.g. 'C:/images/photo.jpg'), "
                    + "a URL pointing to an image, or base64-encoded image content (prefixed with 'base64:').")
            String source) {

        if (source == null || source.isBlank()) {
            return "Error: source must not be blank.";
        }

        if (visionService == null || !visionService.isAvailable()) {
            return "Error: image object recognition is not available. The model initializes on first "
                    + "use and downloads the PyTorch runtime + detection model (~hundreds of MB) to "
                    + "~/.djl.ai/ on first request. Please retry in a moment.";
        }

        try {
            byte[] imageBytes = loadImageBytes(source);
            List<VisionService.Detection> detections = visionService.detect(imageBytes);

            StringBuilder sb = new StringBuilder();
            sb.append("Image Object Recognition\n");
            sb.append("Source: ").append(displaySource(source)).append("\n");

            if (detections.isEmpty()) {
                sb.append("No objects detected with sufficient confidence.\n");
            } else {
                sb.append("Detected ").append(detections.size()).append(" object(s):\n");
                for (VisionService.Detection d : detections) {
                    sb.append(String.format("  - %s (%.0f%%)%n", d.label(), d.confidence() * 100));
                }
            }

            sb.append("\nNote: these are basic object labels only. For species, make/model, breed, "
                    + "or other fine-grained details, inspect the image with your own vision model.");
            return sb.toString();

        } catch (Exception e) {
            log.error("image_recognize_objects failed for '{}': {}", source, e.getMessage());
            return "Error recognizing image: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private byte[] loadImageBytes(String source) throws Exception {
        String trimmed = source.strip();
        if (trimmed.startsWith(BASE64_PREFIX)) {
            return Base64.getDecoder().decode(trimmed.substring(BASE64_PREFIX.length()));
        } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            URL url = URI.create(trimmed).toURL();
            try (InputStream is = url.openStream()) {
                return is.readAllBytes();
            }
        } else {
            java.io.File file = new java.io.File(trimmed);
            if (!file.exists()) {
                throw new IllegalArgumentException("File not found: " + trimmed);
            }
            try (InputStream is = new java.io.FileInputStream(file)) {
                return is.readAllBytes();
            }
        }
    }

    private String displaySource(String source) {
        String trimmed = source.strip();
        if (trimmed.startsWith(BASE64_PREFIX)) {
            int dataLen = trimmed.length() - BASE64_PREFIX.length();
            long approxBytes = (long) (dataLen * 0.75);
            return "[base64-encoded image, ~" + approxBytes + " bytes]";
        }
        return source;
    }
}
