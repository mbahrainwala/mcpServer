package com.behrainwala.mcp.service;

import com.behrainwala.mcp.config.McpProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Fetches and extracts readable text content from web pages.
 * Uses Jsoup for HTML parsing and content extraction.
 */
@Service
public class WebContentService {

    private static final Logger log = LoggerFactory.getLogger(WebContentService.class);

    private final McpProperties properties;

    public WebContentService(McpProperties properties) {
        this.properties = properties;
    }

    /**
     * Fetches a URL and returns the main text content with metadata.
     *
     * @param url the URL to fetch
     * @return extracted text content from the page
     */
    public String fetchAndExtract(String url) {
        try {
            log.debug("Fetching content from: {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent(properties.getFetch().getUserAgent())
                    .timeout(properties.getFetch().getTimeout() * 1000)
                    .followRedirects(true)
                    .maxBodySize(5 * 1024 * 1024) // 5MB limit
                    .get();

            // Remove non-content elements
            doc.select("script, style, nav, footer, header, aside, .ad, .ads, "
                    + ".advertisement, .sidebar, .menu, .navigation, [role=navigation], "
                    + "[role=banner], [role=complementary], noscript, iframe").remove();

            String title = doc.title();

            // Try to find the main content area
            String content = extractMainContent(doc);

            // Truncate if too long
            int maxLen = properties.getFetch().getMaxContentLength();
            if (content.length() > maxLen) {
                content = content.substring(0, maxLen) + "\n\n[Content truncated at " + maxLen + " characters]";
            }

            return "Title: " + title + "\n" +
                    "URL: " + url + "\n" +
                    "---\n" +
                    content;

        } catch (Exception e) {
            log.error("Failed to fetch content from '{}': {}", url, e.getMessage());
            return "Error fetching URL '" + url + "': " + e.getMessage();
        }
    }

    /**
     * Attempts to extract the main content from the document by looking for
     * common content containers, falling back to the full body text.
     */
    private String extractMainContent(Document doc) {
        // Try common main-content selectors in order of specificity
        String[] selectors = {
                "article", "main", "[role=main]",
                ".post-content", ".article-content", ".entry-content",
                ".content", "#content", "#main-content"
        };

        for (String selector : selectors) {
            Element mainContent = doc.selectFirst(selector);
            if (mainContent != null && mainContent.text().length() > 200) {
                return cleanText(mainContent.text());
            }
        }

        // Fallback: use the entire body
        Element body = doc.body();
        return body != null ? cleanText(body.text()) : "No content found";
    }

    /**
     * Cleans extracted text by normalizing whitespace.
     */
    private String cleanText(String text) {
        return text
                .replaceAll("\\s{3,}", "\n\n")  // Collapse large whitespace gaps
                .replaceAll("[ \\t]{2,}", " ")    // Normalize spaces
                .strip();
    }
}
