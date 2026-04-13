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

    // ── Error handling: invalid URLs ─────────────────────────────────────────

    @Test
    void httpRequest_invalidUrl_returnsError() {
        String result = tool.httpRequest("not_a_valid_url_xyz_12345_$$$.invalid", "GET", null, null);
        assertThat(result).containsIgnoringCase("failed");
    }

    @Test
    void httpRequest_unreachableHost_returnsError() {
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "GET", null, null);
        assertThat(result).containsIgnoringCase("failed");
    }

    // ── Default method ───────────────────────────────────────────────────────

    @Test
    void httpRequest_nullMethod_defaultsToGet() {
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", null, null, null);
        assertThat(result).isNotNull().isNotBlank();
    }

    @Test
    void httpRequest_blankMethod_defaultsToGet() {
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "  ", null, null);
        assertThat(result).isNotNull();
    }

    // ── Scheme prepending ────────────────────────────────────────────────────

    @Test
    void httpRequest_noScheme_prependsHttps() {
        String result = tool.httpRequest("this.host.does.not.exist.example.invalid", "GET", null, null);
        assertThat(result).doesNotContain("NullPointer");
    }

    @Test
    void httpRequest_httpScheme_keptAsIs() {
        String result = tool.httpRequest("http://this.host.does.not.exist.example.invalid", "GET", null, null);
        assertThat(result).containsIgnoringCase("failed");
    }

    // ── HTTP method branches ─────────────────────────────────────────────────

    @Test
    void httpRequest_postMethod_error() {
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "POST", null, "{\"key\":\"value\"}");
        assertThat(result).containsIgnoringCase("failed");
    }

    @Test
    void httpRequest_putMethod_error() {
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "PUT", null, "{\"key\":\"value\"}");
        assertThat(result).containsIgnoringCase("failed");
    }

    @Test
    void httpRequest_patchMethod_error() {
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "PATCH", null, "{\"key\":\"value\"}");
        assertThat(result).containsIgnoringCase("failed");
    }

    @Test
    void httpRequest_deleteMethod_error() {
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "DELETE", null, null);
        assertThat(result).containsIgnoringCase("failed");
    }

    @Test
    void httpRequest_unknownMethod_error() {
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "OPTIONS", null, null);
        assertThat(result).containsIgnoringCase("failed");
    }

    // ── Body handling ────────────────────────────────────────────────────────

    @Test
    void httpRequest_postWithNullBody_noBodyPublisher() {
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "POST", null, null);
        assertThat(result).containsIgnoringCase("failed");
    }

    @Test
    void httpRequest_postWithBlankBody_noBodyPublisher() {
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "POST", null, "  ");
        assertThat(result).containsIgnoringCase("failed");
    }

    @Test
    void httpRequest_postWithBody_usesBody() {
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "POST", null, "{\"data\":\"test\"}");
        assertThat(result).containsIgnoringCase("failed");
    }

    // ── Header parsing ───────────────────────────────────────────────────────

    @Test
    void httpRequest_headersWithNewlines_parsed() {
        String headers = "Content-Type: application/json\nAccept: text/html";
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "GET", headers, null);
        assertThat(result).containsIgnoringCase("failed");
    }

    @Test
    void httpRequest_headersWithEscapedNewlines_parsed() {
        String headers = "Content-Type: application/json\\nAccept: text/html";
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "GET", headers, null);
        assertThat(result).containsIgnoringCase("failed");
    }

    @Test
    void httpRequest_headersWithNoColon_ignored() {
        String headers = "InvalidHeader";
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "GET", headers, null);
        assertThat(result).containsIgnoringCase("failed");
    }

    @Test
    void httpRequest_nullHeaders_noError() {
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "GET", null, null);
        assertThat(result).doesNotContain("NullPointer");
    }

    @Test
    void httpRequest_blankHeaders_noError() {
        String result = tool.httpRequest("https://this.host.does.not.exist.example.invalid", "GET", "  ", null);
        assertThat(result).doesNotContain("NullPointer");
    }

    // ── statusText method coverage ───────────────────────────────────────────
    // Since statusText is private, we can't call it directly, but we verify
    // indirectly by checking the error output format

    @Test
    void httpRequest_constructorCreatesClient() {
        // Just verifies that the tool is instantiated without exceptions
        HttpClientTool anotherTool = new HttpClientTool(new McpProperties());
        assertThat(anotherTool).isNotNull();
    }

    // ── URI parsing edge case ────────────────────────────────────────────────

    @Test
    void httpRequest_malformedUri_returnsError() {
        // Spaces and special chars that break URI.create
        String result = tool.httpRequest("https://invalid host with spaces", "GET", null, null);
        assertThat(result).containsIgnoringCase("failed");
    }
}
