package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * MCP tool for building, explaining, and debugging Excel formulas.
 */
@Service
public class ExcelFormulaTool {

    // ── Comprehensive Excel function reference ──
    private static final Map<String, FunctionInfo> FUNCTIONS = new LinkedHashMap<>();

    record FunctionInfo(String name, String category, String syntax, String description,
                        String returnType, List<String> examples, String notes) {}

    static {
        // Lookup & Reference
        fn("VLOOKUP", "Lookup", "VLOOKUP(lookup_value, table_array, col_index_num, [range_lookup])",
                "Search first column of a range and return a value from another column in the same row.",
                "Value", List.of("=VLOOKUP(\"Apple\",A1:C10,3,FALSE)", "=VLOOKUP(B2,Products!A:D,2,0)"),
                "Use FALSE/0 for exact match. Limited to left-to-right lookup. Consider INDEX/MATCH or XLOOKUP instead.");
        fn("HLOOKUP", "Lookup", "HLOOKUP(lookup_value, table_array, row_index_num, [range_lookup])",
                "Search first row of a range and return a value from another row.", "Value",
                List.of("=HLOOKUP(\"Q1\",A1:D5,3,FALSE)"), "Horizontal version of VLOOKUP.");
        fn("XLOOKUP", "Lookup", "XLOOKUP(lookup_value, lookup_array, return_array, [if_not_found], [match_mode], [search_mode])",
                "Modern replacement for VLOOKUP/HLOOKUP. Searches any direction, supports wildcards and binary search.",
                "Value/Array", List.of("=XLOOKUP(D2,A:A,B:B,\"Not Found\")", "=XLOOKUP(\"*phone*\",A:A,B:B,,2)"),
                "Excel 365/2021+. Replaces VLOOKUP, HLOOKUP, and INDEX/MATCH in most cases.");
        fn("INDEX", "Lookup", "INDEX(array, row_num, [col_num])",
                "Return the value at a given row and column in a range.", "Value",
                List.of("=INDEX(A1:C10,3,2)", "=INDEX(B:B,MATCH(D2,A:A,0))"),
                "Most powerful when combined with MATCH.");
        fn("MATCH", "Lookup", "MATCH(lookup_value, lookup_array, [match_type])",
                "Return the relative position of a value in a range.", "Number",
                List.of("=MATCH(\"Apple\",A:A,0)", "=MATCH(MAX(B:B),B:B,0)"),
                "Use 0 for exact match, 1 for less-than, -1 for greater-than.");
        fn("OFFSET", "Lookup", "OFFSET(reference, rows, cols, [height], [width])",
                "Return a reference offset from a starting cell.", "Reference",
                List.of("=OFFSET(A1,2,3)", "=SUM(OFFSET(A1,0,0,5,1))"),
                "Dynamic reference. Volatile function — recalculates every time.");
        fn("INDIRECT", "Lookup", "INDIRECT(ref_text, [a1])",
                "Return the value of a cell reference specified by text string.", "Value",
                List.of("=INDIRECT(\"A\"&B1)", "=SUM(INDIRECT(\"Sheet2!A1:A10\"))"),
                "Useful for dynamic sheet/range references. Volatile.");
        fn("CHOOSE", "Lookup", "CHOOSE(index_num, value1, value2, ...)",
                "Return a value from a list based on an index number.", "Value",
                List.of("=CHOOSE(2,\"Red\",\"Blue\",\"Green\")", "=CHOOSE(MONTH(A1),\"Jan\",\"Feb\",...)"), null);

        // Math & Stats
        fn("SUMIF", "Math", "SUMIF(range, criteria, [sum_range])",
                "Sum cells that meet a single condition.", "Number",
                List.of("=SUMIF(A:A,\"Apple\",B:B)", "=SUMIF(C:C,\">100\")", "=SUMIF(A:A,\"*fruit*\",B:B)"),
                "Use SUMIFS for multiple conditions.");
        fn("SUMIFS", "Math", "SUMIFS(sum_range, criteria_range1, criteria1, [criteria_range2, criteria2], ...)",
                "Sum cells that meet multiple conditions.", "Number",
                List.of("=SUMIFS(D:D,A:A,\"Apple\",B:B,\">10\")", "=SUMIFS(E:E,C:C,\">=\"&DATE(2024,1,1),C:C,\"<\"&DATE(2025,1,1))"),
                "Note: sum_range is the FIRST argument (unlike SUMIF where it's last).");
        fn("COUNTIF", "Math", "COUNTIF(range, criteria)",
                "Count cells that meet a condition.", "Number",
                List.of("=COUNTIF(A:A,\"Apple\")", "=COUNTIF(B:B,\">0\")", "=COUNTIF(A:A,\"*\")"), null);
        fn("COUNTIFS", "Math", "COUNTIFS(criteria_range1, criteria1, [criteria_range2, criteria2], ...)",
                "Count cells that meet multiple conditions.", "Number",
                List.of("=COUNTIFS(A:A,\"Apple\",B:B,\">10\")"), null);
        fn("AVERAGEIF", "Math", "AVERAGEIF(range, criteria, [average_range])",
                "Average cells that meet a condition.", "Number",
                List.of("=AVERAGEIF(A:A,\"Apple\",B:B)"), null);
        fn("AVERAGEIFS", "Math", "AVERAGEIFS(average_range, criteria_range1, criteria1, ...)",
                "Average cells that meet multiple conditions.", "Number",
                List.of("=AVERAGEIFS(D:D,A:A,\"Apple\",B:B,\">5\")"), null);
        fn("SUMPRODUCT", "Math", "SUMPRODUCT(array1, [array2], ...)",
                "Multiply corresponding elements and return the sum. Extremely versatile for complex criteria.",
                "Number",
                List.of("=SUMPRODUCT(A1:A10,B1:B10)", "=SUMPRODUCT((A1:A10=\"Apple\")*(B1:B10>5)*C1:C10)"),
                "Can replace SUMIFS for complex conditions. Array-aware without Ctrl+Shift+Enter.");
        fn("ROUND", "Math", "ROUND(number, num_digits)", "Round to specified decimal places.", "Number",
                List.of("=ROUND(3.14159,2)", "=ROUND(A1,-2)"), "Use negative num_digits to round to tens, hundreds, etc.");
        fn("ROUNDUP", "Math", "ROUNDUP(number, num_digits)", "Always round up (away from zero).", "Number",
                List.of("=ROUNDUP(3.141,2)"), null);
        fn("ROUNDDOWN", "Math", "ROUNDDOWN(number, num_digits)", "Always round down (toward zero).", "Number",
                List.of("=ROUNDDOWN(3.149,2)"), null);
        fn("MOD", "Math", "MOD(number, divisor)", "Return remainder after division.", "Number",
                List.of("=MOD(10,3)", "=MOD(ROW(),2)"), "MOD(ROW(),2)=0 for even rows — useful in conditional formatting.");
        fn("ABS", "Math", "ABS(number)", "Return absolute value.", "Number", List.of("=ABS(-5)"), null);
        fn("INT", "Math", "INT(number)", "Round down to the nearest integer.", "Number", List.of("=INT(3.9)"), null);
        fn("CEILING", "Math", "CEILING(number, significance)", "Round up to nearest multiple.", "Number",
                List.of("=CEILING(23,5)"), "CEILING(23,5) = 25");
        fn("FLOOR", "Math", "FLOOR(number, significance)", "Round down to nearest multiple.", "Number",
                List.of("=FLOOR(23,5)"), "FLOOR(23,5) = 20");
        fn("RAND", "Math", "RAND()", "Random number between 0 and 1. Volatile.", "Number",
                List.of("=RAND()", "=INT(RAND()*100)+1"), null);
        fn("RANDBETWEEN", "Math", "RANDBETWEEN(bottom, top)", "Random integer between two values.", "Number",
                List.of("=RANDBETWEEN(1,100)"), null);

        // Text
        fn("CONCATENATE", "Text", "CONCATENATE(text1, text2, ...) or text1 & text2",
                "Join text strings. Modern: CONCAT() or TEXTJOIN().", "Text",
                List.of("=A1&\" \"&B1", "=CONCAT(A1:A10)", "=TEXTJOIN(\", \",TRUE,A1:A10)"), null);
        fn("TEXTJOIN", "Text", "TEXTJOIN(delimiter, ignore_empty, text1, ...)",
                "Join text with a delimiter. Excel 2019+.", "Text",
                List.of("=TEXTJOIN(\", \",TRUE,A1:A10)"), null);
        fn("LEFT", "Text", "LEFT(text, num_chars)", "Extract characters from the left.", "Text",
                List.of("=LEFT(A1,3)"), null);
        fn("RIGHT", "Text", "RIGHT(text, num_chars)", "Extract characters from the right.", "Text",
                List.of("=RIGHT(A1,4)"), null);
        fn("MID", "Text", "MID(text, start_num, num_chars)", "Extract characters from the middle.", "Text",
                List.of("=MID(A1,3,5)"), null);
        fn("LEN", "Text", "LEN(text)", "Return the length of a text string.", "Number",
                List.of("=LEN(A1)"), null);
        fn("TRIM", "Text", "TRIM(text)", "Remove extra spaces (leading, trailing, and double spaces).", "Text",
                List.of("=TRIM(A1)"), null);
        fn("CLEAN", "Text", "CLEAN(text)", "Remove non-printable characters.", "Text",
                List.of("=CLEAN(A1)"), null);
        fn("SUBSTITUTE", "Text", "SUBSTITUTE(text, old_text, new_text, [instance_num])",
                "Replace occurrences of text.", "Text",
                List.of("=SUBSTITUTE(A1,\" \",\"_\")", "=SUBSTITUTE(A1,\"-\",\"\")"), null);
        fn("REPLACE", "Text", "REPLACE(old_text, start_num, num_chars, new_text)",
                "Replace characters by position.", "Text", List.of("=REPLACE(A1,1,3,\"XYZ\")"), null);
        fn("FIND", "Text", "FIND(find_text, within_text, [start_num])",
                "Find position of text (case-sensitive).", "Number",
                List.of("=FIND(\"@\",A1)"), "Case-sensitive. Use SEARCH for case-insensitive.");
        fn("SEARCH", "Text", "SEARCH(find_text, within_text, [start_num])",
                "Find position of text (case-insensitive, supports wildcards).", "Number",
                List.of("=SEARCH(\"apple\",A1)"), null);
        fn("TEXT", "Text", "TEXT(value, format_text)", "Format a number/date as text.", "Text",
                List.of("=TEXT(TODAY(),\"YYYY-MM-DD\")", "=TEXT(0.15,\"0.0%\")", "=TEXT(1234.5,\"$#,##0.00\")"), null);
        fn("VALUE", "Text", "VALUE(text)", "Convert text that looks like a number to a number.", "Number",
                List.of("=VALUE(\"123\")"), null);
        fn("UPPER", "Text", "UPPER(text)", "Convert to uppercase.", "Text", List.of("=UPPER(A1)"), null);
        fn("LOWER", "Text", "LOWER(text)", "Convert to lowercase.", "Text", List.of("=LOWER(A1)"), null);
        fn("PROPER", "Text", "PROPER(text)", "Capitalize first letter of each word.", "Text",
                List.of("=PROPER(\"john smith\")"), null);

        // Date & Time
        fn("TODAY", "Date", "TODAY()", "Current date (no time). Volatile.", "Date", List.of("=TODAY()"), null);
        fn("NOW", "Date", "NOW()", "Current date and time. Volatile.", "DateTime", List.of("=NOW()"), null);
        fn("DATE", "Date", "DATE(year, month, day)", "Create a date from components.", "Date",
                List.of("=DATE(2024,3,15)", "=DATE(YEAR(A1),MONTH(A1)+1,1)-1"), null);
        fn("YEAR", "Date", "YEAR(serial_number)", "Extract year.", "Number", List.of("=YEAR(A1)"), null);
        fn("MONTH", "Date", "MONTH(serial_number)", "Extract month.", "Number", List.of("=MONTH(A1)"), null);
        fn("DAY", "Date", "DAY(serial_number)", "Extract day.", "Number", List.of("=DAY(A1)"), null);
        fn("EDATE", "Date", "EDATE(start_date, months)", "Add months to a date.", "Date",
                List.of("=EDATE(A1,3)", "=EDATE(TODAY(),-6)"), null);
        fn("EOMONTH", "Date", "EOMONTH(start_date, months)", "Last day of a month, offset by months.", "Date",
                List.of("=EOMONTH(A1,0)", "=EOMONTH(TODAY(),1)"), "EOMONTH(A1,0) gives last day of same month.");
        fn("DATEDIF", "Date", "DATEDIF(start_date, end_date, unit)",
                "Difference between dates. Unit: \"Y\",\"M\",\"D\",\"YM\",\"MD\",\"YD\".", "Number",
                List.of("=DATEDIF(A1,B1,\"Y\")", "=DATEDIF(A1,TODAY(),\"M\")"),
                "Undocumented but works. \"YM\" = months ignoring years, \"MD\" = days ignoring months.");
        fn("NETWORKDAYS", "Date", "NETWORKDAYS(start_date, end_date, [holidays])",
                "Number of working days between two dates.", "Number",
                List.of("=NETWORKDAYS(A1,B1)", "=NETWORKDAYS(A1,B1,Holidays!A:A)"), null);
        fn("WORKDAY", "Date", "WORKDAY(start_date, days, [holidays])",
                "Date that is N working days from start.", "Date",
                List.of("=WORKDAY(TODAY(),10)"), null);
        fn("WEEKDAY", "Date", "WEEKDAY(serial_number, [return_type])", "Day of week as number.", "Number",
                List.of("=WEEKDAY(A1,2)"), "return_type 2: Mon=1..Sun=7");

        // Logical
        fn("IF", "Logical", "IF(logical_test, value_if_true, [value_if_false])",
                "Return different values based on a condition.", "Value",
                List.of("=IF(A1>10,\"High\",\"Low\")", "=IF(A1=\"\",\"Empty\",A1)"), null);
        fn("IFS", "Logical", "IFS(logical_test1, value1, [logical_test2, value2], ...)",
                "Multiple conditions without nesting. Excel 2019+.", "Value",
                List.of("=IFS(A1>=90,\"A\",A1>=80,\"B\",A1>=70,\"C\",TRUE,\"F\")"), null);
        fn("IFERROR", "Logical", "IFERROR(value, value_if_error)",
                "Return alternate value if formula produces an error.", "Value",
                List.of("=IFERROR(A1/B1,0)", "=IFERROR(VLOOKUP(A1,B:C,2,0),\"Not Found\")"), null);
        fn("IFNA", "Logical", "IFNA(value, value_if_na)", "Return alternate value if #N/A error.", "Value",
                List.of("=IFNA(XLOOKUP(A1,B:B,C:C),\"Missing\")"), null);
        fn("AND", "Logical", "AND(logical1, logical2, ...)", "TRUE if all conditions are TRUE.", "Boolean",
                List.of("=AND(A1>0,A1<100)"), null);
        fn("OR", "Logical", "OR(logical1, logical2, ...)", "TRUE if any condition is TRUE.", "Boolean",
                List.of("=OR(A1=\"Yes\",A1=\"Y\")"), null);
        fn("NOT", "Logical", "NOT(logical)", "Reverse a boolean.", "Boolean", List.of("=NOT(A1>10)"), null);
        fn("SWITCH", "Logical", "SWITCH(expression, value1, result1, [value2, result2], ..., [default])",
                "Match an expression against a list of values. Excel 2019+.", "Value",
                List.of("=SWITCH(A1,1,\"Jan\",2,\"Feb\",3,\"Mar\",\"Other\")"), null);

        // Statistical
        fn("AVERAGE", "Statistical", "AVERAGE(number1, number2, ...)", "Arithmetic mean.", "Number",
                List.of("=AVERAGE(A1:A100)"), null);
        fn("MEDIAN", "Statistical", "MEDIAN(number1, number2, ...)", "Middle value.", "Number",
                List.of("=MEDIAN(A1:A100)"), null);
        fn("MODE", "Statistical", "MODE(number1, number2, ...)", "Most frequent value.", "Number",
                List.of("=MODE(A1:A100)"), null);
        fn("STDEV", "Statistical", "STDEV(number1, number2, ...)", "Sample standard deviation.", "Number",
                List.of("=STDEV(A1:A100)"), "STDEV.S = sample, STDEV.P = population");
        fn("VAR", "Statistical", "VAR(number1, number2, ...)", "Sample variance.", "Number",
                List.of("=VAR(A1:A100)"), null);
        fn("PERCENTILE", "Statistical", "PERCENTILE(array, k)", "Return the k-th percentile.", "Number",
                List.of("=PERCENTILE(A1:A100,0.9)"), null);
        fn("RANK", "Statistical", "RANK(number, ref, [order])", "Rank of a number in a list.", "Number",
                List.of("=RANK(B2,B:B,0)"), "0 or omit = descending, 1 = ascending");
        fn("LARGE", "Statistical", "LARGE(array, k)", "Return the k-th largest value.", "Number",
                List.of("=LARGE(A1:A100,1)", "=LARGE(A:A,3)"), null);
        fn("SMALL", "Statistical", "SMALL(array, k)", "Return the k-th smallest value.", "Number",
                List.of("=SMALL(A1:A100,1)"), null);
        fn("CORREL", "Statistical", "CORREL(array1, array2)", "Correlation coefficient between two datasets.", "Number",
                List.of("=CORREL(A1:A100,B1:B100)"), "Returns -1 to 1. Use for linear relationship strength.");
        fn("FORECAST", "Statistical", "FORECAST(x, known_y's, known_x's)", "Predict a value using linear regression.", "Number",
                List.of("=FORECAST(13,B1:B12,A1:A12)"), null);

        // Array/Dynamic
        fn("FILTER", "Dynamic", "FILTER(array, include, [if_empty])",
                "Filter a range based on criteria. Returns dynamic array. Excel 365+.", "Array",
                List.of("=FILTER(A1:C100,B1:B100>50)", "=FILTER(A:C,A:A=\"Apple\",\"None found\")"), null);
        fn("SORT", "Dynamic", "SORT(array, [sort_index], [sort_order], [by_col])",
                "Sort a range. Returns dynamic array. Excel 365+.", "Array",
                List.of("=SORT(A1:C100,2,-1)", "=SORT(FILTER(A:C,B:B>50),3,1)"), null);
        fn("UNIQUE", "Dynamic", "UNIQUE(array, [by_col], [exactly_once])",
                "Return unique values. Excel 365+.", "Array",
                List.of("=UNIQUE(A1:A100)", "=UNIQUE(A:A,,TRUE)"), null);
        fn("SEQUENCE", "Dynamic", "SEQUENCE(rows, [columns], [start], [step])",
                "Generate a sequence of numbers. Excel 365+.", "Array",
                List.of("=SEQUENCE(10)", "=SEQUENCE(5,3,1,2)"), null);
        fn("LET", "Dynamic", "LET(name1, value1, [name2, value2], ..., calculation)",
                "Assign names to intermediate results. Excel 365+.", "Value",
                List.of("=LET(x,A1*2,y,B1*3,x+y)"), "Improves readability and performance of complex formulas.");
        fn("LAMBDA", "Dynamic", "LAMBDA([parameter1, parameter2, ...], calculation)",
                "Create custom functions. Excel 365+.", "Function",
                List.of("=LAMBDA(x,y,x^2+y^2)(3,4)"), "Can be named via Name Manager for reuse.");
    }

