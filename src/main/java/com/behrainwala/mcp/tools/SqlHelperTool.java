package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP tool for SQL assistance: formatting, explaining, building, and referencing SQL.
 * Purely string-based analysis — no database connection required.
 */
@Service
public class SqlHelperTool {

    // ── Keywords ────────────────────────────────────────────────────────────

    private static final List<String> MAJOR_CLAUSES = List.of(
            "SELECT", "FROM", "WHERE", "GROUP BY", "ORDER BY", "HAVING",
            "LIMIT", "OFFSET", "UNION", "UNION ALL", "INTERSECT", "EXCEPT",
            "INSERT INTO", "UPDATE", "DELETE", "SET", "VALUES",
            "CREATE TABLE", "ALTER TABLE", "DROP TABLE", "DROP"
    );

    private static final List<String> JOIN_KEYWORDS = List.of(
            "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "OUTER JOIN",
            "FULL OUTER JOIN", "CROSS JOIN", "JOIN"
    );

    private static final List<String> SUB_KEYWORDS = List.of(
            "ON", "AND", "OR", "CASE", "WHEN", "THEN", "ELSE", "END",
            "IN", "EXISTS", "BETWEEN", "LIKE", "IS NULL", "IS NOT NULL",
            "ASC", "DESC", "DISTINCT", "AS"
    );

    // ── Tool 1: sql_format ──────────────────────────────────────────────────

    @Tool(name = "sql_format", description = "Format and pretty-print a SQL query with proper indentation, "
            + "uppercase keywords, and nested subquery support. Makes messy SQL readable.")
    public String formatSql(
            @ToolParam(description = "The SQL query to format") String sql) {

        if (sql == null || sql.isBlank()) {
            return "Error: No SQL provided.";
        }

        try {
            String normalized = normalizeWhitespace(sql);
            String result = doFormat(normalized, 0);
            return "Formatted SQL\n"
                    + "─────────────\n\n"
                    + result.stripTrailing() + "\n";
        } catch (Exception e) {
            return "Error formatting SQL: " + e.getMessage();
        }
    }

    private String doFormat(String sql, int baseIndent) {
        StringBuilder sb = new StringBuilder();
        String indent = "  ".repeat(baseIndent);
        String subIndent = "  ".repeat(baseIndent + 1);

        // Handle subqueries recursively
        sql = formatSubqueries(sql, baseIndent + 1);

        // Tokenize preserving strings and identifiers
        List<String> tokens = tokenize(sql);

        int i = 0;
        boolean firstToken = true;

        while (i < tokens.size()) {
            String token = tokens.get(i);
            String upper = token.toUpperCase();

            // Check for multi-word keywords
            String twoWord = (i + 1 < tokens.size()) ? upper + " " + tokens.get(i + 1).toUpperCase() : "";
            String threeWord = (i + 2 < tokens.size()) ? twoWord + " " + tokens.get(i + 2).toUpperCase() : "";

            // Three-word keywords
            if (matchesKeyword(threeWord, "FULL OUTER JOIN") || matchesKeyword(threeWord, "IS NOT NULL")) {
                if (!firstToken) sb.append("\n");
                if (isJoinLike(threeWord)) {
                    sb.append(indent).append(threeWord);
                } else if ("IS NOT NULL".equals(threeWord)) {
                    sb.append(" ").append(threeWord);
                } else {
                    sb.append(indent).append(threeWord);
                }
                i += 3;
                firstToken = false;
                continue;
            }

            // Two-word keywords
            if (matchesMajorClause(twoWord) || matchesJoin(twoWord) || "IS NULL".equalsIgnoreCase(twoWord)) {
                if (!firstToken) sb.append("\n");
                if ("IS NULL".equalsIgnoreCase(twoWord)) {
                    sb.append(" ").append(twoWord.toUpperCase());
                } else if (matchesJoin(twoWord)) {
                    sb.append(indent).append(twoWord.toUpperCase());
                } else {
                    sb.append(indent).append(twoWord.toUpperCase());
                }
                i += 2;
                firstToken = false;
                continue;
            }

            // Single-word major clauses
            if (matchesMajorClause(upper)) {
                if (!firstToken) sb.append("\n");
                sb.append(indent).append(upper);
                i++;
                firstToken = false;
                continue;
            }

            // Single-word joins
            if ("JOIN".equalsIgnoreCase(token)) {
                if (!firstToken) sb.append("\n");
                sb.append(indent).append("JOIN");
                i++;
                firstToken = false;
                continue;
            }

            // Sub-keywords that get their own line with extra indent
            if ("AND".equalsIgnoreCase(token) || "OR".equalsIgnoreCase(token)) {
                sb.append("\n").append(subIndent).append(upper);
                i++;
                firstToken = false;
                continue;
            }

            if ("ON".equalsIgnoreCase(token)) {
                sb.append("\n").append(subIndent).append("ON");
                i++;
                firstToken = false;
                continue;
            }

            if ("CASE".equalsIgnoreCase(token)) {
                sb.append("\n").append(subIndent).append("CASE");
                i++;
                firstToken = false;
                continue;
            }

            if ("WHEN".equalsIgnoreCase(token) || "THEN".equalsIgnoreCase(token)
                    || "ELSE".equalsIgnoreCase(token)) {
                sb.append("\n").append(subIndent).append("  ").append(upper);
                i++;
                firstToken = false;
                continue;
            }

            if ("END".equalsIgnoreCase(token)) {
                sb.append("\n").append(subIndent).append("END");
                i++;
                firstToken = false;
                continue;
            }

            // Comma: same line, then newline for next item
            if (",".equals(token)) {
                sb.append(",\n").append(subIndent);
                i++;
                firstToken = false;
                continue;
            }

            // Regular token
            if (firstToken) {
                sb.append(indent).append(token);
                firstToken = false;
            } else {
                sb.append(" ").append(uppercaseIfKeyword(token));
            }
            i++;
        }

        return sb.toString();
    }

    private String formatSubqueries(String sql, int depth) {
        // Find parenthesized subqueries containing SELECT
        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < sql.length()) {
            if (sql.charAt(i) == '(') {
                int close = findMatchingParen(sql, i);
                if (close > i) {
                    String inner = sql.substring(i + 1, close).trim();
                    if (inner.toUpperCase().startsWith("SELECT")) {
                        String formatted = doFormat(inner, depth);
                        result.append("(\n").append(formatted).append("\n").append("  ".repeat(depth - 1)).append(")");
                        i = close + 1;
                        continue;
                    }
                }
            }
            result.append(sql.charAt(i));
            i++;
        }

