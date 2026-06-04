package com.behrainwala.mcp.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 100% line, branch, and statement coverage for McpProperties and its inner classes.
 * No branches exist in this class — every method is a straight-line getter or setter.
 */
class McpPropertiesTest {

    // ── McpProperties (outer class) ───────────────────────────────────────────

    @Test
    void defaultConstructor_createsSearchAndFetchInstances() {
        McpProperties props = new McpProperties();

        assertThat(props.getSearch()).isNotNull();
        assertThat(props.getFetch()).isNotNull();
    }

    @Test
    void getSearch_returnsMutableSearchInstance() {
        McpProperties props = new McpProperties();

        // Call twice to confirm same instance is returned each time
        assertThat(props.getSearch()).isSameAs(props.getSearch());
    }

    @Test
    void setSearch_replacesSearchInstance() {
        McpProperties props = new McpProperties();
        McpProperties.Search replacement = new McpProperties.Search();
        props.setSearch(replacement);

        assertThat(props.getSearch()).isSameAs(replacement);
    }

    @Test
    void getFetch_returnsSameFetchInstanceEachTime() {
        McpProperties props = new McpProperties();

        assertThat(props.getFetch()).isSameAs(props.getFetch());
    }

    // ── McpProperties.Search ──────────────────────────────────────────────────

    @Test
    void search_getMaxResults_returnsTen() {
        McpProperties.Search search = new McpProperties.Search();

        assertThat(search.getMaxResults()).isEqualTo(10);
    }

    @Test
    void search_getTimeout_returnsThirty() {
        McpProperties.Search search = new McpProperties.Search();

        assertThat(search.getTimeout()).isEqualTo(30);
    }

    // ── McpProperties.Fetch ───────────────────────────────────────────────────

    @Test
    void fetch_getMaxContentLength_returnsFiftyThousand() {
        McpProperties.Fetch fetch = new McpProperties.Fetch();

        assertThat(fetch.getMaxContentLength()).isEqualTo(50_000);
    }

    @Test
    void fetch_getTimeout_returnsThirty() {
        McpProperties.Fetch fetch = new McpProperties.Fetch();

        assertThat(fetch.getTimeout()).isEqualTo(30);
    }

    @Test
    void fetch_getUserAgent_returnsExpectedString() {
        McpProperties.Fetch fetch = new McpProperties.Fetch();

        assertThat(fetch.getUserAgent()).isEqualTo("LMStudio-MCP/1.0 (Web Content Fetcher)");
    }
}