    private static void fn(String name, String category, String syntax, String desc,
                            String returnType, List<String> examples, String notes) {
        FUNCTIONS.put(name.toUpperCase(), new FunctionInfo(name, category, syntax, desc, returnType, examples, notes));
    }

    @Tool(name = "excel_function_lookup", description = "Look up any Excel function: syntax, parameters, examples, and tips. "
            + "Covers 80+ functions across Lookup, Math, Text, Date, Logical, Statistical, and Dynamic Array categories. "
            + "Can also search by category or keyword.")
    public String functionLookup(
            @ToolParam(description = "Function name (e.g. 'VLOOKUP', 'SUMIFS') or category "
                    + "('lookup', 'math', 'text', 'date', 'logical', 'statistical', 'dynamic') "
                    + "or keyword search (e.g. 'count with condition')") String query) {

        String q = query.strip().toUpperCase();

        // Direct function lookup
        FunctionInfo info = FUNCTIONS.get(q);
        if (info != null) {
            return formatFunction(info);
        }

        // Category search
        String categorySearch = q.toLowerCase();
        List<FunctionInfo> categoryResults = FUNCTIONS.values().stream()
                .filter(f -> f.category.toLowerCase().contains(categorySearch))
                .toList();

        if (!categoryResults.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Excel Functions — ").append(categorySearch).append(" (").append(categoryResults.size()).append(" functions)\n");
            sb.append("─".repeat(40)).append("\n\n");
            for (FunctionInfo f : categoryResults) {
                sb.append(String.format("%-15s %s\n", f.name, f.description.length() > 60
                        ? f.description.substring(0, 60) + "..." : f.description));
            }
            sb.append("\nUse excel_function_lookup with a specific function name for full details.");
            return sb.toString();
        }

        // Keyword search across descriptions
        String[] keywords = q.toLowerCase().split("\\s+");
        List<FunctionInfo> matches = FUNCTIONS.values().stream()
                .filter(f -> {
                    String searchable = (f.name + " " + f.description + " " + f.category + " " +
                            (f.notes != null ? f.notes : "")).toLowerCase();
                    return Arrays.stream(keywords).allMatch(searchable::contains);
                })
                .toList();

        if (!matches.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Search results for '").append(query).append("' (").append(matches.size()).append(" matches)\n");
            sb.append("─".repeat(40)).append("\n\n");
            for (FunctionInfo f : matches) {
                sb.append(String.format("%-15s [%s] %s\n", f.name, f.category, f.description));
            }
            return sb.toString();
        }

        return "No function found for '" + query + "'. Try:\n"
                + "  - Function name: VLOOKUP, SUMIFS, INDEX, etc.\n"
                + "  - Category: lookup, math, text, date, logical, statistical, dynamic\n"
                + "  - Keywords: 'count condition', 'find text', 'date difference'";
    }

