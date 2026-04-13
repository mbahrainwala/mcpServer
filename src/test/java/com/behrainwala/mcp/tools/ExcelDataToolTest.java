package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelDataToolTest {

    private ExcelDataTool tool;

    @BeforeEach
    void setUp() {
        tool = new ExcelDataTool();
    }

    // ── analyzeData ─────────────────────────────────────────

    @Nested
    class AnalyzeDataTests {

        @Test
        void headerOnly_returnsError() {
            String result = tool.analyzeData("Header");
            assertThat(result).isEqualTo("Error: Need at least a header row and one data row.");
        }

        @Test
        void emptyString_returnsError() {
            String result = tool.analyzeData("");
            assertThat(result).contains("Error");
        }

        @Test
        void numericColumn_showsStatistics() {
            String csv = "Name,Score\nAlice,95\nBob,82\nCarol,78";
            String result = tool.analyzeData(csv);
            assertThat(result)
                    .contains("Data Analysis")
                    .contains("Rows: 3")
                    .contains("Columns: 2")
                    .contains("Headers: Name, Score")
                    .contains("Type: Numeric")
                    .contains("Min:").contains("Max:")
                    .contains("Mean:").contains("Sum:")
                    .contains("Count:");
        }

        @Test
        void textColumn_showsUniqueValues() {
            String csv = "Name,Score\nAlice,95\nBob,82";
            String result = tool.analyzeData(csv);
            assertThat(result)
                    .contains("Name:")
                    .contains("Type: Text")
                    .contains("Unique values: 2");
        }

        @Test
        void textColumnFewUnique_showsFrequencies() {
            String csv = "Status\nOpen\nClosed\nOpen\nOpen\nClosed";
            String result = tool.analyzeData(csv);
            assertThat(result)
                    .contains("Type: Text")
                    .contains("Unique values: 2")
                    .contains("Values:")
                    .contains("Open(3)")
                    .contains("Closed(2)");
        }

        @Test
        void textColumnMoreThan10Unique_noFrequencies() {
            StringBuilder csv = new StringBuilder("Item\n");
            for (int i = 1; i <= 12; i++) {
                csv.append("item").append(i).append("\n");
            }
            String result = tool.analyzeData(csv.toString().trim());
            assertThat(result)
                    .contains("Type: Text")
                    .contains("Unique values: 12")
                    .doesNotContain("Values:");
        }

        @Test
        void blanksInNumericColumn_showsBlanks() {
            String csv = "Value\n10\n\n30";
            String result = tool.analyzeData(csv);
            assertThat(result).contains("Blanks:");
        }

        @Test
        void blanksInTextColumn_showsBlanks() {
            String csv = "Name\nAlice\n\nBob";
            String result = tool.analyzeData(csv);
            assertThat(result).contains("Blanks:");
        }

        @Test
        void tabSeparatedData_parsesCorrectly() {
            String tsv = "Name\tScore\nAlice\t95\nBob\t82";
            String result = tool.analyzeData(tsv);
            assertThat(result)
                    .contains("Rows: 2")
                    .contains("Columns: 2")
                    .contains("Headers: Name, Score");
        }

        @Test
        void dataQuality_completeness() {
            String csv = "A,B\n1,2\n3,4";
            String result = tool.analyzeData(csv);
            assertThat(result)
                    .contains("DATA QUALITY")
                    .contains("Completeness:")
                    .contains("Missing cells:");
        }

        @Test
        void dataQuality_unevenRows() {
            String csv = "A,B,C\n1,2\n3,4,5";
            String result = tool.analyzeData(csv);
            assertThat(result).contains("Missing cells:");
        }

        @Test
        void dataQuality_blankCells() {
            String csv = "A,B\n1,\n,4";
            String result = tool.analyzeData(csv);
            assertThat(result).contains("Missing cells:");
        }

        @Test
        void suggestedFormulas_forNumericColumns() {
            String csv = "Value\n10\n20\n30";
            String result = tool.analyzeData(csv);
            assertThat(result)
                    .contains("SUGGESTED FORMULAS")
                    .contains("=SUM(")
                    .contains("=AVERAGE(")
                    .contains("=COUNTIF(");
        }

        @Test
        void suggestedFormulas_notForTextColumns() {
            String csv = "Name\nAlice\nBob\nCarol";
            String result = tool.analyzeData(csv);
            assertThat(result)
                    .contains("SUGGESTED FORMULAS")
                    .doesNotContain("=SUM(");
        }

        @Test
        void multipleNumericColumns_multipleFormulas() {
            String csv = "X,Y\n1,10\n2,20\n3,30";
            String result = tool.analyzeData(csv);
            assertThat(result).contains("=SUM(A2:").contains("=SUM(B2:");
        }

        @Test
        void columnLetterBeyondZ() {
            StringBuilder header = new StringBuilder();
            StringBuilder row = new StringBuilder();
            for (int i = 0; i < 27; i++) {
                if (i > 0) {
                    header.append(",");
                    row.append(",");
                }
                header.append("Col").append(i);
                row.append(i * 10);
            }
            String csv = header + "\n" + row;
            String result = tool.analyzeData(csv);
            assertThat(result).contains("=SUM(AA");
        }

        @Test
        void fmtForInteger_noDecimalPoint() {
            String csv = "Value\n100\n200\n300";
            String result = tool.analyzeData(csv);
            assertThat(result).contains("Mean: 200");
        }

        @Test
        void fmtForDecimal_showsDecimal() {
            String csv = "Value\n1\n2";
            String result = tool.analyzeData(csv);
            assertThat(result).contains("1.5");
        }

        @Test
        void mixedNumericAndText_detectedAsNumeric() {
            String csv = "Value\n10\n20\n30\nfoo";
            String result = tool.analyzeData(csv);
            assertThat(result).contains("Type: Numeric");
        }

        @Test
        void mainlyTextColumn_detectedAsText() {
            String csv = "Value\n10\nfoo\nbar\nbaz";
            String result = tool.analyzeData(csv);
            assertThat(result).contains("Type: Text");
        }

        @Test
        void nullInput_returnsErrorMessage() {
            String result = tool.analyzeData(null);
            assertThat(result).startsWith("Error analyzing data:");
        }

        @Test
        void numericColumnNoBlanks_noBlanksLabel() {
            String csv = "Value\n10\n20\n30";
            String result = tool.analyzeData(csv);
            assertThat(result).contains("Count: 3");
        }
    }

    // ── chartRecommend ──────────────────────────────────────

    @Nested
    class ChartRecommendTests {

        @Test
        void overTime_recommendsLineChart() {
            assertThat(tool.chartRecommend("sales over time")).contains("RECOMMENDED: Line Chart");
        }

        @Test
        void trend_recommendsLineChart() {
            assertThat(tool.chartRecommend("trend in temperature")).contains("RECOMMENDED: Line Chart");
        }

        @Test
        void monthly_recommendsLineChart() {
            assertThat(tool.chartRecommend("monthly sales data")).contains("RECOMMENDED: Line Chart");
        }

        @Test
        void quarterly_recommendsLineChart() {
            assertThat(tool.chartRecommend("quarterly earnings")).contains("RECOMMENDED: Line Chart");
        }

        @Test
        void yearly_recommendsLineChart() {
            assertThat(tool.chartRecommend("yearly results")).contains("RECOMMENDED: Line Chart");
        }

        @Test
        void timeSeries_recommendsLineChart() {
            assertThat(tool.chartRecommend("time series of sensor data")).contains("RECOMMENDED: Line Chart");
        }

        @Test
        void growth_recommendsLineChart() {
            assertThat(tool.chartRecommend("revenue growth")).contains("RECOMMENDED: Line Chart");
        }

        @Test
        void share_recommendsPieChart() {
            assertThat(tool.chartRecommend("market share by company")).contains("RECOMMENDED: Pie Chart");
        }

        @Test
        void proportion_recommendsPieChart() {
            assertThat(tool.chartRecommend("proportion of budget")).contains("RECOMMENDED: Pie Chart");
        }

        @Test
        void percentage_recommendsPieChart() {
            assertThat(tool.chartRecommend("percentage breakdown")).contains("RECOMMENDED: Pie Chart");
        }

        @Test
        void composition_recommendsPieChart() {
            assertThat(tool.chartRecommend("team composition")).contains("RECOMMENDED: Pie Chart");
        }

        @Test
        void breakdown_recommendsPieChart() {
            assertThat(tool.chartRecommend("cost breakdown")).contains("RECOMMENDED: Pie Chart");
        }

        @Test
        void distributionOfCategories_recommendsPieChart() {
            assertThat(tool.chartRecommend("distribution of categories")).contains("RECOMMENDED: Pie Chart");
        }

        @Test
        void compare_recommendsBarChart() {
            assertThat(tool.chartRecommend("compare products")).contains("RECOMMENDED: Clustered Bar/Column Chart");
        }

        @Test
        void vs_recommendsBarChart() {
            assertThat(tool.chartRecommend("budget vs actual")).contains("RECOMMENDED: Clustered Bar/Column Chart");
        }

        @Test
        void versus_recommendsBarChart() {
            assertThat(tool.chartRecommend("plan versus actual")).contains("RECOMMENDED: Clustered Bar/Column Chart");
        }

        @Test
        void byDepartment_recommendsBarChart() {
            assertThat(tool.chartRecommend("costs by department")).contains("RECOMMENDED: Clustered Bar/Column Chart");
        }

        @Test
        void byCategory_recommendsBarChart() {
            assertThat(tool.chartRecommend("sales by category")).contains("RECOMMENDED: Clustered Bar/Column Chart");
        }

        @Test
        void ranking_recommendsBarChart() {
            assertThat(tool.chartRecommend("ranking of sellers")).contains("RECOMMENDED: Clustered Bar/Column Chart");
        }

        @Test
        void actual_recommendsBarChart() {
            assertThat(tool.chartRecommend("actual spending figures")).contains("RECOMMENDED: Clustered Bar/Column Chart");
        }

        @Test
        void correlation_recommendsScatterPlot() {
            assertThat(tool.chartRecommend("correlation between price and demand")).contains("RECOMMENDED: Scatter Plot");
        }

        @Test
        void relationship_recommendsScatterPlot() {
            assertThat(tool.chartRecommend("relationship between height and weight")).contains("RECOMMENDED: Scatter Plot");
        }

        @Test
        void scatter_recommendsScatterPlot() {
            assertThat(tool.chartRecommend("scatter plot")).contains("RECOMMENDED: Scatter Plot");
        }

        @Test
        void regression_recommendsScatterPlot() {
            assertThat(tool.chartRecommend("regression analysis")).contains("RECOMMENDED: Scatter Plot");
        }

        @Test
        void xAndY_recommendsScatterPlot() {
            assertThat(tool.chartRecommend("x and y values")).contains("RECOMMENDED: Scatter Plot");
        }

        @Test
        void twoVariables_recommendsScatterPlot() {
            assertThat(tool.chartRecommend("two variables plotted")).contains("RECOMMENDED: Scatter Plot");
        }

        @Test
        void heat_recommendsHeatmap() {
            assertThat(tool.chartRecommend("heat map")).contains("RECOMMENDED: Conditional Formatting Heatmap");
        }

        @Test
        void matrix_recommendsHeatmap() {
            assertThat(tool.chartRecommend("matrix view")).contains("RECOMMENDED: Conditional Formatting Heatmap");
        }

        @Test
        void intensity_recommendsHeatmap() {
            assertThat(tool.chartRecommend("intensity display")).contains("RECOMMENDED: Conditional Formatting Heatmap");
        }

        @Test
        void density_recommendsHeatmap() {
            assertThat(tool.chartRecommend("density of events")).contains("RECOMMENDED: Conditional Formatting Heatmap");
        }

        @Test
        void funnel_recommendsFunnelChart() {
            assertThat(tool.chartRecommend("funnel chart")).contains("RECOMMENDED: Funnel Chart");
        }

        @Test
        void conversion_recommendsFunnelChart() {
            assertThat(tool.chartRecommend("conversion rates")).contains("RECOMMENDED: Funnel Chart");
        }

        @Test
        void pipeline_recommendsFunnelChart() {
            assertThat(tool.chartRecommend("sales pipeline")).contains("RECOMMENDED: Funnel Chart");
        }

        @Test
        void stages_recommendsFunnelChart() {
            assertThat(tool.chartRecommend("stages of recruitment")).contains("RECOMMENDED: Funnel Chart");
        }

        @Test
        void gantt_recommendsGanttChart() {
            assertThat(tool.chartRecommend("gantt chart")).contains("RECOMMENDED: Stacked Bar Chart (Gantt-style)");
        }

        @Test
        void timeline_recommendsGanttChart() {
            assertThat(tool.chartRecommend("timeline of events")).contains("RECOMMENDED: Stacked Bar Chart (Gantt-style)");
        }

        @Test
        void schedule_recommendsGanttChart() {
            assertThat(tool.chartRecommend("schedule visualization")).contains("RECOMMENDED: Stacked Bar Chart (Gantt-style)");
        }

        @Test
        void project_recommendsGanttChart() {
            assertThat(tool.chartRecommend("project plan")).contains("RECOMMENDED: Stacked Bar Chart (Gantt-style)");
        }

        @Test
        void unknown_showsSelectionGuide() {
            assertThat(tool.chartRecommend("some random data"))
                    .contains("CHART SELECTION GUIDE")
                    .contains("Describe your specific data for a tailored recommendation.");
        }

        @Test
        void outputIncludesDescription() {
            assertThat(tool.chartRecommend("test desc")).contains("Data: test desc");
        }
    }

    // ── conditionalFormat ───────────────────────────────────

    @Nested
    class ConditionalFormatTests {

        @Test
        void duplicate_returnsHighlightDuplicates() {
            assertThat(tool.conditionalFormat("highlight duplicates"))
                    .contains("HIGHLIGHT DUPLICATES")
                    .contains("COUNTIF");
        }

        @Test
        void aboveAverage() {
            assertThat(tool.conditionalFormat("cells above average"))
                    .contains("HIGHLIGHT ABOVE/BELOW AVERAGE")
                    .contains("AVERAGE");
        }

        @Test
        void belowAverage() {
            assertThat(tool.conditionalFormat("flag below average")).contains("HIGHLIGHT ABOVE/BELOW AVERAGE");
        }

        @Test
        void trafficLight() {
            assertThat(tool.conditionalFormat("traffic light for scores"))
                    .contains("TRAFFIC LIGHT (3-COLOR RULES)")
                    .contains("Rule 1 (Red)")
                    .contains("Rule 2 (Yellow)")
                    .contains("Rule 3 (Green)");
        }

        @Test
        void redKeyword() {
            assertThat(tool.conditionalFormat("red for low")).contains("TRAFFIC LIGHT");
        }

        @Test
        void greenKeyword() {
            assertThat(tool.conditionalFormat("green for high")).contains("TRAFFIC LIGHT");
        }

        @Test
        void scoreKeyword() {
            assertThat(tool.conditionalFormat("score coloring")).contains("TRAFFIC LIGHT");
        }

        @Test
        void gradeKeyword() {
            assertThat(tool.conditionalFormat("grade based formatting")).contains("TRAFFIC LIGHT");
        }

        @Test
        void thresholdKeyword() {
            assertThat(tool.conditionalFormat("threshold rules")).contains("TRAFFIC LIGHT");
        }

        @Test
        void entireRow() {
            assertThat(tool.conditionalFormat("highlight entire row if status overdue"))
                    .contains("HIGHLIGHT ENTIRE ROW BASED ON A CELL VALUE")
                    .contains("=$E1=");
        }

        @Test
        void wholeRow() {
            assertThat(tool.conditionalFormat("color whole row")).contains("HIGHLIGHT ENTIRE ROW BASED ON A CELL VALUE");
        }

        @Test
        void alternating() {
            assertThat(tool.conditionalFormat("alternating row colors"))
                    .contains("ALTERNATING ROW COLORS (ZEBRA STRIPES)")
                    .contains("MOD(ROW(),2)");
        }

        @Test
        void zebraKeyword() {
            assertThat(tool.conditionalFormat("zebra stripes")).contains("ALTERNATING ROW COLORS");
        }

        @Test
        void stripeKeyword() {
            assertThat(tool.conditionalFormat("stripe the rows")).contains("ALTERNATING ROW COLORS");
        }

        @Test
        void bandedKeyword() {
            assertThat(tool.conditionalFormat("banded rows")).contains("ALTERNATING ROW COLORS");
        }

        @Test
        void dataBar() {
            assertThat(tool.conditionalFormat("add data bar"))
                    .contains("IN-CELL DATA BARS");
        }

        @Test
        void barInCell() {
            assertThat(tool.conditionalFormat("bar in cell chart")).contains("IN-CELL DATA BARS");
        }

        @Test
        void inCellKeyword() {
            assertThat(tool.conditionalFormat("in-cell visualization")).contains("IN-CELL DATA BARS");
        }

        @Test
        void blankKeyword() {
            assertThat(tool.conditionalFormat("highlight blank cells"))
                    .contains("HIGHLIGHT BLANK/EMPTY CELLS")
                    .contains("LEN(TRIM(A1))=0");
        }

        @Test
        void emptyKeyword() {
            assertThat(tool.conditionalFormat("find empty cells")).contains("HIGHLIGHT BLANK/EMPTY CELLS");
        }

        @Test
        void missingKeyword() {
            assertThat(tool.conditionalFormat("highlight missing data")).contains("HIGHLIGHT BLANK/EMPTY CELLS");
        }

        @Test
        void unknown_returnsQuickReference() {
            assertThat(tool.conditionalFormat("something unrelated"))
                    .contains("CONDITIONAL FORMATTING QUICK REFERENCE")
                    .contains("CUSTOM FORMULA RULES");
        }

        @Test
        void outputIncludesRequest() {
            assertThat(tool.conditionalFormat("test request")).contains("Request: test request");
        }
    }

    // ── pivotGuide ──────────────────────────────────────────

    @Nested
    class PivotGuideTests {

        @Test
        void revenueWithRegion() {
            String result = tool.pivotGuide("Total revenue by product and region");
            assertThat(result)
                    .contains("ROWS:     Product")
                    .contains("COLUMNS:  Region")
                    .contains("VALUES:   Sum of Revenue");
        }

        @Test
        void salesWithMonth() {
            assertThat(tool.pivotGuide("monthly sales totals")).contains("COLUMNS:  Date");
        }

        @Test
        void revenueWithQuarter() {
            assertThat(tool.pivotGuide("quarterly revenue")).contains("COLUMNS:  Date");
        }

        @Test
        void revenueWithYear() {
            assertThat(tool.pivotGuide("yearly revenue analysis")).contains("COLUMNS:  Date");
        }

        @Test
        void amountWithArea() {
            assertThat(tool.pivotGuide("amount by area")).contains("COLUMNS:  Region");
        }

        @Test
        void amountWithLocation() {
            assertThat(tool.pivotGuide("amount by location")).contains("COLUMNS:  Region");
        }

        @Test
        void revenueWithQuantity() {
            assertThat(tool.pivotGuide("Revenue and quantity by product"))
                    .contains("Sum of Revenue")
                    .contains("Count/Sum of Quantity");
        }

        @Test
        void revenueWithCount() {
            assertThat(tool.pivotGuide("revenue with count of items")).contains("Count/Sum of Quantity");
        }

        @Test
        void revenueMonthAndRegion_showsFilter() {
            assertThat(tool.pivotGuide("monthly revenue by product and region")).contains("FILTERS:  Date");
        }

        @Test
        void salesMonthAndProduct_showsFilter() {
            assertThat(tool.pivotGuide("monthly sales by product")).contains("FILTERS:  Date");
        }

        @Test
        void noRevenueOrSalesOrAmount_genericLayout() {
            String result = tool.pivotGuide("employee names departments ratings");
            assertThat(result)
                    .contains("ROWS:     Your main grouping category")
                    .contains("COLUMNS:  Your secondary grouping")
                    .contains("VALUES:   The numbers to summarize")
                    .contains("FILTERS:  Fields to filter");
        }

        @Test
        void commonSections_alwaysPresent() {
            String result = tool.pivotGuide("anything");
            assertThat(result)
                    .contains("STEP 1: CREATE PIVOT TABLE")
                    .contains("STEP 2: ARRANGE FIELDS")
                    .contains("STEP 3: CUSTOMIZE")
                    .contains("STEP 4: CALCULATED FIELDS")
                    .contains("STEP 5: FORMATTING")
                    .contains("STEP 6: ENHANCEMENTS")
                    .contains("PRO TIPS");
        }

        @Test
        void outputIncludesScenario() {
            assertThat(tool.pivotGuide("my scenario")).contains("Scenario: my scenario");
        }
    }

    // ── vbaSnippet ──────────────────────────────────────────

    @Nested
    class VbaSnippetTests {

        @Test
        void loopAndHighlight() {
            String result = tool.vbaSnippet("loop through rows and highlight if > 100");
            assertThat(result)
                    .contains("Sub HighlightCells()")
                    .contains("Interior.Color")
                    .contains("End Sub");
        }

        @Test
        void loopAndColor() {
            assertThat(tool.vbaSnippet("loop and color cells")).contains("Sub HighlightCells()");
        }

        @Test
        void blankRow() {
            String result = tool.vbaSnippet("remove blank rows");
            assertThat(result)
                    .contains("Sub RemoveBlankRows()")
                    .contains("deleted")
                    .contains("End Sub");
        }

        @Test
        void emptyRow() {
            assertThat(tool.vbaSnippet("delete empty row")).contains("Sub RemoveBlankRows()");
        }

        @Test
        void deleteRow() {
            assertThat(tool.vbaSnippet("delete row if blank")).contains("Sub RemoveBlankRows()");
        }

        @Test
        void pdf() {
            String result = tool.vbaSnippet("save as PDF");
            assertThat(result)
                    .contains("Sub ExportToPDF()")
                    .contains("ExportAsFixedFormat")
                    .contains("End Sub");
        }

        @Test
        void export() {
            assertThat(tool.vbaSnippet("export data")).contains("Sub ExportToPDF()");
        }

        @Test
        void copySheet() {
            String result = tool.vbaSnippet("copy data from multiple sheets into one");
            assertThat(result)
                    .contains("Sub ConsolidateSheets()")
                    .contains("Consolidated")
                    .contains("End Sub");
        }

        @Test
        void copyAndConsolidate() {
            assertThat(tool.vbaSnippet("copy and consolidate worksheets")).contains("Sub ConsolidateSheets()");
        }

        @Test
        void uniqueSheet() {
            String result = tool.vbaSnippet("create sheet for each unique value");
            assertThat(result)
                    .contains("Sub CreateSheetsFromColumn()")
                    .contains("dict")
                    .contains("End Sub");
        }

        @Test
        void protectSheet() {
            String result = tool.vbaSnippet("protect all sheets");
            assertThat(result)
                    .contains("Sub ProtectAllSheets()")
                    .contains("Sub UnprotectAllSheets()")
                    .contains("End Sub");
        }

        @Test
        void unknown_returnsGenericTemplate() {
            String result = tool.vbaSnippet("some random automation");
            assertThat(result)
                    .contains("Sub ProcessData()")
                    .contains("COMMON VBA PATTERNS")
                    .contains("Describe your specific task for targeted code.");
        }

        @Test
        void outputIncludesTask() {
            assertThat(tool.vbaSnippet("my task")).contains("Task: my task");
        }

        @Test
        void outputIncludesHowToUse() {
            assertThat(tool.vbaSnippet("anything"))
                    .contains("HOW TO USE:")
                    .contains("Alt+F11")
                    .contains(".xlsm");
        }
    }

    // ── shortcutReference ───────────────────────────────────

    @Nested
    class ShortcutReferenceTests {

        @Test
        void all_returnsAllCategories() {
            String result = tool.shortcutReference("all");
            assertThat(result)
                    .contains("NAVIGATION")
                    .contains("SELECTION")
                    .contains("FORMATTING")
                    .contains("FORMULAS")
                    .contains("DATA & EDITING")
                    .contains("FILE & WORKBOOK");
        }

        @Test
        void navigation() {
            assertThat(tool.shortcutReference("navigation"))
                    .contains("NAVIGATION")
                    .contains("Ctrl+Home");
        }

        @Test
        void moveKeyword() {
            assertThat(tool.shortcutReference("move around")).contains("NAVIGATION");
        }

        @Test
        void goToKeyword() {
            assertThat(tool.shortcutReference("go to cell")).contains("NAVIGATION");
        }

        @Test
        void selection() {
            assertThat(tool.shortcutReference("selection"))
                    .contains("SELECTION")
                    .contains("Ctrl+A");
        }

        @Test
        void highlightKeyword() {
            assertThat(tool.shortcutReference("highlight cells")).contains("SELECTION");
        }

        @Test
        void formatting() {
            assertThat(tool.shortcutReference("formatting"))
                    .contains("FORMATTING")
                    .contains("Ctrl+B");
        }

        @Test
        void boldKeyword() {
            assertThat(tool.shortcutReference("bold text")).contains("FORMATTING");
        }

        @Test
        void fontKeyword() {
            assertThat(tool.shortcutReference("change font")).contains("FORMATTING");
        }

        @Test
        void colorKeyword() {
            assertThat(tool.shortcutReference("color cells")).contains("FORMATTING");
        }

        @Test
        void formulas() {
            assertThat(tool.shortcutReference("formulas"))
                    .contains("FORMULAS")
                    .contains("F2");
        }

        @Test
        void enterKeyword() {
            assertThat(tool.shortcutReference("enter values")).contains("FORMULAS");
        }

        @Test
        void sumKeyword() {
            assertThat(tool.shortcutReference("sum shortcut")).contains("FORMULAS");
        }

        @Test
        void functionKeyword() {
            assertThat(tool.shortcutReference("insert function")).contains("FORMULAS");
        }

        @Test
        void data() {
            assertThat(tool.shortcutReference("data operations"))
                    .contains("DATA & EDITING")
                    .contains("Ctrl+C");
        }

        @Test
        void pasteKeyword() {
            assertThat(tool.shortcutReference("paste special")).contains("DATA & EDITING");
        }

        @Test
        void copyKeyword() {
            assertThat(tool.shortcutReference("copy cells")).contains("DATA & EDITING");
        }

        @Test
        void insertKeyword() {
            assertThat(tool.shortcutReference("insert row")).contains("DATA & EDITING");
        }

        @Test
        void deleteKeyword() {
            assertThat(tool.shortcutReference("delete column")).contains("DATA & EDITING");
        }

        @Test
        void file() {
            assertThat(tool.shortcutReference("file operations"))
                    .contains("FILE & WORKBOOK")
                    .contains("Ctrl+S");
        }

        @Test
        void saveKeyword() {
            assertThat(tool.shortcutReference("save workbook")).contains("FILE & WORKBOOK");
        }

        @Test
        void printKeyword() {
            assertThat(tool.shortcutReference("print sheet")).contains("FILE & WORKBOOK");
        }

        @Test
        void workbookKeyword() {
            assertThat(tool.shortcutReference("workbook management")).contains("FILE & WORKBOOK");
        }

        @Test
        void unknownQuery_returnsNoMatch() {
            String result = tool.shortcutReference("xyzzy");
            assertThat(result)
                    .contains("No shortcuts matched 'xyzzy'")
                    .contains("Try:");
        }

        @Test
        void whitespaceQuery_trimmed() {
            assertThat(tool.shortcutReference("  all  ")).contains("NAVIGATION");
        }
    }
}