        return result.toString();
    }

    private int findMatchingParen(String sql, int openPos) {
        int depth = 1;
        boolean inString = false;
        char stringChar = 0;
        for (int i = openPos + 1; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inString) {
                if (c == stringChar && (i + 1 >= sql.length() || sql.charAt(i + 1) != stringChar)) {
                    inString = false;
                }
            } else {
                if (c == '\'' || c == '"') {
                    inString = true;
                    stringChar = c;
                } else if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private List<String> tokenize(String sql) {
        List<String> tokens = new ArrayList<>();
        Pattern tokenPattern = Pattern.compile(
                "'[^']*'" +                // single-quoted strings
                "|\"[^\"]*\"" +            // double-quoted identifiers
                "|\\b\\w+\\b" +            // words
                "|[(),;]" +                // punctuation
                "|[<>!=]+|\\*|\\.|\\+"     // operators
        );
        Matcher m = tokenPattern.matcher(sql);
        while (m.find()) {
            tokens.add(m.group());
        }
        return tokens;
    }

    private boolean matchesMajorClause(String token) {
        return MAJOR_CLAUSES.stream().anyMatch(k -> k.equalsIgnoreCase(token));
    }

    private boolean matchesJoin(String token) {
        return JOIN_KEYWORDS.stream().anyMatch(k -> k.equalsIgnoreCase(token));
    }

    private boolean matchesKeyword(String token, String keyword) {
        return keyword.equalsIgnoreCase(token);
    }

    private boolean isJoinLike(String token) {
        return token.toUpperCase().contains("JOIN");
    }

    private String uppercaseIfKeyword(String token) {
        String upper = token.toUpperCase();
        for (String kw : MAJOR_CLAUSES) if (kw.equalsIgnoreCase(token)) return upper;
        for (String kw : JOIN_KEYWORDS) if (kw.equalsIgnoreCase(token)) return upper;
        for (String kw : SUB_KEYWORDS) if (kw.equalsIgnoreCase(token)) return upper;
        // Common functions
        if (Set.of("COUNT", "SUM", "AVG", "MIN", "MAX", "COALESCE", "IFNULL",
                "NULLIF", "CAST", "CONVERT", "UPPER", "LOWER", "TRIM", "LENGTH",
                "SUBSTRING", "CONCAT", "NOW", "CURRENT_TIMESTAMP", "DATE", "YEAR",
                "MONTH", "DAY", "EXTRACT", "ROUND", "ABS", "CEIL", "FLOOR",
                "ROW_NUMBER", "RANK", "DENSE_RANK", "LAG", "LEAD", "OVER",
                "PARTITION", "BY", "ROWS", "RANGE", "UNBOUNDED", "PRECEDING",
                "FOLLOWING", "CURRENT", "ROW", "NULL", "NOT", "TRUE", "FALSE",
                "ALL", "ANY", "SOME"
        ).contains(upper)) {
            return upper;
        }
        return token;
    }

    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    // ── Tool 2: sql_explain ─────────────────────────────────────────────────

    @Tool(name = "sql_explain", description = "Explain a SQL query in plain English. "
            + "Returns a numbered step-by-step breakdown of what the query does, "
            + "including tables, columns, joins, filters, grouping, ordering, and aggregations.")
    public String explainSql(
            @ToolParam(description = "The SQL query to explain") String sql) {

        if (sql == null || sql.isBlank()) {
            return "Error: No SQL provided.";
        }

        try {
            String normalized = normalizeWhitespace(sql);
            String upper = normalized.toUpperCase();

            StringBuilder sb = new StringBuilder();
            sb.append("SQL Explanation\n");
            sb.append("───────────────\n\n");
            sb.append("Query:\n  ").append(normalized).append("\n\n");
            sb.append("Step-by-step breakdown:\n\n");

            int step = 1;

            // Determine query type
            if (upper.startsWith("SELECT")) {
                sb.append(step++).append(". OPERATION: This is a SELECT query (data retrieval).\n\n");
            } else if (upper.startsWith("INSERT")) {
                sb.append(step++).append(". OPERATION: This is an INSERT query (adding data).\n\n");
                step = explainInsert(normalized, sb, step);
                return sb.toString();
            } else if (upper.startsWith("UPDATE")) {
                sb.append(step++).append(". OPERATION: This is an UPDATE query (modifying data).\n\n");
                step = explainUpdate(normalized, sb, step);
                return sb.toString();
            } else if (upper.startsWith("DELETE")) {
                sb.append(step++).append(". OPERATION: This is a DELETE query (removing data).\n\n");
                step = explainDelete(normalized, sb, step);
                return sb.toString();
            } else if (upper.startsWith("CREATE TABLE")) {
                sb.append(step++).append(". OPERATION: This is a CREATE TABLE statement (schema definition).\n\n");
                step = explainCreateTable(normalized, sb, step);
                return sb.toString();
            } else {
                sb.append(step++).append(". OPERATION: ").append(normalized.split("\\s+")[0].toUpperCase()).append(" statement.\n\n");
            }

            // FROM clause - tables
            Matcher fromMatcher = Pattern.compile("(?i)\\bFROM\\s+(.+?)(?:\\s+(?:WHERE|GROUP|ORDER|HAVING|LIMIT|JOIN|LEFT|RIGHT|INNER|OUTER|CROSS|FULL|UNION|$))", Pattern.CASE_INSENSITIVE).matcher(normalized);
            if (fromMatcher.find()) {
                String tables = fromMatcher.group(1).trim();
                sb.append(step++).append(". TABLES: Reading from ").append(tables).append(".\n\n");
            }

            // JOINs
            Pattern joinPattern = Pattern.compile("(?i)((?:LEFT|RIGHT|INNER|OUTER|FULL OUTER|CROSS)?\\s*JOIN)\\s+(\\w+)(?:\\s+(?:AS\\s+)?(\\w+))?\\s+ON\\s+(.+?)(?=\\s+(?:LEFT|RIGHT|INNER|OUTER|CROSS|FULL|JOIN|WHERE|GROUP|ORDER|HAVING|LIMIT|UNION)|$)");
            Matcher joinMatcher = joinPattern.matcher(normalized);
            while (joinMatcher.find()) {
                String joinType = joinMatcher.group(1).trim().toUpperCase();
                String table = joinMatcher.group(2);
                String alias = joinMatcher.group(3);
                String condition = joinMatcher.group(4).trim();
                sb.append(step++).append(". JOIN: ").append(joinType)
                        .append(" with table '").append(table).append("'");
                if (alias != null) sb.append(" (aliased as '").append(alias).append("')");
                sb.append(" where ").append(condition).append(".\n\n");
            }

            // SELECT columns
            Matcher selectMatcher = Pattern.compile("(?i)SELECT\\s+(.*?)\\s+FROM", Pattern.CASE_INSENSITIVE).matcher(normalized);
            if (selectMatcher.find()) {
                String columns = selectMatcher.group(1).trim();
                if ("*".equals(columns)) {
                    sb.append(step++).append(". COLUMNS: Selecting all columns (*).\n\n");
                } else {
                    sb.append(step++).append(". COLUMNS: Selecting ").append(columns).append(".\n\n");

                    // Check for aggregations
                    List<String> aggregations = new ArrayList<>();
                    Pattern aggPattern = Pattern.compile("(?i)(COUNT|SUM|AVG|MIN|MAX)\\s*\\([^)]+\\)");
                    Matcher aggMatcher = aggPattern.matcher(columns);
                    while (aggMatcher.find()) {
                        aggregations.add(aggMatcher.group());
                    }
                    if (!aggregations.isEmpty()) {
                        sb.append(step++).append(". AGGREGATIONS: Computing ")
                                .append(String.join(", ", aggregations)).append(".\n\n");
                    }
                }
            }

            // DISTINCT
            if (upper.contains("SELECT DISTINCT")) {
                sb.append(step++).append(". DISTINCT: Removing duplicate rows from the result.\n\n");
            }

            // WHERE clause
            Matcher whereMatcher = Pattern.compile("(?i)\\bWHERE\\s+(.+?)(?:\\s+(?:GROUP|ORDER|HAVING|LIMIT|UNION)|$)").matcher(normalized);
            if (whereMatcher.find()) {
                String conditions = whereMatcher.group(1).trim();
                sb.append(step++).append(". FILTER: Only rows where ").append(conditions).append(".\n\n");
            }

            // GROUP BY
            Matcher groupMatcher = Pattern.compile("(?i)\\bGROUP\\s+BY\\s+(.+?)(?:\\s+(?:HAVING|ORDER|LIMIT|UNION)|$)").matcher(normalized);
            if (groupMatcher.find()) {
                String groupCols = groupMatcher.group(1).trim();
                sb.append(step++).append(". GROUPING: Grouping results by ").append(groupCols).append(".\n\n");
            }

            // HAVING
            Matcher havingMatcher = Pattern.compile("(?i)\\bHAVING\\s+(.+?)(?:\\s+(?:ORDER|LIMIT|UNION)|$)").matcher(normalized);
            if (havingMatcher.find()) {
                String havingCond = havingMatcher.group(1).trim();
                sb.append(step++).append(". HAVING: After grouping, only keep groups where ").append(havingCond).append(".\n\n");
            }

            // ORDER BY
            Matcher orderMatcher = Pattern.compile("(?i)\\bORDER\\s+BY\\s+(.+?)(?:\\s+(?:LIMIT|OFFSET|UNION)|$)").matcher(normalized);
            if (orderMatcher.find()) {
                String orderCols = orderMatcher.group(1).trim();
                sb.append(step++).append(". ORDERING: Sorting results by ").append(orderCols).append(".\n\n");
            }

            // LIMIT / OFFSET
            Matcher limitMatcher = Pattern.compile("(?i)\\bLIMIT\\s+(\\d+)").matcher(normalized);
            if (limitMatcher.find()) {
                sb.append(step++).append(". LIMIT: Returning at most ").append(limitMatcher.group(1)).append(" rows.\n\n");
            }
            Matcher offsetMatcher = Pattern.compile("(?i)\\bOFFSET\\s+(\\d+)").matcher(normalized);
            if (offsetMatcher.find()) {
                sb.append(step++).append(". OFFSET: Skipping the first ").append(offsetMatcher.group(1)).append(" rows.\n\n");
            }

            // UNION
            if (upper.contains("UNION ALL")) {
                sb.append(step++).append(". UNION ALL: Combining results with another query (including duplicates).\n\n");
            } else if (upper.contains("UNION")) {
                sb.append(step++).append(". UNION: Combining results with another query (removing duplicates).\n\n");
            }

            // Subqueries
            if (normalized.contains("(") && Pattern.compile("(?i)\\(\\s*SELECT").matcher(normalized).find()) {
                sb.append(step).append(". NOTE: This query contains one or more subqueries (nested SELECT statements).\n\n");
            }

            return sb.toString();

        } catch (Exception e) {
            return "Error explaining SQL: " + e.getMessage();
        }
    }

    private int explainInsert(String sql, StringBuilder sb, int step) {
        Matcher m = Pattern.compile("(?i)INSERT\\s+INTO\\s+(\\w+)\\s*\\(([^)]+)\\)").matcher(sql);
        if (m.find()) {
            sb.append(step++).append(". TABLE: Inserting into '").append(m.group(1)).append("'.\n\n");
            sb.append(step++).append(". COLUMNS: ").append(m.group(2).trim()).append(".\n\n");
        }
        Matcher v = Pattern.compile("(?i)VALUES\\s*\\((.+?)\\)").matcher(sql);
        if (v.find()) {
            sb.append(step++).append(". VALUES: ").append(v.group(1).trim()).append(".\n\n");
        }
        if (sql.toUpperCase().contains("SELECT")) {
            sb.append(step++).append(". NOTE: Values are sourced from a SELECT subquery.\n\n");
        }
        return step;
    }

    private int explainUpdate(String sql, StringBuilder sb, int step) {
        Matcher m = Pattern.compile("(?i)UPDATE\\s+(\\w+)").matcher(sql);
        if (m.find()) {
            sb.append(step++).append(". TABLE: Updating table '").append(m.group(1)).append("'.\n\n");
        }
        Matcher s = Pattern.compile("(?i)SET\\s+(.+?)(?:\\s+WHERE|$)").matcher(sql);
        if (s.find()) {
            String sets = s.group(1).trim();
            String[] assignments = sets.split("\\s*,\\s*");
            sb.append(step++).append(". SET: Changing values:\n");
            for (String a : assignments) {
                sb.append("     - ").append(a.trim()).append("\n");
            }
            sb.append("\n");
        }
        Matcher w = Pattern.compile("(?i)WHERE\\s+(.+)$").matcher(sql);
        if (w.find()) {
            sb.append(step++).append(". FILTER: Only rows where ").append(w.group(1).trim()).append(".\n\n");
        } else {
            sb.append(step++).append(". WARNING: No WHERE clause - this will update ALL rows!\n\n");
        }
        return step;
    }

    private int explainDelete(String sql, StringBuilder sb, int step) {
        Matcher m = Pattern.compile("(?i)DELETE\\s+FROM\\s+(\\w+)").matcher(sql);
        if (m.find()) {
            sb.append(step++).append(". TABLE: Deleting from '").append(m.group(1)).append("'.\n\n");
        }
        Matcher w = Pattern.compile("(?i)WHERE\\s+(.+)$").matcher(sql);
        if (w.find()) {
            sb.append(step++).append(". FILTER: Only rows where ").append(w.group(1).trim()).append(".\n\n");
        } else {
            sb.append(step++).append(". WARNING: No WHERE clause - this will delete ALL rows!\n\n");
        }
        return step;
    }

    private int explainCreateTable(String sql, StringBuilder sb, int step) {
        Matcher m = Pattern.compile("(?i)CREATE\\s+TABLE\\s+(\\w+)\\s*\\((.+)\\)\\s*$").matcher(sql);
        if (m.find()) {
            sb.append(step++).append(". TABLE: Creating table '").append(m.group(1)).append("'.\n\n");
            String body = m.group(2);
            String[] parts = body.split(",(?![^(]*\\))");
            sb.append(step++).append(". COLUMNS:\n");
            for (String part : parts) {
                sb.append("     - ").append(part.trim()).append("\n");
            }
            sb.append("\n");
        }
        return step;
    }

    // ── Tool 3: sql_build ───────────────────────────────────────────────────

    @Tool(name = "sql_build", description = "Build a SQL query from a plain English description. "
            + "Supports common patterns like selecting, filtering, joining, grouping, and table creation. "
            + "Includes dialect-specific syntax hints.")
    public String buildSql(
            @ToolParam(description = "Plain English description of the desired query, "
                    + "e.g., 'select all users where age > 18' or 'count orders by status'") String description,
            @ToolParam(description = "SQL dialect: mysql, postgresql, sqlite, sqlserver, or standard. Default: standard.",
                    required = false) String dialect) {

        if (description == null || description.isBlank()) {
            return "Error: No description provided.";
        }

        if (dialect == null || dialect.isBlank()) {
            dialect = "standard";
        }
        dialect = dialect.toLowerCase();

        try {
            String lower = description.toLowerCase().trim();
            StringBuilder sb = new StringBuilder();
            sb.append("Generated SQL\n");
            sb.append("─────────────\n");
            sb.append("Description: ").append(description).append("\n");
            sb.append("Dialect: ").append(dialect).append("\n\n");

            String query = generateQuery(lower, dialect);
            sb.append(query).append("\n");

            // Add dialect hints
            String hints = getDialectHints(dialect);
            if (!hints.isEmpty()) {
                sb.append("\nDialect notes (").append(dialect).append("):\n").append(hints);
            }

            return sb.toString();

        } catch (Exception e) {
            return "Error building SQL: " + e.getMessage();
        }
    }

    private String generateQuery(String desc, String dialect) {
        // Create table pattern
        Matcher createMatch = Pattern.compile("create\\s+(?:a\\s+)?table\\s+(?:for\\s+|named?\\s+)?(\\w+)\\s+with\\s+(.+)").matcher(desc);
        if (createMatch.find()) {
            return buildCreateTable(createMatch.group(1), createMatch.group(2), dialect);
        }

        // Count/aggregate by pattern
        Matcher countBy = Pattern.compile("count\\s+(\\w+)\\s+by\\s+(\\w+)").matcher(desc);
        if (countBy.find()) {
            String table = countBy.group(1);
            String groupCol = countBy.group(2);
            return "SELECT " + groupCol + ", COUNT(*) AS count\nFROM " + table
                    + "\nGROUP BY " + groupCol + "\nORDER BY count DESC"
                    + buildLimit(dialect) + ";";
        }

        // Sum/average pattern
        Matcher aggMatch = Pattern.compile("(sum|average|avg|min|max)\\s+(?:of\\s+)?(\\w+)\\s+(?:from|in)\\s+(\\w+)(?:\\s+(?:by|grouped?\\s+by)\\s+(\\w+))?").matcher(desc);
        if (aggMatch.find()) {
            String func = aggMatch.group(1).equalsIgnoreCase("average") ? "AVG" : aggMatch.group(1).toUpperCase();
            String col = aggMatch.group(2);
            String table = aggMatch.group(3);
            String groupCol = aggMatch.group(4);
            String sql = "SELECT " + (groupCol != null ? groupCol + ", " : "")
                    + func + "(" + col + ") AS " + func.toLowerCase() + "_" + col
                    + "\nFROM " + table;
            if (groupCol != null) sql += "\nGROUP BY " + groupCol;
            return sql + ";";
        }

        // Join pattern
        Matcher joinMatch = Pattern.compile("join\\s+(\\w+)\\s+and\\s+(\\w+)(?:\\s+on\\s+(.+))?").matcher(desc);
        if (joinMatch.find()) {
            String t1 = joinMatch.group(1);
            String t2 = joinMatch.group(2);
            String onClause = joinMatch.group(3);
            if (onClause == null || onClause.isBlank()) {
                onClause = t1 + ".id = " + t2 + "." + t1 + "_id";
            }
            return "SELECT *\nFROM " + t1 + "\nJOIN " + t2 + "\n  ON " + onClause + ";";
        }

        // Select with conditions
        Matcher selectWhere = Pattern.compile("select\\s+(.+?)\\s+from\\s+(\\w+)\\s+where\\s+(.+)").matcher(desc);
        if (selectWhere.find()) {
            String cols = selectWhere.group(1).trim();
            if ("all".equalsIgnoreCase(cols) || "everything".equalsIgnoreCase(cols)) cols = "*";
            String table = selectWhere.group(2);
            String where = selectWhere.group(3).trim();
            return "SELECT " + cols + "\nFROM " + table + "\nWHERE " + where + ";";
        }

        // Select all from with where
        Matcher selectAllWhere = Pattern.compile("(?:select|get|find|fetch)\\s+(?:all\\s+)?(\\w+)\\s+where\\s+(.+)").matcher(desc);
        if (selectAllWhere.find()) {
            String table = selectAllWhere.group(1);
            String where = selectAllWhere.group(2).trim();
            return "SELECT *\nFROM " + table + "\nWHERE " + where + ";";
        }

        // Simple select all
        Matcher selectAll = Pattern.compile("(?:select|get|find|fetch|show|list)\\s+(?:all\\s+)?(?:from\\s+)?(\\w+)").matcher(desc);
        if (selectAll.find()) {
            String table = selectAll.group(1);
            return "SELECT *\nFROM " + table + ";";
        }

        // Insert pattern
        Matcher insertMatch = Pattern.compile("insert\\s+(?:into\\s+)?(\\w+)\\s+(?:values?|with)\\s+(.+)").matcher(desc);
        if (insertMatch.find()) {
            String table = insertMatch.group(1);
            String values = insertMatch.group(2).trim();
            return "INSERT INTO " + table + "\nVALUES (" + values + ");";
        }

        // Update pattern
        Matcher updateMatch = Pattern.compile("update\\s+(\\w+)\\s+set\\s+(.+?)\\s+where\\s+(.+)").matcher(desc);
        if (updateMatch.find()) {
            return "UPDATE " + updateMatch.group(1) + "\nSET " + updateMatch.group(2).trim()
                    + "\nWHERE " + updateMatch.group(3).trim() + ";";
        }

        // Delete pattern
        Matcher deleteMatch = Pattern.compile("delete\\s+(?:from\\s+)?(\\w+)\\s+where\\s+(.+)").matcher(desc);
        if (deleteMatch.find()) {
            return "DELETE FROM " + deleteMatch.group(1) + "\nWHERE " + deleteMatch.group(2).trim() + ";";
        }

        // Fallback: try to build something reasonable
        return """
                -- Could not auto-generate SQL from the description.
                -- Tip: Try more specific phrasing like:
                --   'select all users where age > 18'
                --   'count orders by status'
                --   'join users and orders'
                --   'create table for users with name email age'
                --   'sum of amount from payments by customer_id'""";
    }

    private String buildCreateTable(String tableName, String columnsDesc, String dialect) {
        String[] cols = columnsDesc.split("\\s+(?:and|,)\\s*|\\s+");
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName).append(" (\n");

        String autoInc = switch (dialect) {
            case "mysql" -> "INT AUTO_INCREMENT";
            case "postgresql" -> "SERIAL";
            case "sqlite" -> "INTEGER";
            case "sqlserver" -> "INT IDENTITY(1,1)";
            default -> "INT AUTO_INCREMENT";
        };

        sb.append("  id ").append(autoInc).append(" PRIMARY KEY");

        for (String col : cols) {
            col = col.trim().toLowerCase();
            if (col.isEmpty() || col.equals("and")) continue;

            String type = inferColumnType(col, dialect);
            sb.append(",\n  ").append(col).append(" ").append(type);
        }

        sb.append("\n);");
        return sb.toString();
    }

    private String inferColumnType(String colName, String dialect) {
        String name = colName.toLowerCase();
        if (name.contains("email") || name.contains("name") || name.contains("title")
                || name.contains("description") || name.contains("address") || name.contains("url")
                || name.contains("phone") || name.contains("password") || name.contains("username")) {
            return "VARCHAR(255)";
        }
        if (name.contains("age") || name.contains("count") || name.contains("quantity")
                || name.contains("number") || name.contains("num")) {
            return "INT";
        }
        if (name.contains("price") || name.contains("amount") || name.contains("salary")
                || name.contains("cost") || name.contains("balance") || name.contains("total")) {
            return "DECIMAL(10,2)";
        }
        if (name.contains("date") || name.contains("created") || name.contains("updated")
                || name.contains("birthday") || name.contains("born")) {
            return dialect.equals("sqlserver") ? "DATETIME2" : "TIMESTAMP";
        }
        if (name.contains("active") || name.contains("enabled") || name.contains("deleted")
                || name.contains("verified") || name.contains("is_")) {
            return dialect.equals("postgresql") ? "BOOLEAN" : "TINYINT(1)";
        }
        if (name.contains("text") || name.contains("content") || name.contains("body")
                || name.contains("bio") || name.contains("notes")) {
            return "TEXT";
        }
        return "VARCHAR(255)";
    }

    private String buildLimit(String dialect) {
        return switch (dialect) {
            case "sqlserver" -> ""; // uses TOP N after SELECT
            default -> "";
        };
    }

    private String getDialectHints(String dialect) {
        return switch (dialect) {
            case "mysql" -> """
                      - Use LIMIT N for row limits
                      - AUTO_INCREMENT for auto-generated IDs
                      - Use backticks for reserved word identifiers: `order`
                      - String concatenation: CONCAT(a, b)
                    """;
            case "postgresql" -> """
                      - Use LIMIT N / OFFSET M for pagination
                      - SERIAL or GENERATED ALWAYS AS IDENTITY for auto IDs
                      - Use double quotes for identifiers: "order"
                      - String concatenation: a || b
                      - Supports RETURNING clause on INSERT/UPDATE/DELETE
                    """;
            case "sqlite" -> """
                      - Use LIMIT N / OFFSET M for pagination
                      - INTEGER PRIMARY KEY for auto-increment
                      - No native BOOLEAN type (use 0/1)
                      - String concatenation: a || b
                    """;
            case "sqlserver" -> """
                      - Use TOP N after SELECT instead of LIMIT
                      - IDENTITY(1,1) for auto-generated IDs
                      - Use square brackets for identifiers: [order]
                      - String concatenation: a + b or CONCAT(a, b)
                      - Pagination: OFFSET M ROWS FETCH NEXT N ROWS ONLY
                    """;
            default -> "";
        };
    }

    // ── Tool 4: sql_reference ───────────────────────────────────────────────

    @Tool(name = "sql_reference", description = "SQL syntax reference with explanations and examples. "
            + "Topics: joins, aggregates, subqueries, window_functions, indexes, constraints, "
            + "data_types, string_functions, date_functions, case_expressions, set_operations, transactions.")
    public String sqlReference(
            @ToolParam(description = "The topic to look up, e.g., 'joins', 'aggregates', 'subqueries', "
                    + "'window_functions', 'indexes', 'constraints', 'data_types', 'string_functions', "
                    + "'date_functions', 'case_expressions', 'set_operations', 'transactions'") String topic) {

        if (topic == null || topic.isBlank()) {
            return "Error: No topic provided. Available topics: joins, aggregates, subqueries, "
                    + "window_functions, indexes, constraints, data_types, string_functions, "
                    + "date_functions, case_expressions, set_operations, transactions.";
        }

        String key = topic.toLowerCase().trim().replaceAll("[\\s_-]+", "_");
        String content = REFERENCE_MAP.get(key);

        if (content == null) {
            // Try partial match
            for (Map.Entry<String, String> entry : REFERENCE_MAP.entrySet()) {
                if (entry.getKey().contains(key) || key.contains(entry.getKey())) {
                    content = entry.getValue();
                    break;
                }
            }
        }

        if (content == null) {
            return "Topic '" + topic + "' not found.\n\n"
                    + "Available topics:\n"
                    + "  - joins           Types of joins and when to use each\n"
                    + "  - aggregates      COUNT, SUM, AVG, MIN, MAX, GROUP BY\n"
                    + "  - subqueries      Nested queries, correlated subqueries\n"
                    + "  - window_functions ROW_NUMBER, RANK, LAG, LEAD, OVER\n"
                    + "  - indexes         Creating and using indexes\n"
                    + "  - constraints     PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK\n"
                    + "  - data_types      Common SQL data types\n"
                    + "  - string_functions String manipulation functions\n"
                    + "  - date_functions  Date/time functions\n"
                    + "  - case_expressions CASE WHEN conditional logic\n"
                    + "  - set_operations  UNION, INTERSECT, EXCEPT\n"
                    + "  - transactions    BEGIN, COMMIT, ROLLBACK\n";
        }

        return "SQL Reference: " + topic + "\n"
                + "─".repeat(Math.max(16 + topic.length(), 30)) + "\n\n"
                + content;
    }

    // ── Reference content ───────────────────────────────────────────────────

    private static final Map<String, String> REFERENCE_MAP = new LinkedHashMap<>();

    static {
        REFERENCE_MAP.put("joins", """
                Joins combine rows from two or more tables based on a related column.

                TYPES:

                1. INNER JOIN - Returns only matching rows from both tables.
                   SELECT *
                   FROM orders o
                   INNER JOIN customers c ON o.customer_id = c.id;

                2. LEFT JOIN (LEFT OUTER JOIN) - All rows from left table, matching from right.
                   SELECT *
                   FROM customers c
                   LEFT JOIN orders o ON c.id = o.customer_id;
                   -- Customers with no orders will have NULL in order columns.

                3. RIGHT JOIN (RIGHT OUTER JOIN) - All rows from right table, matching from left.
                   SELECT *
                   FROM orders o
                   RIGHT JOIN customers c ON o.customer_id = c.id;

                4. FULL OUTER JOIN - All rows from both tables, NULLs where no match.
                   SELECT *
                   FROM customers c
                   FULL OUTER JOIN orders o ON c.id = o.customer_id;

                5. CROSS JOIN - Cartesian product (every combination).
                   SELECT *
                   FROM colors
                   CROSS JOIN sizes;

                6. SELF JOIN - A table joined to itself.
                   SELECT e.name AS employee, m.name AS manager
                   FROM employees e
                   LEFT JOIN employees m ON e.manager_id = m.id;

                TIPS:
                - Always specify the join condition with ON.
                - Use table aliases (a, b) for readability.
                - LEFT JOIN is the most commonly used outer join.
                - Watch for unintended cartesian products (missing ON clause).
                """);

        REFERENCE_MAP.put("aggregates", """
                Aggregate functions compute a single result from a set of rows.

                FUNCTIONS:

                COUNT(*) - Count all rows
                COUNT(col) - Count non-NULL values
                COUNT(DISTINCT col) - Count unique values
                SUM(col) - Total of numeric column
                AVG(col) - Average of numeric column
                MIN(col) - Smallest value
                MAX(col) - Largest value

                EXAMPLES:

                -- Basic aggregation
                SELECT COUNT(*) AS total_orders,
                       SUM(amount) AS total_revenue,
                       AVG(amount) AS avg_order_value,
                       MIN(amount) AS smallest_order,
                       MAX(amount) AS largest_order
                FROM orders;

                -- GROUP BY: aggregate per group
                SELECT status, COUNT(*) AS count, AVG(amount) AS avg_amount
                FROM orders
                GROUP BY status;

                -- HAVING: filter groups (like WHERE but for aggregates)
                SELECT customer_id, COUNT(*) AS order_count
                FROM orders
                GROUP BY customer_id
                HAVING COUNT(*) > 5;

                -- Multiple grouping columns
                SELECT YEAR(created_at) AS yr, MONTH(created_at) AS mo, SUM(amount)
                FROM orders
                GROUP BY YEAR(created_at), MONTH(created_at)
                ORDER BY yr, mo;

                RULES:
                - Every non-aggregated column in SELECT must be in GROUP BY.
                - WHERE filters rows BEFORE grouping; HAVING filters AFTER.
                - NULL values are ignored by all aggregate functions except COUNT(*).
                """);

        REFERENCE_MAP.put("subqueries", """
                A subquery is a query nested inside another query.

                TYPES:

                1. Scalar subquery (returns single value):
                   SELECT name, salary
                   FROM employees
                   WHERE salary > (SELECT AVG(salary) FROM employees);

                2. Row subquery (returns single row):
                   SELECT *
                   FROM employees
                   WHERE (dept_id, salary) = (SELECT dept_id, MAX(salary) FROM employees);

                3. Table subquery (returns multiple rows):
                   SELECT *
                   FROM employees
                   WHERE dept_id IN (SELECT id FROM departments WHERE location = 'NYC');

                4. Correlated subquery (references outer query):
                   SELECT e.name, e.salary
                   FROM employees e
                   WHERE e.salary > (
                       SELECT AVG(e2.salary)
                       FROM employees e2
                       WHERE e2.dept_id = e.dept_id
                   );

                5. EXISTS subquery:
                   SELECT c.name
                   FROM customers c
                   WHERE EXISTS (
                       SELECT 1 FROM orders o WHERE o.customer_id = c.id
                   );

                6. Subquery in FROM (derived table):
                   SELECT dept_name, avg_salary
                   FROM (
                       SELECT d.name AS dept_name, AVG(e.salary) AS avg_salary
                       FROM departments d
                       JOIN employees e ON d.id = e.dept_id
                       GROUP BY d.name
                   ) AS dept_stats
                   WHERE avg_salary > 50000;

                TIPS:
                - Use EXISTS instead of IN for better performance with large datasets.
                - Correlated subqueries run once per outer row (can be slow).
                - Consider CTEs (WITH clause) for readability over nested subqueries.
                """);

        REFERENCE_MAP.put("window_functions", """
                Window functions perform calculations across a set of rows related to the current row.

                SYNTAX:
                  function_name(...) OVER (
                      [PARTITION BY col1, col2]
                      [ORDER BY col3 ASC|DESC]
                      [ROWS BETWEEN ... AND ...]
                  )

                RANKING FUNCTIONS:

                ROW_NUMBER() - Unique sequential number per partition
                RANK()       - Rank with gaps for ties
                DENSE_RANK() - Rank without gaps for ties
                NTILE(n)     - Divide into n roughly equal buckets

                SELECT name, department, salary,
                       ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) AS rn,
                       RANK() OVER (ORDER BY salary DESC) AS salary_rank
                FROM employees;

                VALUE FUNCTIONS:

                LAG(col, n)  - Value from n rows before current row
                LEAD(col, n) - Value from n rows after current row
                FIRST_VALUE(col) - First value in the window frame
                LAST_VALUE(col)  - Last value in the window frame
                NTH_VALUE(col, n) - Nth value in the window frame

                SELECT date, revenue,
                       LAG(revenue, 1) OVER (ORDER BY date) AS prev_day,
                       revenue - LAG(revenue, 1) OVER (ORDER BY date) AS daily_change
                FROM daily_sales;

                AGGREGATE WINDOW FUNCTIONS:

                SELECT name, department, salary,
                       SUM(salary) OVER (PARTITION BY department) AS dept_total,
                       AVG(salary) OVER (PARTITION BY department) AS dept_avg,
                       salary * 100.0 / SUM(salary) OVER (PARTITION BY department) AS pct_of_dept
                FROM employees;

                RUNNING TOTALS:

                SELECT date, amount,
                       SUM(amount) OVER (ORDER BY date ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_total
                FROM transactions;

                FRAME CLAUSES:
                  ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW  (default with ORDER BY)
                  ROWS BETWEEN 3 PRECEDING AND 3 FOLLOWING          (7-row moving window)
                  ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING
                """);

        REFERENCE_MAP.put("indexes", """
                Indexes speed up data retrieval but slow down writes.

                CREATE INDEX:
                  CREATE INDEX idx_name ON table_name (column);
                  CREATE INDEX idx_multi ON table_name (col1, col2);
                  CREATE UNIQUE INDEX idx_email ON users (email);

                DROP INDEX:
                  DROP INDEX idx_name;                    -- Standard / PostgreSQL
                  DROP INDEX idx_name ON table_name;      -- MySQL / SQL Server

                TYPES:
                - B-tree (default): Good for equality and range queries.
                - Hash: Good for equality only (PostgreSQL).
                - GIN/GiST: Full-text search, JSONB, arrays (PostgreSQL).
                - Covering index: Includes extra columns to avoid table lookup.

                WHEN TO INDEX:
                - Columns in WHERE clauses used frequently.
                - Columns used in JOIN conditions.
                - Columns used in ORDER BY.
                - Foreign key columns.

                WHEN NOT TO INDEX:
                - Small tables (full scan is fast enough).
                - Columns with very low cardinality (e.g., boolean).
                - Tables with heavy INSERT/UPDATE/DELETE activity.

                COMPOSITE INDEX ORDER MATTERS:
                  CREATE INDEX idx ON orders (customer_id, created_at);
                  -- Efficient for: WHERE customer_id = ? AND created_at > ?
                  -- Efficient for: WHERE customer_id = ?
                  -- NOT efficient for: WHERE created_at > ? (leading column not used)

                VIEWING INDEXES:
                  SHOW INDEX FROM table_name;                           -- MySQL
                  SELECT * FROM pg_indexes WHERE tablename = 'table';   -- PostgreSQL
                  SELECT * FROM sys.indexes WHERE object_id = ...;      -- SQL Server
                """);

        REFERENCE_MAP.put("constraints", """
                Constraints enforce rules on table data.

                PRIMARY KEY - Unique identifier for each row (NOT NULL + UNIQUE):
                  CREATE TABLE users (
                      id INT PRIMARY KEY,
                      name VARCHAR(100)
                  );

                FOREIGN KEY - References a primary key in another table:
                  CREATE TABLE orders (
                      id INT PRIMARY KEY,
                      customer_id INT,
                      FOREIGN KEY (customer_id) REFERENCES customers(id)
                          ON DELETE CASCADE
                          ON UPDATE CASCADE
                  );

                UNIQUE - No duplicate values:
                  ALTER TABLE users ADD CONSTRAINT uq_email UNIQUE (email);

                NOT NULL - Column cannot be NULL:
                  CREATE TABLE users (
                      id INT PRIMARY KEY,
                      email VARCHAR(255) NOT NULL
                  );

                CHECK - Custom condition:
                  CREATE TABLE products (
                      id INT PRIMARY KEY,
                      price DECIMAL(10,2) CHECK (price > 0),
                      quantity INT CHECK (quantity >= 0)
                  );

                DEFAULT - Default value when not specified:
                  CREATE TABLE orders (
                      id INT PRIMARY KEY,
                      status VARCHAR(20) DEFAULT 'pending',
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                  );

                ON DELETE / ON UPDATE actions:
                  CASCADE    - Propagate the change
                  SET NULL   - Set to NULL
                  SET DEFAULT - Set to default value
                  RESTRICT   - Prevent the change
                  NO ACTION  - Same as RESTRICT (default)
                """);

        REFERENCE_MAP.put("data_types", """
                Common SQL data types across dialects.

                NUMERIC TYPES:
                  INT / INTEGER         4 bytes, -2B to +2B
                  SMALLINT              2 bytes, -32768 to +32767
                  BIGINT                8 bytes, very large range
                  DECIMAL(p,s) / NUMERIC Exact precision (e.g., DECIMAL(10,2) for currency)
                  FLOAT / REAL          Approximate floating point
                  DOUBLE / FLOAT(53)    Double precision floating point

                STRING TYPES:
                  CHAR(n)               Fixed-length string
                  VARCHAR(n)            Variable-length string (max n)
                  TEXT                  Unlimited length text
                  NVARCHAR(n)           Unicode variable-length (SQL Server)

                DATE/TIME TYPES:
                  DATE                  Date only (YYYY-MM-DD)
                  TIME                  Time only (HH:MM:SS)
                  DATETIME              Date + time
                  TIMESTAMP             Date + time (often with timezone)
                  INTERVAL              Time duration (PostgreSQL)

                BOOLEAN:
                  BOOLEAN               true/false (PostgreSQL, MySQL)
                  BIT                   0/1 (SQL Server)
                  TINYINT(1)            0/1 (MySQL alternative)

                BINARY:
                  BLOB                  Binary large object
                  BYTEA                 Binary data (PostgreSQL)
                  VARBINARY(n)          Variable-length binary (SQL Server)

                SPECIAL:
                  JSON / JSONB          JSON data (PostgreSQL, MySQL 5.7+)
                  UUID                  Universally unique identifier (PostgreSQL)
                  ARRAY                 Array type (PostgreSQL)
                  ENUM                  Enumerated type (MySQL, PostgreSQL)
                  XML                   XML data (SQL Server, PostgreSQL)

                AUTO-INCREMENT:
                  MySQL:      INT AUTO_INCREMENT
                  PostgreSQL: SERIAL / BIGSERIAL / GENERATED ALWAYS AS IDENTITY
                  SQLite:     INTEGER PRIMARY KEY (auto-increments)
                  SQL Server: INT IDENTITY(1,1)
                """);

        REFERENCE_MAP.put("string_functions", """
                Common string functions across SQL dialects.

                LENGTH / CHARACTER_LENGTH:
                  SELECT LENGTH('hello');                     -- 5
                  SELECT CHARACTER_LENGTH('hello');           -- 5
                  SELECT LEN('hello');                        -- 5 (SQL Server)

                UPPER / LOWER:
                  SELECT UPPER('hello');                      -- 'HELLO'
                  SELECT LOWER('HELLO');                      -- 'hello'

                TRIM / LTRIM / RTRIM:
                  SELECT TRIM('  hello  ');                   -- 'hello'
                  SELECT LTRIM('  hello');                    -- 'hello'
                  SELECT RTRIM('hello  ');                    -- 'hello'

                SUBSTRING / SUBSTR:
                  SELECT SUBSTRING('hello world', 7, 5);     -- 'world'
                  SELECT SUBSTR('hello world', 7);           -- 'world' (MySQL, SQLite)

                CONCAT / ||:
                  SELECT CONCAT('hello', ' ', 'world');      -- 'hello world'
                  SELECT 'hello' || ' ' || 'world';          -- 'hello world' (PostgreSQL, SQLite)
                  SELECT 'hello' + ' ' + 'world';            -- 'hello world' (SQL Server)

                REPLACE:
                  SELECT REPLACE('hello world', 'world', 'sql');  -- 'hello sql'

                POSITION / CHARINDEX / INSTR:
                  SELECT POSITION('world' IN 'hello world');      -- 7 (PostgreSQL)
                  SELECT CHARINDEX('world', 'hello world');       -- 7 (SQL Server)
                  SELECT INSTR('hello world', 'world');           -- 7 (MySQL, SQLite)

                LEFT / RIGHT:
                  SELECT LEFT('hello', 3);                   -- 'hel'
                  SELECT RIGHT('hello', 3);                  -- 'llo'

                LPAD / RPAD:
                  SELECT LPAD('42', 5, '0');                 -- '00042'
                  SELECT RPAD('hi', 5, '.');                 -- 'hi...'

                REVERSE:
                  SELECT REVERSE('hello');                   -- 'olleh'

                COALESCE (not strictly string, but commonly used):
                  SELECT COALESCE(nickname, first_name, 'Anonymous');
                """);

        REFERENCE_MAP.put("date_functions", """
                Common date/time functions across SQL dialects.

                CURRENT DATE/TIME:
                  SELECT CURRENT_DATE;                       -- Date only
                  SELECT CURRENT_TIMESTAMP;                  -- Date + time
                  SELECT NOW();                              -- MySQL, PostgreSQL
                  SELECT GETDATE();                          -- SQL Server

                EXTRACT PARTS:
                  SELECT EXTRACT(YEAR FROM created_at);      -- Standard, PostgreSQL
                  SELECT YEAR(created_at);                   -- MySQL, SQL Server
                  SELECT MONTH(created_at);
                  SELECT DAY(created_at);
                  SELECT HOUR(created_at);

                DATE ARITHMETIC:
                  -- Add interval
                  SELECT created_at + INTERVAL '7 days';              -- PostgreSQL
                  SELECT DATE_ADD(created_at, INTERVAL 7 DAY);        -- MySQL
                  SELECT DATEADD(day, 7, created_at);                 -- SQL Server
                  SELECT DATE(created_at, '+7 days');                 -- SQLite

                  -- Difference
                  SELECT age_2 - age_1;                               -- PostgreSQL (returns interval)
                  SELECT DATEDIFF(date2, date1);                      -- MySQL (returns days)
                  SELECT DATEDIFF(day, date1, date2);                 -- SQL Server

                FORMATTING:
                  SELECT TO_CHAR(now(), 'YYYY-MM-DD HH24:MI:SS');     -- PostgreSQL
                  SELECT DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s');     -- MySQL
                  SELECT FORMAT(GETDATE(), 'yyyy-MM-dd HH:mm:ss');   -- SQL Server
                  SELECT strftime('%Y-%m-%d', 'now');                 -- SQLite

                PARSING:
                  SELECT TO_DATE('2024-01-15', 'YYYY-MM-DD');        -- PostgreSQL
                  SELECT STR_TO_DATE('2024-01-15', '%Y-%m-%d');      -- MySQL
                  SELECT CAST('2024-01-15' AS DATE);                 -- Standard

                TRUNCATE:
                  SELECT DATE_TRUNC('month', created_at);            -- PostgreSQL
                  SELECT DATE(created_at);                           -- MySQL (truncate to day)

                COMMON PATTERNS:
                  -- Records from last 30 days
                  WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'

                  -- Records from this month
                  WHERE EXTRACT(MONTH FROM created_at) = EXTRACT(MONTH FROM CURRENT_DATE)
                    AND EXTRACT(YEAR FROM created_at) = EXTRACT(YEAR FROM CURRENT_DATE)
                """);

        REFERENCE_MAP.put("case_expressions", """
                CASE expressions add conditional logic to SQL queries.

                SIMPLE CASE (matching a value):
                  SELECT name, status,
                      CASE status
                          WHEN 'A' THEN 'Active'
                          WHEN 'I' THEN 'Inactive'
                          WHEN 'P' THEN 'Pending'
                          ELSE 'Unknown'
                      END AS status_label
                  FROM users;

                SEARCHED CASE (evaluating conditions):
                  SELECT name, salary,
                      CASE
                          WHEN salary >= 100000 THEN 'High'
                          WHEN salary >= 50000 THEN 'Medium'
                          WHEN salary >= 25000 THEN 'Low'
                          ELSE 'Entry'
                      END AS salary_band
                  FROM employees;

                CASE IN WHERE CLAUSE:
                  SELECT *
                  FROM orders
                  WHERE CASE
                      WHEN @status IS NOT NULL THEN status = @status
                      ELSE 1 = 1
                  END;

                CASE IN ORDER BY:
                  SELECT * FROM tasks
                  ORDER BY
                      CASE priority
                          WHEN 'critical' THEN 1
                          WHEN 'high' THEN 2
                          WHEN 'medium' THEN 3
                          WHEN 'low' THEN 4
                          ELSE 5
                      END;

                CASE WITH AGGREGATION:
                  SELECT
                      COUNT(CASE WHEN status = 'active' THEN 1 END) AS active_count,
                      COUNT(CASE WHEN status = 'inactive' THEN 1 END) AS inactive_count,
                      SUM(CASE WHEN status = 'active' THEN amount ELSE 0 END) AS active_total
                  FROM accounts;

                COALESCE (shorthand for common CASE):
                  SELECT COALESCE(nickname, first_name, 'Anonymous') AS display_name
                  FROM users;
                  -- Equivalent to:
                  -- CASE WHEN nickname IS NOT NULL THEN nickname
                  --      WHEN first_name IS NOT NULL THEN first_name
                  --      ELSE 'Anonymous' END

                NULLIF:
                  SELECT value / NULLIF(divisor, 0)  -- Avoids division by zero
                  FROM data;
                """);

        REFERENCE_MAP.put("set_operations", """
                Set operations combine results of two or more SELECT statements.

                UNION - Combine and remove duplicates:
                  SELECT name FROM customers
                  UNION
                  SELECT name FROM suppliers;

                UNION ALL - Combine and keep duplicates (faster):
                  SELECT name FROM customers
                  UNION ALL
                  SELECT name FROM suppliers;

                INTERSECT - Only rows in both queries:
                  SELECT customer_id FROM orders
                  INTERSECT
                  SELECT customer_id FROM returns;

                EXCEPT / MINUS - Rows in first but not second:
                  SELECT customer_id FROM orders
                  EXCEPT
                  SELECT customer_id FROM blacklist;
                  -- Note: Oracle uses MINUS instead of EXCEPT.

                RULES:
                - All queries must have the same number of columns.
                - Corresponding columns must have compatible data types.
                - Column names come from the first query.
                - ORDER BY applies to the final result (place at the end).

                EXAMPLE WITH ORDER BY:
                  SELECT name, 'customer' AS type FROM customers
                  UNION ALL
                  SELECT name, 'supplier' AS type FROM suppliers
                  ORDER BY name;
                """);

        REFERENCE_MAP.put("transactions", """
                Transactions ensure a group of operations either all succeed or all fail.

                BASIC SYNTAX:
                  BEGIN TRANSACTION;       -- or just BEGIN (PostgreSQL)
                  -- ... SQL statements ...
                  COMMIT;                  -- Save all changes
                  -- or --
                  ROLLBACK;                -- Undo all changes

                EXAMPLE:
                  BEGIN TRANSACTION;
                  UPDATE accounts SET balance = balance - 100 WHERE id = 1;
                  UPDATE accounts SET balance = balance + 100 WHERE id = 2;
                  COMMIT;

                SAVEPOINTS (partial rollback):
                  BEGIN;
                  INSERT INTO orders VALUES (...);
                  SAVEPOINT sp1;
                  INSERT INTO order_items VALUES (...);
                  -- Oops, undo just the items:
                  ROLLBACK TO sp1;
                  -- The order insert is still intact.
                  COMMIT;

                ISOLATION LEVELS (from least to most strict):
                  SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
                  SET TRANSACTION ISOLATION LEVEL READ COMMITTED;       -- Default (PostgreSQL, SQL Server)
                  SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;      -- Default (MySQL)
                  SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

                  READ UNCOMMITTED - Can see uncommitted changes from other transactions (dirty reads).
                  READ COMMITTED   - Only sees committed changes. May see different data on re-read.
                  REPEATABLE READ  - Same reads return same results within transaction.
                  SERIALIZABLE     - Full isolation; transactions behave as if executed sequentially.

                DIALECT DIFFERENCES:
                  MySQL:      START TRANSACTION; or BEGIN;
                  PostgreSQL: BEGIN; (auto-commits each statement outside a transaction)
                  SQL Server: BEGIN TRANSACTION;
                  SQLite:     BEGIN TRANSACTION; (file-level locking)

                TIPS:
                - Keep transactions short to reduce lock contention.
                - Always include error handling with ROLLBACK.
                - Use the lowest isolation level that meets your needs.
                """);
    }
}