    @Tool(name = "excel_formula_explain", description = "Break down a complex Excel formula and explain each part "
            + "in plain English. Handles nested functions, operators, and cell references.")
    public String explainFormula(
            @ToolParam(description = "The Excel formula to explain (e.g. '=IF(VLOOKUP(A1,Sheet2!A:C,3,0)>100,\"High\",\"Low\")')") String formula) {

        String f = formula.strip();
        if (f.startsWith("=")) f = f.substring(1);

        StringBuilder sb = new StringBuilder();
        sb.append("Formula Explanation\n");
        sb.append("───────────────────\n");
        sb.append("Formula: =").append(f).append("\n\n");

        // Extract and explain functions
        List<String> functions = extractFunctions(f);
        sb.append("FUNCTIONS USED (").append(functions.size()).append(")\n");
        for (String func : functions) {
            FunctionInfo info = FUNCTIONS.get(func.toUpperCase());
            if (info != null) {
                sb.append("  ").append(func).append(": ").append(info.description).append("\n");
            } else {
                sb.append("  ").append(func).append(": (custom or less common function)\n");
            }
        }

        // Explain cell references
        sb.append("\nCELL REFERENCES\n");
        Set<String> refs = extractReferences(f);
        for (String ref : refs) {
            sb.append("  ").append(ref).append(": ").append(explainReference(ref)).append("\n");
        }

        // Step-by-step breakdown
        sb.append("\nSTEP-BY-STEP BREAKDOWN\n");
        sb.append(breakdownFormula(f, 1));

        // Identify potential issues
        String warnings = checkForIssues(f);
        if (!warnings.isEmpty()) {
            sb.append("\nPOTENTIAL ISSUES\n").append(warnings);
        }

        return sb.toString();
    }

