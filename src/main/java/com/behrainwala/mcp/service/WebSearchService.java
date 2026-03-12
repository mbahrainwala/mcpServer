package com.behrainwala.mcp.service;

import com.behrainwala.mcp.config.McpProperties;
import com.behrainwala.mcp.model.SearchResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs web searches using DuckDuckGo's HTML interface.
 * This approach requires no API key and provides real-time search results.
 */
@Service
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);
    private static final String DUCKDUCKGO_HTML_URL = "https://html.duckduckgo.com/html/";

    private final McpProperties properties;

    public WebSearchService(McpProperties properties) {
        this.properties = properties;
    }

    /**
     * Searches the web using DuckDuckGo and returns structured results.
     *
     * @param query      the search query
     * @param maxResults maximum number of results to return (capped by config)
     * @return list of search results with title, URL, and snippet
     */
    public List<SearchResult> search(String query, int maxResults) {
        int limit = Math.min(maxResults, properties.getSearch().getMaxResults());
        List<SearchResult> results = new ArrayList<>();

        try {
            log.debug("Searching DuckDuckGo for: {}", query);

            Document doc = Jsoup.connect(DUCKDUCKGO_HTML_URL)
                    .data("q", query)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(properties.getSearch().getTimeout() * 1000)
                    .post();

            Elements resultElements = doc.select(".result");

            for (Element result : resultElements) {
                if (results.size() >= limit) break;

                Element titleLink = result.selectFirst(".result__a");
                Element snippet = result.selectFirst(".result__snippet");

                if (titleLink != null) {
                    String title = titleLink.text();
                    String url = titleLink.attr("href");
                    String snippetText = snippet != null ? snippet.text() : "";

                    // DuckDuckGo wraps URLs in a redirect — extract the actual URL
                    if (url.contains("uddg=")) {
                        try {
                            String encoded = url.substring(url.indexOf("uddg=") + 5);
                            if (encoded.contains("&")) {
                                encoded = encoded.substring(0, encoded.indexOf("&"));
                            }
                            url = java.net.URLDecoder.decode(encoded, StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            // Keep the original URL if decoding fails
                        }
                    }

                    results.add(new SearchResult(title, url, snippetText));
                }
            }

            log.debug("Found {} results for query: {}", results.size(), query);

        } catch (Exception e) {
            log.error("Search failed for query '{}': {}", query, e.getMessage());
            results.add(new SearchResult(
                    "Search Error",
                    "",
                    "Failed to perform search: " + e.getMessage()
            ));
        }

        return results;
    }
}
