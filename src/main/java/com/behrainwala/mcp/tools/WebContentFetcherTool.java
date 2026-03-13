package com.behrainwala.mcp.tools;

import com.behrainwala.mcp.service.WebContentService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool that fetches and extracts readable content from web pages.
 * Use this after web_search to read the full content of a specific page.
 */
@Service
public class WebContentFetcherTool {

    private final WebContentService contentService;

    public WebContentFetcherTool(WebContentService contentService) {
        this.contentService = contentService;
    }

    @Tool(name = "fetch_webpage", description = "Fetch and extract the main text content from a web page URL. "
            + "Use this tool after web_search to read the full content of a specific search result. "
            + "Returns the page title and cleaned text content with HTML tags, ads, and navigation removed.")
    public String fetch(
            @ToolParam(description = "The full URL of the web page to fetch (must start with http:// or https://)") String url) {

        if (url == null || url.isBlank()) {
            return "Error: URL is required";
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        return contentService.fetchAndExtract(url);
    }
}
