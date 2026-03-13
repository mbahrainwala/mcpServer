package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP tool for text analysis and transformation operations useful during coding.
 */
@Service
public class TextTool {

    @Tool(name = "text_diff", description = "Compare two blocks of text line by line and show the differences. "
            + "Useful for comparing code snippets, config files, or any two text blocks.")
    public String diffText(
            @ToolParam(description = "The original text (before)") String text1,
            @ToolParam(description = "The modified text (after)") String text2) {

        String[] lines1 = text1.split("\n", -1);
        String[] lines2 = text2.split("\n", -1);

        // Simple LCS-based diff
        int[][] lcs = computeLCS(lines1, lines2);
        List<String> diff = buildDiff(lines1, lines2, lcs);

        int added = 0, removed = 0, unchanged = 0;
        for (String line : diff) {
            if (line.startsWith("+")) added++;
            else if (line.startsWith("-")) removed++;
            else unchanged++;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Text Diff\n");
        sb.append("─────────\n");
        sb.append("Lines — Original: ").append(lines1.length)
                .append(" | Modified: ").append(lines2.length).append("\n");
        sb.append("Changes — Added: ").append(added)
                .append(" | Removed: ").append(removed)
                .append(" | Unchanged: ").append(unchanged).append("\n\n");

        for (String line : diff) {
            sb.append(line).append("\n");
        }

        return sb.toString().strip();
    }

    @Tool(name = "text_analyze", description = "Analyze text and return statistics: character count, word count, "
            + "line count, sentence count, average word length, most frequent words, and readability metrics.")
    public String analyzeText(
            @ToolParam(description = "The text to analyze") String text) {

        if (text == null || text.isEmpty()) return "Error: text is required";

        int charCount = text.length();
        int charCountNoSpaces = text.replaceAll("\\s", "").length();
        String[] lines = text.split("\n", -1);
        String[] words = text.split("\\s+");
        int wordCount = text.isBlank() ? 0 : words.length;

        // Sentences (approximate)
        int sentenceCount = text.split("[.!?]+").length;

        // Average word length
        double avgWordLen = wordCount > 0
                ? Arrays.stream(words).mapToInt(String::length).average().orElse(0)
                : 0;

        // Word frequency
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (String w : words) {
            String clean = w.toLowerCase().replaceAll("[^a-zA-Z0-9']", "");
            if (clean.length() > 2) {
                freq.merge(clean, 1, Integer::sum);
            }
        }
        List<Map.Entry<String, Integer>> topWords = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .toList();

        // Unique words
        Set<String> uniqueWords = new HashSet<>(freq.keySet());

        // Paragraph count
        int paragraphs = text.split("\n\\s*\n").length;

        StringBuilder sb = new StringBuilder();
        sb.append("Text Analysis\n");
        sb.append("─────────────\n\n");
        sb.append("COUNTS\n");
        sb.append("  Characters: ").append(charCount).append(" (").append(charCountNoSpaces).append(" without spaces)\n");
        sb.append("  Words: ").append(wordCount).append("\n");
        sb.append("  Unique words: ").append(uniqueWords.size()).append("\n");
        sb.append("  Lines: ").append(lines.length).append("\n");
        sb.append("  Paragraphs: ").append(paragraphs).append("\n");
        sb.append("  Sentences: ~").append(sentenceCount).append("\n\n");

        sb.append("AVERAGES\n");
        sb.append("  Avg word length: ").append(String.format("%.1f", avgWordLen)).append(" chars\n");
        if (sentenceCount > 0) {
            sb.append("  Avg words/sentence: ").append(String.format("%.1f", (double) wordCount / sentenceCount)).append("\n");
        }
        sb.append("  Avg words/line: ").append(String.format("%.1f", (double) wordCount / lines.length)).append("\n\n");

        if (!topWords.isEmpty()) {
            sb.append("TOP WORDS\n");
            for (int i = 0; i < topWords.size(); i++) {
                var entry = topWords.get(i);
                sb.append("  ").append(i + 1).append(". '").append(entry.getKey())
                        .append("' — ").append(entry.getValue()).append(" times\n");
            }
        }

        return sb.toString();
    }

    @Tool(name = "text_transform", description = "Transform text between common formats: "
            + "camelCase, snake_case, PascalCase, kebab-case, UPPER_SNAKE_CASE, Title Case, "
            + "or apply operations like reverse, sort lines, remove duplicates, number lines.")
    public String transformText(
            @ToolParam(description = "The text to transform") String text,
            @ToolParam(description = """
                    Transformation to apply:
                      Case: 'camel', 'snake', 'pascal', 'kebab', 'upper_snake', 'title', 'upper', 'lower'
                      Lines: 'sort', 'sort_reverse', 'unique', 'reverse_lines', 'number_lines', 'trim_lines'
                      Text: 'reverse', 'count_chars'""") String operation) {

        String op = operation.strip().toLowerCase();

        return switch (op) {
            // Case conversions (for identifiers)
            case "camel", "camelcase" -> {
                String[] words = splitIdentifier(text);
                yield caseResult(text, op, words.length > 0
                        ? words[0].toLowerCase() + Arrays.stream(words).skip(1)
                        .map(w -> capitalize(w.toLowerCase())).collect(Collectors.joining())
                        : text);
            }
            case "snake", "snake_case" -> {
                String[] words = splitIdentifier(text);
                yield caseResult(text, op, Arrays.stream(words)
                        .map(String::toLowerCase).collect(Collectors.joining("_")));
            }
            case "pascal", "pascalcase" -> {
                String[] words = splitIdentifier(text);
                yield caseResult(text, op, Arrays.stream(words)
                        .map(w -> capitalize(w.toLowerCase())).collect(Collectors.joining()));
            }
            case "kebab", "kebab-case" -> {
                String[] words = splitIdentifier(text);
                yield caseResult(text, op, Arrays.stream(words)
                        .map(String::toLowerCase).collect(Collectors.joining("-")));
            }
            case "upper_snake", "screaming_snake", "constant" -> {
                String[] words = splitIdentifier(text);
                yield caseResult(text, op, Arrays.stream(words)
                        .map(String::toUpperCase).collect(Collectors.joining("_")));
            }
            case "title", "title_case" ->
                    caseResult(text, op, Arrays.stream(text.split("\\s+"))
                            .map(this::capitalize).collect(Collectors.joining(" ")));
            case "upper", "uppercase" -> caseResult(text, op, text.toUpperCase());
            case "lower", "lowercase" -> caseResult(text, op, text.toLowerCase());

            // Line operations
            case "sort" -> {
                String[] lines = text.split("\n");
                Arrays.sort(lines);
                yield lineResult(op, lines);
            }
            case "sort_reverse" -> {
                String[] lines = text.split("\n");
                Arrays.sort(lines, Comparator.reverseOrder());
                yield lineResult(op, lines);
            }
            case "unique" -> {
                String[] lines = text.split("\n");
                String result = Arrays.stream(lines).distinct().collect(Collectors.joining("\n"));
                int removed = lines.length - result.split("\n").length;
                yield "Unique Lines (removed " + removed + " duplicates):\n" + result;
            }
            case "reverse_lines" -> {
                String[] lines = text.split("\n");
                Collections.reverse(Arrays.asList(lines));
                yield lineResult(op, lines);
            }
            case "number_lines" -> {
                String[] lines = text.split("\n");
                StringBuilder sb = new StringBuilder();
                int width = String.valueOf(lines.length).length();
                for (int i = 0; i < lines.length; i++) {
                    sb.append(String.format("%" + width + "d", i + 1)).append("  ").append(lines[i]).append("\n");
                }
                yield "Numbered Lines:\n" + sb.toString().strip();
            }
            case "trim_lines" -> {
                String result = Arrays.stream(text.split("\n"))
                        .map(String::strip)
                        .collect(Collectors.joining("\n"));
                yield "Trimmed Lines:\n" + result;
            }

            // Text operations
            case "reverse" -> "Reversed:\n" + new StringBuilder(text).reverse();

            default -> "Unknown operation '" + operation + "'. Use: camel, snake, pascal, kebab, "
                    + "upper_snake, title, upper, lower, sort, unique, reverse_lines, number_lines, "
                    + "trim_lines, reverse";
        };
    }

    // ── Diff helpers ──

    private int[][] computeLCS(String[] a, String[] b) {
        int[][] dp = new int[a.length + 1][b.length + 1];
        for (int i = 1; i <= a.length; i++) {
            for (int j = 1; j <= b.length; j++) {
                dp[i][j] = a[i - 1].equals(b[j - 1])
                        ? dp[i - 1][j - 1] + 1
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return dp;
    }

    private List<String> buildDiff(String[] a, String[] b, int[][] lcs) {
        int i = a.length, j = b.length;

        List<String> reversed = new ArrayList<>();
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && a[i - 1].equals(b[j - 1])) {
                reversed.add("  " + a[i - 1]);
                i--; j--;
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                reversed.add("+ " + b[j - 1]);
                j--;
            } else {
                reversed.add("- " + a[i - 1]);
                i--;
            }
        }

        Collections.reverse(reversed);
        return reversed;
    }

    // ── Case conversion helpers ──

    private String[] splitIdentifier(String text) {
        // Split on: camelCase boundaries, underscores, hyphens, spaces
        return text
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                .split("[\\s_\\-]+");
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String caseResult(String input, String operation, String output) {
        return "Case Conversion (" + operation + ")\n"
                + "Input:  " + input + "\n"
                + "Output: " + output;
    }

    private String lineResult(String operation, String[] lines) {
        return operation.replace("_", " ") + " (" + lines.length + " lines):\n"
                + String.join("\n", lines);
    }
}
