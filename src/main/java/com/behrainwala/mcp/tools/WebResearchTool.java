package com.behrainwala.mcp.tools;

import com.behrainwala.mcp.model.SearchResult;
import com.behrainwala.mcp.service.WebContentService;
import com.behrainwala.mcp.service.WebSearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Combines web_search + fetch_webpage × N into a single tool call.
 * Replaces a 3–6 step chain and caps content size automatically to reduce token consumption.
 */
@Service
public class WebResearchTool {

    private static final int DEFAULT_SOURCES = 2;
    private static final int MAX_SOURCES = 5;
    private static final int DEFAULT_CHARS_PER_SOURCE = 2000;
    private static final int MAX_CHARS_PER_SOURCE = 10000;

    private final WebSearchService searchService;
    private final WebContentService contentService;

    public WebResearchTool(WebSearchService searchService, WebContentService contentService) {
        this.searchService = searchService;
        this.contentService = contentService;
    }

    @Tool(name = "web_research", description = "Research a topic with a single call: searches the web, "
            + "fetches the top pages, and returns condensed content with source attribution. "
            + "Use this instead of calling web_search + fetch_webpage separately — it saves multiple "
            + "tool round-trips and automatically limits content size to reduce token consumption. "
            + "Best for: fact-finding, current events, documentation look-ups, how-to questions.")
    public String research(
            @ToolParam(description = "The research question or search query") String query,
            @ToolParam(description = "Number of sources to fetch and include (1–5, default 2). "
                    + "Use 1 for a quick fact, 3–5 for comprehensive research.", required = false) Integer maxSources,
            @ToolParam(description = "Maximum characters to include from each source (default 2000, max 10000). "
                    + "Increase to 5000+ if you need full article text.", required = false) Integer maxCharsPerSource) {

        if (query == null || query.isBlank()) return "Error: query is required";

        int sources = clamp(maxSources, 1, MAX_SOURCES, DEFAULT_SOURCES);
        int charsPerSource = clamp(maxCharsPerSource, 100, MAX_CHARS_PER_SOURCE, DEFAULT_CHARS_PER_SOURCE);

        List<SearchResult> results = searchService.search(query, sources);
        if (results.isEmpty()) {
            return "No search results found for: " + query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Research: ").append(query).append("\n");
        sb.append("Sources fetched: ").append(Math.min(results.size(), sources)).append("\n\n");

        int fetched = 0;
        StringBuilder sourceList = new StringBuilder("\nSOURCES:\n");

        for (SearchResult result : results) {
            if (fetched >= sources) break;
            String url = result.url();
            if (url == null || url.isBlank() || url.startsWith("Search Error")) continue;

            fetched++;
            sb.append("─".repeat(60)).append("\n");
            sb.append("Source ").append(fetched).append(": ").append(result.title()).append("\n");
            sb.append("URL: ").append(url).append("\n");
            sb.append("─".repeat(60)).append("\n");

            if (!result.snippet().isBlank()) {
                sb.append("Summary: ").append(result.snippet()).append("\n\n");
            }

            try {
                String pageContent = contentService.fetchAndExtract(url);
                int contentStart = pageContent.indexOf("---\n");
                String body = contentStart >= 0 ? pageContent.substring(contentStart + 4) : pageContent;

                if (body.length() > charsPerSource) {
                    body = body.substring(0, charsPerSource)
                            + "\n[… truncated — use fetch_webpage with maxChars=" + (charsPerSource * 2) + " for more]";
                }
                sb.append(body).append("\n\n");
            } catch (Exception e) {
                sb.append("[Could not fetch page: ").append(e.getMessage()).append("]\n\n");
            }

            sourceList.append("[").append(fetched).append("] ").append(url).append("\n");
        }

        sb.append(sourceList);
        return sb.toString();
    }

    private int clamp(Integer value, int min, int max, int defaultVal) {
        if (value == null) return defaultVal;
        return Math.max(min, Math.min(max, value));
    }
}