    @Tool(name = "excel_formula_build", description = "Build an Excel formula from a plain English description. "
            + "Describe what you want to calculate and get the formula with explanation.")
    public String buildFormula(
            @ToolParam(description = "Plain English description of what the formula should do. "
                    + "Examples: 'sum column B where column A is Apple', "
                    + "'count unique values in A1:A100', "
                    + "'find the last non-empty cell in column A', "
                    + "'calculate percentage change between B1 and B2'") String description) {

        String lower = description.toLowerCase();
        StringBuilder sb = new StringBuilder();
        sb.append("Formula Builder\n");
        sb.append("───────────────\n");
        sb.append("Request: ").append(description).append("\n\n");

        // Pattern matching for common requests
        if (lower.contains("sum") && (lower.contains("where") || lower.contains("if") || lower.contains("condition") || lower.contains("criteria"))) {
            if (lower.contains("multiple") || lower.contains("and") || lower.contains("conditions")) {
                sb.append("FORMULA: =SUMIFS(sum_range, criteria_range1, criteria1, criteria_range2, criteria2)\n\n");
                sb.append("EXAMPLE:\n  =SUMIFS(C:C, A:A, \"Apple\", B:B, \">10\")\n\n");
                sb.append("This sums column C where column A is \"Apple\" AND column B is greater than 10.\n");
                sb.append("Add more criteria_range/criteria pairs for additional conditions.");
            } else {
                sb.append("FORMULA: =SUMIF(criteria_range, criteria, sum_range)\n\n");
                sb.append("EXAMPLE:\n  =SUMIF(A:A, \"Apple\", B:B)\n\n");
                sb.append("This sums column B for all rows where column A equals \"Apple\".");
            }
        } else if (lower.contains("count") && lower.contains("unique")) {
            sb.append("FORMULA (Excel 365): =COUNTA(UNIQUE(range))\n");
            sb.append("FORMULA (older Excel): =SUMPRODUCT(1/COUNTIF(range, range))\n\n");
            sb.append("EXAMPLE:\n  =COUNTA(UNIQUE(A1:A100))\n  =SUMPRODUCT(1/COUNTIF(A1:A100,A1:A100))\n\n");
            sb.append("Note: The SUMPRODUCT version fails with blank cells. Use:\n");
            sb.append("  =SUMPRODUCT((A1:A100<>\"\")/COUNTIF(A1:A100,A1:A100&\"\"))");
        } else if (lower.contains("last") && (lower.contains("non-empty") || lower.contains("non empty") || lower.contains("value"))) {
            sb.append("FORMULA: =LOOKUP(2,1/(range<>\"\"),range)\n\n");
            sb.append("EXAMPLE:\n  =LOOKUP(2,1/(A:A<>\"\"),A:A)\n\n");
            sb.append("This finds the last non-empty cell in column A.\n");
            sb.append("For numbers only: =LOOKUP(9.99E+307,A:A)");
        } else if (lower.contains("percentage") && lower.contains("change")) {
            sb.append("FORMULA: =(new_value - old_value) / old_value\n\n");
            sb.append("EXAMPLE:\n  =(B2-B1)/B1\n  =TEXT((B2-B1)/B1,\"0.0%\")\n\n");
            sb.append("With error handling:\n  =IFERROR((B2-B1)/B1, 0)");
        } else if (lower.contains("vlookup") || (lower.contains("lookup") && lower.contains("return"))) {
            sb.append("RECOMMENDED: Use XLOOKUP (Excel 365) or INDEX/MATCH (all versions)\n\n");
            sb.append("XLOOKUP:\n  =XLOOKUP(lookup_value, lookup_range, return_range, \"Not Found\")\n\n");
            sb.append("INDEX/MATCH:\n  =INDEX(return_range, MATCH(lookup_value, lookup_range, 0))\n\n");
            sb.append("VLOOKUP:\n  =VLOOKUP(lookup_value, table, column_number, FALSE)\n\n");
            sb.append("INDEX/MATCH is preferred because:\n");
            sb.append("  - Works in any direction (VLOOKUP only looks right)\n");
            sb.append("  - Column insertions don't break it\n");
            sb.append("  - More flexible with multiple criteria");
        } else if (lower.contains("duplicate") && (lower.contains("find") || lower.contains("highlight") || lower.contains("remove"))) {
            sb.append("FIND DUPLICATES:\n");
            sb.append("  =COUNTIF(A:A, A1) > 1\n\n");
            sb.append("MARK FIRST OCCURRENCE:\n");
            sb.append("  =COUNTIF(A$1:A1, A1) = 1\n\n");
            sb.append("EXTRACT UNIQUE (Excel 365):\n");
            sb.append("  =UNIQUE(A1:A100)\n\n");
            sb.append("REMOVE DUPLICATES:\n");
            sb.append("  Use Data → Remove Duplicates, or =UNIQUE() in Excel 365.\n\n");
            sb.append("CONDITIONAL FORMATTING:\n");
            sb.append("  Select range → Conditional Formatting → Highlight Cell Rules → Duplicate Values");
        } else if (lower.contains("concatenate") || lower.contains("combine") || lower.contains("join") || lower.contains("merge text")) {
            sb.append("JOIN TEXT:\n");
            sb.append("  =A1 & \" \" & B1                    (simple concatenation)\n");
            sb.append("  =TEXTJOIN(\", \", TRUE, A1:A10)     (join with delimiter, Excel 2019+)\n");
            sb.append("  =CONCAT(A1:A10)                    (join without delimiter, Excel 2019+)\n");
            sb.append("  =CONCATENATE(A1,\" \",B1)           (legacy)");
        } else if (lower.contains("rank") || lower.contains("top") || lower.contains("bottom")) {
            sb.append("RANK:\n");
            sb.append("  =RANK(B2, B:B, 0)                  (0=descending, 1=ascending)\n\n");
            sb.append("TOP N VALUES:\n");
            sb.append("  =LARGE(B:B, 1)                     (1st largest)\n");
            sb.append("  =LARGE(B:B, ROW(A1))               (drag down for top N)\n\n");
            sb.append("BOTTOM N VALUES:\n");
            sb.append("  =SMALL(B:B, 1)                     (1st smallest)\n\n");
            sb.append("TOP N WITH FILTER (Excel 365):\n");
            sb.append("  =SORT(A1:B100, 2, -1)              (sort by col 2, descending)");
        } else if (lower.contains("date") && (lower.contains("difference") || lower.contains("between") || lower.contains("days"))) {
            sb.append("DATE DIFFERENCE:\n");
            sb.append("  =B1-A1                             (days between two dates)\n");
            sb.append("  =DATEDIF(A1,B1,\"Y\")               (years)\n");
            sb.append("  =DATEDIF(A1,B1,\"M\")               (months)\n");
            sb.append("  =DATEDIF(A1,B1,\"D\")               (days)\n");
            sb.append("  =NETWORKDAYS(A1,B1)                (working days)\n\n");
            sb.append("AGE CALCULATION:\n");
            sb.append("  =DATEDIF(A1,TODAY(),\"Y\")&\" years, \"&DATEDIF(A1,TODAY(),\"YM\")&\" months\"");
        } else if (lower.contains("dynamic") && (lower.contains("dropdown") || lower.contains("list") || lower.contains("validation"))) {
            sb.append("DYNAMIC DROPDOWN (Data Validation):\n\n");
            sb.append("1. Named range approach:\n");
            sb.append("   =OFFSET(Sheet2!$A$1,0,0,COUNTA(Sheet2!$A:$A),1)\n\n");
            sb.append("2. Table approach (recommended):\n");
            sb.append("   Create a Table (Ctrl+T) and reference: =Table1[Column1]\n\n");
            sb.append("3. UNIQUE dependent dropdown (Excel 365):\n");
            sb.append("   =UNIQUE(FILTER(B:B, A:A=D1))\n\n");
            sb.append("Apply via: Data → Data Validation → List → Source: formula");
        } else {
            sb.append("I can help build formulas for common tasks. Here are some patterns:\n\n");
            sb.append("LOOKUP:    =XLOOKUP(what, where_to_look, what_to_return)\n");
            sb.append("SUM IF:    =SUMIFS(sum_range, criteria_range, criteria)\n");
            sb.append("COUNT IF:  =COUNTIFS(range1, criteria1, range2, criteria2)\n");
            sb.append("NESTED IF: =IFS(test1, val1, test2, val2, TRUE, default)\n");
            sb.append("ERROR:     =IFERROR(formula, fallback_value)\n");
            sb.append("TEXT:      =TEXT(value, \"format_code\")\n");
            sb.append("DATE:      =DATEDIF(start, end, \"Y\"/\"M\"/\"D\")\n\n");
            sb.append("Try describing your specific need, e.g.:\n");
            sb.append("  'sum column B where A is Apple and C is > 100'\n");
            sb.append("  'find duplicates in column A'\n");
            sb.append("  'percentage change between two cells'");
        }

        return sb.toString();
    }

