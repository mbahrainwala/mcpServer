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
            + "Returns the page title and cleaned text content with HTML tags, ads, and navigation removed. "
            + "Use maxChars to limit response size and reduce token consumption — "
            + "e.g. 1000 for a quick fact, 5000 for a full article.")
    public String fetch(
            @ToolParam(description = "The full URL of the web page to fetch (must start with http:// or https://)") String url,
            @ToolParam(description = "Maximum characters to return (default 5000, 0 = unlimited up to server max of 50000). "
                    + "Lower values save tokens. Increase if you need the full article.", required = false) Integer maxChars) {

        if (url == null || url.isBlank()) {
            return "Error: URL is required";
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        String content = contentService.fetchAndExtract(url);

        int limit = (maxChars != null && maxChars > 0) ? maxChars : 5000;
        if (content.length() > limit) {
            content = content.substring(0, limit)
                    + "\n\n[Truncated at " + limit + " chars — pass a larger maxChars to read more]";
        }

        return content;
    }
}
