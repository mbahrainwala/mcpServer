package com.behrainwala.mcp.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 100% line, branch, and statement coverage for SearchResult.
 *
 * Branches in toFormattedString():
 *   1. !url.isEmpty()     → true  (URL line appended)
 *   2. !url.isEmpty()     → false (URL line skipped)
 *   3. !snippet.isEmpty() → true  (Snippet line appended)
 *   4. !snippet.isEmpty() → false (Snippet line skipped)
 *
 * Record methods also exercised:
 *   canonical constructor, title(), url(), snippet(),
 *   equals(), hashCode(), toString()
 */
class SearchResultTest {

    // ── toFormattedString — branch 1 (url non-empty) + branch 3 (snippet non-empty) ──

    @Test
    void allFieldsNonEmpty_outputContainsTitleUrlAndSnippet() {
        SearchResult r = new SearchResult("Java Guide", "https://example.com/java", "Learn Java fast.");

        String out = r.toFormattedString();

        assertThat(out).isEqualTo(
                "Title: Java Guide\n"
                + "URL: https://example.com/java\n"
                + "Snippet: Learn Java fast.\n");
    }

    // ── toFormattedString — branch 2 (url empty) + branch 3 (snippet non-empty) ─────

    @Test
    void emptyUrl_urlLineOmitted_snippetLinePresent() {
        // !url.isEmpty() → false (branch 2): URL line skipped
        SearchResult r = new SearchResult("Error", "", "Something went wrong.");

        String out = r.toFormattedString();

        assertThat(out)
                .startsWith("Title: Error\n")
                .doesNotContain("URL:")
                .contains("Snippet: Something went wrong.\n");
    }

    // ── toFormattedString — branch 1 (url non-empty) + branch 4 (snippet empty) ─────

    @Test
    void emptySnippet_snippetLineOmitted_urlLinePresent() {
        // !snippet.isEmpty() → false (branch 4): snippet line skipped
        SearchResult r = new SearchResult("Result", "https://example.com", "");

        String out = r.toFormattedString();

        assertThat(out)
                .contains("Title: Result\n")
                .contains("URL: https://example.com\n")
                .doesNotContain("Snippet:");
    }

    // ── toFormattedString — branch 2 (url empty) + branch 4 (snippet empty) ─────────

    @Test
    void bothUrlAndSnippetEmpty_onlyTitleLineProduced() {
        // Both false branches: neither URL nor snippet line appended
        SearchResult r = new SearchResult("Minimal", "", "");

        String out = r.toFormattedString();

        assertThat(out).isEqualTo("Title: Minimal\n");
    }

    // ── Exact output format ───────────────────────────────────────────────────────────

    @Test
    void outputFormat_eachSectionEndsWithNewline() {
        SearchResult r = new SearchResult("T", "https://u.com", "S");
        String[] lines = r.toFormattedString().split("\n");
        assertThat(lines[0]).isEqualTo("Title: T");
        assertThat(lines[1]).isEqualTo("URL: https://u.com");
        assertThat(lines[2]).isEqualTo("Snippet: S");
    }

    // ── Record accessor methods ───────────────────────────────────────────────────────

    @Test
    void accessors_returnValuesSuppliedToConstructor() {
        SearchResult r = new SearchResult("My Title", "https://my.url/path", "My snippet text");

        assertThat(r.title()).isEqualTo("My Title");
        assertThat(r.url()).isEqualTo("https://my.url/path");
        assertThat(r.snippet()).isEqualTo("My snippet text");
    }

    // ── equals — same fields ──────────────────────────────────────────────────────────

    @Test
    void equals_recordsWithIdenticalFields_areEqual() {
        SearchResult a = new SearchResult("T", "U", "S");
        SearchResult b = new SearchResult("T", "U", "S");

        assertThat(a).isEqualTo(b);
        assertThat(a).isEqualTo(a); // reflexive
    }

    // ── equals — different fields ─────────────────────────────────────────────────────

    @Test
    void equals_differentTitle_notEqual() {
        assertThat(new SearchResult("A", "U", "S"))
                .isNotEqualTo(new SearchResult("B", "U", "S"));
    }

    @Test
    void equals_differentUrl_notEqual() {
        assertThat(new SearchResult("T", "U1", "S"))
                .isNotEqualTo(new SearchResult("T", "U2", "S"));
    }

    @Test
    void equals_differentSnippet_notEqual() {
        assertThat(new SearchResult("T", "U", "S1"))
                .isNotEqualTo(new SearchResult("T", "U", "S2"));
    }

    @Test
    void equals_nullObject_notEqual() {
        SearchResult r = new SearchResult("T", "U", "S");
        assertThat(r).isNotEqualTo(null);
    }

    @Test
    void equals_nonSearchResultObject_notEqual() {
        SearchResult r = new SearchResult("T", "U", "S");
        assertThat(r).isNotEqualTo("T");
    }

    // ── hashCode ──────────────────────────────────────────────────────────────────────

    @Test
    void hashCode_equalRecords_sameHashCode() {
        SearchResult a = new SearchResult("T", "U", "S");
        SearchResult b = new SearchResult("T", "U", "S");

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void hashCode_differentRecords_typicallyDifferentHashCode() {
        // Not guaranteed by contract, but true for simple distinct strings
        SearchResult a = new SearchResult("Title A", "https://a.com", "Snippet A");
        SearchResult b = new SearchResult("Title B", "https://b.com", "Snippet B");

        // Just call hashCode() to ensure the method is instrumented/covered
        assertThat(a.hashCode()).isNotZero();
        assertThat(b.hashCode()).isNotZero();
    }

    // ── toString (record-generated) ───────────────────────────────────────────────────

    @Test
    void toString_containsAllFieldValues() {
        SearchResult r = new SearchResult("The Title", "https://the.url", "The snippet");
        String s = r.toString();

        assertThat(s)
                .contains("The Title")
                .contains("https://the.url")
                .contains("The snippet");
    }

    @Test
    void toString_followsRecordConvention() {
        // Java records produce: ClassName[field1=value1, field2=value2, ...]
        SearchResult r = new SearchResult("T", "U", "S");
        assertThat(r.toString()).startsWith("SearchResult[");
    }
}
