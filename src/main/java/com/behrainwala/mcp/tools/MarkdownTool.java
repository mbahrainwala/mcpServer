package com.behrainwala.mcp.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP tool that helps an LLM prepare and consume Markdown / HTML content:
 * convert HTML to Markdown, strip Markdown formatting, and extract fenced code blocks.
 * Useful when feeding web pages or LLM-generated content back into a model.
 */
@Service
public class MarkdownTool {

    private static final Pattern CODE_BLOCK = Pattern.compile(
            "```([a-zA-Z0-9_+\\-]*)\\s*\\n([\\s\\S]*?)```", Pattern.MULTILINE);

    private static final Pattern[] MD_STRIPPERS = new Pattern[]{
            Pattern.compile("```[\\s\\S]*?```"),                  // fenced code blocks
            Pattern.compile("`([^`]+)`"),                          // inline code
            Pattern.compile("!\\[([^]]*)]\\([^)]*\\)"),       // images
            Pattern.compile("\\[([^]]+)]\\([^)]*\\)"),        // links — keep label
            Pattern.compile("^#{1,6}\\s+", Pattern.MULTILINE),    // heading markers
            Pattern.compile("^\\s*>\\s?", Pattern.MULTILINE),     // blockquote markers
            Pattern.compile("^\\s*[-*+]\\s+", Pattern.MULTILINE), // unordered list markers
            Pattern.compile("^\\s*\\d+\\.\\s+", Pattern.MULTILINE),// ordered list markers
            Pattern.compile("\\*\\*([^*]+)\\*\\*"),               // bold
            Pattern.compile("__([^_]+)__"),                        // bold
            Pattern.compile("\\*([^*\\n]+)\\*"),                  // italic
            Pattern.compile("_([^_\\n]+)_"),                       // italic
            Pattern.compile("~~([^~]+)~~"),                        // strikethrough
            Pattern.compile("^[-*_]{3,}\\s*$", Pattern.MULTILINE) // horizontal rules
    };

    private static final String[] MD_REPLACEMENTS = new String[]{
            "", "$1", "$1", "$1", "", "", "", "", "$1", "$1", "$1", "$1", "$1", ""
    };

    @Tool(name = "html_to_markdown", description = "Convert HTML to Markdown. Handles headings, paragraphs, "
            + "lists, code blocks, links, images, bold/italic, blockquotes, and tables. "
            + "Useful for cleaning web content before sending it to an LLM — Markdown is more "
            + "token-efficient and parses better than raw HTML.")
    public String htmlToMarkdown(
            @ToolParam(description = "HTML content to convert") String html) {

        if (html == null || html.isBlank()) return "Error: html is required";
        Document doc = Jsoup.parse(html);
        // Drop noise
        doc.select("script, style, noscript, iframe, head meta, head link").remove();

        StringBuilder out = new StringBuilder();
        doc.body();
        Element root = doc.body();
        convertNode(root, out, 0);

        // Normalize blank lines (no more than 2 consecutive newlines)
        String result = out.toString().replaceAll("\\n{3,}", "\n\n").strip();
        return result.isEmpty() ? "(empty)" : result;
    }

