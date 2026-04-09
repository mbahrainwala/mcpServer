package com.behrainwala.mcp.tools;

import com.behrainwala.mcp.config.McpProperties;
import com.behrainwala.mcp.service.WebContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebContentFetcherToolTest {

    private WebContentFetcherTool tool;

    @BeforeEach
    void setUp() {
        tool = new WebContentFetcherTool(new WebContentService(new McpProperties()));
    }

    @Test
    void fetch_blankUrl_returnsError() {
        String result = tool.fetch("");
        assertThat(result).containsIgnoringCase("error").containsIgnoringCase("url");
    }

    @Test
    void fetch_nullUrl_returnsError() {
        String result = tool.fetch(null);
        assertThat(result).containsIgnoringCase("error").containsIgnoringCase("url");
    }

    @Test
    void fetch_schemePrependedForBareHost() {
        // If we pass a bare host, https:// gets prepended before attempt
        // It will fail with a network error, but not an NPE or schema error
        String result = tool.fetch("this.host.does.not.exist.example.invalid");
        assertThat(result).isNotNull().doesNotContainIgnoringCase("NullPointer");
    }
}
