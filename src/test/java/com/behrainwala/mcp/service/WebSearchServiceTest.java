package com.behrainwala.mcp.service;

import com.behrainwala.mcp.config.McpProperties;
import com.behrainwala.mcp.model.SearchResult;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 100% line, branch, and statement coverage for WebSearchService.
 *
 * Branches in scope:
 *   search:
 *     1. results.size() >= limit → break (limit reached)
 *     2. results.size() < limit  → continue loop
 *     3. titleLink != null       → process result
 *     4. titleLink == null       → skip result
 *     5. snippet != null         → use snippet text
 *     6. snippet == null         → use ""
 *     7. uddgIdx >= 0            → URL is a DDG redirect; decode it
 *     8. uddgIdx < 0             → plain URL; keep as-is
 *     9. encoded.contains("&")   → truncate before "&"
 *    10. !encoded.contains("&")  → decode full encoded string
 *    11. URLDecoder throws        → catch, keep original URL
 *    12. outer try succeeds       → normal return
 *    13. outer try throws         → add "Search Error" result
 */
class WebSearchServiceTest {

    private WebSearchService service;

    @BeforeEach
    void setUp() {
        service = new WebSearchService(new McpProperties());
    }

    // ── HTML fixtures ─────────────────────────────────────────────────────────

    /** Builds a single DuckDuckGo-style result block. Pass {@code null} for snippet to omit it. */
    private static String block(String title, String href, String snippet) {
        String snip = snippet == null ? ""
                : "<div class=\"result__snippet\">" + snippet + "</div>";
        return "<div class=\"result\">"
                + "<a class=\"result__a\" href=\"" + href + "\">" + title + "</a>"
                + snip + "</div>";
    }

    /** Wraps one or more block strings in a minimal HTML page and parses it. */
    private static Document page(String... blocks) {
        StringBuilder sb = new StringBuilder("<html><body>");
        for (String b : blocks) sb.append(b);
        sb.append("</body></html>");
        return Jsoup.parse(sb.toString());
    }

    /** Opens a MockedStatic<Jsoup> whose connect().post() returns the given Document. */
    private MockedStatic<Jsoup> stubPost(Document doc) throws IOException {
        MockedStatic<Jsoup> jsoupStatic = Mockito.mockStatic(Jsoup.class);
        Connection conn = Mockito.mock(Connection.class, Mockito.RETURNS_SELF);
        jsoupStatic.when(() -> Jsoup.connect(anyString())).thenReturn(conn);
        when(conn.post()).thenReturn(doc);
        return jsoupStatic;
    }

    // ── Branches 7 + 10: uddg URL without & ──────────────────────────────────

