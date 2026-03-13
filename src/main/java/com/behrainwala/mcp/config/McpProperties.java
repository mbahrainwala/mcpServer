package com.behrainwala.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed configuration properties for the MCP server tools.
 * Bound from the {@code mcp.*} prefix in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    private Search search = new Search();
    private final Fetch fetch = new Fetch();

    public Search getSearch() { return search; }
    public void setSearch(Search search) { this.search = search; }
    public Fetch getFetch() { return fetch; }

    public static class Search {
        private static final int maxResults = 10;
        private static final int timeout = 30;

        public int getMaxResults() { return maxResults; }
        public int getTimeout() { return timeout; }
    }

    public static class Fetch {
        private static final int maxContentLength = 50000;
        private static final int timeout = 30;
        private static final String userAgent = "LMStudio-MCP/1.0 (Web Content Fetcher)";

        public int getMaxContentLength() { return maxContentLength; }
        public int getTimeout() { return timeout; }
        public String getUserAgent() { return userAgent; }
    }
}
