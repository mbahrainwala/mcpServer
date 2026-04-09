package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextToolTest {

    private TextTool tool;

    @BeforeEach
    void setUp() {
        tool = new TextTool();
    }

    // ── diffText ─────────────────────────────────────────────────────────────

    @Test
    void diffText_identical() {
        String result = tool.diffText("hello", "hello");
        assertThat(result).contains("  hello"); // unchanged prefix
    }

    @Test
    void diffText_addedLine() {
        String result = tool.diffText("line1", "line1\nline2");
        assertThat(result).contains("+ line2");
    }

    @Test
    void diffText_removedLine() {
        String result = tool.diffText("line1\nline2", "line1");
        assertThat(result).contains("- line2");
    }

    @Test
    void diffText_stats() {
        String result = tool.diffText("a\nb", "a\nc");
        assertThat(result).contains("Added").contains("Removed");
    }

    // ── analyzeText ──────────────────────────────────────────────────────────

    @Test
    void analyzeText_wordCount() {
        String result = tool.analyzeText("the quick brown fox");
        assertThat(result).contains("4");
    }

    @Test
    void analyzeText_emptyInput() {
        String result = tool.analyzeText("");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("error"), s -> assertThat(s).contains("0"));
    }

    @Test
    void analyzeText_lineCount() {
        String result = tool.analyzeText("line1\nline2\nline3");
        assertThat(result).contains("3");
    }

    @Test
    void analyzeText_includesTopWords() {
        String result = tool.analyzeText("hello hello hello world world");
        assertThat(result).containsIgnoringCase("hello");
    }

    // ── transformText ────────────────────────────────────────────────────────

    @Test
    void transformText_snake() {
        String result = tool.transformText("helloWorld", "snake");
        assertThat(result).contains("hello_world");
    }

    @Test
    void transformText_camel() {
        String result = tool.transformText("hello_world", "camel");
        assertThat(result).contains("helloWorld");
    }

    @Test
    void transformText_pascal() {
        String result = tool.transformText("hello_world", "pascal");
        assertThat(result).contains("HelloWorld");
    }

    @Test
    void transformText_kebab() {
        String result = tool.transformText("helloWorld", "kebab");
        assertThat(result).contains("hello-world");
    }

    @Test
    void transformText_upperSnake() {
        String result = tool.transformText("hello_world", "upper_snake");
        assertThat(result).contains("HELLO_WORLD");
    }

    @Test
    void transformText_upper() {
        String result = tool.transformText("hello", "upper");
        assertThat(result).contains("HELLO");
    }

    @Test
    void transformText_lower() {
        String result = tool.transformText("HELLO", "lower");
        assertThat(result).contains("hello");
    }

    @Test
    void transformText_sort() {
        String result = tool.transformText("banana\napple\ncherry", "sort");
        int applePos = result.indexOf("apple");
        int bananaPos = result.indexOf("banana");
        assertThat(applePos).isLessThan(bananaPos);
    }

    @Test
    void transformText_unique() {
        String result = tool.transformText("a\nb\na\nc", "unique");
        assertThat(result).containsIgnoringCase("removed").contains("1");
    }

    @Test
    void transformText_reverse() {
        String result = tool.transformText("hello", "reverse");
        assertThat(result).contains("olleh");
    }

    @Test
    void transformText_numberLines() {
        String result = tool.transformText("a\nb\nc", "number_lines");
        assertThat(result).contains("1").contains("2").contains("3");
    }

    @Test
    void transformText_unknownOp() {
        String result = tool.transformText("hello", "xyzzy");
        assertThat(result).containsIgnoringCase("unknown");
    }
}
