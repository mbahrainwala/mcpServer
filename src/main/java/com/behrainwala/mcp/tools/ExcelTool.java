package com.behrainwala.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExcelTool {

    private static final Logger log = LoggerFactory.getLogger(ExcelTool.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE64_PREFIX = "base64:";
    private static final int MAX_ROWS_PER_SHEET = 500;
    private static final int MAX_COLS_PER_SHEET = 26;
    private static final int MAX_TEXT_LENGTH = 80_000;

    // ── Config ────────────────────────────────────────────────────────────────

    private record ExcelConfig(
            String font, int fontSize, String accentColor,
            boolean alternateRows, boolean freezeHeader, boolean autoWidth,
            String docTitle, String docAuthor) {

        static ExcelConfig defaults() {
            return new ExcelConfig("Calibri", 11, "2E74B5",
                    true, true, true, null, null);
        }

        /** Very light tint of accent (85% white + 15% accent) for alternating rows. */
        String lightAccent() {
            byte[] rgb = hexToRgb(accentColor);
            int r = Math.min(255, (rgb[0] & 0xFF) * 15 / 100 + 217);
            int g = Math.min(255, (rgb[1] & 0xFF) * 15 / 100 + 217);
            int b = Math.min(255, (rgb[2] & 0xFF) * 15 / 100 + 217);
            return String.format("%02X%02X%02X", r, g, b);
        }
    }

    private record ParsedCell(String value, boolean bold, String fgColor, String bgColor, String numFmt) {}
    private record SheetSpec(String name, List<String> lines) {}

    // ── Tools ─────────────────────────────────────────────────────────────────

    @Tool(name = "excel_to_text",
          description = "Read an Excel workbook (.xlsx or .xls) and return its content as structured text. "
                  + "Each sheet is rendered as a markdown table with column headers, cell values, and "
                  + "formula results shown alongside the formula (e.g. 1,500 [=SUM(B2:B5)]). "
                  + "Use this so an LLM can understand the workbook structure before editing it. "
                  + "Accepts a local file path, a URL, or base64-encoded content (prefixed with 'base64:').")
    public String excelToText(
            @ToolParam(description = "Absolute local file path (e.g. 'C:/data/report.xlsx'), "
                    + "a URL, or base64: prefixed content.") String source) {

        if (source == null || source.isBlank()) return "Error: source must not be blank.";
        try {
            byte[] bytes = loadBytes(source);
            try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
                StringBuilder sb = new StringBuilder();
                sb.append("Excel Workbook Extraction\n─────────────────────────\n");
                sb.append("Source : ").append(displaySource(source)).append("\n");
                sb.append("Sheets : ");
                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(wb.getSheetName(i));
                }
                sb.append("\n");

                FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
                for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                    Sheet sheet = wb.getSheetAt(si);
                    sb.append("\n").append("═".repeat(60)).append("\n");
                    sb.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                    sb.append("═".repeat(60)).append("\n");
                    renderSheet(sb, sheet, evaluator);
                    if (sb.length() > MAX_TEXT_LENGTH) {
                        sb.append("\n\n... [truncated — ").append(wb.getNumberOfSheets() - si - 1).append(" more sheet(s) not shown]");
                        break;
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.error("excel_to_text failed for '{}': {}", source, e.getMessage(), e);
            return "Error reading Excel file: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    @Tool(name = "excel_create",
          description = "Create a professional .xlsx Excel workbook from structured content. "
                  + "Optionally starts with a YAML frontmatter block (between --- lines).\n\n"
                  + "FRONTMATTER (all optional):\n"
                  + "  font: Calibri           # font family\n"
                  + "  fontSize: 11            # base font size in points\n"
                  + "  accentColor: \"#2E74B5\" # header row and accent colour (hex)\n"
                  + "  alternateRows: true     # shade every other data row with a light tint\n"
                  + "  freezeHeader: true      # freeze the first row of each sheet\n"
                  + "  autoWidth: true         # auto-size column widths\n"
                  + "  title: My Workbook      # document metadata\n"
                  + "  author: Jane Smith\n\n"
                  + "CONTENT SYNTAX:\n"
                  + "  ## Sheet: Name          — start a new worksheet named Name\n"
                  + "  # Heading text          — add a bold full-width heading row\n"
                  + "  | Col1 | Col2 | Col3 |  — table (first row = header, styled with accent)\n"
                  + "  |------|------|------|  — separator row (ignored)\n"
                  + "  Formulas: start cell value with = e.g. =SUM(B2:B10)\n"
                  + "  Cell format prefixes (add before value):\n"
                  + "    {bold}text            — bold cell\n"
                  + "    {currency}1234.56     — format as $1,234.56\n"
                  + "    {pct}0.25             — format as 25.00%\n"
                  + "    {date}2025-01-15      — format as date\n"
                  + "    {int}1500             — format as integer with thousands separator\n"
                  + "    {color:#FF0000}text   — font colour\n"
                  + "    {bg:#FFF3E0}text      — cell background colour\n"
                  + "  ::: widths 25 12 12 14  — set column widths in characters (A, B, C, ...)\n"
                  + "  ::: freeze 1 0          — freeze N rows and M columns\n"
                  + "  Multiple tables and headings are supported within a single sheet.")
    public String excelCreate(
            @ToolParam(description = "Absolute path where the .xlsx file should be saved, "
                    + "e.g. 'C:/reports/budget.xlsx'. Parent directory must exist.") String outputPath,
            @ToolParam(description = "Workbook content: optional YAML frontmatter then sheet definitions.") String content) {

        if (outputPath == null || outputPath.isBlank()) return "Error: outputPath must not be blank.";
        if (content == null || content.isBlank()) return "Error: content must not be blank.";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            ExcelConfig cfg = parseFrontmatter(content);
            String body = stripFrontmatter(content);
            List<SheetSpec> sheets = parseSheets(body);

            // Ensure at least one sheet
            if (sheets.isEmpty()) sheets = List.of(new SheetSpec("Sheet1", List.of(body)));

            Map<String, XSSFCellStyle> styleCache = new HashMap<>();
            for (SheetSpec spec : sheets) {
                XSSFSheet sheet = wb.createSheet(spec.name());
                buildSheet(wb, sheet, spec, cfg, styleCache);
            }

            if (cfg.docTitle() != null) wb.getProperties().getCoreProperties().setTitle(cfg.docTitle());
            if (cfg.docAuthor() != null) wb.getProperties().getCoreProperties().setCreator(cfg.docAuthor());

            File outFile = new File(outputPath.strip());
            try (FileOutputStream fos = new FileOutputStream(outFile)) { wb.write(fos); }

            return "Workbook created successfully.\nPath    : " + outFile.getAbsolutePath()
                    + "\nSize    : " + formatSize(outFile.length())
                    + "\nSheets  : " + sheets.stream().map(SheetSpec::name).reduce((a, b) -> a + ", " + b).orElse("")
                    + "\nFormat  : XLSX";
        } catch (Exception e) {
            log.error("excel_create failed for '{}': {}", outputPath, e.getMessage(), e);
            return "Error creating workbook: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    @Tool(name = "excel_edit",
          description = "Edit an existing .xlsx workbook using a JSON array of operations.\n\n"
                  + "Supported operations:\n"
                  + "  {\"type\":\"set_cell\",\"sheet\":\"Sheet1\",\"cell\":\"B2\",\"value\":1500}\n"
                  + "  {\"type\":\"set_cell\",\"sheet\":\"Sheet1\",\"cell\":\"F2\",\"formula\":\"=SUM(B2:E2)\"}\n"
                  + "  {\"type\":\"append_row\",\"sheet\":\"Sheet1\",\"values\":[\"Item\",100,200,\"=SUM(B{row}:C{row})\"]}\n"
                  + "  {\"type\":\"insert_row\",\"sheet\":\"Sheet1\",\"after_row\":3,\"values\":[...]}\n"
                  + "  {\"type\":\"delete_row\",\"sheet\":\"Sheet1\",\"row\":4}\n"
                  + "  {\"type\":\"fill_formula\",\"sheet\":\"Sheet1\",\"range\":\"F2:F20\",\"formula\":\"=SUM(B{row}:E{row})\"}\n"
                  + "  {\"type\":\"set_style\",\"sheet\":\"Sheet1\",\"range\":\"A1:F1\","
                  + "\"bold\":true,\"bgColor\":\"#2E74B5\",\"fontColor\":\"#FFFFFF\"}\n"
                  + "  {\"type\":\"set_column_width\",\"sheet\":\"Sheet1\",\"column\":\"A\",\"width\":25}\n"
                  + "  {\"type\":\"freeze_panes\",\"sheet\":\"Sheet1\",\"rows\":1,\"cols\":0}\n"
                  + "  {\"type\":\"add_sheet\",\"name\":\"New Sheet\"}\n"
                  + "  {\"type\":\"rename_sheet\",\"from\":\"Sheet1\",\"to\":\"Summary\"}\n"
                  + "  {\"type\":\"delete_sheet\",\"name\":\"OldSheet\"}\n"
                  + "In values arrays and fill_formula, use {row} for 1-based row number, {col} for column letter.\n"
                  + "Only .xlsx format is supported for editing.")
    public String excelEdit(
            @ToolParam(description = "Absolute local file path to the .xlsx file to edit.") String source,
            @ToolParam(description = "JSON array of edit operations.") String operations,
            @ToolParam(description = "Save path. Omit to overwrite source.", required = false) String outputPath) {

        if (source == null || source.isBlank()) return "Error: source must not be blank.";
        if (operations == null || operations.isBlank()) return "Error: operations must not be blank.";

        try {
            JsonNode opsNode = MAPPER.readTree(operations);
            if (!opsNode.isArray()) return "Error: operations must be a JSON array.";

            File sourceFile = new File(source.strip());
            if (!sourceFile.exists()) return "Error: File not found: " + source;

            XSSFWorkbook wb;
            try (FileInputStream fis = new FileInputStream(sourceFile)) {
                wb = new XSSFWorkbook(fis);
            }

            List<String> warnings = new ArrayList<>();
            int opCount = 0;

            for (JsonNode op : opsNode) {
                String type = op.path("type").asText("");
                try {
                    applyOperation(wb, op, type);
                    opCount++;
                } catch (Exception e) {
                    warnings.add("Operation '" + type + "' failed: " + e.getMessage());
                }
            }

            String savePath = (outputPath != null && !outputPath.isBlank()) ? outputPath.strip() : source.strip();
            File outFile = new File(savePath);
            try (FileOutputStream fos = new FileOutputStream(outFile)) { wb.write(fos); }
            wb.close();

            StringBuilder result = new StringBuilder("Workbook edited successfully.\n");
            result.append("Saved to : ").append(outFile.getAbsolutePath()).append("\n");
            result.append("Size     : ").append(formatSize(outFile.length())).append("\n");
            result.append("Applied  : ").append(opCount).append(" operation(s)\n");
            if (!warnings.isEmpty()) {
                result.append("\nWarnings:\n");
                warnings.forEach(w -> result.append("  • ").append(w).append("\n"));
            }
            return result.toString().strip();

        } catch (Exception e) {
            log.error("excel_edit failed for '{}': {}", source, e.getMessage(), e);
            return "Error editing workbook: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    // ── Extraction ────────────────────────────────────────────────────────────

    private void renderSheet(StringBuilder sb, Sheet sheet, FormulaEvaluator evaluator) {
        int firstRow = sheet.getFirstRowNum();
        int lastRow  = Math.min(sheet.getLastRowNum(), firstRow + MAX_ROWS_PER_SHEET - 1);
        if (firstRow < 0) { sb.append("(empty)\n"); return; }

        // Find max column
        int maxCol = 0;
        for (int r = firstRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row != null) maxCol = Math.max(maxCol, row.getLastCellNum());
        }
        maxCol = Math.min(maxCol, MAX_COLS_PER_SHEET);
        if (maxCol == 0) { sb.append("(empty)\n"); return; }

        sb.append("Rows: ").append(sheet.getLastRowNum() + 1)
          .append("  Columns: ").append(maxCol).append("\n\n");

        // Collect cell texts to calculate column widths for formatting
        List<String[]> table = new ArrayList<>();
        for (int r = firstRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            String[] rowData = new String[maxCol];
            for (int c = 0; c < maxCol; c++) {
                Cell cell = row != null ? row.getCell(c) : null;
                rowData[c] = readCellValue(cell, evaluator);
            }
            table.add(rowData);
        }

        // Column widths
        int[] widths = new int[maxCol];
        for (String[] row : table)
            for (int c = 0; c < maxCol; c++)
                widths[c] = Math.max(widths[c], row[c].length());
        for (int c = 0; c < maxCol; c++) widths[c] = Math.max(widths[c], 3);

        String sep = buildSep(widths);
        sb.append(sep).append("\n");
        for (int r = 0; r < table.size(); r++) {
            sb.append("| ");
            for (int c = 0; c < maxCol; c++)
                sb.append(String.format("%-" + widths[c] + "s", table.get(r)[c])).append(" | ");
            sb.append("\n");
            if (r == 0) sb.append(sep).append("\n");
        }
        sb.append(sep).append("\n");

        if (sheet.getLastRowNum() > firstRow + MAX_ROWS_PER_SHEET - 1)
            sb.append("\n... [").append(sheet.getLastRowNum() - firstRow - MAX_ROWS_PER_SHEET + 1)
              .append(" more rows not shown]\n");
    }

    private String readCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case BLANK   -> { return ""; }
                case BOOLEAN -> { return String.valueOf(cell.getBooleanCellValue()); }
                case STRING  -> { return cell.getStringCellValue(); }
                case NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell))
                        return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                    return formatNumber(cell.getNumericCellValue());
                }
                case FORMULA -> {
                    String formula = "=" + cell.getCellFormula();
                    try {
                        CellValue cv = evaluator.evaluate(cell);
                        String val = switch (cv.getCellType()) {
                            case NUMERIC -> formatNumber(cv.getNumberValue());
                            case STRING  -> cv.getStringValue();
                            case BOOLEAN -> String.valueOf(cv.getBooleanValue());
                            default -> "";
                        };
                        return val.isEmpty() ? formula : val + " [" + formula + "]";
                    } catch (Exception e) { return formula; }
                }
                case ERROR -> { return "[ERR]"; }
                default -> { return ""; }
            }
        } catch (Exception e) { return ""; }
    }

    // ── Creation ──────────────────────────────────────────────────────────────

    private ExcelConfig parseFrontmatter(String content) {
        String norm = content.replace("\r\n", "\n").replace("\r", "\n");
        if (!norm.startsWith("---")) return ExcelConfig.defaults();
        int end = norm.indexOf("\n---", 3);
        if (end < 0) return ExcelConfig.defaults();
        String yaml = norm.substring(3, end).strip();

        Map<String, String> p = new LinkedHashMap<>();
        for (String line : yaml.split("\n")) {
            int c = line.indexOf(':'); if (c < 0) continue;
            String k = line.substring(0, c).strip().toLowerCase();
            String v = line.substring(c + 1).strip();
            if (v.length() >= 2 && ((v.charAt(0) == '"' && v.charAt(v.length()-1) == '"')
                    || (v.charAt(0) == '\'' && v.charAt(v.length()-1) == '\'')))
                v = v.substring(1, v.length() - 1);
            p.put(k, v);
        }

        return new ExcelConfig(
                p.getOrDefault("font", "Calibri"),
                parseInt(p.getOrDefault("fontsize", "11"), 11),
                resolveColor(p.getOrDefault("accentcolor", "#2E74B5")),
                !"false".equalsIgnoreCase(p.getOrDefault("alternaterows", "true")),
                !"false".equalsIgnoreCase(p.getOrDefault("freezeheader", "true")),
                !"false".equalsIgnoreCase(p.getOrDefault("autowidth", "true")),
                emptyToNull(p.get("title")),
                emptyToNull(p.get("author"))
        );
    }

    private String stripFrontmatter(String content) {
        String norm = content.replace("\r\n", "\n").replace("\r", "\n");
        if (!norm.startsWith("---")) return norm;
        int end = norm.indexOf("\n---", 3);
        return end < 0 ? norm : norm.substring(end + 4).strip();
    }

    private List<SheetSpec> parseSheets(String body) {
        List<SheetSpec> sheets = new ArrayList<>();
        String currentName = "Sheet1";
        List<String> currentLines = new ArrayList<>();
        Pattern sheetPattern = Pattern.compile("^##\\s+[Ss]heet:\\s*(.+)$");

        for (String line : body.split("\n")) {
            Matcher m = sheetPattern.matcher(line.strip());
            if (m.matches()) {
                if (!currentLines.isEmpty() || !sheets.isEmpty())
                    sheets.add(new SheetSpec(currentName, new ArrayList<>(currentLines)));
                currentName = m.group(1).strip();
                currentLines.clear();
            } else {
                currentLines.add(line);
            }
        }
        sheets.add(new SheetSpec(currentName, currentLines));
        return sheets;
    }

    private void buildSheet(XSSFWorkbook wb, XSSFSheet sheet, SheetSpec spec,
                             ExcelConfig cfg, Map<String, XSSFCellStyle> styleCache) {
        int currentRow = 0;
        int[] explicitWidths = null;
        List<String[]> tableBuffer = new ArrayList<>();

        for (String line : spec.lines()) {
            line = line.replace("\r", "");

            // Width directive
            if (line.strip().startsWith("::: widths")) {
                String[] parts = line.strip().substring(10).strip().split("\\s+");
                explicitWidths = Arrays.stream(parts).mapToInt(s -> parseInt(s, 10)).toArray();
                continue;
            }

            // Freeze directive
            if (line.strip().startsWith("::: freeze")) {
                String[] parts = line.strip().substring(10).strip().split("\\s+");
                int fRows = parts.length > 0 ? parseInt(parts[0], 1) : 1;
                int fCols = parts.length > 1 ? parseInt(parts[1], 0) : 0;
                sheet.createFreezePane(fCols, fRows);
                continue;
            }

            // Table row
            if (line.strip().startsWith("|")) {
                tableBuffer.add(splitTableCells(line));
                continue;
            }

            // Flush table
            if (!tableBuffer.isEmpty()) {
                currentRow = flushTable(wb, sheet, tableBuffer, currentRow, cfg, styleCache);
                tableBuffer.clear();
            }

            // Heading row (# or ## but not ## Sheet:)
            if (line.startsWith("# ") || (line.startsWith("## ") && !line.toLowerCase().startsWith("## sheet:"))) {
                String text = line.replaceFirst("^#+\\s*", "").strip();
                Row r = sheet.createRow(currentRow++);
                r.setHeightInPoints(20);
                Cell c = r.createCell(0);
                c.setCellValue(text);
                c.setCellStyle(getStyle(wb, true, false, true, null, cfg.accentColor(), null, cfg, styleCache));
                // Merge across reasonable width (will be adjusted by auto-size anyway)
                sheet.addMergedRegion(new CellRangeAddress(currentRow - 1, currentRow - 1, 0, 9));
                continue;
            }

            // Blank line → empty row separator
            if (line.isBlank()) { currentRow++; continue; }

            // Plain text → italic note row
            Row r = sheet.createRow(currentRow++);
            Cell c = r.createCell(0);
            c.setCellValue(line.strip());
            XSSFCellStyle noteStyle = getStyle(wb, false, false, false, "808080", null, null, cfg, styleCache);
            c.setCellStyle(noteStyle);
        }

        // Flush trailing table
        if (!tableBuffer.isEmpty())
            flushTable(wb, sheet, tableBuffer, currentRow, cfg, styleCache);

        // Column widths
        if (explicitWidths != null) {
            for (int i = 0; i < explicitWidths.length; i++)
                sheet.setColumnWidth(i, explicitWidths[i] * 256);
        } else if (cfg.autoWidth()) {
            int maxCol = 0;
            for (Row row : sheet) if (row != null) maxCol = Math.max(maxCol, row.getLastCellNum());
            for (int i = 0; i < maxCol; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, (int)(sheet.getColumnWidth(i) * 1.15) + 512);
            }
        }

        // Default freeze header row (unless explicit ::: freeze was used)
        if (cfg.freezeHeader() && sheet.getPaneInformation() == null)
            sheet.createFreezePane(0, 1);
    }

    private int flushTable(XSSFWorkbook wb, XSSFSheet sheet, List<String[]> rawRows,
                            int startRow, ExcelConfig cfg, Map<String, XSSFCellStyle> styleCache) {
        // Skip separator rows
        List<String[]> rows = rawRows.stream()
                .filter(cells -> !(cells.length > 0 && Arrays.stream(cells).allMatch(c -> c.strip().matches("[-:|]+"))))
                .toList();
        if (rows.isEmpty()) return startRow;

        int maxCols = rows.stream().mapToInt(r -> r.length).max().orElse(1);
        int currentRow = startRow;

        for (int ri = 0; ri < rows.size(); ri++) {
            boolean isHeader = (ri == 0);
            boolean isAlt    = !isHeader && cfg.alternateRows() && (ri % 2 == 0);
            Row row = sheet.createRow(currentRow++);
            row.setHeightInPoints(18);

            String[] cells = rows.get(ri);
            for (int ci = 0; ci < maxCols; ci++) {
                String raw = ci < cells.length ? cells[ci].strip() : "";
                ParsedCell pc = parseCell(raw);
                Cell cell = row.createCell(ci);
                setCellValue(cell, pc.value());
                cell.setCellStyle(getStyle(wb, isHeader, isAlt, pc.bold(),
                        pc.fgColor(), pc.bgColor(), pc.numFmt(), cfg, styleCache));
            }
        }
        return currentRow;
    }

    // ── Cell parsing ──────────────────────────────────────────────────────────

    private ParsedCell parseCell(String raw) {
        if (raw == null) return new ParsedCell("", false, null, null, null);
        boolean bold = false;
        String fgColor = null, bgColor = null, numFmt = null;

        while (raw.startsWith("{")) {
            int end = raw.indexOf('}');
            if (end < 0) break;
            String tag = raw.substring(1, end).toLowerCase().strip();
            raw = raw.substring(end + 1).strip();
            if (tag.equals("bold"))                    bold = true;
            else if (tag.startsWith("color:"))         fgColor = resolveColor(tag.substring(6));
            else if (tag.startsWith("bg:"))            bgColor = resolveColor(tag.substring(3));
            else if (tag.equals("currency"))           numFmt = "$#,##0.00";
            else if (tag.equals("pct") || tag.equals("percent")) numFmt = "0.00%";
            else if (tag.equals("date"))               numFmt = "yyyy-mm-dd";
            else if (tag.equals("int") || tag.equals("integer")) numFmt = "#,##0";
            else if (tag.equals("num") || tag.equals("number"))  numFmt = "#,##0.00";
        }
        return new ParsedCell(raw, bold, fgColor, bgColor, numFmt);
    }

    private void setCellValue(Cell cell, String value) {
        if (value == null || value.isBlank()) { cell.setBlank(); return; }
        if (value.startsWith("=")) { cell.setCellFormula(value.substring(1)); return; }
        try {
            double d = Double.parseDouble(value.replace(",", "").replace("$", ""));
            cell.setCellValue(d);
        } catch (NumberFormatException e) {
            cell.setCellValue(value);
        }
    }

    // ── Style cache ───────────────────────────────────────────────────────────

    private XSSFCellStyle getStyle(XSSFWorkbook wb, boolean header, boolean alt, boolean bold,
                                    String fgColor, String bgColor, String numFmt,
                                    ExcelConfig cfg, Map<String, XSSFCellStyle> cache) {
        String key = header + "|" + alt + "|" + bold + "|" + fgColor + "|" + bgColor + "|" + numFmt;
        return cache.computeIfAbsent(key, k -> buildStyle(wb, header, alt, bold, fgColor, bgColor, numFmt, cfg));
    }

    private XSSFCellStyle buildStyle(XSSFWorkbook wb, boolean header, boolean alt, boolean bold,
                                      String fgColor, String bgColor, String numFmt, ExcelConfig cfg) {
        XSSFCellStyle style = wb.createCellStyle();

        // Fill
        String effectiveBg = bgColor != null ? bgColor
                : header ? cfg.accentColor()
                : alt    ? cfg.lightAccent()
                : null;
        if (effectiveBg != null) {
            style.setFillForegroundColor(new XSSFColor(hexToRgb(effectiveBg), null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        // Font
        XSSFFont font = wb.createFont();
        font.setFontName(cfg.font());
        font.setFontHeightInPoints((short) cfg.fontSize());
        if (bold || header) font.setBold(true);
        String effectiveFg = fgColor != null ? fgColor : header ? "FFFFFF" : null;
        if (effectiveFg != null) font.setColor(new XSSFColor(hexToRgb(effectiveFg), null));
        style.setFont(font);

        // Alignment
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(header ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);
        style.setWrapText(false);

        // Border — thin light gray on bottom and right
        XSSFColor borderColor = new XSSFColor(hexToRgb("D0D0D0"), null);
        style.setBorderBottom(BorderStyle.THIN); style.setBottomBorderColor(borderColor);
        style.setBorderRight(BorderStyle.THIN);  style.setRightBorderColor(borderColor);

        // Number format
        if (numFmt != null) style.setDataFormat(wb.createDataFormat().getFormat(numFmt));

        return style;
    }

    // ── Edit operations ───────────────────────────────────────────────────────

    private void applyOperation(XSSFWorkbook wb, JsonNode op, String type) throws Exception {
        switch (type) {

            case "set_cell" -> {
                XSSFSheet sheet = requireSheet(wb, op);
                CellReference ref = new CellReference(op.path("cell").asText("A1"));
                Row row = sheet.getRow(ref.getRow());
                if (row == null) row = sheet.createRow(ref.getRow());
                Cell cell = row.getCell(ref.getCol());
                if (cell == null) cell = row.createCell(ref.getCol());
                if (op.has("formula")) cell.setCellFormula(op.path("formula").asText().replaceFirst("^=", ""));
                else if (op.has("value")) setJsonValue(cell, op.path("value"));
            }

            case "append_row" -> {
                XSSFSheet sheet = requireSheet(wb, op);
                int next = sheet.getLastRowNum() + 1;
                if (sheet.getLastRowNum() == 0 && sheet.getRow(0) == null) next = 0;
                Row row = sheet.createRow(next);
                setRowValues(row, op.path("values"), next + 1);
            }

            case "insert_row" -> {
                XSSFSheet sheet = requireSheet(wb, op);
                int after = op.path("after_row").asInt(0);  // 1-based
                int insertAt = after; // convert to 0-based insert position
                if (insertAt <= sheet.getLastRowNum())
                    sheet.shiftRows(insertAt, sheet.getLastRowNum(), 1);
                Row row = sheet.createRow(insertAt);
                setRowValues(row, op.path("values"), insertAt + 1);
            }

            case "delete_row" -> {
                XSSFSheet sheet = requireSheet(wb, op);
                int rowNum = op.path("row").asInt(1) - 1; // 1-based → 0-based
                int last = sheet.getLastRowNum();
                if (rowNum < last) sheet.shiftRows(rowNum + 1, last, -1);
                else { Row r = sheet.getRow(rowNum); if (r != null) sheet.removeRow(r); }
            }

            case "fill_formula" -> {
                XSSFSheet sheet = requireSheet(wb, op);
                CellRangeAddress range = CellRangeAddress.valueOf(op.path("range").asText("A1:A1"));
                String tmpl = op.path("formula").asText().replaceFirst("^=", "");
                for (int r = range.getFirstRow(); r <= range.getLastRow(); r++) {
                    for (int c = range.getFirstColumn(); c <= range.getLastColumn(); c++) {
                        Row row = sheet.getRow(r);
                        if (row == null) row = sheet.createRow(r);
                        Cell cell = row.getCell(c);
                        if (cell == null) cell = row.createCell(c);
                        String formula = tmpl
                                .replace("{row}", String.valueOf(r + 1))
                                .replace("{col}", CellReference.convertNumToColString(c));
                        cell.setCellFormula(formula);
                    }
                }
            }

            case "set_style" -> {
                XSSFSheet sheet = requireSheet(wb, op);
                CellRangeAddress range = CellRangeAddress.valueOf(op.path("range").asText("A1"));
                boolean bold    = op.path("bold").asBoolean(false);
                String bgColor  = emptyToNull(op.path("bgColor").asText());
                String fgColor  = emptyToNull(op.path("fontColor").asText());
                String numFmt   = emptyToNull(op.path("format").asText());
                String resolvedBg = bgColor != null ? resolveColor(bgColor) : null;
                String resolvedFg = fgColor != null ? resolveColor(fgColor) : null;
                String resolvedFmt = numFmt == null ? null : switch (numFmt.toLowerCase()) {
                    case "currency" -> "$#,##0.00";
                    case "percent", "pct" -> "0.00%";
                    case "date" -> "yyyy-mm-dd";
                    case "int", "integer" -> "#,##0";
                    default -> numFmt;
                };
                for (int r = range.getFirstRow(); r <= range.getLastRow(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) row = sheet.createRow(r);
                    for (int c = range.getFirstColumn(); c <= range.getLastColumn(); c++) {
                        Cell cell = row.getCell(c);
                        if (cell == null) cell = row.createCell(c);
                        XSSFCellStyle base = (XSSFCellStyle) cell.getCellStyle();
                        XSSFCellStyle ns = wb.createCellStyle();
                        ns.cloneStyleFrom(base);
                        if (resolvedBg != null) {
                            ns.setFillForegroundColor(new XSSFColor(hexToRgb(resolvedBg), null));
                            ns.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        }
                        if (bold || resolvedFg != null) {
                            XSSFFont f = wb.createFont();
                            XSSFFont of = (XSSFFont) wb.getFontAt(base.getFontIndex());
                            f.setFontName(of.getFontName());
                            f.setFontHeightInPoints(of.getFontHeightInPoints());
                            if (bold) f.setBold(true);
                            if (resolvedFg != null) f.setColor(new XSSFColor(hexToRgb(resolvedFg), null));
                            ns.setFont(f);
                        }
                        if (resolvedFmt != null) ns.setDataFormat(wb.createDataFormat().getFormat(resolvedFmt));
                        cell.setCellStyle(ns);
                    }
                }
            }

            case "set_column_width" -> {
                XSSFSheet sheet = requireSheet(wb, op);
                String col = op.path("column").asText("A");
                int colIdx = CellReference.convertColStringToIndex(col);
                int width  = op.path("width").asInt(10);
                sheet.setColumnWidth(colIdx, width * 256);
            }

            case "freeze_panes" -> {
                XSSFSheet sheet = requireSheet(wb, op);
                int rows = op.path("rows").asInt(1);
                int cols = op.path("cols").asInt(0);
                sheet.createFreezePane(cols, rows);
            }

            case "add_sheet" -> {
                String name = op.path("name").asText("Sheet" + (wb.getNumberOfSheets() + 1));
                if (wb.getSheet(name) == null) wb.createSheet(name);
            }

            case "rename_sheet" -> {
                String from = op.path("from").asText();
                String to   = op.path("to").asText();
                int idx = wb.getSheetIndex(from);
                if (idx < 0) throw new IllegalArgumentException("Sheet not found: " + from);
                wb.setSheetName(idx, to);
            }

            case "delete_sheet" -> {
                String name = op.path("name").asText();
                int idx = wb.getSheetIndex(name);
                if (idx < 0) throw new IllegalArgumentException("Sheet not found: " + name);
                wb.removeSheetAt(idx);
            }

            default -> throw new IllegalArgumentException("Unknown operation type: '" + type + "'");
        }
    }

    private XSSFSheet requireSheet(XSSFWorkbook wb, JsonNode op) {
        if (op.has("sheet")) {
            XSSFSheet s = wb.getSheet(op.path("sheet").asText());
            if (s == null) throw new IllegalArgumentException("Sheet not found: " + op.path("sheet").asText());
            return s;
        }
        return wb.getSheetAt(0);
    }

    private void setJsonValue(Cell cell, JsonNode val) {
        if (val.isNumber()) cell.setCellValue(val.asDouble());
        else if (val.isBoolean()) cell.setCellValue(val.asBoolean());
        else { String s = val.asText(); if (s.startsWith("=")) cell.setCellFormula(s.substring(1)); else cell.setCellValue(s); }
    }

    private void setRowValues(Row row, JsonNode values, int rowNum1based) {
        if (values.isArray()) {
            for (int i = 0; i < values.size(); i++) {
                Cell cell = row.createCell(i);
                JsonNode v = values.get(i);
                if (v.isTextual()) {
                    String s = v.asText()
                            .replace("{row}", String.valueOf(rowNum1based))
                            .replace("{col}", CellReference.convertNumToColString(i));
                    if (s.startsWith("=")) cell.setCellFormula(s.substring(1));
                    else cell.setCellValue(s);
                } else setJsonValue(cell, v);
            }
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private String[] splitTableCells(String line) {
        String[] parts = line.split("\\|");
        int s = (parts.length > 0 && parts[0].isBlank()) ? 1 : 0;
        int e = (parts.length > s && parts[parts.length - 1].isBlank()) ? parts.length - 1 : parts.length;
        return Arrays.copyOfRange(parts, s, e);
    }

    private String buildSep(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) sb.append("-".repeat(w + 2)).append("+");
        return sb.toString();
    }

    private String formatNumber(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return String.valueOf(val);
        if (val == Math.floor(val) && Math.abs(val) < 1e15)
            return String.format("%,.0f", val);
        return String.format("%,.2f", val).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private byte[] loadBytes(String source) throws Exception {
        String s = source.strip();
        if (s.startsWith(BASE64_PREFIX)) return Base64.getDecoder().decode(s.substring(BASE64_PREFIX.length()));
        if (s.startsWith("http://") || s.startsWith("https://"))
            try (var is = URI.create(s).toURL().openStream()) { return is.readAllBytes(); }
        return Files.readAllBytes(Path.of(s));
    }

    private String displaySource(String s) {
        if (s != null && s.strip().startsWith(BASE64_PREFIX)) {
            int len = s.strip().length() - BASE64_PREFIX.length();
            return "[base64-encoded workbook, ~" + formatSize((long)(len * 0.75)) + "]";
        }
        return s;
    }

    private static byte[] hexToRgb(String hex) {
        if (hex == null || hex.isBlank()) hex = "000000";
        hex = hex.replace("#", "").toUpperCase();
        if (hex.length() == 3) hex = "" + hex.charAt(0) + hex.charAt(0)
                + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
        if (hex.length() < 6) hex = "000000";
        return new byte[]{
            (byte) Integer.parseInt(hex.substring(0, 2), 16),
            (byte) Integer.parseInt(hex.substring(2, 4), 16),
            (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    private String resolveColor(String color) {
        if (color == null) return null;
        color = color.strip();
        if (color.startsWith("#")) return color.substring(1).toUpperCase();
        return switch (color.toLowerCase()) {
            case "red"          -> "FF0000";
            case "blue"         -> "0070C0";
            case "green"        -> "00B050";
            case "orange"       -> "FF6600";
            case "purple"       -> "7030A0";
            case "black"        -> "000000";
            case "white"        -> "FFFFFF";
            case "gray","grey"  -> "808080";
            case "yellow"       -> "FFD700";
            case "teal"         -> "008080";
            default             -> color.length() == 6 ? color.toUpperCase() : "000000";
        };
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s == null ? "" : s.strip()); } catch (Exception e) { return def; }
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
