package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownToolTest {

    private MarkdownTool tool;

    @BeforeEach
    void setUp() {
        tool = new MarkdownTool();
    }

    @Test
    void htmlToMarkdown_basicHeading() {
        String result = tool.htmlToMarkdown("<h1>Title</h1><p>Body text.</p>");
        assertThat(result).contains("# Title").contains("Body text.");
    }

    @Test
    void htmlToMarkdown_unorderedList() {
        String html = "<ul><li>one</li><li>two</li><li>three</li></ul>";
        String result = tool.htmlToMarkdown(html);
        assertThat(result).contains("- one").contains("- two").contains("- three");
    }

    @Test
    void htmlToMarkdown_orderedList() {
        String html = "<ol><li>first</li><li>second</li></ol>";
        String result = tool.htmlToMarkdown(html);
        assertThat(result).contains("1. first").contains("2. second");
    }

    @Test
    void htmlToMarkdown_link() {
        String result = tool.htmlToMarkdown("<a href=\"https://example.com\">click here</a>");
        assertThat(result).contains("[click here](https://example.com)");
    }

    @Test
    void htmlToMarkdown_codeBlock() {
        String html = "<pre><code class=\"language-java\">int x = 1;</code></pre>";
        String result = tool.htmlToMarkdown(html);
        assertThat(result).contains("```java").contains("int x = 1;");
    }

    @Test
    void htmlToMarkdown_boldItalic() {
        String result = tool.htmlToMarkdown("<p><strong>bold</strong> and <em>italic</em></p>");
        assertThat(result).contains("**bold**").contains("*italic*");
    }

    @Test
    void htmlToMarkdown_blockquote() {
        String result = tool.htmlToMarkdown("<blockquote><p>quoted text</p></blockquote>");
        assertThat(result).contains("> quoted text");
    }

    @Test
    void htmlToMarkdown_simpleTable() {
        String html = "<table><tr><th>a</th><th>b</th></tr><tr><td>1</td><td>2</td></tr></table>";
        String result = tool.htmlToMarkdown(html);
        assertThat(result).contains("| a | b |");
        assertThat(result).contains("| 1 | 2 |");
        assertThat(result).contains("--- |");
    }

    @Test
    void markdownToText_stripsBold() {
        String result = tool.markdownToText("This is **bold** and *italic*.");
        assertThat(result).isEqualTo("This is bold and italic.");
    }

    @Test
    void markdownToText_stripsHeadings() {
        String result = tool.markdownToText("# Heading\nSome text.\n## Sub");
        assertThat(result).contains("Heading").contains("Some text.").contains("Sub");
        assertThat(result).doesNotContain("#");
    }

    @Test
    void markdownToText_stripsLinkKeepsAnchor() {
        String result = tool.markdownToText("Click [here](https://example.com) please.");
        assertThat(result).isEqualTo("Click here please.");
    }

    @Test
    void markdownToText_stripsCodeBlock() {
        String md = "Before\n```python\nprint('hi')\n```\nAfter";
        String result = tool.markdownToText(md);
        assertThat(result).contains("Before").contains("After");
        assertThat(result).doesNotContain("print");
        assertThat(result).doesNotContain("```");
    }

    @Test
    void extractCodeBlocks_findsBlocks() {
        String md = "intro\n```python\nprint('a')\n```\nmiddle\n```java\nint x;\n```\nend";
        String result = tool.extractCodeBlocks(md, null);
        assertThat(result).contains("Extracted 2 code block(s)");
        assertThat(result).contains("(lang=python)").contains("print('a')");
        assertThat(result).contains("(lang=java)").contains("int x;");
    }

    @Test
    void extractCodeBlocks_filterByLanguage() {
        String md = "```python\nprint('a')\n```\n```java\nint x;\n```";
        String result = tool.extractCodeBlocks(md, "java");
        assertThat(result).contains("int x;");
        assertThat(result).doesNotContain("print('a')");
    }

    @Test
    void extractCodeBlocks_noBlocksFound() {
        String result = tool.extractCodeBlocks("just plain text, no code", null);
        assertThat(result).contains("No code blocks found");
    }
}
