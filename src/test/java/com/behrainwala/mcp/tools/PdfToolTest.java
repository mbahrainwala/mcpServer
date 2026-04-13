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

import java.io.File;
import java.nio.file.Path;
import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(result).contains("No extractable text found");
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
