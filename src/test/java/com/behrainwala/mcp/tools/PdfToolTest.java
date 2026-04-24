package com.behrainwala.mcp.tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PdfToolTest {

    private PdfTool tool;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        tool = new PdfTool();
    }

    // ── pdfToText ────────────────────────────────────────────────────────────

    @Test
    void pdfToText_invalidPath_returnsError() {
        String result = tool.pdfToText("/does/not/exist/file.pdf", null, null);
        assertThat(result).startsWith("Error extracting PDF text:");
    }

    @Test
    void pdfToText_blankSource_returnsError() {
        String result = tool.pdfToText("", null, null);
        assertThat(result).contains("Error");
    }

    @Test
    void pdfToText_nullSource_returnsError() {
        String result = tool.pdfToText(null, null, null);
        assertThat(result).contains("Error");
    }

    @Test
    void pdfToText_whitespaceSource_returnsError() {
        String result = tool.pdfToText("   ", null, null);
        assertThat(result).contains("Error");
    }

    @Test
    void pdfToText_validPdf_extractsText() throws Exception {
        File pdfFile = createTestPdf("Hello PDF World");
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), null, null);
        assertThat(result)
                .contains("Hello PDF World")
                .contains("PDF Text Extraction")
                .contains("Source :")
                .contains("Pages  : 1");
    }

    @Test
    void pdfToText_withPageRange_startsAt1() throws Exception {
        File pdfFile = createMultiPagePdf("Page1", "Page2", "Page3");
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), 1, 1);
        assertThat(result).contains("Page1").doesNotContain("Page2");
    }

    @Test
    void pdfToText_withPageRange_endPage() throws Exception {
        File pdfFile = createMultiPagePdf("Page1", "Page2", "Page3");
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), 2, 3);
        assertThat(result).contains("Page2").contains("Page3");
    }

    @Test
    void pdfToText_startPage0_treatedAs1() throws Exception {
        File pdfFile = createTestPdf("Content");
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), 0, null);
        assertThat(result).contains("Content");
    }

    @Test
    void pdfToText_endPage0_treatedAsLast() throws Exception {
        File pdfFile = createTestPdf("Content");
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), null, 0);
        assertThat(result).contains("Content");
    }

    @Test
    void pdfToText_endPageExceedsTotalPages_capped() throws Exception {
        File pdfFile = createTestPdf("Content");
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), 1, 100);
        assertThat(result).contains("Content");
    }

    @Test
    void pdfToText_startPageExceedsTotalPages_returnsError() throws Exception {
        File pdfFile = createTestPdf("Content");
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), 99, 100);
        assertThat(result).contains("Error: startPage (99) exceeds total pages");
    }

    @Test
    void pdfToText_startGreaterThanEnd_returnsError() throws Exception {
        File pdfFile = createMultiPagePdf("Page1", "Page2", "Page3");
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), 3, 1);
        assertThat(result).contains("Error: startPage (3) is greater than endPage (1)");
    }

    @Test
    void pdfToText_blankPdf_showsNoExtractableText() throws Exception {
        File pdfFile = createBlankPdf();
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), null, null);
        assertThat(result).contains("No extractable text found")
                .contains("pdf_to_images");
    }

    @Test
    void pdfToText_headerIncludesSourcePath() throws Exception {
        File pdfFile = createTestPdf("Some text");
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), null, null);
        assertThat(result).contains(pdfFile.getAbsolutePath());
    }

    // ── pdfMetadata ──────────────────────────────────────────────────────────

    @Test
    void pdfMetadata_invalidPath_returnsError() {
        String result = tool.pdfMetadata("/does/not/exist/file.pdf");
        assertThat(result).startsWith("Error reading PDF metadata:");
    }

    @Test
    void pdfMetadata_blankSource_returnsError() {
        String result = tool.pdfMetadata("  ");
        assertThat(result).contains("Error");
    }

    @Test
    void pdfMetadata_nullSource_returnsError() {
        String result = tool.pdfMetadata(null);
        assertThat(result).contains("Error");
    }

    @Test
    void pdfMetadata_validPdf_returnsStructure() throws Exception {
        File pdfFile = createTestPdf("Metadata test");
        String result = tool.pdfMetadata(pdfFile.getAbsolutePath());
        assertThat(result)
                .contains("PDF Metadata")
                .contains("Pages")
                .contains("Encrypted")
                .contains("PDF Version");
    }

    @Test
    void pdfMetadata_showsPageSize() throws Exception {
        File pdfFile = createTestPdf("Size test");
        String result = tool.pdfMetadata(pdfFile.getAbsolutePath());
        assertThat(result).contains("Page Size (p.1)");
    }

    @Test
    void pdfMetadata_showsRotation() throws Exception {
        File pdfFile = createTestPdf("Rotation test");
        String result = tool.pdfMetadata(pdfFile.getAbsolutePath());
        assertThat(result).contains("Rotation (p.1)");
    }

    @Test
    void pdfMetadata_localFile_showsFileSize() throws Exception {
        File pdfFile = createTestPdf("File size check");
        String result = tool.pdfMetadata(pdfFile.getAbsolutePath());
        assertThat(result).contains("File Size");
    }

    @Test
    void pdfMetadata_withMetadataSet_showsTitle() throws Exception {
        File pdfFile = createPdfWithMetadata();
        String result = tool.pdfMetadata(pdfFile.getAbsolutePath());
        assertThat(result).contains("Test Title").contains("Test Author");
    }

    @Test
    void pdfMetadata_source_included() throws Exception {
        File pdfFile = createTestPdf("Test");
        String result = tool.pdfMetadata(pdfFile.getAbsolutePath());
        assertThat(result).contains("Source           : " + pdfFile.getAbsolutePath());
    }

    // ── Helper: formatSize coverage ──────────────────────────────────────────

    @Test
    void pdfMetadata_smallFile_showsBytes() throws Exception {
        // Small PDFs will be in KB range; but we test the method works
        File pdfFile = createTestPdf("x");
        String result = tool.pdfMetadata(pdfFile.getAbsolutePath());
        assertThat(result).containsPattern("File Size\\s+: \\d+");
    }

    // ── renderPagesToBase64Jpeg ────────────────────────────────────────────

    @Test
    void renderPagesToBase64Jpeg_validPdf_returnsBase64Images() throws Exception {
        File pdfFile = createTestPdf("Image render test");
        PdfTool.PdfImageResult result = tool.renderPagesToBase64Jpeg(pdfFile.getAbsolutePath(), null, null, null);

        assertThat(result.base64Images()).hasSize(1);
        assertThat(result.startPage()).isEqualTo(1);
        assertThat(result.endPage()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);

        // Verify the base64 data decodes to a valid JPEG
        byte[] imageBytes = Base64.getDecoder().decode(result.base64Images().getFirst());
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isGreaterThan(0);
        assertThat(img.getHeight()).isGreaterThan(0);
    }

    @Test
    void renderPagesToBase64Jpeg_multiPage_returnsAllPages() throws Exception {
        File pdfFile = createMultiPagePdf("A", "B", "C");
        PdfTool.PdfImageResult result = tool.renderPagesToBase64Jpeg(pdfFile.getAbsolutePath(), null, null, null);

        assertThat(result.base64Images()).hasSize(3);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void renderPagesToBase64Jpeg_pageRange_rendersSubset() throws Exception {
        File pdfFile = createMultiPagePdf("A", "B", "C");
        PdfTool.PdfImageResult result = tool.renderPagesToBase64Jpeg(pdfFile.getAbsolutePath(), 2, 3, null);

        assertThat(result.base64Images()).hasSize(2);
        assertThat(result.startPage()).isEqualTo(2);
        assertThat(result.endPage()).isEqualTo(3);
    }

    @Test
    void renderPagesToBase64Jpeg_capsAtMaxPages() throws Exception {
        // Create a 7-page PDF — max is 5 pages
        File pdfFile = createMultiPagePdf("A", "B", "C", "D", "E", "F", "G");
        PdfTool.PdfImageResult result = tool.renderPagesToBase64Jpeg(pdfFile.getAbsolutePath(), null, null, null);

        assertThat(result.base64Images()).hasSize(5);
        assertThat(result.endPage()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(7);
    }

    @Test
    void renderPagesToBase64Jpeg_customDpi_producesLargerImage() throws Exception {
        File pdfFile = createTestPdf("DPI test");

        PdfTool.PdfImageResult lowDpi = tool.renderPagesToBase64Jpeg(pdfFile.getAbsolutePath(), null, null, 72);
        PdfTool.PdfImageResult highDpi = tool.renderPagesToBase64Jpeg(pdfFile.getAbsolutePath(), null, null, 200);

        // Higher DPI should produce a larger base64 string (more pixels)
        assertThat(highDpi.base64Images().getFirst().length())
                .isGreaterThan(lowDpi.base64Images().getFirst().length());
    }

    @Test
    void renderPagesToBase64Jpeg_dpiCappedAt300() throws Exception {
        File pdfFile = createTestPdf("Max DPI test");

        PdfTool.PdfImageResult at300 = tool.renderPagesToBase64Jpeg(pdfFile.getAbsolutePath(), null, null, 300);
        PdfTool.PdfImageResult at999 = tool.renderPagesToBase64Jpeg(pdfFile.getAbsolutePath(), null, null, 999);

        // Both should produce the same size since 999 is capped to 300
        assertThat(at999.base64Images().getFirst().length())
                .isEqualTo(at300.base64Images().getFirst().length());
    }

    @Test
    void renderPagesToBase64Jpeg_blankSource_throws() {
        assertThatThrownBy(() -> tool.renderPagesToBase64Jpeg("", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void renderPagesToBase64Jpeg_nullSource_throws() {
        assertThatThrownBy(() -> tool.renderPagesToBase64Jpeg(null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void renderPagesToBase64Jpeg_startPageExceedsTotal_throws() throws Exception {
        File pdfFile = createTestPdf("Single page");
        assertThatThrownBy(() -> tool.renderPagesToBase64Jpeg(pdfFile.getAbsolutePath(), 99, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds total pages");
    }

    @Test
    void renderPagesToBase64Jpeg_startGreaterThanEnd_throws() throws Exception {
        File pdfFile = createMultiPagePdf("A", "B", "C");
        assertThatThrownBy(() -> tool.renderPagesToBase64Jpeg(pdfFile.getAbsolutePath(), 3, 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than endPage");
    }

    // ── renderPagesToFiles ─────────────────────────────────────────────────

    @Test
    void renderPagesToFiles_validPdf_savesJpegFiles() throws Exception {
        File pdfFile = createTestPdf("File render test");
        PdfTool.PdfFileResult result = tool.renderPagesToFiles(pdfFile.getAbsolutePath(), null, null, null);

        assertThat(result.filePaths()).hasSize(1);
        assertThat(result.startPage()).isEqualTo(1);
        assertThat(result.endPage()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);

        // Verify the file exists and is a valid JPEG
        Path savedFile = Path.of(result.filePaths().getFirst());
        assertThat(Files.exists(savedFile)).isTrue();
        assertThat(savedFile.getFileName().toString()).isEqualTo("page_1.jpg");

        // Verify the parent directory name contains the PDF filename for identification
        String parentDirName = savedFile.getParent().getFileName().toString();
        assertThat(parentDirName).startsWith("pdf_images_test_");

        BufferedImage img = ImageIO.read(savedFile.toFile());
        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isGreaterThan(0);
    }

    @Test
    void renderPagesToFiles_multiPage_savesAllFiles() throws Exception {
        File pdfFile = createMultiPagePdf("A", "B", "C");
        PdfTool.PdfFileResult result = tool.renderPagesToFiles(pdfFile.getAbsolutePath(), null, null, null);

        assertThat(result.filePaths()).hasSize(3);
        for (String path : result.filePaths()) {
            assertThat(Files.exists(Path.of(path))).isTrue();
        }
    }

    @Test
    void renderPagesToFiles_pageRange_savesSubset() throws Exception {
        File pdfFile = createMultiPagePdf("A", "B", "C");
        PdfTool.PdfFileResult result = tool.renderPagesToFiles(pdfFile.getAbsolutePath(), 2, 3, null);

        assertThat(result.filePaths()).hasSize(2);
        assertThat(result.filePaths().get(0)).contains("page_2.jpg");
        assertThat(result.filePaths().get(1)).contains("page_3.jpg");
    }

    @Test
    void renderPagesToFiles_blankSource_throws() {
        assertThatThrownBy(() -> tool.renderPagesToFiles("", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renderPagesToBase64Jpeg_blankPdf_rendersImage() throws Exception {
        // Even a blank PDF (no text) should render as a valid image
        File pdfFile = createBlankPdf();
        PdfTool.PdfImageResult result = tool.renderPagesToBase64Jpeg(pdfFile.getAbsolutePath(), null, null, null);

        assertThat(result.base64Images()).hasSize(1);
        byte[] imageBytes = Base64.getDecoder().decode(result.base64Images().getFirst());
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        assertThat(img).isNotNull();
    }

    // ── OCR fallback ─────────────────────────────────────────────────────────

    @Test
    void pdfToText_blankPdfWithOcrAvailable_returnsOcrText() throws Exception {
        OcrService mockOcr = mock(OcrService.class);
        when(mockOcr.isAvailable()).thenReturn(true);
        when(mockOcr.ocr(any(BufferedImage.class))).thenReturn("Scanned text from OCR");
        tool.setOcrService(mockOcr);

        File pdfFile = createBlankPdf();
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), null, null);

        assertThat(result).contains("Scanned text from OCR");
        assertThat(result).contains("[OCR via Tesseract");
        verify(mockOcr).ocr(any(BufferedImage.class));
    }

    @Test
    void pdfToText_blankPdfOcrReturnsBlank_showsNoTextMessage() throws Exception {
        OcrService mockOcr = mock(OcrService.class);
        when(mockOcr.isAvailable()).thenReturn(true);
        when(mockOcr.ocr(any(BufferedImage.class))).thenReturn("");
        tool.setOcrService(mockOcr);

        File pdfFile = createBlankPdf();
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), null, null);

        assertThat(result).contains("No extractable text found");
        assertThat(result).contains("OCR was attempted");
    }

    @Test
    void pdfToText_ocrNotAvailable_showsFallbackMessage() throws Exception {
        OcrService mockOcr = mock(OcrService.class);
        when(mockOcr.isAvailable()).thenReturn(false);
        tool.setOcrService(mockOcr);

        File pdfFile = createBlankPdf();
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), null, null);

        assertThat(result).contains("No extractable text found");
        assertThat(result).contains("pdf_to_images");
        verify(mockOcr, never()).ocr(any());
    }

    @Test
    void pdfToText_textPdfWithOcrAvailable_doesNotCallOcr() throws Exception {
        OcrService mockOcr = mock(OcrService.class);
        when(mockOcr.isAvailable()).thenReturn(true);
        tool.setOcrService(mockOcr);

        File pdfFile = createTestPdf("Already has text");
        tool.pdfToText(pdfFile.getAbsolutePath(), null, null);

        verify(mockOcr, never()).ocr(any());
    }

    @Test
    void pdfToText_ocrServiceNull_showsFallbackMessage() throws Exception {
        // default: ocrService is null (no Spring context in unit tests)
        File pdfFile = createBlankPdf();
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), null, null);

        assertThat(result).contains("No extractable text found");
        assertThat(result).contains("pdf_to_images");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private File createTestPdf(String text) throws Exception {
        File pdfFile = tempDir.resolve("test_" + System.nanoTime() + ".pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            doc.save(pdfFile);
        }
        return pdfFile;
    }

    private File createMultiPagePdf(String... pageTexts) throws Exception {
        File pdfFile = tempDir.resolve("multi_" + System.nanoTime() + ".pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            for (String text : pageTexts) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText(text);
                    cs.endText();
                }
            }
            doc.save(pdfFile);
        }
        return pdfFile;
    }

    private File createBlankPdf() throws Exception {
        File pdfFile = tempDir.resolve("blank_" + System.nanoTime() + ".pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(pdfFile);
        }
        return pdfFile;
    }

    private File createPdfWithMetadata() throws Exception {
        File pdfFile = tempDir.resolve("meta_" + System.nanoTime() + ".pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            PDDocumentInformation info = doc.getDocumentInformation();
            info.setTitle("Test Title");
            info.setAuthor("Test Author");
            info.setSubject("Test Subject");
            info.setKeywords("test, keywords");
            info.setCreator("Test Creator");
            info.setProducer("Test Producer");
            info.setCreationDate(Calendar.getInstance());
            info.setModificationDate(Calendar.getInstance());

            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("PDF with metadata");
                cs.endText();
            }
            doc.save(pdfFile);
        }
        return pdfFile;
    }
}
