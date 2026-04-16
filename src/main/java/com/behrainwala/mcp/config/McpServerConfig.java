package com.behrainwala.mcp.config;

import com.behrainwala.mcp.tools.*;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registers all MCP tools with the Spring AI MCP server.
 * Each tool service is converted into ToolCallbackProvider instances that the MCP
 * framework exposes to connected clients (e.g. LM Studio).
 */
@Configuration
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    @Bean
    public ToolCallbackProvider allToolsProvider(
            // Web tools
            WebSearchTool webSearchTool,
            WebContentFetcherTool webContentFetcherTool,
            // Math tools
            MathTool mathTool,
            AdvancedMathTool advancedMathTool,
            CalculusTool calculusTool,
            // Knowledge tools
            DateTimeTool dateTimeTool,
            WikipediaTool wikipediaTool,
            UnitConverterTool unitConverterTool,
            DictionaryTool dictionaryTool,
            // Coding tools
            RegexTool regexTool,
            JsonTool jsonTool,
            EncodingTool encodingTool,
            HttpClientTool httpClientTool,
            CronTool cronTool,
            TextTool textTool,
            // Science tools
            PhysicsTool physicsTool,
            ChemistryTool chemistryTool,
            BiologyTool biologyTool,
            // Excel tools
            ExcelFormulaTool excelFormulaTool,
            ExcelDataTool excelDataTool,
            // Document tools
            PdfTool pdfTool) {

        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        webSearchTool,
                        webContentFetcherTool,
                        mathTool,
                        advancedMathTool,
                        calculusTool,
                        dateTimeTool,
                        wikipediaTool,
                        unitConverterTool,
                        dictionaryTool,
                        regexTool,
                        jsonTool,
                        encodingTool,
                        httpClientTool,
                        cronTool,
                        textTool,
                        physicsTool,
                        chemistryTool,
                        biologyTool,
                        excelFormulaTool,
                        excelDataTool,
                        pdfTool
                )
                .build();
    }

    /**
     * Registers the pdf_to_images tool directly as a SyncToolSpecification so we can
     * return MCP ImageContent (base64 JPEG) instead of plain text. This allows LLMs
     * to use vision to decode image-based / scanned PDFs.
     */
    @Bean
    public List<McpServerFeatures.SyncToolSpecification> pdfImageToolSpecs(PdfTool pdfTool) {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of(
                        "source", Map.of(
                                "type", "string",
                                "description", "Absolute local file path (e.g. 'C:/docs/report.pdf'), "
                                        + "a URL pointing to a PDF, or base64-encoded PDF content (prefixed with 'base64:')."
                        ),
                        "startPage", Map.of(
                                "type", "integer",
                                "description", "First page to render (1-based, inclusive). Omit or set 0 for the beginning."
                        ),
                        "endPage", Map.of(
                                "type", "integer",
                                "description", "Last page to render (1-based, inclusive). Omit or set 0 for the last page. "
                                        + "Max 5 pages per call to limit token usage."
                        ),
                        "dpi", Map.of(
                                "type", "integer",
                                "description", "Render resolution in DPI. Default 150, max 300. "
                                        + "Higher values produce sharper images but larger payloads."
                        ),
                        "textOnly", Map.of(
                                "type", "boolean",
                                "description", "When true, saves rendered images to temporary files on disk and returns "
                                        + "file paths as plain text instead of inline image content. Use this if your LLM "
                                        + "does not support image content blocks in tool results. Default: false."
                        )
                ),
                List.of("source"),  // required
                false,              // additionalProperties
                null,               // defs
                null                // definitions
        );

        McpSchema.Tool tool = new McpSchema.Tool(
                "pdf_to_images",
                "Render PDF pages as images",
                "Render PDF pages as JPEG images and return them as base64-encoded image content. "
                        + "Use this when pdf_to_text returns no extractable text (image-based or scanned PDFs). "
                        + "The LLM can then use vision to read the page contents. "
                        + "Accepts local file paths, URLs, or base64-encoded PDF content. Max 5 pages per call.",
                inputSchema,
                null, // outputSchema
                null, // annotations
                null  // meta
        );

        McpServerFeatures.SyncToolSpecification spec = new McpServerFeatures.SyncToolSpecification(
                tool,
                (exchange, args) -> {
                    try {
                        String source = (String) args.get("source");
                        Integer startPage = args.get("startPage") instanceof Number n ? n.intValue() : null;
                        Integer endPage = args.get("endPage") instanceof Number n ? n.intValue() : null;
                        Integer dpi = args.get("dpi") instanceof Number n ? n.intValue() : null;
                        boolean textOnly = Boolean.TRUE.equals(args.get("textOnly"));

                        if (textOnly) {
                            // Save images to disk and return file paths as text — compatible
                            // with LLMs that do not support ImageContent in tool results.
                            PdfTool.PdfFileResult result = pdfTool.renderPagesToFiles(source, startPage, endPage, dpi);

                            StringBuilder sb = new StringBuilder();
                            sb.append("PDF Image Rendering (files saved to disk)\n");
                            sb.append("──────────────────────────────────────────\n");
                            sb.append("Source : ").append(result.source()).append("\n");
                            sb.append("Pages  : ").append(result.startPage()).append("–").append(result.endPage())
                              .append(" of ").append(result.totalPages()).append("\n");
                            sb.append("Images : ").append(result.filePaths().size()).append(" page(s) rendered as JPEG\n\n");
                            sb.append("Saved files:\n");
                            for (String path : result.filePaths()) {
                                sb.append("  • ").append(path).append("\n");
                            }

                            return new McpSchema.CallToolResult(
                                    List.of(new McpSchema.TextContent(sb.toString())),
                                    false
                            );
                        }

                        // Default: return inline ImageContent blocks
                        PdfTool.PdfImageResult result = pdfTool.renderPagesToBase64Jpeg(source, startPage, endPage, dpi);

                        List<McpSchema.Content> content = new ArrayList<>();

                        // Add a text summary first
                        content.add(new McpSchema.TextContent(
                                "PDF Image Rendering\n"
                                        + "───────────────────\n"
                                        + "Source : " + result.source() + "\n"
                                        + "Pages  : " + result.startPage() + "–" + result.endPage()
                                        + " of " + result.totalPages() + "\n"
                                        + "Images : " + result.base64Images().size() + " page(s) rendered as JPEG\n"
                        ));

                        // Add each page as an ImageContent
                        for (int i = 0; i < result.base64Images().size(); i++) {
                            content.add(new McpSchema.ImageContent(
                                    (McpSchema.Annotations) null,
                                    result.base64Images().get(i),
                                    "image/jpeg"
                            ));
                        }

                        return new McpSchema.CallToolResult(content, false);

                    } catch (Exception e) {
                        log.error("pdf_to_images failed: {}", e.getMessage(), e);
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(
                                        "Error rendering PDF images: " + e.getClass().getSimpleName() + ": " + e.getMessage()
                                )),
                                true
                        );
                    }
                }
        );

        return List.of(spec);
    }
}
