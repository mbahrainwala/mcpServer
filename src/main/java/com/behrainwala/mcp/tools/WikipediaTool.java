package com.behrainwala.mcp.tools;

import com.behrainwala.mcp.config.McpProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * MCP tool for looking up factual information on Wikipedia.
 * Useful for quick reference on people, places, concepts, events, etc.
 */
@Service
public class WikipediaTool {

    private static final Logger log = LoggerFactory.getLogger(WikipediaTool.class);
    private static final String WIKIPEDIA_API = "https://en.wikipedia.org/api/rest_v1/page/summary/";
    private static final String WIKIPEDIA_SEARCH = "https://en.wikipedia.org/w/api.php";

    private final McpProperties properties;

    public WikipediaTool(McpProperties properties) {
        this.properties = properties;
    }

    @Tool(name = "wikipedia_lookup", description = "Look up a topic on Wikipedia and get a concise summary. "
            + "Use this for factual information about people, places, events, concepts, organizations, etc. "
            + "Returns the article title, summary, and a link to the full article.")
    public String lookup(
            @ToolParam(description = "The topic to look up on Wikipedia (e.g. 'Albert Einstein', 'Quantum Computing', 'Tokyo')") String topic) {

        if (topic == null || topic.isBlank()) {
            return "Error: topic is required";
        }

        try {
            // First try direct page summary
            String encoded = URLEncoder.encode(topic.replace(" ", "_"), StandardCharsets.UTF_8);
            String apiUrl = WIKIPEDIA_API + encoded;

            log.debug("Looking up Wikipedia article: {}", topic);

            Document doc = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .timeout(properties.getSearch().getTimeout() * 1000)
                    .get();

            String json = doc.body().text();

            // Parse the JSON response (simple extraction without a JSON library dependency)
            String title = extractJsonField(json, "title");
            String extract = extractJsonField(json, "extract");
            String description = extractJsonField(json, "description");
            String pageUrl = extractJsonField(json, "content_urls", "desktop", "page");

            if (extract != null && !extract.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Wikipedia: ").append(title != null ? title : topic).append("\n");
                if (description != null && !description.isEmpty()) {
                    sb.append("Description: ").append(description).append("\n");
                }
                sb.append("---\n");
                sb.append(extract).append("\n");
                if (pageUrl != null) {
                    sb.append("\nFull article: ").append(pageUrl);
                }
                return sb.toString();
            }

            // If direct lookup fails, try search
            return searchWikipedia(topic);

        } catch (Exception e) {
            log.error("Wikipedia lookup failed for '{}': {}", topic, e.getMessage());
            return searchWikipedia(topic);
        }
    }

    /**
     * Fallback: search Wikipedia and return the best match summary.
     */
    private String searchWikipedia(String query) {
        try {
            String searchUrl = WIKIPEDIA_SEARCH + "?action=opensearch&search="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&limit=3&namespace=0&format=json";

            Document doc = Jsoup.connect(searchUrl)
                    .ignoreContentType(true)
                    .timeout(properties.getSearch().getTimeout() * 1000)
                    .get();

            String json = doc.body().text();

            // OpenSearch returns: ["query", ["title1","title2"], ["desc1","desc2"], ["url1","url2"]]
            // Try to extract the first result title and look it up
            int firstTitleStart = json.indexOf("[\"", json.indexOf("[\"") + 1);
            if (firstTitleStart != -1) {
                int titleStart = firstTitleStart + 2;
                int titleEnd = json.indexOf("\"", titleStart);
                if (titleEnd != -1) {
                    String bestMatch = json.substring(titleStart, titleEnd);
                    // Recursive lookup with the best match
                    String encoded = URLEncoder.encode(bestMatch.replace(" ", "_"), StandardCharsets.UTF_8);
                    Document summaryDoc = Jsoup.connect(WIKIPEDIA_API + encoded)
                            .ignoreContentType(true)
                            .timeout(properties.getSearch().getTimeout() * 1000)
                            .get();
                    String summaryJson = summaryDoc.body().text();
                    String extract = extractJsonField(summaryJson, "extract");
                    String title = extractJsonField(summaryJson, "title");

                    if (extract != null && !extract.isEmpty()) {
                        return "Wikipedia: " + (title != null ? title : bestMatch) + "\n---\n" + extract;
                    }
                }
            }

            return "No Wikipedia article found for: " + query;

        } catch (Exception e) {
            return "Wikipedia search failed: " + e.getMessage();
        }
    }

    /**
     * Simple JSON field extraction without requiring a JSON library.
     * Handles basic string values only.
     */
    private String extractJsonField(String json, String... fieldPath) {
        try {
            String current = json;
            for (String field : fieldPath) {
                int fieldIndex = current.indexOf("\"" + field + "\"");
                if (fieldIndex == -1) return null;
                current = current.substring(fieldIndex + field.length() + 2);
            }

            // Skip colon and whitespace
            int colonIndex = current.indexOf(":");
            if (colonIndex == -1) return null;
            current = current.substring(colonIndex + 1).stripLeading();

            if (current.startsWith("\"")) {
                // String value
                int end = 1;
                while (end < current.length()) {
                    if (current.charAt(end) == '"' && current.charAt(end - 1) != '\\') break;
                    end++;
                }
                return current.substring(1, end)
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\\\", "\\");
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
