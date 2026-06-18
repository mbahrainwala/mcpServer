package com.behrainwala.mcp.config;

import com.behrainwala.mcp.tools.*;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 100% line, branch, and statement coverage for McpServerConfig.
 *
 * Key design decision: methods are called DIRECTLY on a plain {@code new McpServerConfig()}
 * instance, not through the Spring CGLIB proxy. Spring's @Configuration enhancement wraps
 * @Bean methods in a proxy subclass that JaCoCo does not instrument, so @SpringBootTest
 * does not produce coverage for this class.
 *
 * Branches in pdfImageToolSpecs lambda (12 total):
 *   1–2.  startPage instanceof Number   → true / false
 *   3–4.  endPage   instanceof Number   → true / false
 *   5–6.  dpi       instanceof Number   → true / false
 *   7–8.  textOnly == true              → true / false
 *   9–10. for-each over filePaths       → iterate / exit
 *  11–12. for-each over base64Images    → iterate / exit
 */
class McpServerConfigTest {

    private McpServerConfig config;

    @BeforeEach
    void setUp() {
        // Direct instantiation bypasses the CGLIB proxy Spring would normally create
        config = new McpServerConfig();
    }

    // ── allToolsProvider ──────────────────────────────────────────────────────

    @Test
    void allToolsProvider_withMockedDependencies_returnsNonNullProvider() {
        // Spring AI's MethodToolCallbackProvider.builder() walks the mock's class
        // hierarchy via AnnotationUtils, finding @Tool annotations on the real class.
        ToolCallbackProvider provider = config.allToolsProvider(
                mock(WebSearchTool.class),
                mock(WebContentFetcherTool.class),
                mock(WebResearchTool.class),
                mock(MathTool.class),
                mock(AdvancedMathTool.class),
                mock(CalculusTool.class),
                mock(DateTimeTool.class),
                mock(WikipediaTool.class),
                mock(UnitConverterTool.class),
                mock(DictionaryTool.class),
                mock(RegexTool.class),
                mock(JsonTool.class),
                mock(EncodingTool.class),
                mock(HttpClientTool.class),
                mock(CronTool.class),
                mock(TextTool.class),
                mock(PhysicsTool.class),
                mock(ChemistryTool.class),
                mock(BiologyTool.class),
                mock(FluidDynamicsTool.class),
                mock(AerospaceTool.class),
                mock(ExcelFormulaTool.class),
                mock(ExcelDataTool.class),
                mock(PdfTool.class),
                mock(ImageTool.class),
                mock(DocumentTool.class),
                mock(ExcelTool.class),
                mock(CsvTool.class),
                mock(NetworkTool.class),
                mock(ColorTool.class),
                mock(DataGeneratorTool.class),
                mock(NumberBaseTool.class),
                mock(ProbabilityTool.class),
                mock(AstronomyTool.class),
                mock(SqlHelperTool.class),
                mock(FinanceTool.class),
                mock(HealthTool.class),
                mock(StockTool.class),
                mock(AiTextTool.class),
                mock(EmbeddingTool.class),
                mock(MarkdownTool.class),
                mock(PromptTool.class),
                mock(FileSystemTool.class)
        );

        assertThat(provider).isNotNull();
    }

    // ── pdfImageToolSpecs helper ──────────────────────────────────────────────

    /**
     * Calls pdfImageToolSpecs, extracts the single spec, and invokes its handler
     * with the supplied args map. Returns the CallToolResult.
     */
    private McpSchema.CallToolResult invoke(PdfTool pdfTool, Map<String, Object> args) {
        List<McpServerFeatures.SyncToolSpecification> specs = config.pdfImageToolSpecs(pdfTool);
        assertThat(specs).hasSize(1);
        // SyncToolSpecification is a record; spec.call() returns the BiFunction handler.
        // The exchange parameter is not used by the lambda, so null is safe.
        return specs.get(0).call().apply(null, args);
    }

    // ── Branch 7 (textOnly=false) + branches 1,3,5 (all args are Number) ─────
    //    Also exercises the base64Images for-each loop (branches 11–12)

