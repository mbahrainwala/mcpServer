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

import static org.bytedeco.leptonica.global.leptonica.*;
import static org.bytedeco.tesseract.global.tesseract.OEM_LSTM_ONLY;
import static org.bytedeco.tesseract.global.tesseract.PSM_AUTO;

/**
 * Tesseract OCR wrapper backed by bytedeco native binaries (no external installation required).
 * <p>
 * On first use, downloads eng.traineddata from tesseract-ocr/tessdata_best (~10 MB) and caches
 * it under ~/.behrainwala-mcp/tessdata-best/. Subsequent calls use the cached file.
 * <p>
 * Uses the LSTM neural-net engine (OEM_LSTM_ONLY) for best accuracy. Each call pre-processes the
 * image: convert to grayscale, then apply an S-curve contrast boost to recover faint text before
 * passing to Tesseract.
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    // tessdata_best gives ~2x better accuracy vs tessdata_fast; worth the extra ~6 MB download.
    private static final String TESSDATA_URL =
            "https://raw.githubusercontent.com/tesseract-ocr/tessdata_best/main/eng.traineddata";
    private static final Path TESSDATA_DIR =
            Path.of(System.getProperty("user.home"), ".behrainwala-mcp", "tessdata-best");

    // S-curve contrast factor passed to pixContrastTRC. 0 = no change, 1 = maximum.
    // 0.5 is a strong boost that pulls near-white faint text away from the background.
    private static final float CONTRAST_FACTOR = 0.5f;

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
                log.info("Downloading Tesseract eng.traineddata (tessdata_best, ~10 MB) to {} ...", TESSDATA_DIR);
                try (InputStream in = URI.create(TESSDATA_URL).toURL().openStream()) {
                    Files.copy(in, trainedData, StandardCopyOption.REPLACE_EXISTING);
                }
                log.info("Downloaded eng.traineddata ({} bytes)", Files.size(trainedData));
            }

            String dataPath = TESSDATA_DIR.toAbsolutePath().toString();
            try (TessBaseAPI probe = new TessBaseAPI()) {
                if (probe.Init(dataPath, "eng", OEM_LSTM_ONLY) != 0) {
                    log.warn("Tesseract Init() returned non-zero — OCR will not be available");
                    return;
                }
                probe.End();
            }

            available = true;
            log.info("Tesseract OCR ready (LSTM engine, tessdata: {})", dataPath);
        } catch (Exception e) {
            log.warn("OCR not available: {}", e.getMessage());
        }
    }

    /**
     * Runs Tesseract OCR on a single page image and returns the extracted text.
     * <p>
     * Pre-processing pipeline applied before OCR:
     *   1. Convert RGB → grayscale (removes color noise, reduces image size)
     *   2. S-curve contrast enhancement (pixContrastTRC) — pulls faint/light-gray text
     *      away from the white background so Tesseract's LSTM can detect it reliably.
     * <p>
     * Each call creates its own TessBaseAPI instance, making this method thread-safe.
     */
    public String ocr(BufferedImage image) throws IOException {
        String dataPath = TESSDATA_DIR.toAbsolutePath().toString();
        TessBaseAPI api = new TessBaseAPI();
        try {
            if (api.Init(dataPath, "eng", OEM_LSTM_ONLY) != 0) {
                throw new IOException("Tesseract Init failed");
            }
            api.SetPageSegMode(PSM_AUTO);
            // Maintain word spacing so columns and tables stay readable
            api.SetVariable("preserve_interword_spaces", "1");

            byte[] pngBytes;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", baos);
                pngBytes = baos.toByteArray();
            }

            // Load image into Leptonica — deallocate the BytePointer AFTER pixReadMem copies
            // the data, not before (the linter previously inverted this order, causing a UAF).
            BytePointer bp = new BytePointer(pngBytes);
            PIX rawPix = pixReadMem(bp, pngBytes.length);
            bp.deallocate();

            // Grayscale conversion — LSTM works well with grayscale; equal channel weights
            PIX pixGray = pixConvertRGBToGray(rawPix, 0.0f, 0.0f, 0.0f);

            // S-curve contrast boost — darkens mid-gray regions (faint text) and brightens
            // near-white regions (paper background), sharpening the separation between them
            PIX pixEnhanced = pixContrastTRC(null, pixGray, CONTRAST_FACTOR);


            try{
                api.SetImage(pixEnhanced);
                BytePointer textPtr = api.GetUTF8Text();
                try {
                    return textPtr != null ? textPtr.getString().strip() : "";
                } finally {
                    if (textPtr != null) textPtr.deallocate();
                }
            } finally {
                if(pixEnhanced!=null)
                    pixEnhanced.close();
                if(pixGray!=null)
                    pixGray.close();
                if(rawPix!=null)
                    rawPix.close();
            }
        } finally {
            api.End();
            api.close();
        }
    }
}
