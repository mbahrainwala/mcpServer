package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises ImageRecognitionTool without loading DJL/PyTorch by injecting a stub VisionService.
 * The real model is covered by manual/integration runs (first use downloads native libs).
 */
class ImageRecognitionToolTest {

    private ImageRecognitionTool tool;

    @TempDir
    Path tempDir;

    /** Stub that returns canned detections, overriding all DJL-backed behaviour. */
    static class StubVisionService extends VisionService {
        boolean available = true;
        List<Detection> toReturn = List.of();

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public List<Detection> detect(byte[] imageBytes) {
            return toReturn;
        }
    }

    private StubVisionService stub;

    @BeforeEach
    void setUp() {
        tool = new ImageRecognitionTool();
        stub = new StubVisionService();
        tool.setVisionService(stub);
    }

    @Test
    void blankSource_returnsError() {
        assertThat(tool.recognizeObjects("  ")).contains("must not be blank");
    }

    @Test
    void nullSource_returnsError() {
        assertThat(tool.recognizeObjects(null)).contains("must not be blank");
    }

    @Test
    void serviceUnavailable_returnsRetryMessage() {
        stub.available = false;
        String result = tool.recognizeObjects("base64:AAAA");
        assertThat(result)
                .contains("not available")
                .contains("first");
    }

    @Test
    void noVisionService_returnsRetryMessage() {
        tool.setVisionService(null);
        assertThat(tool.recognizeObjects("base64:AAAA")).contains("not available");
    }

    @Test
    void detectsObjects_formatsLabelsWithConfidence() {
        stub.toReturn = List.of(
                new VisionService.Detection("car", 0.94, 0, 0, 0.5, 0.5),
                new VisionService.Detection("dog", 0.88, 0.1, 0.1, 0.3, 0.3));
        String result = tool.recognizeObjects("base64:" + b64Png());

        assertThat(result)
                .contains("Detected 2 object(s)")
                .contains("car (94%)")
                .contains("dog (88%)");
    }

    @Test
    void detectsObjects_alwaysIncludesFineDetailCaveat() {
        stub.toReturn = List.of(new VisionService.Detection("flower", 0.91, 0, 0, 1, 1));
        String result = tool.recognizeObjects("base64:" + b64Png());

        // The LLM must be told to use its own vision model for species/make/model.
        assertThat(result)
                .contains("basic object labels only")
                .contains("species")
                .contains("your own vision model");
    }

    @Test
    void noDetections_reportsNoneFound() {
        stub.toReturn = List.of();
        String result = tool.recognizeObjects("base64:" + b64Png());
        assertThat(result).contains("No objects detected");
    }

    @Test
    void missingFile_returnsError() {
        String result = tool.recognizeObjects(tempDir.resolve("ghost.jpg").toString());
        assertThat(result).contains("Error").contains("File not found");
    }

    @Test
    void readsLocalFile() throws Exception {
        Path img = tempDir.resolve("pic.png");
        Files.write(img, Base64.decode(b64Png()));
        stub.toReturn = List.of(new VisionService.Detection("potted plant", 0.77, 0, 0, 1, 1));

        String result = tool.recognizeObjects(img.toString());
        assertThat(result).contains("potted plant (77%)");
    }

    // 1x1 transparent PNG, base64 (content is irrelevant — the stub ignores the bytes)
    private static String b64Png() {
        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
    }

    static final class Base64 {
        static byte[] decode(String s) {
            return java.util.Base64.getDecoder().decode(s);
        }
    }
}
