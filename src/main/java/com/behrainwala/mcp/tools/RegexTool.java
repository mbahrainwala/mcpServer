package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * MCP tool for testing and debugging regular expressions.
 * LLMs frequently produce incorrect regex — this tool validates them against real input.
 */
@Service
public class RegexTool {

    @Tool(name = "regex_test", description = "Test a regular expression against input text. "
            + "Returns whether it matches, all match groups, and captured groups. "
            + "Use this to validate regex patterns before suggesting them to the user.")
    public String testRegex(
            @ToolParam(description = "The regular expression pattern to test") String pattern,
            @ToolParam(description = "The input text to test against") String input,
            @ToolParam(description = "Regex flags as a string: 'i' (case-insensitive), 'm' (multiline), "
                    + "'s' (dotall), 'g' (find all matches). Combine like 'ig'. Default: none.", required = false) String flags) {

        int regexFlags = 0;
        boolean findAll = false;

        if (flags != null) {
            for (char f : flags.toLowerCase().toCharArray()) {
                switch (f) {
                    case 'i' -> regexFlags |= Pattern.CASE_INSENSITIVE;
                    case 'm' -> regexFlags |= Pattern.MULTILINE;
                    case 's' -> regexFlags |= Pattern.DOTALL;
                    case 'g' -> findAll = true;
                }
            }
        }

        Pattern compiled;
        try {
            compiled = Pattern.compile(pattern, regexFlags);
        } catch (PatternSyntaxException e) {
            return "INVALID REGEX\n"
                    + "─────────────\n"
                    + "Pattern: " + pattern + "\n"
                    + "Error: " + e.getDescription() + "\n"
                    + "Position: " + e.getIndex() + "\n\n"
                    + "Common fixes:\n"
                    + "  - Escape special chars: . * + ? ( ) [ ] { } \\ ^ $ |\n"
                    + "  - Use \\\\d instead of \\d in Java strings\n"
                    + "  - Check for unbalanced parentheses/brackets";
        }

        Matcher matcher = compiled.matcher(input);

        StringBuilder sb = new StringBuilder();
        sb.append("Regex Test Results\n");
        sb.append("──────────────────\n");
        sb.append("Pattern: ").append(pattern).append("\n");
        sb.append("Flags: ").append(flags != null ? flags : "none").append("\n");
        sb.append("Input: ").append(truncate(input, 200)).append("\n\n");

        if (findAll) {
            List<MatchInfo> matches = new ArrayList<>();
            while (matcher.find()) {
                matches.add(extractMatch(matcher));
            }

            sb.append("Total matches: ").append(matches.size()).append("\n\n");
            for (int i = 0; i < matches.size() && i < 50; i++) {
                sb.append("Match ").append(i + 1).append(":\n");
                sb.append(matches.get(i).format("  "));
            }
            if (matches.size() > 50) {
                sb.append("... and ").append(matches.size() - 50).append(" more matches\n");
            }
        } else {
            boolean found = matcher.find();
            sb.append("Matches: ").append(found ? "YES" : "NO").append("\n");
            boolean fullMatch = compiled.matcher(input).matches();
            sb.append("Full match: ").append(fullMatch ? "YES" : "NO").append("\n\n");

            if (found) {
                sb.append(extractMatch(matcher).format(""));
            }
        }

        return sb.toString();
    }

