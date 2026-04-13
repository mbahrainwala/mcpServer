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
        assertThat(result).contains("  hello").contains("Unchanged: 1");
    }

    @Test
    void diffText_addedLine() {
        String result = tool.diffText("line1", "line1\nline2");
        assertThat(result).contains("+ line2").contains("Added: 1");
    }

    @Test
    void diffText_removedLine() {
        String result = tool.diffText("line1\nline2", "line1");
        assertThat(result).contains("- line2").contains("Removed: 1");
    }

    @Test
    void diffText_changedLine() {
        String result = tool.diffText("a\nb", "a\nc");
        assertThat(result).contains("- b").contains("+ c");
    }

    @Test
    void diffText_multipleChanges() {
        String result = tool.diffText("a\nb\nc", "a\nd\nc\ne");
        assertThat(result).contains("Added:").contains("Removed:");
    }

    @Test
    void diffText_emptyOriginal() {
        String result = tool.diffText("", "new line");
        assertThat(result).contains("+ new line");
    }

    @Test
    void diffText_emptyModified() {
        String result = tool.diffText("old line", "");
        assertThat(result).contains("- old line");
    }

    @Test
    void diffText_stats() {
        String result = tool.diffText("a\nb", "a\nc");
        assertThat(result)
                .contains("Original: 2")
                .contains("Modified: 2");
    }

    // ── analyzeText ──────────────────────────────────────────────────────────

    @Test
    void analyzeText_nullInput_returnsError() {
        String result = tool.analyzeText(null);
        assertThat(result).contains("Error: text is required");
    }

    @Test
    void analyzeText_emptyInput_returnsError() {
        String result = tool.analyzeText("");
        assertThat(result).contains("Error: text is required");
    }

    @Test
    void analyzeText_wordCount() {
        String result = tool.analyzeText("the quick brown fox");
        assertThat(result).contains("Words: 4");
    }

    @Test
    void analyzeText_lineCount() {
        String result = tool.analyzeText("line1\nline2\nline3");
        assertThat(result).contains("Lines: 3");
    }

    @Test
    void analyzeText_characterCount() {
        String result = tool.analyzeText("hello world");
        assertThat(result).contains("Characters: 11").contains("10 without spaces");
    }

    @Test
    void analyzeText_blankText_zeroWords() {
        String result = tool.analyzeText("   ");
        assertThat(result).contains("Words: 0");
    }

    @Test
    void analyzeText_paragraphCount() {
        String result = tool.analyzeText("first paragraph\n\nsecond paragraph");
        assertThat(result).contains("Paragraphs: 2");
    }

    @Test
    void analyzeText_sentenceCount() {
        String result = tool.analyzeText("Hello. World! How?");
        assertThat(result).contains("Sentences:");
    }

    @Test
    void analyzeText_averageWordLength() {
        String result = tool.analyzeText("the quick brown fox");
        assertThat(result).contains("Avg word length:");
    }

    @Test
    void analyzeText_avgWordsPerSentence() {
        String result = tool.analyzeText("Hello World. Goodbye World.");
        assertThat(result).contains("Avg words/sentence:");
    }

    @Test
    void analyzeText_topWords() {
        String result = tool.analyzeText("hello hello hello world world foo");
        assertThat(result).contains("TOP WORDS").contains("hello");
    }

    @Test
    void analyzeText_shortWords_filteredFromFrequency() {
        // Words of length <= 2 are filtered from frequency
        String result = tool.analyzeText("I am a man on me at my");
        assertThat(result).contains("Unique words:");
    }

    @Test
    void analyzeText_uniqueWords() {
        String result = tool.analyzeText("the the the fox fox");
        assertThat(result).contains("Unique words:");
    }

    // ── transformText case conversions ────────────────────────────────────────

    @Test
    void transformText_camel() {
        String result = tool.transformText("hello_world", "camel");
        assertThat(result).contains("helloWorld").contains("Case Conversion");
    }

    @Test
    void transformText_camelCase_alias() {
        String result = tool.transformText("hello_world", "camelCase");
        assertThat(result).contains("helloWorld");
    }

    @Test
    void transformText_snake() {
        String result = tool.transformText("helloWorld", "snake");
        assertThat(result).contains("hello_world");
    }

    @Test
    void transformText_snake_case_alias() {
        String result = tool.transformText("helloWorld", "snake_case");
        assertThat(result).contains("hello_world");
    }

    @Test
    void transformText_pascal() {
        String result = tool.transformText("hello_world", "pascal");
        assertThat(result).contains("HelloWorld");
    }

    @Test
    void transformText_pascalCase_alias() {
        String result = tool.transformText("hello_world", "pascalCase");
        assertThat(result).contains("HelloWorld");
    }

    @Test
    void transformText_kebab() {
        String result = tool.transformText("helloWorld", "kebab");
        assertThat(result).contains("hello-world");
    }

    @Test
    void transformText_kebabCase_alias() {
        String result = tool.transformText("helloWorld", "kebab-case");
        assertThat(result).contains("hello-world");
    }

    @Test
    void transformText_upperSnake() {
        String result = tool.transformText("hello_world", "upper_snake");
        assertThat(result).contains("HELLO_WORLD");
    }

    @Test
    void transformText_screamingSnake_alias() {
        String result = tool.transformText("hello_world", "screaming_snake");
        assertThat(result).contains("HELLO_WORLD");
    }

    @Test
    void transformText_constant_alias() {
        String result = tool.transformText("hello_world", "constant");
        assertThat(result).contains("HELLO_WORLD");
    }

    @Test
    void transformText_title() {
        String result = tool.transformText("hello world", "title");
        assertThat(result).contains("Hello World");
    }

    @Test
    void transformText_title_case_alias() {
        String result = tool.transformText("hello world", "title_case");
        assertThat(result).contains("Hello World");
    }

    @Test
    void transformText_upper() {
        String result = tool.transformText("hello", "upper");
        assertThat(result).contains("HELLO");
    }

    @Test
    void transformText_uppercase_alias() {
        String result = tool.transformText("hello", "uppercase");
        assertThat(result).contains("HELLO");
    }

    @Test
    void transformText_lower() {
        String result = tool.transformText("HELLO", "lower");
        assertThat(result).contains("hello");
    }

    @Test
    void transformText_lowercase_alias() {
        String result = tool.transformText("HELLO", "lowercase");
        assertThat(result).contains("hello");
    }

    // ── transformText line operations ─────────────────────────────────────────

    @Test
    void transformText_sort() {
        String result = tool.transformText("banana\napple\ncherry", "sort");
        int applePos = result.indexOf("apple");
        int bananaPos = result.indexOf("banana");
        int cherryPos = result.indexOf("cherry");
        assertThat(applePos).isLessThan(bananaPos);
        assertThat(bananaPos).isLessThan(cherryPos);
    }

    @Test
    void transformText_sort_reverse() {
        String result = tool.transformText("apple\nbanana\ncherry", "sort_reverse");
        int cherryPos = result.indexOf("cherry");
        int bananaPos = result.indexOf("banana");
        int applePos = result.indexOf("apple");
        assertThat(cherryPos).isLessThan(bananaPos);
        assertThat(bananaPos).isLessThan(applePos);
    }

    @Test
    void transformText_unique() {
        String result = tool.transformText("a\nb\na\nc", "unique");
        assertThat(result).contains("removed 1 duplicates");
    }

    @Test
    void transformText_reverse_lines() {
        String result = tool.transformText("first\nsecond\nthird", "reverse_lines");
        int thirdPos = result.indexOf("third");
        int firstPos = result.indexOf("first");
        assertThat(thirdPos).isLessThan(firstPos);
    }

    @Test
    void transformText_number_lines() {
        String result = tool.transformText("a\nb\nc", "number_lines");
        assertThat(result).contains("1  a").contains("2  b").contains("3  c");
    }

    @Test
    void transformText_trim_lines() {
        String result = tool.transformText("  hello  \n  world  ", "trim_lines");
        assertThat(result).contains("Trimmed Lines:\nhello\nworld");
    }

    // ── transformText text operations ─────────────────────────────────────────

    @Test
    void transformText_reverse() {
        String result = tool.transformText("hello", "reverse");
        assertThat(result).contains("olleh").contains("Reversed:");
    }

    // ── transformText unknown ─────────────────────────────────────────────────

    @Test
    void transformText_unknownOp() {
        String result = tool.transformText("hello", "xyzzy");
        assertThat(result).contains("Unknown operation");
    }

    // ── splitIdentifier edge cases ───────────────────────────────────────────

    @Test
    void transformText_camelFromHyphenated() {
        String result = tool.transformText("my-cool-function", "camel");
        assertThat(result).contains("myCoolFunction");
    }

    @Test
    void transformText_snakeFromPascalCase() {
        String result = tool.transformText("MyPascalCase", "snake");
        assertThat(result).contains("my_pascal_case");
    }

    @Test
    void transformText_kebabFromConsecutiveUppercase() {
        String result = tool.transformText("HTTPSConnection", "kebab");
        assertThat(result).contains("-");
    }
}
