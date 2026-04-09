package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegexToolTest {

    private RegexTool tool;

    @BeforeEach
    void setUp() {
        tool = new RegexTool();
    }

    // ── testRegex ────────────────────────────────────────────────────────────

    @Test
    void testRegex_match() {
        String result = tool.testRegex("\\d+", "hello 123 world", null);
        assertThat(result).contains("YES");
    }

    @Test
    void testRegex_noMatch() {
        String result = tool.testRegex("\\d+", "hello world", null);
        assertThat(result).contains("NO");
    }

    @Test
    void testRegex_caseInsensitive() {
        String result = tool.testRegex("HELLO", "hello world", "i");
        assertThat(result).contains("YES");
    }

    @Test
    void testRegex_findAll() {
        String result = tool.testRegex("\\d+", "1 and 2 and 3", "g");
        assertThat(result).contains("3"); // 3 matches
    }

    @Test
    void testRegex_invalidPattern_returnsError() {
        String result = tool.testRegex("[invalid", "test", null);
        assertThat(result).containsIgnoringCase("invalid");
    }

    @Test
    void testRegex_captureGroups() {
        String result = tool.testRegex("(\\w+)@(\\w+)", "user@example", null);
        assertThat(result).contains("Group 1").contains("Group 2");
    }

    // ── regexReplace ─────────────────────────────────────────────────────────

    @Test
    void regexReplace_all() {
        String result = tool.regexReplace("o", "0", "foo bar boo", true);
        assertThat(result).contains("f00 bar b00");
    }

    @Test
    void regexReplace_first() {
        String result = tool.regexReplace("o", "0", "foo bar boo", false);
        assertThat(result).contains("f0o");
    }

    @Test
    void regexReplace_backreference() {
        String result = tool.regexReplace("(\\w+)@(\\w+)", "$2@$1", "user@host", true);
        assertThat(result).contains("host@user");
    }

    @Test
    void regexReplace_defaultReplaceAll() {
        String result = tool.regexReplace("a", "b", "banana", null);
        assertThat(result).contains("bbnbnb");
    }

    // ── explainRegex ─────────────────────────────────────────────────────────

    @Test
    void explainRegex_valid() {
        String result = tool.explainRegex("^\\d{3}-\\d{4}$");
        assertThat(result).contains("VALID").containsIgnoringCase("Digit");
    }

    @Test
    void explainRegex_invalid() {
        String result = tool.explainRegex("[unclosed");
        assertThat(result).contains("INVALID");
    }

    @Test
    void explainRegex_anchors() {
        String result = tool.explainRegex("^hello$");
        assertThat(result).contains("Start of string").contains("End of string");
    }
}
