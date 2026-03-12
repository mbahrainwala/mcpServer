package com.behrainwala.mcp.model;

/**
 * Represents a single web search result.
 */
public record SearchResult(String title, String url, String snippet) {

    /**
     * Formats this result as a readable text block.
     */
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(title).append("\n");
        if (!url.isEmpty()) {
            sb.append("URL: ").append(url).append("\n");
        }
        if (!snippet.isEmpty()) {
            sb.append("Snippet: ").append(snippet).append("\n");
        }
        return sb.toString();
    }
}
