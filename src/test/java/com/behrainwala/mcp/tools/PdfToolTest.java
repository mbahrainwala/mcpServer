package com.behrainwala.mcp.tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

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
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void pdfToText_blankSource_returnsError() {
        String result = tool.pdfToText("", null, null);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void pdfToText_validPdf_extractsText() throws Exception {
        File pdfFile = createTestPdf("Hello PDF World");
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), null, null);
        assertThat(result).contains("Hello PDF World");
    }

    @Test
    void pdfToText_pageRangeOutOfBounds_returnsError() throws Exception {
        File pdfFile = createTestPdf("Test content");
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), 99, 100);
        assertThat(result).containsIgnoringCase("error").containsIgnoringCase("page");
    }

    @Test
    void pdfToText_startGreaterThanEnd_returnsError() throws Exception {
        File pdfFile = createTestPdf("Test content");
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), 5, 2);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void pdfToText_header_includesSource() throws Exception {
        File pdfFile = createTestPdf("Some text");
        String result = tool.pdfToText(pdfFile.getAbsolutePath(), null, null);
        assertThat(result).contains("PDF Text Extraction").contains(pdfFile.getAbsolutePath());
    }

    // ── pdfMetadata ──────────────────────────────────────────────────────────

    @Test
    void pdfMetadata_invalidPath_returnsError() {
        String result = tool.pdfMetadata("/does/not/exist/file.pdf");
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void pdfMetadata_blankSource_returnsError() {
        String result = tool.pdfMetadata("  ");
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void pdfMetadata_validPdf_returnsStructure() throws Exception {
        File pdfFile = createTestPdf("Metadata test");
        String result = tool.pdfMetadata(pdfFile.getAbsolutePath());
        assertThat(result).contains("PDF Metadata")
                .containsIgnoringCase("Pages")
                .containsIgnoringCase("Encrypted");
    }

    @Test
    void pdfMetadata_singlePage_showsPageSize() throws Exception {
        File pdfFile = createTestPdf("Size test");
        String result = tool.pdfMetadata(pdfFile.getAbsolutePath());
        assertThat(result).containsIgnoringCase("Page Size");
    }

    @Test
    void pdfMetadata_localFile_showsFileSize() throws Exception {
        File pdfFile = createTestPdf("File size check");
        String result = tool.pdfMetadata(pdfFile.getAbsolutePath());
        assertThat(result).containsIgnoringCase("File Size");
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private File createTestPdf(String text) throws Exception {
        File pdfFile = tempDir.resolve("test.pdf").toFile();
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
}
