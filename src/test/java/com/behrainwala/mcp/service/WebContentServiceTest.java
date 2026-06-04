package com.behrainwala.mcp.service;

import com.behrainwala.mcp.config.McpProperties;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 100% line, branch, and statement coverage for WebContentService.
 *
 * Branches in scope:
 *   fetchAndExtract:
 *     1. content.length() > maxLen → truncate
 *     2. content.length() <= maxLen → no truncation
 *     3. outer try → success
 *     4. outer try → exception (catch block)
 *   extractMainContent (called by fetchAndExtract):
 *     5. selector found + text.length() > 200 → early return
 *     6. selector found + text.length() <= 200 → continue loop
 *     7. selector returns null → continue loop
 *     8. no selector matched → body != null → return body text
 *     9. no selector matched → body == null → return "No content found"
 */
class WebContentServiceTest {

    private WebContentService service;

    @BeforeEach
    void setUp() {
        service = new WebContentService(new McpProperties());
    }

    /**
     * Opens a MockedStatic<Jsoup> that intercepts Jsoup.connect() and returns a
     * mock Connection whose get() returns the supplied Document.
     * Use in try-with-resources so the mock is closed after each test.
     */
    private MockedStatic<Jsoup> stubConnect(Document doc) throws IOException {
        MockedStatic<Jsoup> jsoupStatic = Mockito.mockStatic(Jsoup.class);
        Connection conn = Mockito.mock(Connection.class, Mockito.RETURNS_SELF);
        jsoupStatic.when(() -> Jsoup.connect(anyString())).thenReturn(conn);
        when(conn.get()).thenReturn(doc);
        return jsoupStatic;
    }

    // ── Branch 5: selector found with enough content ──────────────────────────

    @Test
    void articleSelectorWithEnoughContent_extractedAndReturned() throws Exception {
        // <article> has > 200 chars → extractMainContent returns article text (branch 5)
        // content is short → no truncation (branch 2)
        Document doc = Jsoup.parse(
                "<html><head><title>Article Page</title></head><body>"
                + "<article>" + "Tech news paragraph. ".repeat(15) + "</article>"
                + "</body></html>");

        try (MockedStatic<Jsoup> m = stubConnect(doc)) {
            String result = service.fetchAndExtract("https://example.com/article");

            assertThat(result)
                    .startsWith("Title: Article Page\n")
                    .contains("URL: https://example.com/article\n")
                    .contains("---\n")
                    .contains("Tech news paragraph");
        }
    }

    // ── Branch 6: selector found but too short, next selector succeeds ─────────

    @Test
    void firstSelectorTooShort_nextSelectorUsed() throws Exception {
        // <article> has 6 chars (< 200) → branch 6 (continue loop)
        // <main> has > 200 chars → branch 5 on second iteration (early return)
        Document doc = Jsoup.parse(
                "<html><head><title>Page</title></head><body>"
                + "<article>Short.</article>"
                + "<main>" + "Main body text sentence. ".repeat(15) + "</main>"
                + "</body></html>");

        try (MockedStatic<Jsoup> m = stubConnect(doc)) {
            String result = service.fetchAndExtract("https://example.com");

            assertThat(result)
                    .contains("Main body text sentence")
                    .doesNotContain("Short.");
        }
    }

    // ── Branches 7 + 8: no selector matches, body fallback ────────────────────

    @Test
    void noKnownSelectorPresent_bodyUsedAsFallback() throws Exception {
        // No article/main/role=main/.content etc. → all selectFirst() calls return null (branch 7)
        // Falls through to body fallback, body != null (branch 8)
        Document doc = Jsoup.parse(
                "<html><head><title>Plain Page</title></head><body>"
                + "<p>Fallback paragraph shown when no known selector is present.</p>"
                + "</body></html>");

        try (MockedStatic<Jsoup> m = stubConnect(doc)) {
            String result = service.fetchAndExtract("https://example.com");

            assertThat(result).contains("Fallback paragraph");
        }
    }

    // ── Branch 9: body == null ─────────────────────────────────────────────────

    @Test
    void bodyIsNull_returnsNoContentFound() throws Exception {
        // All selectors return null; doc.body() returns null → "No content found" (branch 9)
        Document mockDoc = Mockito.mock(Document.class);
        when(mockDoc.select(anyString())).thenReturn(new Elements());
        when(mockDoc.title()).thenReturn("Empty Doc");
        when(mockDoc.selectFirst(anyString())).thenReturn(null);
        when(mockDoc.body()).thenReturn(null);

        try (MockedStatic<Jsoup> m = stubConnect(mockDoc)) {
            String result = service.fetchAndExtract("https://example.com");

            assertThat(result).contains("No content found");
        }
    }

    // ── Branch 1: content exceeds maxContentLength ────────────────────────────

    @Test
    void contentExceedsMaxLength_truncationMarkerAppended() throws Exception {
        // maxContentLength is 50 000; create 55 000 chars of article text (branch 1)
        Document doc = Jsoup.parse(
                "<html><head><title>Long Article</title></head><body>"
                + "<article>" + "X".repeat(55_000) + "</article>"
                + "</body></html>");

        try (MockedStatic<Jsoup> m = stubConnect(doc)) {
            String result = service.fetchAndExtract("https://example.com");

            assertThat(result)
                    .contains("[Content truncated at 50000 characters]")
                    .startsWith("Title: Long Article");
        }
    }

    // ── Branch 4: Jsoup throws an exception ───────────────────────────────────

    @Test
    void jsoupThrowsException_errorMessageContainsUrlAndCause() throws Exception {
        // conn.get() throws IOException → caught by outer catch (branch 4)
        MockedStatic<Jsoup> jsoupStatic = Mockito.mockStatic(Jsoup.class);
        Connection conn = Mockito.mock(Connection.class, Mockito.RETURNS_SELF);
        jsoupStatic.when(() -> Jsoup.connect(anyString())).thenReturn(conn);
        when(conn.get()).thenThrow(new IOException("Connection timed out"));

        try (jsoupStatic) {
            String result = service.fetchAndExtract("https://bad.example.com");

            assertThat(result)
                    .startsWith("Error fetching URL 'https://bad.example.com':")
                    .contains("Connection timed out");
        }
    }

    // ── cleanText: whitespace normalisation (no branch, pure line coverage) ───

    @Test
    void cleanText_largeWhitespaceGapsCollapsed() throws Exception {
        // Multi-space and multi-newline content in the fetched text; cleanText is exercised
        // by any successful fetchAndExtract call; this test verifies the normalisation
        // is applied (the 3-space gap becomes a blank-line separator, extra spaces collapse).
        Document doc = Jsoup.parse(
                "<html><head><title>Spacing</title></head><body>"
                + "<article>"
                + "Word1   Word2   Word3   Word4   Word5   "  // 3-space gaps → \n\n by LARGE_GAPS
                + "More text to pad the content above 200 chars. ".repeat(8)
                + "</article></body></html>");

        try (MockedStatic<Jsoup> m = stubConnect(doc)) {
            String result = service.fetchAndExtract("https://example.com");

            // Result should not contain 3+ consecutive spaces (they were collapsed)
            assertThat(result).doesNotMatch(".*[ ]{3,}.*");
        }
    }
}
