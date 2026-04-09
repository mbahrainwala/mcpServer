package com.behrainwala.mcp.config;

import com.behrainwala.mcp.tools.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers all MCP tools with the Spring AI MCP server.
 * Each tool service is converted into ToolCallbackProvider instances that the MCP
 * framework exposes to connected clients (e.g. LM Studio).
 */
@Configuration
public class McpServerConfig {

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
}