    private void convertNode(Node node, StringBuilder out, int listDepth) {
        if (node instanceof TextNode tn) {
            String txt = tn.text();
            if (!txt.isEmpty()) out.append(txt);
            return;
        }
        if (!(node instanceof Element el)) return;
        String tag = el.tagName().toLowerCase();

        switch (tag) {
            case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                int level = tag.charAt(1) - '0';
                out.append("\n\n").repeat("#", level).append(" ").append(el.text()).append("\n\n");
            }
            case "p" -> {
                out.append("\n\n");
                for (Node c : el.childNodes()) convertNode(c, out, listDepth);
                out.append("\n\n");
            }
            case "br" -> out.append("  \n");
            case "hr" -> out.append("\n\n---\n\n");
            case "strong", "b" -> out.append("**").append(el.text()).append("**");
            case "em", "i" -> out.append("*").append(el.text()).append("*");
            case "del", "s", "strike" -> out.append("~~").append(el.text()).append("~~");
            case "code" -> {
                // Inline code only — code blocks are <pre><code>
                if (!"pre".equals(el.parent() != null ? el.parent().tagName().toLowerCase() : "")) {
                    out.append("`").append(el.text()).append("`");
                } else {
                    out.append(el.text());
                }
            }
            case "pre" -> {
                String lang = "";
                Element code = el.selectFirst("code");
                if (code != null) {
                    for (String cls : code.classNames()) {
                        if (cls.startsWith("language-")) { lang = cls.substring("language-".length()); break; }
                        if (cls.startsWith("lang-")) { lang = cls.substring("lang-".length()); break; }
                    }
                }
                String text = code != null ? code.text() : el.text();
                out.append("\n\n```").append(lang).append("\n").append(text).append("\n```\n\n");
            }
            case "a" -> {
                String href = el.attr("href");
                String text = el.text();
                if (href.isEmpty()) out.append(text);
                else out.append("[").append(text).append("](").append(href).append(")");
            }
            case "img" -> {
                String src = el.attr("src");
                String alt = el.attr("alt");
                out.append("![").append(alt).append("](").append(src).append(")");
            }
            case "ul" -> {
                out.append("\n");
                for (Element li : el.children()) {
                    if ("li".equalsIgnoreCase(li.tagName())) {
                        out.repeat("  ", listDepth).append("- ");
                        for (Node c : li.childNodes()) convertNode(c, out, listDepth + 1);
                        out.append("\n");
                    }
                }
                out.append("\n");
            }
            case "ol" -> {
                out.append("\n");
                int i = 1;
                for (Element li : el.children()) {
                    if ("li".equalsIgnoreCase(li.tagName())) {
                        out.repeat("  ", listDepth).append(i++).append(". ");
                        for (Node c : li.childNodes()) convertNode(c, out, listDepth + 1);
                        out.append("\n");
                    }
                }
                out.append("\n");
            }
            case "blockquote" -> {
                StringBuilder inner = new StringBuilder();
                for (Node c : el.childNodes()) convertNode(c, inner, listDepth);
                String quoted = inner.toString().strip().replaceAll("(?m)^", "> ");
                out.append("\n\n").append(quoted).append("\n\n");
            }
            case "table" -> {
                out.append("\n\n");
                List<List<String>> rows = new ArrayList<>();
                for (Element tr : el.select("tr")) {
                    List<String> row = new ArrayList<>();
                    for (Element cell : tr.select("th, td")) {
                        row.add(cell.text().replace("|", "\\|"));
                    }
                    if (!row.isEmpty()) rows.add(row);
                }
                if (!rows.isEmpty()) {
                    int cols = rows.getFirst().size();
                    out.append("| ").append(String.join(" | ", rows.getFirst())).append(" |\n");
                    out.append("|").repeat(" --- |", cols).append("\n");
                    for (int r = 1; r < rows.size(); r++) {
                        // Pad rows to header column count
                        List<String> row = rows.get(r);
                        while (row.size() < cols) row.add("");
                        out.append("| ").append(String.join(" | ", row.subList(0, cols))).append(" |\n");
                    }
                }
                out.append("\n");
            }
            default -> {
                for (Node c : el.childNodes()) convertNode(c, out, listDepth);
            }
        }
    }

    @Tool(name = "markdown_to_text", description = "Strip Markdown formatting and return plain text. "
            + "Removes headings, bold/italic, links (keeps anchor text), images, code blocks, "
            + "blockquote markers, list bullets, and horizontal rules. Use to feed a clean version of "
            + "Markdown content into an LLM that doesn't need the syntax.")
    public String markdownToText(
            @ToolParam(description = "Markdown content to strip") String markdown) {

        if (markdown == null) return "Error: markdown is required";
        String s = markdown;
        for (int i = 0; i < MD_STRIPPERS.length; i++) {
            s = MD_STRIPPERS[i].matcher(s).replaceAll(MD_REPLACEMENTS[i]);
        }
        // Collapse triple+ blank lines
        s = s.replaceAll("\\n{3,}", "\n\n").strip();
        return s.isEmpty() ? "(empty)" : s;
    }

    @Tool(name = "extract_code_blocks", description = "Extract fenced code blocks from Markdown. "
            + "Returns each block with its language tag (if present) and content, separated by markers. "
            + "Useful when an LLM produces a mixed-format response and you need just the code.")
    public String extractCodeBlocks(
            @ToolParam(description = "Markdown text containing fenced code blocks") String markdown,
            @ToolParam(description = "Optional language filter (e.g. 'python'). Returns only blocks of this language.",
                    required = false) String language) {

        if (markdown == null) return "Error: markdown is required";
        String filter = language == null || language.isBlank() ? null : language.strip().toLowerCase();

        Matcher m = CODE_BLOCK.matcher(markdown);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        while (m.find()) {
            String lang = m.group(1).trim();
            String code = m.group(2);
            if (filter != null && !filter.equals(lang.toLowerCase())) continue;
            count++;
            sb.append("---BLOCK ").append(count).append(" (lang=").append(lang.isEmpty() ? "none" : lang)
                    .append(")---\n").append(code).append("\n");
        }
        if (count == 0) {
            return filter == null
                    ? "No code blocks found."
                    : "No code blocks found for language '" + filter + "'.";
        }
        return "Extracted " + count + " code block(s)\n"
                + "─".repeat(30) + "\n"
                + sb.toString().strip();
    }
}