    @Tool(name = "regex_replace", description = "Perform a regex find-and-replace on input text. "
            + "Returns the transformed text. Supports backreferences ($1, $2, etc.) in the replacement.")
    public String regexReplace(
            @ToolParam(description = "The regular expression pattern") String pattern,
            @ToolParam(description = "The replacement string. Use $1, $2 for group backreferences.") String replacement,
            @ToolParam(description = "The input text") String input,
            @ToolParam(description = "Replace all occurrences (true) or just the first (false). Default: true.", required = false) Boolean replaceAll) {

        try {
            Pattern compiled = Pattern.compile(pattern);
            Matcher matcher = compiled.matcher(input);
            boolean all = replaceAll == null || replaceAll;
            String result = all ? matcher.replaceAll(replacement) : matcher.replaceFirst(replacement);

            // Count replacements
            Matcher counter = compiled.matcher(input);
            int count = 0;
            while (counter.find()) count++;

            return "Regex Replace\n" +
                    "─────────────\n" +
                    "Pattern: " + pattern + "\n" +
                    "Replace: " + replacement + "\n" +
                    "Mode: " + (all ? "Replace all" : "Replace first") + "\n" +
                    "Matches found: " + count + "\n\n" +
                    "BEFORE:\n" + truncate(input, 500) + "\n\n" +
                    "AFTER:\n" + truncate(result, 500);

        } catch (PatternSyntaxException e) {
            return "Invalid regex: " + e.getDescription();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "regex_explain", description = "Break down a regular expression and explain each part in plain English. "
            + "Helps understand complex regex patterns.")
    public String explainRegex(
            @ToolParam(description = "The regular expression pattern to explain") String pattern) {

        StringBuilder sb = new StringBuilder();
        sb.append("Regex Explanation\n");
        sb.append("─────────────────\n");
        sb.append("Pattern: ").append(pattern).append("\n\n");

        // Validate first
        try {
            Pattern.compile(pattern);
            sb.append("Status: VALID ✓\n\n");
        } catch (PatternSyntaxException e) {
            sb.append("Status: INVALID ✗ — ").append(e.getDescription()).append("\n\n");
        }

        sb.append("Breakdown:\n");
        int pos = 0;
        int groupNum = 1;

        while (pos < pattern.length()) {
            char c = pattern.charAt(pos);
            String remaining = pattern.substring(pos);

            if (c == '^') {
                sb.append("  ^          Start of string/line\n"); pos++;
            } else if (c == '$') {
                sb.append("  $          End of string/line\n"); pos++;
            } else if (c == '.') {
                sb.append("  .          Any character (except newline)\n"); pos++;
            } else if (c == '*') {
                sb.append("  *          Zero or more of the preceding\n"); pos++;
            } else if (c == '+') {
                sb.append("  +          One or more of the preceding\n"); pos++;
            } else if (c == '?') {
                if (pos > 0 && (pattern.charAt(pos - 1) == '*' || pattern.charAt(pos - 1) == '+' || pattern.charAt(pos - 1) == '?')) {
                    sb.append("  ?          Make preceding quantifier lazy (non-greedy)\n");
                } else {
                    sb.append("  ?          Zero or one of the preceding (optional)\n");
                }
                pos++;
            } else if (c == '|') {
                sb.append("  |          OR — alternative\n"); pos++;
            } else if (c == '(' && pos + 1 < pattern.length() && pattern.charAt(pos + 1) == '?') {
                if (remaining.startsWith("(?:")) {
                    sb.append("  (?:...)    Non-capturing group\n"); pos += 3;
                } else if (remaining.startsWith("(?=")) {
                    sb.append("  (?=...)    Positive lookahead\n"); pos += 3;
                } else if (remaining.startsWith("(?!")) {
                    sb.append("  (?!...)    Negative lookahead\n"); pos += 3;
                } else if (remaining.startsWith("(?<=")) {
                    sb.append("  (?<=...)   Positive lookbehind\n"); pos += 4;
                } else if (remaining.startsWith("(?<!")) {
                    sb.append("  (?<!...)   Negative lookbehind\n"); pos += 4;
                } else if (remaining.startsWith("(?<")) {
                    int close = remaining.indexOf('>');
                    if (close > 0) {
                        String name = remaining.substring(3, close);
                        sb.append("  (?<").append(name).append(">...)  Named capturing group '").append(name).append("' (group ").append(groupNum++).append(")\n");
                        pos += close + 1;
                    } else { pos++; }
                } else { sb.append("  (?...)     Special group\n"); pos += 2; }
            } else if (c == '(') {
                sb.append("  (...)      Capturing group ").append(groupNum++).append("\n"); pos++;
            } else if (c == ')') {
                sb.append("  )          End of group\n"); pos++;
            } else if (c == '[') {
                int close = findClosingBracket(pattern, pos);
                if (close > pos) {
                    String charClass = pattern.substring(pos, close + 1);
                    sb.append("  ").append(padRight(charClass, 12))
                            .append(explainCharClass(charClass)).append("\n");
                    pos = close + 1;
                } else { pos++; }
            } else if (c == '{') {
                int close = pattern.indexOf('}', pos);
                if (close > pos) {
                    String quantifier = pattern.substring(pos, close + 1);
                    sb.append("  ").append(padRight(quantifier, 12))
                            .append(explainQuantifier(quantifier)).append("\n");
                    pos = close + 1;
                } else { pos++; }
            } else if (c == '\\' && pos + 1 < pattern.length()) {
                char next = pattern.charAt(pos + 1);
                String escaped = "\\" + next;
                sb.append("  ").append(padRight(escaped, 12)).append(explainEscape(next)).append("\n");
                pos += 2;
            } else {
                sb.append("  ").append(padRight(String.valueOf(c), 12)).append("Literal '").append(c).append("'\n");
                pos++;
            }
        }

        return sb.toString();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private record MatchInfo(String fullMatch, int start, int end, List<GroupInfo> groups) {
        String format(String indent) {
            StringBuilder sb = new StringBuilder();
            sb.append(indent).append("Full match: '").append(fullMatch).append("'");
            sb.append(" [position ").append(start).append("-").append(end).append("]\n");
            for (GroupInfo g : groups) {
                sb.append(indent).append("  Group ").append(g.index).append(": '")
                        .append(g.value != null ? g.value : "<not captured>").append("'\n");
            }
            return sb.toString();
        }
    }

    private record GroupInfo(int index, String value) {}

    private MatchInfo extractMatch(Matcher matcher) {
        List<GroupInfo> groups = new ArrayList<>();
        for (int i = 1; i <= matcher.groupCount(); i++) {
            groups.add(new GroupInfo(i, matcher.group(i)));
        }
        return new MatchInfo(matcher.group(), matcher.start(), matcher.end(), groups);
    }

    private int findClosingBracket(String pattern, int openPos) {
        for (int i = openPos + 1; i < pattern.length(); i++) {
            if (pattern.charAt(i) == '\\' && i + 1 < pattern.length()) { i++; continue; }
            if (pattern.charAt(i) == ']') return i;
        }
        return -1;
    }

    private String explainCharClass(String charClass) {
        if (charClass.startsWith("[^")) return "Any character NOT in: " + charClass.substring(2, charClass.length() - 1);
        return "Any character in: " + charClass.substring(1, charClass.length() - 1);
    }

    private String explainQuantifier(String q) {
        String inner = q.substring(1, q.length() - 1);
        if (inner.contains(",")) {
            String[] parts = inner.split(",", 2);
            if (parts[1].isEmpty()) return "At least " + parts[0] + " of the preceding";
            return "Between " + parts[0] + " and " + parts[1] + " of the preceding";
        }
        return "Exactly " + inner + " of the preceding";
    }

    private String explainEscape(char c) {
        return switch (c) {
            case 'd' -> "Digit [0-9]";
            case 'D' -> "Non-digit [^0-9]";
            case 'w' -> "Word character [a-zA-Z0-9_]";
            case 'W' -> "Non-word character";
            case 's' -> "Whitespace [ \\t\\n\\r\\f]";
            case 'S' -> "Non-whitespace";
            case 'b' -> "Word boundary";
            case 'B' -> "Non-word boundary";
            case 'n' -> "Newline";
            case 'r' -> "Carriage return";
            case 't' -> "Tab";
            case '\\' -> "Literal backslash";
            case '.' -> "Literal dot";
            case '*' -> "Literal asterisk";
            case '+' -> "Literal plus";
            case '?' -> "Literal question mark";
            case '(' -> "Literal open parenthesis";
            case ')' -> "Literal close parenthesis";
            case '[' -> "Literal open bracket";
            case ']' -> "Literal close bracket";
            case '{' -> "Literal open brace";
            case '}' -> "Literal close brace";
            case '^' -> "Literal caret";
            case '$' -> "Literal dollar sign";
            case '|' -> "Literal pipe";
            default -> "Escaped '" + c + "'";
        };
    }

    private String padRight(String s, int width) {
        return s.length() >= width ? s + " " : s + " ".repeat(width - s.length());
    }

    private String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "... [truncated]";
    }
}
