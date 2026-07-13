package com.behrainwala.mcp.tools;

import ai.djl.Application;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Self-contained image object recognition backed by Deep Java Library (DJL) + PyTorch.
 * <p>
 * On first use, DJL downloads the PyTorch native runtime and an object-detection model from
 * the model zoo, caching both under {@code ~/.djl.ai/}. Subsequent calls run fully offline on
 * the local CPU — no API keys, no per-image network access. This mirrors {@link OcrService}'s
 * "download once, then offline" pattern.
 * <p>
 * The model produces coarse object categories (COCO-style: person, car, dog, potted plant, …)
 * plus the fine-grained dog/bird classes the backbone happens to know. It is NOT a fine-grained
 * species / make-model identifier — that is left to the calling LLM's own vision model.
 * <p>
 * The model can be overridden without a recompile via the system property
 * {@code mcp.vision.model-url} (e.g. {@code -Dmcp.vision.model-url=djl://ai.djl.pytorch/yolov5s}).
 */
@Service
public class VisionService {

    private static final Logger log = LoggerFactory.getLogger(VisionService.class);

    /** Optional override; when blank, DJL resolves the default PyTorch object-detection model. */
    private static final String MODEL_URL = System.getProperty("mcp.vision.model-url", "");

    /** Detections below this confidence are dropped before returning to the caller. */
    private static final double MIN_CONFIDENCE = 0.20;

    private volatile boolean initialized = false;
    private volatile boolean available = false;
    private volatile ZooModel<Image, DetectedObjects> model;

    /** A single recognised object: a label, a 0–1 confidence, and a normalised bounding box. */
    public record Detection(String label, double confidence, double x, double y, double width, double height) {}

    public boolean isAvailable() {
        ensureInitialized();
        return available;
    }

    private synchronized void ensureInitialized() {
        if (initialized) return;
        initialized = true;
        try {
            Criteria.Builder<Image, DetectedObjects> builder = Criteria.builder()
                    .setTypes(Image.class, DetectedObjects.class)
                    .optApplication(Application.CV.OBJECT_DETECTION)
                    .optEngine("PyTorch");
            if (!MODEL_URL.isBlank()) {
                builder.optModelUrls(MODEL_URL);
            }

            log.info("Loading DJL object-detection model (first run downloads PyTorch + model, ~hundreds of MB) ...");
            model = builder.build().loadModel();
            available = true;
            log.info("Image object recognition ready (DJL/PyTorch).");
        } catch (Throwable t) {
            // Throwable: native-library load failures can surface as UnsatisfiedLinkError, not Exception.
            log.warn("Image object recognition not available: {}", t.toString());
        }
    }

    /**
     * Detects objects in the given encoded image (JPEG/PNG/etc.).
     * A fresh {@link Predictor} is created per call — predictors are not thread-safe, the model is.
     *
     * @return detections above {@link #MIN_CONFIDENCE}, highest confidence first
     */
    public List<Detection> detect(byte[] imageBytes) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Object recognition model is not available");
        }
        Image img = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(imageBytes));
        DetectedObjects result;
        try (Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            result = predictor.predict(img);
        }

        List<Detection> detections = new ArrayList<>();
        List<DetectedObjects.DetectedObject> items = result.items();
        for (DetectedObjects.DetectedObject item : items) {
            if (item.getProbability() < MIN_CONFIDENCE) continue;
            BoundingBox box = item.getBoundingBox();
            Rectangle r = box.getBounds();
            detections.add(new Detection(
                    item.getClassName(),
                    item.getProbability(),
                    r.getX(), r.getY(), r.getWidth(), r.getHeight()));
        }
        detections.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
        return detections;
    }
}