    @Test
    void lambda_textOnlyFalse_numericArgs_returnsImageContent() throws Exception {
        PdfTool pdfTool = mock(PdfTool.class);

        // Real record instances — cleaner than mocking and avoids stubbing accessors
        PdfTool.PdfImageResult imgResult = new PdfTool.PdfImageResult(
                List.of("base64EncodedImageData=="), 1, 1, 1, "test.pdf");
        when(pdfTool.renderPagesToBase64Jpeg(any(), any(), any(), any()))
                .thenReturn(imgResult);

        Map<String, Object> args = new HashMap<>();
        args.put("source",    "test.pdf");
        args.put("startPage", 1);        // instanceof Number → true  (branch 1)
        args.put("endPage",   1);        // instanceof Number → true  (branch 3)
        args.put("dpi",       150);      // instanceof Number → true  (branch 5)
        args.put("textOnly",  false);    // textOnly = false           (branch 8)

        McpSchema.CallToolResult result = invoke(pdfTool, args);

        assertThat(result.isError()).isFalse();
        // First content element is a TextContent summary; remaining are ImageContent
        List<McpSchema.Content> content = result.content();
        assertThat(content).hasSizeGreaterThanOrEqualTo(2);
        assertThat(content.get(0)).isInstanceOf(McpSchema.TextContent.class);
        assertThat(content.get(1)).isInstanceOf(McpSchema.ImageContent.class);
        String summary = ((McpSchema.TextContent) content.get(0)).text();
        assertThat(summary)
                .contains("PDF Image Rendering")
                .contains("test.pdf");
    }

    // ── Branch 8 (textOnly=true) + branches 2,4,6 (all args are null) ────────
    //    Also exercises the filePaths for-each loop (branches 9–10)

    @Test
    void lambda_textOnlyTrue_nullArgs_returnsFilePathText() throws Exception {
        PdfTool pdfTool = mock(PdfTool.class);

        PdfTool.PdfFileResult fileResult = new PdfTool.PdfFileResult(
                List.of("/tmp/page1.jpg", "/tmp/page2.jpg"), 1, 2, 2, "scan.pdf");
        when(pdfTool.renderPagesToFiles(any(), any(), any(), any()))
                .thenReturn(fileResult);

        Map<String, Object> args = new HashMap<>();
        args.put("source",    "scan.pdf");
        args.put("startPage", null);      // instanceof Number → false (branch 2)
        args.put("endPage",   null);      // instanceof Number → false (branch 4)
        args.put("dpi",       null);      // instanceof Number → false (branch 6)
        args.put("textOnly",  Boolean.TRUE); // textOnly = true         (branch 7)

        McpSchema.CallToolResult result = invoke(pdfTool, args);

        assertThat(result.isError()).isFalse();
        List<McpSchema.Content> content = result.content();
        assertThat(content).hasSize(1);
        String text = ((McpSchema.TextContent) content.get(0)).text();
        assertThat(text)
                .contains("PDF Image Rendering (files saved to disk)")
                .contains("scan.pdf")
                .contains("/tmp/page1.jpg")
                .contains("/tmp/page2.jpg")
                .contains("2 page(s)");
    }

    // ── Exception path — outer catch block ────────────────────────────────────

    @Test
    void lambda_pdfToolThrowsException_returnsErrorResult() throws Exception {
        PdfTool pdfTool = mock(PdfTool.class);
        when(pdfTool.renderPagesToBase64Jpeg(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Render failed"));

        Map<String, Object> args = new HashMap<>();
        args.put("source",    "broken.pdf");
        args.put("startPage", 1);
        args.put("endPage",   1);
        args.put("dpi",       150);
        args.put("textOnly",  false);

        McpSchema.CallToolResult result = invoke(pdfTool, args);

        assertThat(result.isError()).isTrue();
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertThat(text)
                .contains("Error rendering PDF images")
                .contains("RuntimeException")
                .contains("Render failed");
    }

    // ── pdfImageToolSpecs returns a spec with the correct tool name ────────────

    @Test
    void pdfImageToolSpecs_specHasCorrectToolName() {
        PdfTool pdfTool = mock(PdfTool.class);

        List<McpServerFeatures.SyncToolSpecification> specs = config.pdfImageToolSpecs(pdfTool);

        assertThat(specs).hasSize(1);
        assertThat(specs.get(0).tool().name()).isEqualTo("pdf_to_images");
    }
}
