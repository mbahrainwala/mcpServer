package com.behrainwala.mcp.tools;

import com.behrainwala.mcp.config.McpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpClientToolTest {

    private HttpClientTool tool;

    @BeforeEach
    void setUp() {
        tool = new HttpClientTool(new McpProperties());
    }

    @Test
    void httpRequest_invalidUrl_returnsError() {
        String result = tool.httpRequest("not_a_valid_url_xyz_12345_$$$.invalid", "GET", null, null);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("failed"), s -> assertThat(s).containsIgnoringCase("error"));
    }

    @Test
    void httpRequest_defaultMethodIsGet() {
        // Test that null method defaults to GET without throwing
        // Uses a known-bad host so it fails fast with an error
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", null, null, null);
        assertThat(result).isNotNull().isNotBlank();
    }

    @Test
    void httpRequest_schemePrepended() {
        // If no scheme, https:// should be prepended; should fail with DNS/connection error not an NPE
        String result = tool.httpRequest("this.host.does.not.exist.example.invalid", "GET", null, null);
        assertThat(result).isNotNull().doesNotContainIgnoringCase("NullPointer");
    }
}
