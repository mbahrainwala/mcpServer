package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP tool for Excel data analysis: CSV parsing, pivot table logic,
 * chart recommendations, conditional formatting rules, and VBA snippets.
 */
@Service
public class ExcelDataTool {

    private static final MathContext MC = new MathContext(10);

    @Tool(name = "excel_analyze_data", description = "Analyze CSV/tabular data and provide summary statistics, "
            + "data quality assessment, and recommended Excel operations. "
            + "Paste your data as CSV (comma or tab separated) and get instant analysis.")
    public String analyzeData(
            @ToolParam(description = "CSV data with headers. Rows separated by newlines, "
                    + "columns by commas or tabs. Example: 'Name,Score,Grade\\nAlice,95,A\\nBob,82,B'") String csvData) {

        try {
            List<String[]> rows = parseCsv(csvData);
            if (rows.size() < 2) return "Error: Need at least a header row and one data row.";

            String[] headers = rows.getFirst();
            int numCols = headers.length;
            int numRows = rows.size() - 1;

            StringBuilder sb = new StringBuilder();
            sb.append("Data Analysis\n");
            sb.append("─────────────\n");
            sb.append("Rows: ").append(numRows).append(" | Columns: ").append(numCols).append("\n");
            sb.append("Headers: ").append(String.join(", ", headers)).append("\n\n");

            // Analyze each column
            sb.append("COLUMN ANALYSIS\n");
            for (int col = 0; col < numCols; col++) {
                String header = headers[col];
                List<String> values = new ArrayList<>();
                for (int row = 1; row < rows.size(); row++) {
                    if (col < rows.get(row).length) values.add(rows.get(row)[col].strip());
                }

                sb.append("\n  ").append(header).append(":\n");

                // Detect type
                int blanks = (int) values.stream().filter(String::isEmpty).count();
                List<Double> numbers = new ArrayList<>();
                for (String v : values) {
                    try { if (!v.isEmpty()) numbers.add(Double.parseDouble(v.replace(",", ""))); } catch (NumberFormatException ignored) {}
                }

                boolean isNumeric = numbers.size() > values.size() * 0.7;

                if (isNumeric && !numbers.isEmpty()) {
                    DoubleSummaryStatistics stats = numbers.stream().mapToDouble(Double::doubleValue).summaryStatistics();
                    sb.append("    Type: Numeric\n");
                    sb.append("    Min: ").append(fmt(stats.getMin())).append(" | Max: ").append(fmt(stats.getMax())).append("\n");
                    sb.append("    Mean: ").append(fmt(stats.getAverage())).append(" | Sum: ").append(fmt(stats.getSum())).append("\n");
                    sb.append("    Count: ").append(numbers.size());
                    if (blanks > 0) sb.append(" | Blanks: ").append(blanks);
                    sb.append("\n");
                } else {
                    long unique = values.stream().filter(v -> !v.isEmpty()).distinct().count();
                    sb.append("    Type: Text\n");
                    sb.append("    Unique values: ").append(unique).append("\n");
                    if (blanks > 0) sb.append("    Blanks: ").append(blanks).append("\n");
                    if (unique <= 10) {
                        Map<String, Long> freq = values.stream()
                                .filter(v -> !v.isEmpty())
                                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));
                        sb.append("    Values: ");
                        freq.entrySet().stream()
                                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                                .limit(10)
                                .forEach(e -> sb.append(e.getKey()).append("(").append(e.getValue()).append(") "));
                        sb.append("\n");
                    }
                }
            }

            // Data quality
            sb.append("\nDATA QUALITY\n");
            int totalCells = numRows * numCols;
            int missingCells = 0;
            for (int row = 1; row < rows.size(); row++) {
                if (rows.get(row).length < numCols) missingCells += numCols - rows.get(row).length;
                for (String cell : rows.get(row)) {
                    if (cell.isBlank()) missingCells++;
                }
            }
            double completeness = (1 - (double) missingCells / totalCells) * 100;
            sb.append("  Completeness: ").append(fmt(completeness)).append("%\n");
            sb.append("  Missing cells: ").append(missingCells).append("/").append(totalCells).append("\n");

            // Suggested formulas
            sb.append("\nSUGGESTED FORMULAS\n");
            for (int col = 0; col < numCols; col++) {
                String colLetter = columnLetter(col);
                List<Double> numbers = new ArrayList<>();
                for (int row = 1; row < rows.size(); row++) {
                    try {
                        if (col < rows.get(row).length && !rows.get(row)[col].isBlank())
                            numbers.add(Double.parseDouble(rows.get(row)[col].strip().replace(",", "")));
                    } catch (NumberFormatException ignored) {}
                }

                if (numbers.size() > values(rows, col).size() * 0.7) {
                    int lastRow = numRows + 1;
                    sb.append("  ").append(headers[col]).append(":\n");
                    sb.append("    =SUM(").append(colLetter).append("2:").append(colLetter).append(lastRow).append(")\n");
                    sb.append("    =AVERAGE(").append(colLetter).append("2:").append(colLetter).append(lastRow).append(")\n");
                    sb.append("    =COUNTIF(").append(colLetter).append("2:").append(colLetter).append(lastRow).append(",\">0\")\n");
                }
            }

