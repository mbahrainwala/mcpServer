package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegexToolTest {

    private RegexTool tool;

    @BeforeEach
    void setUp() {
        tool = new RegexTool();
    }

    // =========================================================================
    // testRegex
    // =========================================================================

    @Nested
    class TestRegex {

        @Test
        void simpleMatch_found() {
            String result = tool.testRegex("\\d+", "hello 123 world", null);
            assertThat(result)
                    .contains("Matches: YES")
                    .contains("Full match: '123'")
                    .contains("position 6-9");
        }

        @Test
        void simpleMatch_fullMatch_yes() {
            String result = tool.testRegex("\\d+", "12345", null);
            assertThat(result)
                    .contains("Matches: YES")
                    .contains("Full match: YES");
        }

        @Test
        void simpleMatch_fullMatch_no() {
            String result = tool.testRegex("\\d+", "abc 123 def", null);
            assertThat(result)
                    .contains("Matches: YES")
                    .contains("Full match: NO");
        }

        @Test
        void noMatch() {
            String result = tool.testRegex("\\d+", "hello world", null);
            assertThat(result)
                    .contains("Matches: NO")
                    .contains("Full match: NO");
        }

        @Test
        void captureGroups_displayed() {
            String result = tool.testRegex("(\\w+)@(\\w+)\\.(\\w+)", "user@example.com", null);
            assertThat(result)
                    .contains("Group 1: 'user'")
                    .contains("Group 2: 'example'")
                    .contains("Group 3: 'com'");
        }

        @Test
        void captureGroup_notCaptured_displaysNotCaptured() {
            // Optional group that does not match
            String result = tool.testRegex("(a)(b)?(c)", "ac", null);
            assertThat(result)
                    .contains("Group 1: 'a'")
                    .contains("Group 2: '<not captured>'")
                    .contains("Group 3: 'c'");
        }

        @Test
        void nullFlags_treatedAsNone() {
            String result = tool.testRegex("hello", "hello world", null);
            assertThat(result)
                    .contains("Flags: none")
                    .contains("Matches: YES");
        }

        // ── Flags ───────────────────────────────────────────────────────────

        @Test
        void flag_caseInsensitive() {
            String result = tool.testRegex("HELLO", "hello world", "i");
            assertThat(result).contains("Matches: YES");
        }

        @Test
        void flag_multiline() {
            String result = tool.testRegex("^world", "hello\nworld", "m");
            assertThat(result).contains("Matches: YES");
        }

        @Test
        void flag_dotall() {
            String result = tool.testRegex("hello.world", "hello\nworld", "s");
            assertThat(result).contains("Matches: YES");
        }

        @Test
        void flag_global_findsAllMatches() {
            String result = tool.testRegex("\\d+", "1 and 22 and 333", "g");
            assertThat(result)
                    .contains("Total matches: 3")
                    .contains("Match 1:")
                    .contains("Match 2:")
                    .contains("Match 3:");
        }

        @Test
        void flag_global_noMatches() {
            String result = tool.testRegex("\\d+", "no numbers here", "g");
            assertThat(result).contains("Total matches: 0");
        }

        @Test
        void flag_combined_ig() {
            String result = tool.testRegex("hello", "Hello HELLO hello", "ig");
            assertThat(result).contains("Total matches: 3");
        }

        @Test
        void flag_global_withGroups() {
            String result = tool.testRegex("(\\w+)=(\\w+)", "a=1 b=2", "g");
            assertThat(result)
                    .contains("Total matches: 2")
                    .contains("Group 1:")
                    .contains("Group 2:");
        }

        @Test
        void flag_global_moreThan50Matches_truncated() {
            // Build input with 55 matches
            StringBuilder input = new StringBuilder();
            for (int i = 0; i < 55; i++) {
                input.append(i).append(" ");
            }
            String result = tool.testRegex("\\d+", input.toString(), "g");
            assertThat(result)
                    .contains("Total matches: 55")
                    .contains("... and 5 more matches");
        }

        @Test
        void flag_unknownFlag_isIgnored() {
            // 'x' is not a known flag; should not throw
            String result = tool.testRegex("hello", "hello", "x");
            assertThat(result).contains("Matches: YES");
        }

        // ── Invalid pattern ─────────────────────────────────────────────────

        @Test
        void invalidPattern_returnsInvalidRegex() {
            String result = tool.testRegex("[unclosed", "test", null);
            assertThat(result)
                    .contains("INVALID REGEX")
                    .contains("Pattern: [unclosed")
                    .contains("Error:")
                    .contains("Position:")
                    .contains("Common fixes:");
        }

        // ── Truncation ──────────────────────────────────────────────────────

        @Test
        void longInput_isTruncated() {
            String longInput = "a".repeat(300);
            String result = tool.testRegex("a+", longInput, null);
            assertThat(result).contains("... [truncated]");
        }

        @Test
        void shortInput_isNotTruncated() {
            String result = tool.testRegex("hello", "hello", null);
            assertThat(result).doesNotContain("[truncated]");
        }
    }

    // =========================================================================
    // regexReplace
    // =========================================================================

    @Nested
    class RegexReplace {

        @Test
        void replaceAll_default() {
            String result = tool.regexReplace("o", "0", "foo bar boo", null);
            assertThat(result)
                    
                    
                    .contains("f00 bar b00");
        }

        @Test
        void replaceAll_true() {
            String result = tool.regexReplace("a", "X", "banana", true);
            assertThat(result)
                    .contains("Mode: Replace all")
                    .contains("bXnXnX");
        }

        @Test
        void replaceFirst_only() {
            String result = tool.regexReplace("a", "X", "banana", false);
            assertThat(result)
                    .contains("Mode: Replace first")
                    .contains("bXnana");
        }

        @Test
        void backreference_swapGroups() {
            String result = tool.regexReplace("(\\w+)@(\\w+)", "$2@$1", "user@host", true);
            assertThat(result).contains("host@user");
        }

        @Test
        void noMatches_outputUnchanged() {
            String result = tool.regexReplace("\\d+", "X", "no digits", true);
            assertThat(result)
                    .contains("Matches found: 0")
                    .contains("BEFORE:\nno digits")
                    .contains("AFTER:\nno digits");
        }

        @Test
        void invalidPattern_returnsError() {
            String result = tool.regexReplace("[bad", "X", "test", true);
            assertThat(result).startsWith("Invalid regex:");
        }

        @Test
        void longInput_truncatedInOutput() {
            String longInput = "a".repeat(600);
            String result = tool.regexReplace("a", "b", longInput, true);
            assertThat(result).contains("... [truncated]");
        }

        @Test
        void replacementWithDollarSign_backreference() {
            String result = tool.regexReplace("(\\d+)-(\\d+)", "$1/$2", "2024-01-15", true);
            assertThat(result).contains("2024/01");
        }

        @Test
        void emptyReplacement_deletesMatches() {
            String result = tool.regexReplace("\\s+", "", "hello   world", true);
            assertThat(result).contains("helloworld");
        }

        @Test
        void showsPatternAndReplacement() {
            String result = tool.regexReplace("foo", "bar", "foo baz foo", true);
            assertThat(result)
                    .contains("Pattern: foo")
                    .contains("Replace: bar");
        }

        @Test
        void genericException_returnsError() {
            // An invalid replacement backreference can trigger an exception
            String result = tool.regexReplace("(a)", "$2", "a", true);
            assertThat(result).startsWith("Error:");
        }
    }

    // =========================================================================
    // explainRegex
    // =========================================================================

    @Nested
    class ExplainRegex {

        @Test
        void validPattern_showsValid() {
            String result = tool.explainRegex("abc");
            assertThat(result)
                    .contains("Status: VALID")
                    .contains("Breakdown:");
        }

        @Test
        void invalidPattern_showsInvalid() {
            String result = tool.explainRegex("[unclosed");
            assertThat(result).contains("Status: INVALID");
        }

        // ── Anchors ─────────────────────────────────────────────────────────

        @Test
        void caret_explainedAsStartOfString() {
            String result = tool.explainRegex("^abc");
            assertThat(result).contains("Start of string/line");
        }

        @Test
        void dollar_explainedAsEndOfString() {
            String result = tool.explainRegex("abc$");
            assertThat(result).contains("End of string/line");
        }

        // ── Dot and quantifiers ─────────────────────────────────────────────

        @Test
        void dot_explainedAsAnyChar() {
            String result = tool.explainRegex("a.b");
            assertThat(result).contains("Any character (except newline)");
        }

        @Test
        void star_explainedAsZeroOrMore() {
            String result = tool.explainRegex("a*");
            assertThat(result).contains("Zero or more of the preceding");
        }

        @Test
        void plus_explainedAsOneOrMore() {
            String result = tool.explainRegex("a+");
            assertThat(result).contains("One or more of the preceding");
        }

        @Test
        void questionMark_explainedAsOptional() {
            String result = tool.explainRegex("a?b");
            assertThat(result).contains("Zero or one of the preceding (optional)");
        }

        @Test
        void lazyQuantifier_starQuestion() {
            String result = tool.explainRegex("a*?");
            assertThat(result).contains("Make preceding quantifier lazy (non-greedy)");
        }

        @Test
        void lazyQuantifier_plusQuestion() {
            String result = tool.explainRegex("a+?");
            assertThat(result).contains("Make preceding quantifier lazy (non-greedy)");
        }

        @Test
        void lazyQuantifier_questionQuestion() {
            String result = tool.explainRegex("a??");
            assertThat(result).contains("Make preceding quantifier lazy (non-greedy)");
        }

        // ── Alternation ─────────────────────────────────────────────────────

        @Test
        void pipe_explainedAsOr() {
            String result = tool.explainRegex("a|b");
            assertThat(result).contains("OR");
        }

        // ── Groups ──────────────────────────────────────────────────────────

        @Test
        void capturingGroup() {
            String result = tool.explainRegex("(abc)");
            assertThat(result)
                    .contains("Capturing group 1")
                    .contains("End of group");
        }

        @Test
        void nonCapturingGroup() {
            String result = tool.explainRegex("(?:abc)");
            assertThat(result).contains("Non-capturing group");
        }

        @Test
        void positiveLookahead() {
            String result = tool.explainRegex("a(?=b)");
            assertThat(result).contains("Positive lookahead");
        }

        @Test
        void negativeLookahead() {
            String result = tool.explainRegex("a(?!b)");
            assertThat(result).contains("Negative lookahead");
        }

        @Test
        void positiveLookbehind() {
            String result = tool.explainRegex("(?<=a)b");
            assertThat(result).contains("Positive lookbehind");
        }

        @Test
        void negativeLookbehind() {
            String result = tool.explainRegex("(?<!a)b");
            assertThat(result).contains("Negative lookbehind");
        }

        @Test
        void namedCapturingGroup() {
            String result = tool.explainRegex("(?<name>\\w+)");
            assertThat(result).contains("Named capturing group 'name' (group 1)");
        }

        @Test
        void namedCapturingGroup_noClosingAngleBracket_fallsThrough() {
            // (?< without matching > - edge case, just increments pos
            String result = tool.explainRegex("(?<");
            assertThat(result).contains("Breakdown:");
        }

        @Test
        void specialGroup_unknownType() {
            // (?i) is a special group that doesn't match the specific known patterns
            // after (?  ... it matches the else branch "Special group"
            String result = tool.explainRegex("(?i)abc");
            assertThat(result).contains("Special group");
        }

        // ── Character classes ───────────────────────────────────────────────

        @Test
        void characterClass_simple() {
            String result = tool.explainRegex("[abc]");
            assertThat(result).contains("Any character in: abc");
        }

        @Test
        void characterClass_negated() {
            String result = tool.explainRegex("[^abc]");
            assertThat(result).contains("Any character NOT in: abc");
        }

        @Test
        void characterClass_range() {
            String result = tool.explainRegex("[a-z]");
            assertThat(result).contains("Any character in: a-z");
        }

        @Test
        void characterClass_withEscapedBracket() {
            String result = tool.explainRegex("[\\]]");
            assertThat(result).contains("Any character in:");
        }

        @Test
        void characterClass_unclosed_fallsThrough() {
            // findClosingBracket returns -1 when ] is never found
            // This just increments pos (edge case)
            String result = tool.explainRegex("[abc");
            assertThat(result).contains("Breakdown:");
        }

        // ── Quantifiers with braces ─────────────────────────────────────────

        @Test
        void exactQuantifier() {
            String result = tool.explainRegex("a{3}");
            assertThat(result).contains("Exactly 3 of the preceding");
        }

        @Test
        void rangeQuantifier() {
            String result = tool.explainRegex("a{2,5}");
            assertThat(result).contains("Between 2 and 5 of the preceding");
        }

        @Test
        void minQuantifier() {
            String result = tool.explainRegex("a{2,}");
            assertThat(result).contains("At least 2 of the preceding");
        }

        @Test
        void unclosedBrace_fallsThrough() {
            // { without matching } - edge case
            String result = tool.explainRegex("a{");
            assertThat(result).contains("Breakdown:");
        }

        // ── Escape sequences ────────────────────────────────────────────────

        @Test
        void escape_digit() {
            String result = tool.explainRegex("\\d");
            assertThat(result).contains("Digit [0-9]");
        }

        @Test
        void escape_nonDigit() {
            String result = tool.explainRegex("\\D");
            assertThat(result).contains("Non-digit [^0-9]");
        }

        @Test
        void escape_word() {
            String result = tool.explainRegex("\\w");
            assertThat(result).contains("Word character [a-zA-Z0-9_]");
        }

        @Test
        void escape_nonWord() {
            String result = tool.explainRegex("\\W");
            assertThat(result).contains("Non-word character");
        }

        @Test
        void escape_whitespace() {
            String result = tool.explainRegex("\\s");
            assertThat(result).contains("Whitespace");
        }

        @Test
        void escape_nonWhitespace() {
            String result = tool.explainRegex("\\S");
            assertThat(result).contains("Non-whitespace");
        }

        @Test
        void escape_wordBoundary() {
            String result = tool.explainRegex("\\b");
            assertThat(result).contains("Word boundary");
        }

        @Test
        void escape_nonWordBoundary() {
            String result = tool.explainRegex("\\B");
            assertThat(result).contains("Non-word boundary");
        }

        @Test
        void escape_newline() {
            String result = tool.explainRegex("\\n");
            assertThat(result).contains("Newline");
        }

        @Test
        void escape_carriageReturn() {
            String result = tool.explainRegex("\\r");
            assertThat(result).contains("Carriage return");
        }

        @Test
        void escape_tab() {
            String result = tool.explainRegex("\\t");
            assertThat(result).contains("Tab");
        }

        @Test
        void escape_backslash() {
            String result = tool.explainRegex("\\\\");
            assertThat(result).contains("Literal backslash");
        }

        @Test
        void escape_dot() {
            String result = tool.explainRegex("\\.");
            assertThat(result).contains("Literal dot");
        }

        @Test
        void escape_asterisk() {
            String result = tool.explainRegex("\\*");
            assertThat(result).contains("Literal asterisk");
        }

        @Test
        void escape_plus() {
            String result = tool.explainRegex("\\+");
            assertThat(result).contains("Literal plus");
        }

        @Test
        void escape_questionMark() {
            String result = tool.explainRegex("\\?");
            assertThat(result).contains("Literal question mark");
        }

        @Test
        void escape_openParen() {
            String result = tool.explainRegex("\\(");
            assertThat(result).contains("Literal open parenthesis");
        }

        @Test
        void escape_closeParen() {
            String result = tool.explainRegex("\\)");
            assertThat(result).contains("Literal close parenthesis");
        }

        @Test
        void escape_openBracket() {
            String result = tool.explainRegex("\\[");
            assertThat(result).contains("Literal open bracket");
        }

        @Test
        void escape_closeBracket() {
            String result = tool.explainRegex("\\]");
            assertThat(result).contains("Literal close bracket");
        }

        @Test
        void escape_openBrace() {
            String result = tool.explainRegex("\\{");
            assertThat(result).contains("Literal open brace");
        }

        @Test
        void escape_closeBrace() {
            String result = tool.explainRegex("\\}");
            assertThat(result).contains("Literal close brace");
        }

        @Test
        void escape_caret() {
            String result = tool.explainRegex("\\^");
            assertThat(result).contains("Literal caret");
        }

        @Test
        void escape_dollar() {
            String result = tool.explainRegex("\\$");
            assertThat(result).contains("Literal dollar sign");
        }

        @Test
        void escape_pipe() {
            String result = tool.explainRegex("\\|");
            assertThat(result).contains("Literal pipe");
        }

        @Test
        void escape_unknownChar() {
            // Any character not in the known escape list
            String result = tool.explainRegex("\\x");
            assertThat(result).contains("Escaped 'x'");
        }

        // ── Literal characters ──────────────────────────────────────────────

        @Test
        void literal_characters() {
            String result = tool.explainRegex("abc");
            assertThat(result)
                    .contains("Literal 'a'")
                    .contains("Literal 'b'")
                    .contains("Literal 'c'");
        }

        // ── Complex patterns ────────────────────────────────────────────────

        @Test
        void complexPattern_emailLike() {
            String result = tool.explainRegex("^[\\w.]+@[\\w]+\\.[a-z]{2,4}$");
            assertThat(result)
                    .contains("Start of string/line")
                    .contains("End of string/line")
                    .contains("One or more")
                    .contains("Literal dot")
                    .contains("Between 2 and 4");
        }

        @Test
        void complexPattern_phoneNumber() {
            String result = tool.explainRegex("^\\d{3}-\\d{3}-\\d{4}$");
            assertThat(result)
                    .contains("Digit [0-9]")
                    .contains("Exactly 3")
                    .contains("Exactly 4")
                    .contains("Literal '-'");
        }

        // ── padRight ────────────────────────────────────────────────────────

        @Test
        void padRight_longString_getsOneExtraSpace() {
            // When character class is >= 12 chars, padRight adds one space
            String result = tool.explainRegex("[a-zA-Z0-9_]");
            assertThat(result).contains("Any character in: a-zA-Z0-9_");
        }
    }

    // =========================================================================
    // Inner class tests: MatchInfo and GroupInfo
    // =========================================================================

    @Nested
    class MatchInfoAndGroupInfo {

        @Test
        void matchInfo_format_showsFullMatchAndPosition() {
            // Verified through testRegex output
            String result = tool.testRegex("(hello) (world)", "hello world", null);
            assertThat(result)
                    .contains("Full match: 'hello world'")
                    .contains("position 0-11")
                    .contains("Group 1: 'hello'")
                    .contains("Group 2: 'world'");
        }

        @Test
        void matchInfo_format_withIndent() {
            // In findAll mode, matches are formatted with "  " indent
            String result = tool.testRegex("(\\w+)", "hello world", "g");
            assertThat(result)
                    .contains("Match 1:")
                    .contains("Full match: 'hello'")
                    .contains("Match 2:")
                    .contains("Full match: 'world'");
        }

        @Test
        void groupInfo_nullValue_showsNotCaptured() {
            // Optional group that doesn't match
            String result = tool.testRegex("(a)(b)?", "a", null);
            assertThat(result)
                    .contains("Group 1: 'a'")
                    .contains("Group 2: '<not captured>'");
        }
    }
}
