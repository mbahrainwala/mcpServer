package com.behrainwala.mcp.tools;

import com.behrainwala.mcp.model.SearchResult;
import com.behrainwala.mcp.service.WebSearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP tool that provides web search capabilities.
 * When registered with the MCP server, LM Studio models can invoke this tool
 * to search the internet for current information.
 */
@Service
public class WebSearchTool {

    private final WebSearchService searchService;

    public WebSearchTool(WebSearchService searchService) {
        this.searchService = searchService;
    }

    @Tool(name = "web_search", description = "Search the web for current and up-to-date information. "
            + "Use this tool when you need to find recent news, facts, documentation, or any information "
            + "that may not be in your training data. Returns a list of search results with titles, URLs, "
            + "and brief snippets.")
    public String search(
            @ToolParam(description = "The search query. Be specific for better results.") String query,
            @ToolParam(description = "Maximum number of results to return (1-10). Defaults to 5.", required = false) Integer maxResults) {

        int limit = (maxResults != null && maxResults > 0) ? maxResults : 5;
        List<SearchResult> results = searchService.search(query, limit);

        if (results.isEmpty()) {
            return "No results found for: " + query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Search results for: ").append(query).append("\n\n");

        for (int i = 0; i < results.size(); i++) {
            sb.append(i + 1).append(". ").append(results.get(i).toFormattedString()).append("\n");
        }

        return sb.toString();
    }
}