    // ── Helpers ──

    private String formatFunction(FunctionInfo f) {
        StringBuilder sb = new StringBuilder();
        sb.append("Excel: ").append(f.name).append("\n");
        sb.append("═".repeat(f.name.length() + 7)).append("\n\n");
        sb.append("Category: ").append(f.category).append("\n");
        sb.append("Returns: ").append(f.returnType).append("\n\n");
        sb.append("SYNTAX\n  ").append(f.syntax).append("\n\n");
        sb.append("DESCRIPTION\n  ").append(f.description).append("\n\n");
        sb.append("EXAMPLES\n");
        for (String ex : f.examples) sb.append("  ").append(ex).append("\n");
        if (f.notes != null) sb.append("\nNOTES\n  ").append(f.notes).append("\n");
        return sb.toString();
    }

    private List<String> extractFunctions(String formula) {
        List<String> funcs = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[A-Z][A-Z.]+(?=\\()").matcher(formula.toUpperCase());
        while (m.find()) {
            String func = m.group();
            if (!funcs.contains(func)) funcs.add(func);
        }
        return funcs;
    }

    private Set<String> extractReferences(String formula) {
        Set<String> refs = new LinkedHashSet<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\$?[A-Z]{1,3}\\$?\\d+(?::\\$?[A-Z]{1,3}\\$?\\d+)?|[A-Z]+:[A-Z]+|[\\w]+![A-Z$:\\d]+)")
                .matcher(formula.toUpperCase());
        while (m.find()) refs.add(m.group());
        return refs;
    }

