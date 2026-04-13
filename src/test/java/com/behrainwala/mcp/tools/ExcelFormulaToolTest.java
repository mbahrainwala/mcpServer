package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelFormulaToolTest {

    private ExcelFormulaTool tool;

    @BeforeEach
    void setUp() {
        tool = new ExcelFormulaTool();
    }

    // ── functionLookup ──

    @Nested
    class FunctionLookupTests {

        // Direct function lookup (exact match in FUNCTIONS map)

        @ParameterizedTest
        @ValueSource(strings = {
                "VLOOKUP", "HLOOKUP", "XLOOKUP", "INDEX", "MATCH",
                "OFFSET", "INDIRECT", "CHOOSE",
                "SUMIF", "SUMIFS", "COUNTIF", "COUNTIFS",
                "AVERAGEIF", "AVERAGEIFS", "SUMPRODUCT",
                "ROUND", "ROUNDUP", "ROUNDDOWN", "MOD", "ABS",
                "INT", "CEILING", "FLOOR", "RAND", "RANDBETWEEN",
                "CONCATENATE", "TEXTJOIN", "LEFT", "RIGHT", "MID",
                "LEN", "TRIM", "CLEAN", "SUBSTITUTE", "REPLACE",
                "FIND", "SEARCH", "TEXT", "VALUE", "UPPER", "LOWER", "PROPER",
                "TODAY", "NOW", "DATE", "YEAR", "MONTH", "DAY",
                "EDATE", "EOMONTH", "DATEDIF", "NETWORKDAYS", "WORKDAY", "WEEKDAY",
                "IF", "IFS", "IFERROR", "IFNA", "AND", "OR", "NOT", "SWITCH",
                "AVERAGE", "MEDIAN", "MODE", "STDEV", "VAR",
                "PERCENTILE", "RANK", "LARGE", "SMALL", "CORREL", "FORECAST",
                "FILTER", "SORT", "UNIQUE", "SEQUENCE", "LET", "LAMBDA"
        })
        void directLookup_allFunctions(String funcName) {
            String result = tool.functionLookup(funcName);
            assertThat(result)
                    .contains("Excel: " + funcName)
                    .contains("Category:")
                    .contains("Returns:")
                    .contains("SYNTAX")
                    .contains("DESCRIPTION")
                    .contains("EXAMPLES");
        }

        @Test
        void directLookup_caseInsensitive() {
            String result = tool.functionLookup("vlookup");
            assertThat(result).contains("Excel: VLOOKUP");
        }

        @Test
        void directLookup_withWhitespace() {
            String result = tool.functionLookup("  SUMIF  ");
            assertThat(result).contains("Excel: SUMIF");
        }

        @Test
        void directLookup_includesNotes_whenPresent() {
            String result = tool.functionLookup("VLOOKUP");
            assertThat(result).contains("NOTES");
        }

        @Test
        void directLookup_noNotes_whenNull() {
            String result = tool.functionLookup("CHOOSE");
            assertThat(result).doesNotContain("NOTES");
        }

        @Test
        void directLookup_formatsEqualsRepeatingLine() {
            String result = tool.functionLookup("IF");
            assertThat(result).contains("\u2550");
        }

        // Category search -- use categories that are NOT also function names

        @ParameterizedTest
        @ValueSource(strings = {"lookup", "math", "logical", "statistical", "dynamic"})
        void categorySearch_returnsResults(String category) {
            String result = tool.functionLookup(category);
            assertThat(result)
                    .contains("Excel Functions")
                    .contains("functions)")
                    .contains("Use excel_function_lookup with a specific function name for full details.");
        }

        @Test
        void categorySearch_textHitsDirectLookup() {
            // "TEXT" is a function name so direct lookup wins
            String result = tool.functionLookup("text");
            assertThat(result).contains("Excel: TEXT");
        }

        @Test
        void categorySearch_dateHitsDirectLookup() {
            // "DATE" is a function name so direct lookup wins
            String result = tool.functionLookup("date");
            assertThat(result).contains("Excel: DATE");
        }

        @Test
        void categorySearch_truncatesLongDescriptions() {
            String result = tool.functionLookup("lookup");
            assertThat(result).contains("...");
        }

        @Test
        void categorySearch_showsCount() {
            String result = tool.functionLookup("logical");
            assertThat(result).containsPattern("\\(\\d+ functions\\)");
        }

        @Test
        void categorySearch_shortDescriptionNotTruncated() {
            String result = tool.functionLookup("logical");
            assertThat(result).contains("Reverse a boolean.");
        }

        // Keyword search

        @Test
        void keywordSearch_singleKeyword() {
            String result = tool.functionLookup("volatile");
            assertThat(result)
                    .contains("Search results for")
                    .contains("matches)");
        }

        @Test
        void keywordSearch_multipleKeywords() {
            String result = tool.functionLookup("round down");
            assertThat(result).contains("Search results for");
        }

        @Test
        void keywordSearch_matchesNotes() {
            String result = tool.functionLookup("even rows");
            assertThat(result).contains("MOD");
        }

        // No match

        @Test
        void noMatch_returnsHelpText() {
            String result = tool.functionLookup("xyznonexistent12345");
            assertThat(result)
                    .contains("No function found for")
                    .contains("Try:");
        }
    }

    // ── explainFormula ──

    @Nested
    class ExplainFormulaTests {

        @Test
        void simpleFormula_withLeadingEquals() {
            String result = tool.explainFormula("=SUM(A1:A10)");
            assertThat(result)
                    .contains("Formula Explanation")
                    .contains("Formula: =SUM(A1:A10)")
                    .contains("FUNCTIONS USED")
                    .contains("CELL REFERENCES")
                    .contains("STEP-BY-STEP BREAKDOWN");
        }

        @Test
        void simpleFormula_withoutLeadingEquals() {
            String result = tool.explainFormula("SUM(A1:A10)");
            assertThat(result).contains("Formula: =SUM(A1:A10)");
        }

        @Test
        void formulaWithWhitespace() {
            String result = tool.explainFormula("  =SUM(A1:A10)  ");
            assertThat(result).contains("Formula: =SUM(A1:A10)");
        }

        @Test
        void knownFunction_showsDescription() {
            String result = tool.explainFormula("=VLOOKUP(A1,B1:D10,3,FALSE)");
            assertThat(result).containsIgnoringCase("Search first column");
        }

        @Test
        void unknownFunction_showsCustomLabel() {
            String result = tool.explainFormula("=MYFUNCTION(A1)");
            assertThat(result).contains("(custom or less common function)");
        }

        @Test
        void multipleFunctions_deduplicated() {
            String result = tool.explainFormula("=SUM(A1:A5)+SUM(B1:B5)");
            long count = result.lines()
                    .filter(l -> l.trim().startsWith("SUM:"))
                    .count();
            assertThat(count).isEqualTo(1);
        }

        @Test
        void nestedFunctions_extractsBoth() {
            String result = tool.explainFormula("=IF(VLOOKUP(A1,B:D,3,0)>100,\"High\",\"Low\")");
            assertThat(result).contains("IF:").contains("VLOOKUP:");
        }

        // Cell references

        @Test
        void cellReference_relative() {
            String result = tool.explainFormula("=A1+B2");
            assertThat(result).contains("Relative reference");
        }

        @Test
        void cellReference_absoluteBoth() {
            String result = tool.explainFormula("=$A$1");
            assertThat(result).contains("Absolute reference (locked when copying)");
        }

        @Test
        void cellReference_absColRelRow() {
            String result = tool.explainFormula("=$A1");
            assertThat(result).contains("Column locked, row relative");
        }

        @Test
        void cellReference_relColAbsRow() {
            String result = tool.explainFormula("=A$1");
            assertThat(result).contains("Row locked, column relative");
        }

        @Test
        void cellReference_entireColumn() {
            String result = tool.explainFormula("=SUM(A:A)");
            assertThat(result).contains("Entire column(s)");
        }

        @Test
        void cellReference_range() {
            String result = tool.explainFormula("=SUM(A1:B10)");
            assertThat(result).contains("Range A1:B10");
        }

        @Test
        void cellReference_crossSheet() {
            String result = tool.explainFormula("=SUM(Sheet2!A1:A10)");
            assertThat(result).contains("Reference to another sheet");
        }

        // Breakdown

        @Test
        void breakdown_noFunction_simpleExpression() {
            String result = tool.explainFormula("=A1+B1");
            assertThat(result).contains("Simple expression:");
        }

        @Test
        void breakdown_nestedFunctions_stepsNumbered() {
            String result = tool.explainFormula("=IF(SUM(A1:A10)>0,\"Yes\",\"No\")");
            assertThat(result).contains("1. IF(...)");
        }

        @Test
        void breakdown_unknownFunctionInStep() {
            String result = tool.explainFormula("=SUM(A1:A10)");
            assertThat(result).contains("evaluates SUM");
        }

        @Test
        void breakdown_knownFunctionInStep() {
            String result = tool.explainFormula("=VLOOKUP(A1,B:D,3,0)");
            assertThat(result).contains("Search first column");
        }

        // Potential issues / warnings

        @Test
        void issues_vlookupWithoutFalse() {
            String result = tool.explainFormula("=VLOOKUP(A1,B:D,3,TRUE)");
            assertThat(result).contains("POTENTIAL ISSUES");
            assertThat(result).contains("may return approximate matches");
            assertThat(result).contains("Consider XLOOKUP or INDEX/MATCH");
        }

        @Test
        void issues_vlookupWithFalse_noApproxWarning() {
            String result = tool.explainFormula("=VLOOKUP(A1,B:D,3,FALSE)");
            assertThat(result).doesNotContain("may return approximate matches");
            assertThat(result).contains("Consider XLOOKUP or INDEX/MATCH");
        }

        @Test
        void issues_vlookupWithZero_noApproxWarning() {
            String result = tool.explainFormula("=VLOOKUP(A1,B:D,3,0)");
            assertThat(result).doesNotContain("may return approximate matches");
        }

        @Test
        void issues_volatileFunction_offset() {
            String result = tool.explainFormula("=OFFSET(A1,2,3)");
            assertThat(result).contains("volatile function");
        }

        @Test
        void issues_volatileFunction_indirect() {
            String result = tool.explainFormula("=INDIRECT(\"A1\")");
            assertThat(result).contains("volatile function");
        }

        @Test
        void issues_volatileFunction_now() {
            String result = tool.explainFormula("=NOW()");
            assertThat(result).contains("volatile function");
        }

        @Test
        void issues_volatileFunction_today() {
            String result = tool.explainFormula("=TODAY()");
            assertThat(result).contains("volatile function");
        }

        @Test
        void issues_volatileFunction_rand() {
            String result = tool.explainFormula("=RAND()");
            assertThat(result).contains("volatile function");
        }

        @Test
        void issues_unbalancedParentheses() {
            String result = tool.explainFormula("=IF(SUM(A1:A10)>0,\"Yes\"");
            assertThat(result).contains("Unbalanced parentheses");
        }

        @Test
        void issues_balancedParentheses_noWarning() {
            String result = tool.explainFormula("=SUM(A1:A10)");
            assertThat(result).doesNotContain("Unbalanced parentheses");
        }

        @Test
        void noIssues_noPotentialIssuesSection() {
            String result = tool.explainFormula("=A1+B1");
            assertThat(result).doesNotContain("POTENTIAL ISSUES");
        }
    }

    // ── buildFormula ──

    @Nested
    class BuildFormulaTests {

        private void assertHeader(String result, String description) {
            assertThat(result)
                    .contains("Formula Builder")
                    .contains("Request: " + description);
        }

        // SUMIF (single condition)

        @Test
        void sumif_whereKeyword() {
            String desc = "sum column B where column A is Apple";
            String result = tool.buildFormula(desc);
            assertHeader(result, desc);
            assertThat(result).contains("SUMIF(criteria_range, criteria, sum_range)");
        }

        @Test
        void sumif_ifKeyword() {
            String result = tool.buildFormula("sum if value is greater than 10");
            assertThat(result).contains("SUMIF");
        }

        @Test
        void sumif_conditionKeyword() {
            String result = tool.buildFormula("sum with condition on column A");
            assertThat(result).contains("SUMIF");
        }

        @Test
        void sumif_criteriaKeyword() {
            String result = tool.buildFormula("sum using criteria from column B");
            assertThat(result).contains("SUMIF");
        }

        // SUMIFS (multiple conditions)

        @Test
        void sumifs_multipleKeyword() {
            String result = tool.buildFormula("sum where multiple conditions apply");
            assertThat(result).contains("SUMIFS(sum_range, criteria_range1, criteria1, criteria_range2, criteria2)");
        }

        @Test
        void sumifs_andKeyword() {
            String result = tool.buildFormula("sum where A is Apple and B > 10");
            assertThat(result).contains("SUMIFS");
        }

        @Test
        void sumifs_conditionsKeyword() {
            String result = tool.buildFormula("sum if conditions are met for multiple columns");
            assertThat(result).contains("SUMIFS");
        }

        // Count unique

        @Test
        void countUnique() {
            String result = tool.buildFormula("count unique values in column A");
            assertThat(result)
                    .contains("COUNTA(UNIQUE(range))")
                    .contains("SUMPRODUCT");
        }

        // Last non-empty

        @Test
        void lastNonEmpty() {
            String result = tool.buildFormula("find the last non-empty cell in column A");
            assertThat(result).contains("LOOKUP(2,1/(range<>\"\"),range)");
        }

        @Test
        void lastNonEmpty_withSpace() {
            String result = tool.buildFormula("get the last non empty value in column B");
            assertThat(result).contains("LOOKUP(2,1/(range<>\"\"),range)");
        }

        @Test
        void lastValue() {
            String result = tool.buildFormula("find the last value in column A");
            assertThat(result).contains("LOOKUP");
        }

        // Percentage change

        @Test
        void percentageChange() {
            String result = tool.buildFormula("calculate percentage change between B1 and B2");
            assertThat(result)
                    .contains("(new_value - old_value) / old_value")
                    .contains("IFERROR");
        }

        // VLOOKUP / lookup return

        @Test
        void vlookup_keyword() {
            String result = tool.buildFormula("vlookup a value from another sheet");
            assertThat(result)
                    .contains("XLOOKUP")
                    .contains("INDEX/MATCH")
                    .contains("VLOOKUP");
        }

        @Test
        void lookup_return_keyword() {
            String result = tool.buildFormula("lookup a value and return the result from column C");
            assertThat(result)
                    .contains("XLOOKUP")
                    .contains("INDEX/MATCH");
        }

        // Duplicates

        @Test
        void findDuplicates() {
            String result = tool.buildFormula("find duplicates in column A");
            assertThat(result)
                    .contains("FIND DUPLICATES")
                    .contains("COUNTIF(A:A, A1) > 1")
                    .contains("MARK FIRST OCCURRENCE")
                    .contains("EXTRACT UNIQUE")
                    .contains("REMOVE DUPLICATES")
                    .contains("CONDITIONAL FORMATTING");
        }

        @Test
        void highlightDuplicates() {
            String result = tool.buildFormula("highlight duplicates");
            assertThat(result).contains("FIND DUPLICATES");
        }

        @Test
        void removeDuplicates() {
            String result = tool.buildFormula("remove duplicate values");
            assertThat(result).contains("REMOVE DUPLICATES");
        }

        // Concatenate / combine / join / merge text

        @Test
        void concatenate() {
            String result = tool.buildFormula("concatenate two columns");
            assertThat(result)
                    .contains("JOIN TEXT")
                    .contains("TEXTJOIN")
                    .contains("CONCAT");
        }

        @Test
        void combine() {
            String result = tool.buildFormula("combine first and last name");
            assertThat(result).contains("JOIN TEXT");
        }

        @Test
        void joinText() {
            String result = tool.buildFormula("join values with comma");
            assertThat(result).contains("TEXTJOIN");
        }

        @Test
        void mergeText() {
            String result = tool.buildFormula("merge text from two cells");
            assertThat(result).contains("JOIN TEXT");
        }

        // Rank / top / bottom

        @Test
        void rankValues() {
            String result = tool.buildFormula("rank employees by sales");
            assertThat(result)
                    .contains("RANK")
                    .contains("TOP N VALUES")
                    .contains("LARGE")
                    .contains("BOTTOM N VALUES")
                    .contains("SMALL");
        }

        @Test
        void topN() {
            String result = tool.buildFormula("find top 5 values");
            assertThat(result).contains("TOP N VALUES");
        }

        @Test
        void bottomN() {
            String result = tool.buildFormula("find bottom 3 values");
            assertThat(result).contains("BOTTOM N VALUES");
        }

        // Date difference

        @Test
        void dateDifference() {
            String result = tool.buildFormula("date difference between two cells");
            assertThat(result)
                    .contains("DATE DIFFERENCE")
                    .contains("DATEDIF")
                    .contains("NETWORKDAYS")
                    .contains("AGE CALCULATION");
        }

        @Test
        void dateBetween() {
            String result = tool.buildFormula("how many days between two dates");
            assertThat(result).contains("DATE DIFFERENCE");
        }

        // Dynamic dropdown

        @Test
        void dynamicDropdown() {
            String result = tool.buildFormula("create a dynamic dropdown list");
            assertThat(result)
                    .contains("DYNAMIC DROPDOWN")
                    .contains("Named range approach")
                    .contains("Table approach")
                    .contains("UNIQUE dependent dropdown")
                    .contains("Data Validation");
        }

        @Test
        void dynamicValidation() {
            String result = tool.buildFormula("dynamic data validation list");
            assertThat(result).contains("DYNAMIC DROPDOWN");
        }

        // Default / fallback

        @Test
        void defaultFallback_unknownDescription() {
            String result = tool.buildFormula("something completely unrelated to any pattern");
            assertThat(result)
                    .contains("I can help build formulas for common tasks")
                    .contains("LOOKUP:")
                    .contains("SUM IF:")
                    .contains("COUNT IF:")
                    .contains("NESTED IF:")
                    .contains("ERROR:")
                    .contains("TEXT:")
                    .contains("DATE:")
                    .contains("Try describing your specific need");
        }
    }

    // ── Helper method edge-case coverage ──

    @Nested
    class HelperMethodTests {

        @Test
        void extractFunctions_noFunctions() {
            String result = tool.explainFormula("=A1+B1*2");
            assertThat(result).contains("FUNCTIONS USED (0)");
        }

        @Test
        void extractFunctions_dotInFunctionName() {
            String result = tool.explainFormula("=STDEV.S(A1:A100)");
            assertThat(result).contains("STDEV.S");
        }

        @Test
        void extractReferences_noReferences() {
            String result = tool.explainFormula("=1+2");
            assertThat(result).contains("CELL REFERENCES");
        }

        @Test
        void extractReferences_multiColumnRange() {
            String result = tool.explainFormula("=SUM(A:B)");
            assertThat(result).contains("Entire column(s)");
        }

        @Test
        void breakdownFormula_deeplyNested() {
            String result = tool.explainFormula("=IF(AND(A1>0,B1<10),\"Yes\",\"No\")");
            assertThat(result).contains("1. IF(...)");
        }

        @Test
        void breakdownFormula_functionNamePrecededByOperator() {
            String result = tool.explainFormula("=ROUND(SUM(A1:A10),2)");
            assertThat(result).contains("ROUND(...)");
        }

        @Test
        void checkForIssues_multipleIssues() {
            // VLOOKUP with TRUE (no FALSE/0) + INDIRECT (volatile) => all three warnings
            String result = tool.explainFormula("=VLOOKUP(INDIRECT(\"A1\"),B:D,3,TRUE)");
            assertThat(result)
                    .contains("may return approximate matches")
                    .contains("volatile function")
                    .contains("Consider XLOOKUP");
        }

        @Test
        void checkForIssues_noIssues() {
            String result = tool.explainFormula("=SUM(A1:A10)");
            assertThat(result).doesNotContain("POTENTIAL ISSUES");
        }
    }
}