    @Test
    void uddgUrlNoAmpersand_decodedToActualUrl() throws Exception {
        // uddgIdx >= 0 (branch 7), encoded has no "&" (branch 10) → decoded URL
        String enc = URLEncoder.encode("https://example.com/article", StandardCharsets.UTF_8);
        Document doc = page(block("Example Article", "/?uddg=" + enc, "A snippet."));

        try (MockedStatic<Jsoup> m = stubPost(doc)) {
            List<SearchResult> results = service.search("test query", 10);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).url()).isEqualTo("https://example.com/article");
            assertThat(results.get(0).title()).isEqualTo("Example Article");
        }
    }

    // ── Branch 9: uddg URL with & ─────────────────────────────────────────────

    @Test
    void uddgUrlWithAmpersand_truncatedBeforeAmpersandThenDecoded() throws Exception {
        // uddgIdx >= 0 (branch 7), encoded.contains("&") (branch 9) → truncate then decode
        String enc = URLEncoder.encode("https://example.com/page", StandardCharsets.UTF_8);
        Document doc = page(block("Page", "/?uddg=" + enc + "&rut=abc123&other=xyz", "Snip"));

        try (MockedStatic<Jsoup> m = stubPost(doc)) {
            List<SearchResult> results = service.search("query", 10);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).url()).isEqualTo("https://example.com/page");
        }
    }

    // ── Branch 8: URL without uddg ────────────────────────────────────────────

    @Test
    void plainUrlWithoutUddg_keptAsIs() throws Exception {
        // uddgIdx < 0 (branch 8) → URL used verbatim
        Document doc = page(block("Direct Link", "https://direct.example.com/path", "Direct."));

        try (MockedStatic<Jsoup> m = stubPost(doc)) {
            List<SearchResult> results = service.search("query", 10);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).url()).isEqualTo("https://direct.example.com/path");
        }
    }

    // ── Branch 11: URLDecoder throws ─────────────────────────────────────────

    @Test
    void uddgWithInvalidEncoding_decodeExceptionCaught_originalUrlKept() throws Exception {
        // %GG is not valid percent-encoding → URLDecoder.decode() throws (branch 11)
        // catch block keeps the original URL
        Document doc = page(block("Bad", "https://ddg.com/l/?uddg=%GG", "Snip"));

        try (MockedStatic<Jsoup> m = stubPost(doc)) {
            List<SearchResult> results = service.search("query", 10);

            assertThat(results).hasSize(1);
            // original URL retained because decoding failed
            assertThat(results.get(0).url()).contains("uddg=%GG");
        }
    }

    // ── Branches 5 + 6: snippet present vs absent ────────────────────────────

    @Test
    void snippetPresent_includedInSearchResult() throws Exception {
        // snippet != null (branch 5)
        Document doc = page(block("Title", "https://example.com", "Great summary text."));

        try (MockedStatic<Jsoup> m = stubPost(doc)) {
            List<SearchResult> results = service.search("query", 10);

            assertThat(results.get(0).snippet()).isEqualTo("Great summary text.");
        }
    }

    @Test
    void snippetElementAbsent_emptySnippetInResult() throws Exception {
        // .result__snippet not in DOM → snippet == null (branch 6) → snippetText = ""
        Document doc = page(block("Title", "https://example.com", null));

        try (MockedStatic<Jsoup> m = stubPost(doc)) {
            List<SearchResult> results = service.search("query", 10);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).snippet()).isEmpty();
        }
    }

    // ── Branch 4: titleLink is null ───────────────────────────────────────────

    @Test
    void resultBlockWithNoTitleLink_resultSkipped() throws Exception {
        // .result__a is absent → titleLink == null (branch 4) → element not added
        String noLinkBlock = "<div class=\"result\">"
                + "<span class=\"result__snippet\">Some snippet with no link</span>"
                + "</div>";
        Document doc = page(noLinkBlock);

        try (MockedStatic<Jsoup> m = stubPost(doc)) {
            List<SearchResult> results = service.search("query", 10);

            assertThat(results).isEmpty();
        }
    }

    // ── Branch 3 + 4 together: mix of valid and invalid blocks ────────────────

    @Test
    void mixedBlocks_onlyBlocksWithTitleLinkAdded() throws Exception {
        // First block: no link → skipped (branch 4)
        // Second block: has link → added (branch 3)
        String noLink = "<div class=\"result\"><div class=\"result__snippet\">No link</div></div>";
        Document doc = page(noLink, block("Valid Title", "https://valid.com", "Valid snippet."));

        try (MockedStatic<Jsoup> m = stubPost(doc)) {
            List<SearchResult> results = service.search("query", 10);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).title()).isEqualTo("Valid Title");
        }
    }

    // ── Branch 1: limit reached → break ──────────────────────────────────────

    @Test
    void maxResultsLimitRespected_breaksEarlyWhenLimitReached() throws Exception {
        // 5 result blocks, maxResults=2 → loop breaks after 2 (branch 1)
        Document doc = page(
                block("R1", "https://r1.example.com", "S1"),
                block("R2", "https://r2.example.com", "S2"),
                block("R3", "https://r3.example.com", "S3"),
                block("R4", "https://r4.example.com", "S4"),
                block("R5", "https://r5.example.com", "S5"));

        try (MockedStatic<Jsoup> m = stubPost(doc)) {
            List<SearchResult> results = service.search("query", 2);

            assertThat(results).hasSize(2);
            assertThat(results.get(0).title()).isEqualTo("R1");
            assertThat(results.get(1).title()).isEqualTo("R2");
        }
    }

    // ── Branch 2: limit not reached, all results returned ────────────────────

    @Test
    void allResultsReturnedWhenUnderLimit() throws Exception {
        // 3 results, limit=10 → loop finishes naturally (branch 2 for all iterations)
        Document doc = page(
                block("A", "https://a.com", "SA"),
                block("B", "https://b.com", "SB"),
                block("C", "https://c.com", "SC"));

        try (MockedStatic<Jsoup> m = stubPost(doc)) {
            List<SearchResult> results = service.search("query", 10);

            assertThat(results).hasSize(3);
        }
    }

    // ── Config cap: Math.min(requested, config.maxResults) ───────────────────

    @Test
    void maxResultsCappedByConfiguredMaximum() throws Exception {
        // Config.maxResults = 10; request 999 → limit = min(999,10) = 10
        // Build 12 result blocks, expect only 10 returned (branch 1 at 10)
        StringBuilder blocks = new StringBuilder();
        for (int i = 1; i <= 12; i++)
            blocks.append(block("R" + i, "https://r" + i + ".com", "S" + i));
        Document doc = page(blocks.toString());

        try (MockedStatic<Jsoup> m = stubPost(doc)) {
            List<SearchResult> results = service.search("query", 999);

            assertThat(results).hasSize(10);
        }
    }

    // ── Branch 13: outer try throws (Jsoup exception) ────────────────────────

    @Test
    void jsoupPostThrowsException_singleSearchErrorResultReturned() throws Exception {
        // conn.post() throws IOException → outer catch (branch 13)
        MockedStatic<Jsoup> jsoupStatic = Mockito.mockStatic(Jsoup.class);
        Connection conn = Mockito.mock(Connection.class, Mockito.RETURNS_SELF);
        jsoupStatic.when(() -> Jsoup.connect(anyString())).thenReturn(conn);
        when(conn.post()).thenThrow(new IOException("Network unreachable"));

        try (jsoupStatic) {
            List<SearchResult> results = service.search("some query", 5);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).title()).isEqualTo("Search Error");
            assertThat(results.get(0).url()).isEmpty();
            assertThat(results.get(0).snippet()).contains("Network unreachable");
        }
    }
}