    private String explainReference(String ref) {
        if (ref.contains("!")) return "Reference to another sheet: " + ref;
        if (ref.contains(":")) {
            if (ref.matches("[A-Z]+:[A-Z]+")) return "Entire column(s) " + ref;
            return "Range " + ref;
        }
        boolean absCol = ref.startsWith("$");
        boolean absRow = ref.contains("$") && !ref.startsWith("$");
        if (ref.chars().filter(c -> c == '$').count() == 2) return "Absolute reference (locked when copying)";
        if (absCol) return "Column locked, row relative";
        if (absRow) return "Row locked, column relative";
        return "Relative reference (shifts when copying)";
    }

    private String breakdownFormula(String formula, int step) {
        StringBuilder sb = new StringBuilder();
        // Simple nested function detection
        int depth = 0;

        for (int i = 0; i < formula.length(); i++) {
            char c = formula.charAt(i);
            if (c == '(' && i > 0 && Character.isLetter(formula.charAt(i - 1))) {
                if (depth == 0) {
                    int nameStart = i - 1;
                    while (nameStart > 0 && (Character.isLetterOrDigit(formula.charAt(nameStart - 1)) || formula.charAt(nameStart - 1) == '.')) nameStart--;
                    String funcName = formula.substring(nameStart, i);
                    sb.append("  ").append(step).append(". ").append(funcName).append("(...) — ");
                    FunctionInfo info = FUNCTIONS.get(funcName.toUpperCase());
                    sb.append(info != null ? info.description : "evaluates " + funcName).append("\n");
                    step++;
                }
                depth++;
            } else if (c == ')') {
                depth--;
            }
        }

        if (sb.isEmpty()) {
            sb.append("  Simple expression: ").append(formula).append("\n");
        }

        return sb.toString();
    }

    private String checkForIssues(String formula) {
        StringBuilder warnings = new StringBuilder();
        String upper = formula.toUpperCase();

        if (upper.contains("VLOOKUP") && !upper.contains("FALSE") && !upper.contains(",0)")) {
            warnings.append("  ⚠ VLOOKUP without FALSE/0 — may return approximate matches\n");
        }
        if (upper.contains("OFFSET") || upper.contains("INDIRECT") || upper.contains("NOW()") || upper.contains("TODAY()") || upper.contains("RAND()")) {
            warnings.append("  ⚠ Contains volatile function(s) — recalculates on every change, may slow large workbooks\n");
        }
        if (upper.contains("VLOOKUP")) {
            warnings.append("  💡 Consider XLOOKUP or INDEX/MATCH instead of VLOOKUP for more flexibility\n");
        }
        int openParens = (int) formula.chars().filter(c -> c == '(').count();
        int closeParens = (int) formula.chars().filter(c -> c == ')').count();
        if (openParens != closeParens) {
            warnings.append("  ⚠ Unbalanced parentheses: ").append(openParens).append(" open, ").append(closeParens).append(" close\n");
        }

        return warnings.toString();
    }
}