            return sb.toString();

        } catch (Exception e) {
            return "Error analyzing data: " + e.getMessage();
        }
    }

    @Tool(name = "excel_chart_recommend", description = "Recommend the best Excel chart type for your data. "
            + "Describe your data and what you want to show, and get chart type, setup instructions, "
            + "and formatting tips.")
    public String chartRecommend(
            @ToolParam(description = "Description of your data and what you want to visualize. "
                    + "Examples: 'monthly sales over 12 months', 'market share by company', "
                    + "'correlation between price and demand', 'budget vs actual by department'") String description) {

        String lower = description.toLowerCase();

        StringBuilder sb = new StringBuilder();
        sb.append("Chart Recommendation\n");
        sb.append("────────────────────\n");
        sb.append("Data: ").append(description).append("\n\n");

        if (lower.contains("over time") || lower.contains("trend") || lower.contains("monthly")
                || lower.contains("quarterly") || lower.contains("yearly") || lower.contains("time series")
                || lower.contains("growth")) {
            sb.append("RECOMMENDED: Line Chart\n");
            sb.append("─────────────────────\n");
            sb.append("Best for: Showing trends and changes over time\n\n");
            sb.append("SETUP:\n");
            sb.append("  1. Select data with dates/periods in column A, values in column B+\n");
            sb.append("  2. Insert → Line Chart → Line with Markers\n");
            sb.append("  3. Right-click axis → Format Axis to adjust scale\n\n");
            sb.append("ALTERNATIVES:\n");
            sb.append("  - Area chart: emphasizes volume/magnitude over time\n");
            sb.append("  - Column chart: better for discrete time periods (months, quarters)\n\n");
            sb.append("FORMATTING TIPS:\n");
            sb.append("  - Add data labels for key points only (right-click series → Add Data Labels)\n");
            sb.append("  - Use a secondary axis if comparing different scales\n");
            sb.append("  - Add a trendline: right-click series → Add Trendline → Linear/Moving Average");

        } else if (lower.contains("share") || lower.contains("proportion") || lower.contains("percentage")
                || lower.contains("composition") || lower.contains("breakdown") || lower.contains("distribution of categories")) {
            sb.append("RECOMMENDED: Pie Chart (≤6 categories) or Donut Chart\n");
            sb.append("─────────────────────────────────────────────────────\n");
            sb.append("Best for: Showing parts of a whole (100%)\n\n");
            sb.append("SETUP:\n");
            sb.append("  1. Categories in column A, values in column B\n");
            sb.append("  2. Insert → Pie Chart → 2D Pie\n");
            sb.append("  3. Click pie → Add Data Labels → check Percentage\n\n");
            sb.append("ALTERNATIVES:\n");
            sb.append("  - Treemap: for many categories with hierarchies\n");
            sb.append("  - Stacked bar (100%): easier to compare across groups\n");
            sb.append("  - Waffle chart: more accessible, modern look\n\n");
            sb.append("⚠ Avoid pie charts with >6 slices — use a bar chart instead.");

        } else if (lower.contains("compare") || lower.contains("vs") || lower.contains("versus")
                || lower.contains("by department") || lower.contains("by category") || lower.contains("ranking")
                || lower.contains("actual")) {
            sb.append("RECOMMENDED: Clustered Bar/Column Chart\n");
            sb.append("──────────────────────────────────────\n");
            sb.append("Best for: Comparing values across categories\n\n");
            sb.append("SETUP:\n");
            sb.append("  1. Categories in column A, series in columns B, C, etc.\n");
            sb.append("  2. Insert → Column Chart → Clustered Column\n");
            sb.append("  3. Use horizontal bar if category names are long\n\n");
            sb.append("ALTERNATIVES:\n");
            sb.append("  - Stacked column: shows totals AND composition\n");
            sb.append("  - Tornado chart (back-to-back bars): for budget vs actual\n");
            sb.append("  - Lollipop chart: cleaner look, less ink\n\n");
            sb.append("FORMATTING TIPS:\n");
            sb.append("  - Sort bars by value for easier comparison\n");
            sb.append("  - Add data labels, remove gridlines\n");
            sb.append("  - Use consistent colors across reports");

        } else if (lower.contains("correlation") || lower.contains("relationship") || lower.contains("scatter")
                || lower.contains("regression") || lower.contains("x and y") || lower.contains("two variables")) {
            sb.append("RECOMMENDED: Scatter Plot (XY Chart)\n");
            sb.append("────────────────────────────────────\n");
            sb.append("Best for: Showing relationships between two numeric variables\n\n");
            sb.append("SETUP:\n");
            sb.append("  1. X values in column A, Y values in column B\n");
            sb.append("  2. Insert → Scatter → Scatter with only Markers\n");
            sb.append("  3. Right-click data → Add Trendline → choose type\n\n");
            sb.append("TRENDLINE OPTIONS:\n");
            sb.append("  - Linear: straight-line relationship\n");
            sb.append("  - Polynomial: curved relationship\n");
            sb.append("  - Check 'Display R-squared value' to show fit quality\n\n");
            sb.append("ALTERNATIVE:\n");
            sb.append("  - Bubble chart: adds a third variable as bubble size");

        } else if (lower.contains("heat") || lower.contains("matrix") || lower.contains("intensity")
                || lower.contains("density")) {
            sb.append("RECOMMENDED: Conditional Formatting Heatmap\n");
            sb.append("───────────────────────────────────────────\n");
            sb.append("Best for: Spotting patterns in a table of numbers\n\n");
            sb.append("SETUP:\n");
            sb.append("  1. Select the numeric range\n");
            sb.append("  2. Home → Conditional Formatting → Color Scales\n");
            sb.append("  3. Choose Green-Yellow-Red or custom\n\n");
            sb.append("ALTERNATIVE:\n");
            sb.append("  - Surface chart: for 3D continuous data\n");
            sb.append("  - Treemap: for hierarchical heatmaps");

        } else if (lower.contains("funnel") || lower.contains("conversion") || lower.contains("pipeline")
                || lower.contains("stages")) {
            sb.append("RECOMMENDED: Funnel Chart\n");
            sb.append("─────────────────────────\n");
            sb.append("Best for: Showing progressive reduction (sales funnel, conversion rates)\n\n");
            sb.append("SETUP:\n");
            sb.append("  1. Stages in column A (ordered), values in column B\n");
            sb.append("  2. Insert → Funnel (Excel 2016+)\n\n");
            sb.append("ALTERNATIVE (older Excel):\n");
            sb.append("  - Stacked bar chart with hidden series\n");
            sb.append("  - Use helper columns for centering");

        } else if (lower.contains("gantt") || lower.contains("timeline") || lower.contains("schedule")
                || lower.contains("project")) {
            sb.append("RECOMMENDED: Stacked Bar Chart (Gantt-style)\n");
            sb.append("─────────────────────────────────────────────\n");
            sb.append("Best for: Project timelines and schedules\n\n");
            sb.append("SETUP:\n");
            sb.append("  1. Tasks in col A, Start dates in col B, Duration in col C\n");
            sb.append("  2. Insert → Stacked Bar Chart\n");
            sb.append("  3. Format first series (start dates) with no fill\n");
            sb.append("  4. Format axis: set minimum to project start date serial number\n\n");
            sb.append("TIPS:\n");
            sb.append("  - Reverse task order (chart reads bottom-up by default)\n");
            sb.append("  - Add milestones as diamond markers\n");
            sb.append("  - Consider using a dedicated tool (MS Project) for complex schedules");

        } else {
            sb.append("CHART SELECTION GUIDE\n\n");
            sb.append("Choose based on what you want to show:\n\n");
            sb.append("  COMPARISON          → Bar/Column chart\n");
            sb.append("  TREND OVER TIME     → Line chart\n");
            sb.append("  PART OF WHOLE       → Pie/Donut (≤6 cats) or Treemap\n");
            sb.append("  RELATIONSHIP        → Scatter plot\n");
            sb.append("  DISTRIBUTION        → Histogram or Box plot\n");
            sb.append("  RANKING             → Horizontal bar chart (sorted)\n");
            sb.append("  GEOGRAPHICAL        → Map chart (Excel 365)\n");
            sb.append("  FLOW/PROCESS        → Funnel chart\n");
            sb.append("  SCHEDULE            → Gantt (stacked bar)\n");
            sb.append("  MULTIPLE DIMENSIONS → Pivot chart\n\n");
            sb.append("Describe your specific data for a tailored recommendation.");
        }

        return sb.toString();
    }

    @Tool(name = "excel_conditional_format", description = "Generate conditional formatting rules for Excel. "
            + "Describe what you want to highlight and get step-by-step setup instructions with formulas.")
    public String conditionalFormat(
            @ToolParam(description = "What to highlight. Examples: 'highlight duplicates in column A', "
                    + "'color cells above average', 'traffic light for scores (red<60, yellow<80, green≥80)', "
                    + "'highlight entire row if status is Overdue', 'alternating row colors'") String description) {

        String lower = description.toLowerCase();
        StringBuilder sb = new StringBuilder();
        sb.append("Conditional Formatting Rule\n");
        sb.append("───────────────────────────\n");
        sb.append("Request: ").append(description).append("\n\n");

        if (lower.contains("duplicate")) {
            sb.append("HIGHLIGHT DUPLICATES\n\n");
            sb.append("Method 1 — Built-in:\n");
            sb.append("  1. Select the range (e.g. A1:A100)\n");
            sb.append("  2. Home → Conditional Formatting → Highlight Cell Rules → Duplicate Values\n");
            sb.append("  3. Choose formatting\n\n");
            sb.append("Method 2 — Custom formula (more control):\n");
            sb.append("  Formula: =COUNTIF($A:$A,$A1)>1\n");
            sb.append("  Apply to: =$A$1:$A$100\n\n");
            sb.append("HIGHLIGHT FIRST OCCURRENCE ONLY:\n");
            sb.append("  Formula: =COUNTIF($A$1:$A1,$A1)>1\n");
            sb.append("  (Only marks 2nd+ occurrence, leaving the first unmarked)");

        } else if (lower.contains("above average") || lower.contains("below average")) {
            sb.append("HIGHLIGHT ABOVE/BELOW AVERAGE\n\n");
            sb.append("Built-in:\n");
            sb.append("  1. Select range → Conditional Formatting → Top/Bottom Rules\n");
            sb.append("  2. Choose 'Above Average' or 'Below Average'\n\n");
            sb.append("Custom formula:\n");
            sb.append("  Above average: =A1>AVERAGE($A$1:$A$100)\n");
            sb.append("  Below average: =A1<AVERAGE($A$1:$A$100)\n");
            sb.append("  Apply to: =$A$1:$A$100");

        } else if (lower.contains("traffic light") || lower.contains("red") || lower.contains("green")
                || lower.contains("score") || lower.contains("grade") || lower.contains("threshold")) {
            sb.append("TRAFFIC LIGHT (3-COLOR RULES)\n\n");
            sb.append("Method 1 — Icon Sets:\n");
            sb.append("  1. Select range → Conditional Formatting → Icon Sets → 3 Traffic Lights\n");
            sb.append("  2. Click 'Manage Rules' → Edit to set custom thresholds\n\n");
            sb.append("Method 2 — Multiple rules (more control):\n");
            sb.append("  Select range, then create 3 rules (HIGHEST PRIORITY FIRST):\n\n");
            sb.append("  Rule 1 (Red):    =A1<60     → Red fill\n");
            sb.append("  Rule 2 (Yellow): =A1<80     → Yellow fill\n");
            sb.append("  Rule 3 (Green):  =A1>=80    → Green fill\n\n");
            sb.append("  Or use: Conditional Formatting → Color Scales → Red-Yellow-Green\n\n");
            sb.append("💡 Adjust thresholds (60, 80) to match your criteria.\n");
            sb.append("Apply to: select all score cells → create rules under 'New Rule' → 'Use a formula'");

        } else if (lower.contains("entire row") || lower.contains("whole row")) {
            sb.append("HIGHLIGHT ENTIRE ROW BASED ON A CELL VALUE\n\n");
            sb.append("STEPS:\n");
            sb.append("  1. Select the ENTIRE data range (e.g. $A$1:$F$100)\n");
            sb.append("  2. Conditional Formatting → New Rule → 'Use a formula'\n");
            sb.append("  3. Formula (assuming status is in column E):\n\n");
            sb.append("     =$E1=\"Overdue\"        (highlights row if E = 'Overdue')\n");
            sb.append("     =$E1=\"Complete\"        (for a different color)\n\n");
            sb.append("KEY: Lock the column ($E) but NOT the row (1 without $)\n");
            sb.append("This lets the formula adjust row-by-row while always checking column E.\n\n");
            sb.append("COMMON PATTERNS:\n");
            sb.append("  =$E1=\"Overdue\"              → row is overdue\n");
            sb.append("  =$F1<TODAY()                 → due date has passed\n");
            sb.append("  =AND($E1=\"Open\",$F1<TODAY()) → open AND past due");

        } else if (lower.contains("alternating") || lower.contains("zebra") || lower.contains("stripe")
                || lower.contains("banded")) {
            sb.append("ALTERNATING ROW COLORS (ZEBRA STRIPES)\n\n");
            sb.append("Method 1 — Table (recommended):\n");
            sb.append("  Select data → Insert → Table (Ctrl+T) → check 'Banded Rows'\n\n");
            sb.append("Method 2 — Conditional Formatting:\n");
            sb.append("  1. Select range → Conditional Formatting → New Rule → 'Use a formula'\n");
            sb.append("  2. Formula: =MOD(ROW(),2)=0\n");
            sb.append("  3. Format → Fill → choose light gray or color\n");
            sb.append("  4. Apply to: =$A$1:$Z$1000\n\n");
            sb.append("For every 3rd row: =MOD(ROW()-1,3)=0");

        } else if (lower.contains("data bar") || lower.contains("bar in cell") || lower.contains("in-cell")) {
            sb.append("IN-CELL DATA BARS\n\n");
            sb.append("STEPS:\n");
            sb.append("  1. Select the numeric range\n");
            sb.append("  2. Conditional Formatting → Data Bars → choose style\n");
            sb.append("  3. To customize: Manage Rules → Edit Rule\n\n");
            sb.append("TIPS:\n");
            sb.append("  - 'Show Bar Only' hides the number (good for dashboards)\n");
            sb.append("  - Negative values get a different color automatically\n");
            sb.append("  - Use solid fill for printing, gradient for screen");

        } else if (lower.contains("blank") || lower.contains("empty") || lower.contains("missing")) {
            sb.append("HIGHLIGHT BLANK/EMPTY CELLS\n\n");
            sb.append("Built-in:\n");
            sb.append("  Conditional Formatting → Highlight Cell Rules → More Rules\n");
            sb.append("  → Format only cells that contain → Blanks\n\n");
            sb.append("Formula-based:\n");
            sb.append("  =LEN(TRIM(A1))=0\n");
            sb.append("  (Catches truly empty cells AND cells with only spaces)");

        } else {
            sb.append("CONDITIONAL FORMATTING QUICK REFERENCE\n\n");
            sb.append("BUILT-IN RULES (Home → Conditional Formatting):\n");
            sb.append("  Highlight Cell Rules → Greater Than, Less Than, Equal To, Text Contains\n");
            sb.append("  Top/Bottom Rules → Top 10, Above Average\n");
            sb.append("  Data Bars, Color Scales, Icon Sets\n\n");
            sb.append("CUSTOM FORMULA RULES:\n");
            sb.append("  Duplicates:      =COUNTIF($A:$A,$A1)>1\n");
            sb.append("  Entire row:      =$E1=\"value\" (lock column, not row)\n");
            sb.append("  Alternating:     =MOD(ROW(),2)=0\n");
            sb.append("  Past due:        =AND($D1<TODAY(),$E1<>\"Complete\")\n");
            sb.append("  Contains text:   =SEARCH(\"keyword\",A1)>0\n");
            sb.append("  Top 3 values:    =A1>=LARGE($A:$A,3)\n\n");
            sb.append("Describe what you need for specific instructions.");
        }

        return sb.toString();
    }

    @Tool(name = "excel_pivot_guide", description = "Get step-by-step instructions for creating a pivot table. "
            + "Describe your data and what summary you need, and get field placement, "
            + "calculated fields, and formatting guidance.")
    public String pivotGuide(
            @ToolParam(description = "Describe your data columns and what you want to summarize. "
                    + "Example: 'I have Date, Product, Region, Salesperson, Revenue, Quantity. "
                    + "I want total revenue by product and region, with monthly breakdown.'") String description) {

        StringBuilder sb = new StringBuilder();
        sb.append("Pivot Table Guide\n");
        sb.append("──────────────────\n");
        sb.append("Scenario: ").append(description).append("\n\n");

        sb.append("STEP 1: CREATE PIVOT TABLE\n");
        sb.append("  1. Click any cell in your data\n");
        sb.append("  2. Insert → PivotTable\n");
        sb.append("  3. Choose 'New Worksheet' or 'Existing Worksheet'\n");
        sb.append("  4. Click OK\n\n");

        sb.append("STEP 2: ARRANGE FIELDS\n");
        sb.append("  Drag fields from the Field List to these areas:\n\n");

        String lower = description.toLowerCase();

        // Detect common patterns
        if (lower.contains("revenue") || lower.contains("sales") || lower.contains("amount")) {
            sb.append("  ROWS:     Product (or the main category)\n");
            if (lower.contains("region") || lower.contains("area") || lower.contains("location"))
                sb.append("  COLUMNS:  Region\n");
            else if (lower.contains("month") || lower.contains("quarter") || lower.contains("year"))
                sb.append("  COLUMNS:  Date (Excel auto-groups into months/quarters)\n");
            sb.append("  VALUES:   Sum of Revenue\n");
            if (lower.contains("quantity") || lower.contains("count"))
                sb.append("            Count/Sum of Quantity\n");
            if (lower.contains("month") && (lower.contains("region") || lower.contains("product")))
                sb.append("  FILTERS:  Date (or use Timeline slicer)\n");
        } else {
            sb.append("  ROWS:     Your main grouping category\n");
            sb.append("  COLUMNS:  Your secondary grouping (or leave empty)\n");
            sb.append("  VALUES:   The numbers to summarize (Sum, Count, Average)\n");
            sb.append("  FILTERS:  Fields to filter the entire pivot by\n");
        }

        sb.append("\nSTEP 3: CUSTOMIZE\n");
        sb.append("  - Change summary type: click dropdown on Values field → Value Field Settings\n");
        sb.append("    → Choose Sum, Count, Average, Max, Min, etc.\n");
        sb.append("  - Show as %: Value Field Settings → Show Values As → % of Grand Total\n");
        sb.append("  - Group dates: right-click a date → Group → choose Months/Quarters/Years\n");
        sb.append("  - Sort: click dropdown arrow on Row/Column → Sort\n");
        sb.append("  - Filter top N: click dropdown → Value Filters → Top 10\n");

        sb.append("\nSTEP 4: CALCULATED FIELDS\n");
        sb.append("  PivotTable Analyze → Fields, Items & Sets → Calculated Field\n\n");
        sb.append("  Common calculated fields:\n");
        sb.append("    Profit Margin:  =Revenue - Cost\n");
        sb.append("    Avg Price:      =Revenue / Quantity\n");
        sb.append("    Growth:         Not possible in calculated fields — use helper column\n");

        sb.append("\nSTEP 5: FORMATTING\n");
        sb.append("  - Design tab → Report Layout → Show in Tabular Form (best for exports)\n");
        sb.append("  - Design tab → Subtotals → Do Not Show Subtotals (cleaner look)\n");
        sb.append("  - Design tab → Grand Totals → On for Rows and Columns\n");
        sb.append("  - Number format: right-click values → Number Format → choose format\n");

        sb.append("\nSTEP 6: ENHANCEMENTS\n");
        sb.append("  - Add Slicer: PivotTable Analyze → Insert Slicer (visual filter buttons)\n");
        sb.append("  - Add Timeline: PivotTable Analyze → Insert Timeline (date slider)\n");
        sb.append("  - Pivot Chart: PivotTable Analyze → PivotChart\n");
        sb.append("  - Refresh: right-click → Refresh (updates when source data changes)\n");

        sb.append("\nPRO TIPS\n");
        sb.append("  - Use a Table (Ctrl+T) as source — pivot auto-includes new rows\n");
        sb.append("  - Name your pivot: PivotTable Analyze → PivotTable Name\n");
        sb.append("  - Multiple pivots can share one cache (saves memory)\n");
        sb.append("  - Use GETPIVOTDATA to reference pivot values in formulas");

        return sb.toString();
    }

    @Tool(name = "excel_vba_snippet", description = "Generate VBA macro code for common Excel automation tasks. "
            + "Describe what you want to automate and get ready-to-use VBA code with instructions.")
    public String vbaSnippet(
            @ToolParam(description = "What to automate. Examples: 'loop through rows and highlight if value > 100', "
                    + "'copy data from multiple sheets into one', 'auto-save as PDF', "
                    + "'send email for each row', 'create a new sheet for each unique value in column A', "
                    + "'protect all sheets', 'remove blank rows', 'sort multiple sheets'") String description) {

        String lower = description.toLowerCase();
        StringBuilder sb = new StringBuilder();
        sb.append("VBA Macro\n");
        sb.append("─────────\n");
        sb.append("Task: ").append(description).append("\n\n");

        sb.append("HOW TO USE:\n");
        sb.append("  1. Press Alt+F11 to open VBA Editor\n");
        sb.append("  2. Insert → Module\n");
        sb.append("  3. Paste the code below\n");
        sb.append("  4. Press F5 to run, or close editor and use Alt+F8\n");
        sb.append("  5. Save as .xlsm (macro-enabled workbook)\n\n");
        sb.append("CODE:\n");
        sb.append("─────\n");

        if (lower.contains("loop") && (lower.contains("highlight") || lower.contains("color"))) {
            sb.append("Sub HighlightCells()\n");
            sb.append("    Dim ws As Worksheet\n");
            sb.append("    Dim lastRow As Long\n");
            sb.append("    Dim i As Long\n");
            sb.append("    \n");
            sb.append("    Set ws = ActiveSheet\n");
            sb.append("    lastRow = ws.Cells(ws.Rows.Count, \"A\").End(xlUp).Row\n");
            sb.append("    \n");
            sb.append("    For i = 2 To lastRow  'Start from row 2 (skip header)\n");
            sb.append("        If ws.Cells(i, 1).Value > 100 Then  'Column A > 100\n");
            sb.append("            ws.Cells(i, 1).Interior.Color = RGB(255, 200, 200)  'Light red\n");
            sb.append("        End If\n");
            sb.append("    Next i\n");
            sb.append("    \n");
            sb.append("    MsgBox \"Done! Processed \" & lastRow - 1 & \" rows.\"\n");
            sb.append("End Sub");

        } else if (lower.contains("blank row") || lower.contains("empty row") || lower.contains("delete row")) {
            sb.append("Sub RemoveBlankRows()\n");
            sb.append("    Dim ws As Worksheet\n");
            sb.append("    Dim lastRow As Long\n");
            sb.append("    Dim i As Long\n");
            sb.append("    Dim deleted As Long\n");
            sb.append("    \n");
            sb.append("    Set ws = ActiveSheet\n");
            sb.append("    lastRow = ws.Cells(ws.Rows.Count, \"A\").End(xlUp).Row\n");
            sb.append("    deleted = 0\n");
            sb.append("    \n");
            sb.append("    'Loop backwards to avoid skipping rows\n");
            sb.append("    For i = lastRow To 1 Step -1\n");
            sb.append("        If Application.CountA(ws.Rows(i)) = 0 Then\n");
            sb.append("            ws.Rows(i).Delete\n");
            sb.append("            deleted = deleted + 1\n");
            sb.append("        End If\n");
            sb.append("    Next i\n");
            sb.append("    \n");
            sb.append("    MsgBox \"Deleted \" & deleted & \" blank rows.\"\n");
            sb.append("End Sub");

        } else if (lower.contains("pdf") || lower.contains("export")) {
            sb.append("Sub ExportToPDF()\n");
            sb.append("    Dim filePath As String\n");
            sb.append("    Dim fileName As String\n");
            sb.append("    \n");
            sb.append("    fileName = Left(ThisWorkbook.Name, InStrRev(ThisWorkbook.Name, \".\") - 1)\n");
            sb.append("    filePath = ThisWorkbook.Path & \"\\\" & fileName & \"_\" & Format(Now, \"YYYYMMDD\") & \".pdf\"\n");
            sb.append("    \n");
            sb.append("    ActiveSheet.ExportAsFixedFormat _\n");
            sb.append("        Type:=xlTypePDF, _\n");
            sb.append("        FileName:=filePath, _\n");
            sb.append("        Quality:=xlQualityStandard, _\n");
            sb.append("        IncludeDocProperties:=True, _\n");
            sb.append("        OpenAfterPublish:=True\n");
            sb.append("    \n");
            sb.append("    MsgBox \"Exported to: \" & filePath\n");
            sb.append("End Sub");

        } else if (lower.contains("copy") && (lower.contains("sheet") || lower.contains("consolidat"))) {
            sb.append("Sub ConsolidateSheets()\n");
            sb.append("    Dim ws As Worksheet\n");
            sb.append("    Dim masterWs As Worksheet\n");
            sb.append("    Dim lastRow As Long\n");
            sb.append("    Dim masterRow As Long\n");
            sb.append("    Dim headerCopied As Boolean\n");
            sb.append("    \n");
            sb.append("    'Create or clear master sheet\n");
            sb.append("    On Error Resume Next\n");
            sb.append("    Set masterWs = Worksheets(\"Consolidated\")\n");
            sb.append("    On Error GoTo 0\n");
            sb.append("    If masterWs Is Nothing Then\n");
            sb.append("        Set masterWs = Worksheets.Add(After:=Worksheets(Worksheets.Count))\n");
            sb.append("        masterWs.Name = \"Consolidated\"\n");
            sb.append("    Else\n");
            sb.append("        masterWs.Cells.Clear\n");
            sb.append("    End If\n");
            sb.append("    \n");
            sb.append("    masterRow = 1\n");
            sb.append("    headerCopied = False\n");
            sb.append("    \n");
            sb.append("    For Each ws In ThisWorkbook.Worksheets\n");
            sb.append("        If ws.Name <> \"Consolidated\" Then\n");
            sb.append("            lastRow = ws.Cells(ws.Rows.Count, \"A\").End(xlUp).Row\n");
            sb.append("            If lastRow >= 1 Then\n");
            sb.append("                If Not headerCopied Then\n");
            sb.append("                    ws.Rows(1).Copy masterWs.Rows(masterRow)\n");
            sb.append("                    masterRow = masterRow + 1\n");
            sb.append("                    headerCopied = True\n");
            sb.append("                End If\n");
            sb.append("                If lastRow >= 2 Then\n");
            sb.append("                    ws.Rows(\"2:\" & lastRow).Copy masterWs.Rows(masterRow)\n");
            sb.append("                    masterRow = masterRow + lastRow - 1\n");
            sb.append("                End If\n");
            sb.append("            End If\n");
            sb.append("        End If\n");
            sb.append("    Next ws\n");
            sb.append("    \n");
            sb.append("    masterWs.Activate\n");
            sb.append("    MsgBox \"Consolidated \" & masterRow - 1 & \" rows from all sheets.\"\n");
            sb.append("End Sub");

        } else if (lower.contains("unique") && lower.contains("sheet")) {
            sb.append("Sub CreateSheetsFromColumn()\n");
            sb.append("    Dim ws As Worksheet\n");
            sb.append("    Dim newWs As Worksheet\n");
            sb.append("    Dim lastRow As Long\n");
            sb.append("    Dim i As Long\n");
            sb.append("    Dim val As String\n");
            sb.append("    Dim dict As Object\n");
            sb.append("    \n");
            sb.append("    Set ws = ActiveSheet\n");
            sb.append("    Set dict = CreateObject(\"Scripting.Dictionary\")\n");
            sb.append("    lastRow = ws.Cells(ws.Rows.Count, \"A\").End(xlUp).Row\n");
            sb.append("    \n");
            sb.append("    Application.ScreenUpdating = False\n");
            sb.append("    \n");
            sb.append("    For i = 2 To lastRow\n");
            sb.append("        val = CStr(ws.Cells(i, 1).Value)\n");
            sb.append("        If val <> \"\" And Not dict.Exists(val) Then\n");
            sb.append("            dict.Add val, True\n");
            sb.append("            Set newWs = Worksheets.Add(After:=Worksheets(Worksheets.Count))\n");
            sb.append("            newWs.Name = Left(val, 31)  'Sheet names max 31 chars\n");
            sb.append("            ws.Rows(1).Copy newWs.Rows(1)  'Copy header\n");
            sb.append("        End If\n");
            sb.append("    Next i\n");
            sb.append("    \n");
            sb.append("    'Copy data to respective sheets\n");
            sb.append("    For i = 2 To lastRow\n");
            sb.append("        val = CStr(ws.Cells(i, 1).Value)\n");
            sb.append("        If val <> \"\" Then\n");
            sb.append("            Set newWs = Worksheets(Left(val, 31))\n");
            sb.append("            Dim destRow As Long\n");
            sb.append("            destRow = newWs.Cells(newWs.Rows.Count, \"A\").End(xlUp).Row + 1\n");
            sb.append("            ws.Rows(i).Copy newWs.Rows(destRow)\n");
            sb.append("        End If\n");
            sb.append("    Next i\n");
            sb.append("    \n");
            sb.append("    Application.ScreenUpdating = True\n");
            sb.append("    MsgBox \"Created \" & dict.Count & \" sheets.\"\n");
            sb.append("End Sub");

        } else if (lower.contains("protect") && lower.contains("sheet")) {
            sb.append("Sub ProtectAllSheets()\n");
            sb.append("    Dim ws As Worksheet\n");
            sb.append("    Dim pw As String\n");
            sb.append("    \n");
            sb.append("    pw = InputBox(\"Enter password (leave blank for no password):\")\n");
            sb.append("    \n");
            sb.append("    For Each ws In ThisWorkbook.Worksheets\n");
            sb.append("        ws.Protect Password:=pw, DrawingObjects:=True, Contents:=True, Scenarios:=True\n");
            sb.append("    Next ws\n");
            sb.append("    \n");
            sb.append("    MsgBox \"All \" & ThisWorkbook.Worksheets.Count & \" sheets protected.\"\n");
            sb.append("End Sub\n\n");
            sb.append("Sub UnprotectAllSheets()\n");
            sb.append("    Dim ws As Worksheet\n");
            sb.append("    Dim pw As String\n");
            sb.append("    \n");
            sb.append("    pw = InputBox(\"Enter password:\")\n");
            sb.append("    \n");
            sb.append("    For Each ws In ThisWorkbook.Worksheets\n");
            sb.append("        ws.Unprotect Password:=pw\n");
            sb.append("    Next ws\n");
            sb.append("    \n");
            sb.append("    MsgBox \"All sheets unprotected.\"\n");
            sb.append("End Sub");

        } else {
            sb.append("' Template: Loop through rows and process data\n");
            sb.append("Sub ProcessData()\n");
            sb.append("    Dim ws As Worksheet\n");
            sb.append("    Dim lastRow As Long\n");
            sb.append("    Dim i As Long\n");
            sb.append("    \n");
            sb.append("    Set ws = ActiveSheet\n");
            sb.append("    lastRow = ws.Cells(ws.Rows.Count, \"A\").End(xlUp).Row\n");
            sb.append("    \n");
            sb.append("    Application.ScreenUpdating = False  'Speed up\n");
            sb.append("    \n");
            sb.append("    For i = 2 To lastRow  'Skip header\n");
            sb.append("        '--- ADD YOUR LOGIC HERE ---\n");
            sb.append("        'Example: ws.Cells(i, 3).Value = ws.Cells(i, 1).Value * ws.Cells(i, 2).Value\n");
            sb.append("    Next i\n");
            sb.append("    \n");
            sb.append("    Application.ScreenUpdating = True\n");
            sb.append("    MsgBox \"Processed \" & lastRow - 1 & \" rows.\"\n");
            sb.append("End Sub\n\n");
            sb.append("COMMON VBA PATTERNS:\n");
            sb.append("  - Loop through rows: described above\n");
            sb.append("  - Copy data between sheets: use .Copy/.Paste or .Value = .Value\n");
            sb.append("  - Create sheets: Worksheets.Add\n");
            sb.append("  - File operations: Workbooks.Open, .SaveAs, .Close\n");
            sb.append("  - Message box: MsgBox \"text\"\n");
            sb.append("  - Input: InputBox(\"prompt\")\n\n");
            sb.append("Describe your specific task for targeted code.");
        }

        return sb.toString();
    }

    @Tool(name = "excel_shortcut_reference", description = "Look up Excel keyboard shortcuts by action or category. "
            + "Covers navigation, formatting, formulas, selection, and data operations.")
    public String shortcutReference(
            @ToolParam(description = "Action or category to look up. Examples: 'paste special', 'select all', "
                    + "'navigation', 'formatting', 'formulas', 'selection', 'data', 'all'") String query) {

        String q = query.strip().toLowerCase();

        StringBuilder sb = new StringBuilder();
        sb.append("Excel Keyboard Shortcuts\n");
        sb.append("────────────────────────\n\n");

        boolean all = q.equals("all");

        if (all || q.contains("nav") || q.contains("move") || q.contains("go to")) {
            sb.append("NAVIGATION\n");
            sb.append("  Ctrl+Home          Go to cell A1\n");
            sb.append("  Ctrl+End           Go to last used cell\n");
            sb.append("  Ctrl+Arrow         Jump to edge of data region\n");
            sb.append("  Ctrl+G / F5        Go To dialog\n");
            sb.append("  Ctrl+F             Find\n");
            sb.append("  Ctrl+H             Find & Replace\n");
            sb.append("  Ctrl+PageUp/Down   Switch between sheets\n");
            sb.append("  Alt+PageUp/Down    Scroll left/right\n\n");
        }
        if (all || q.contains("select") || q.contains("highlight")) {
            sb.append("SELECTION\n");
            sb.append("  Ctrl+A             Select all (or current region)\n");
            sb.append("  Ctrl+Shift+Arrow   Select to edge of data\n");
            sb.append("  Ctrl+Shift+End     Select to last used cell\n");
            sb.append("  Ctrl+Space         Select entire column\n");
            sb.append("  Shift+Space        Select entire row\n");
            sb.append("  Ctrl+Shift+L       Toggle filters\n");
            sb.append("  Ctrl+Click         Select multiple non-adjacent cells\n\n");
        }
        if (all || q.contains("format") || q.contains("bold") || q.contains("font") || q.contains("color")) {
            sb.append("FORMATTING\n");
            sb.append("  Ctrl+B             Bold\n");
            sb.append("  Ctrl+I             Italic\n");
            sb.append("  Ctrl+U             Underline\n");
            sb.append("  Ctrl+1             Format Cells dialog\n");
            sb.append("  Ctrl+Shift+$       Currency format\n");
            sb.append("  Ctrl+Shift+%       Percentage format\n");
            sb.append("  Ctrl+Shift+#       Date format\n");
            sb.append("  Ctrl+Shift+~       General format\n");
            sb.append("  Alt+H+H            Fill color\n");
            sb.append("  Alt+H+FC           Font color\n");
            sb.append("  Ctrl+Shift+&       Apply border (outline)\n");
            sb.append("  Ctrl+Shift+_       Remove border\n\n");
        }
        if (all || q.contains("formula") || q.contains("enter") || q.contains("sum") || q.contains("function")) {
            sb.append("FORMULAS\n");
            sb.append("  F2                 Edit cell\n");
            sb.append("  Tab                Accept autocomplete\n");
            sb.append("  F4                 Toggle absolute reference ($)\n");
            sb.append("  Alt+=              AutoSum\n");
            sb.append("  Ctrl+`             Show/hide formulas\n");
            sb.append("  Ctrl+Shift+Enter   Array formula (legacy)\n");
            sb.append("  F9                 Evaluate part of formula (in edit mode)\n");
            sb.append("  Ctrl+[             Trace precedents (go to source)\n");
            sb.append("  Ctrl+Shift+{       Select all precedent cells\n");
            sb.append("  Shift+F3           Insert Function dialog\n\n");
        }
        if (all || q.contains("data") || q.contains("paste") || q.contains("copy") || q.contains("insert") || q.contains("delete")) {
            sb.append("DATA & EDITING\n");
            sb.append("  Ctrl+C             Copy\n");
            sb.append("  Ctrl+X             Cut\n");
            sb.append("  Ctrl+V             Paste\n");
            sb.append("  Ctrl+Alt+V         Paste Special\n");
            sb.append("  Ctrl+D             Fill down\n");
            sb.append("  Ctrl+R             Fill right\n");
            sb.append("  Ctrl+Z             Undo\n");
            sb.append("  Ctrl+Y             Redo\n");
            sb.append("  Ctrl+T             Create Table\n");
            sb.append("  Ctrl+-             Delete cells/rows/columns\n");
            sb.append("  Ctrl+Shift++       Insert cells/rows/columns\n");
            sb.append("  Ctrl+;             Enter current date\n");
            sb.append("  Ctrl+Shift+;       Enter current time\n");
            sb.append("  Alt+Enter          New line in cell\n");
            sb.append("  Ctrl+Enter         Fill all selected cells with same value\n\n");
        }
        if (all || q.contains("file") || q.contains("save") || q.contains("print") || q.contains("workbook")) {
            sb.append("FILE & WORKBOOK\n");
            sb.append("  Ctrl+N             New workbook\n");
            sb.append("  Ctrl+O             Open\n");
            sb.append("  Ctrl+S             Save\n");
            sb.append("  F12                Save As\n");
            sb.append("  Ctrl+P             Print\n");
            sb.append("  Ctrl+W             Close workbook\n");
            sb.append("  Alt+F11            Open VBA Editor\n");
            sb.append("  Alt+F8             Macro dialog\n\n");
        }

        if (sb.toString().endsWith("────────────────────────\n\n")) {
            sb.append("No shortcuts matched '").append(query).append("'.\n");
            sb.append("Try: 'navigation', 'selection', 'formatting', 'formulas', 'data', 'file', or 'all'");
        }

        return sb.toString();
    }

    // ── Helpers ──

    private List<String[]> parseCsv(String data) {
        List<String[]> rows = new ArrayList<>();
        for (String line : data.split("\\n")) {
            if (line.contains("\t")) {
                rows.add(line.split("\t", -1));
            } else {
                rows.add(line.split(",", -1));
            }
        }
        return rows;
    }

    private List<String> values(List<String[]> rows, int col) {
        List<String> vals = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            if (col < rows.get(i).length) vals.add(rows.get(i)[col].strip());
        }
        return vals;
    }

    private String columnLetter(int index) {
        StringBuilder sb = new StringBuilder();
        index++;
        while (index > 0) {
            index--;
            sb.insert(0, (char) ('A' + index % 26));
            index /= 26;
        }
        return sb.toString();
    }

    private String fmt(double v) {
        if (v == Math.floor(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return BigDecimal.valueOf(v).round(MC).stripTrailingZeros().toPlainString();
    }
}
