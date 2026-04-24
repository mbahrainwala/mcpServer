package com.behrainwala.mcp.tools;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.bytedeco.leptonica.global.leptonica.pixReadMem;
import static org.bytedeco.tesseract.global.tesseract.PSM_AUTO;

/**
 * Tesseract OCR wrapper backed by bytedeco native binaries (no external installation required).
 * On first use, downloads eng.traineddata (~4 MB) from tesseract-ocr/tessdata_fast and caches
 * it under ~/.behrainwala-mcp/tessdata/. Subsequent calls use the cached file.
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private static final String TESSDATA_URL =
            "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/eng.traineddata";
    private static final Path TESSDATA_DIR =
            Path.of(System.getProperty("user.home"), ".behrainwala-mcp", "tessdata");

    private volatile boolean initialized = false;
    private volatile boolean available = false;

    public boolean isAvailable() {
        ensureInitialized();
        return available;
    }

    private synchronized void ensureInitialized() {
        if (initialized) return;
        initialized = true;
        try {
            Files.createDirectories(TESSDATA_DIR);
            Path trainedData = TESSDATA_DIR.resolve("eng.traineddata");
            if (!Files.exists(trainedData)) {
                log.info("Downloading Tesseract eng.traineddata (~4 MB) to {} ...", TESSDATA_DIR);
                try (InputStream in = URI.create(TESSDATA_URL).toURL().openStream()) {
                    Files.copy(in, trainedData, StandardCopyOption.REPLACE_EXISTING);
                }
                log.info("Downloaded eng.traineddata ({} bytes)", Files.size(trainedData));
            }

            String dataPath = TESSDATA_DIR.toAbsolutePath().toString();

            try(TessBaseAPI probe = new TessBaseAPI()) {
                if (probe.Init(dataPath, "eng") != 0) {
                    log.warn("Tesseract Init() returned non-zero — OCR will not be available");
                    return;
                }
                probe.End();
            }

            available = true;
            log.info("Tesseract OCR ready (tessdata: {})", dataPath);
        } catch (Exception e) {
            log.warn("OCR not available: {}", e.getMessage());
        }
    }

    /**
     * Runs Tesseract OCR on a single page image and returns the extracted text.
     * Each call creates its own TessBaseAPI instance so this method is thread-safe.
     */
    public String ocr(BufferedImage image) throws IOException {
        String dataPath = TESSDATA_DIR.toAbsolutePath().toString();
        TessBaseAPI api = new TessBaseAPI();
        try {
            if (api.Init(dataPath, "eng") != 0) {
                throw new IOException("Tesseract Init failed");
            }
            api.SetPageSegMode(PSM_AUTO);

            byte[] pngBytes;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", baos);
                pngBytes = baos.toByteArray();
            }

            BytePointer bp = new BytePointer(pngBytes);
            bp.deallocate();

            try(PIX pix = pixReadMem(bp, pngBytes.length)) {
                api.SetImage(pix);
                BytePointer textPtr = api.GetUTF8Text();
                try {
                    return textPtr != null ? textPtr.getString().strip() : "";
                } finally {
                    if (textPtr != null) textPtr.deallocate();
                }
            }
        } finally {
            api.End();
            api.close();
        }
    }
}
