package com.behrainwala.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CsvTool {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Tool(name = "csv_to_json", description = "Parse CSV text into a JSON array. "
            + "When hasHeader is true (default), the first row becomes object keys and each subsequent row "
            + "becomes a JSON object. When false, each row becomes a JSON array. "
            + "Handles quoted fields, commas inside quotes, and escaped characters.")
    public String csvToJson(
            @ToolParam(description = "The CSV text to parse") String csv,
            @ToolParam(description = "Whether the first row is a header (default true). "
                    + "true → array of objects; false → array of arrays.", required = false) Boolean hasHeader) {

        if (csv == null || csv.isBlank()) return "Error: csv is required";

        boolean useHeader = hasHeader == null || hasHeader;

        try (CSVParser parser = new CSVParser(new StringReader(csv), buildFormat(useHeader))) {
            List<Object> rows = new ArrayList<>();

            if (useHeader) {
                List<String> headers = parser.getHeaderNames();
                for (CSVRecord record : parser) {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (String header : headers) row.put(header, record.get(header));
                    rows.add(row);
                }
            } else {
                for (CSVRecord record : parser) {
                    List<String> row = new ArrayList<>();
                    record.forEach(row::add);
                    rows.add(row);
                }
            }

            return "Rows: " + rows.size() + "\n\n" + MAPPER.writeValueAsString(rows);
        } catch (Exception e) {
            return "Error parsing CSV: " + e.getMessage();
        }
    }

    @Tool(name = "json_to_csv", description = "Convert a JSON array to CSV. "
            + "Input must be a JSON array of objects (keys become headers) or an array of arrays. "
            + "Returns well-formed CSV with quoted fields where needed.")
    public String jsonToCsv(
            @ToolParam(description = "JSON array of objects or arrays to convert to CSV") String json) {

        if (json == null || json.isBlank()) return "Error: json is required";

        try {
            Object parsed = MAPPER.readValue(json, Object.class);
            if (!(parsed instanceof List<?> rows)) {
                return "Error: JSON must be an array (got " + parsed.getClass().getSimpleName() + ")";
            }
            if (rows.isEmpty()) return "";

            StringWriter sw = new StringWriter();
            Object first = rows.get(0);

            if (first instanceof Map<?, ?> firstMap) {
                List<String> headers = firstMap.keySet().stream()
                        .map(Object::toString)
                        .collect(Collectors.toCollection(ArrayList::new));
                try (CSVPrinter printer = new CSVPrinter(sw,
                        CSVFormat.DEFAULT.builder().setHeader(headers.toArray(String[]::new)).build())) {
                    for (Object row : rows) {
                        if (row instanceof Map<?, ?> map) {
                            printer.printRecord(headers.stream()
                                    .map(h -> map.containsKey(h) ? String.valueOf(map.get(h)) : "")
                                    .toList());
                        }
                    }
                }
            } else {
                try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
                    for (Object row : rows) {
                        if (row instanceof List<?> cells) {
                            printer.printRecord(cells.stream().map(Object::toString).toList());
                        }
                    }
                }
            }

            return sw.toString();
        } catch (Exception e) {
            return "Error converting to CSV: " + e.getMessage();
        }
    }

    @Tool(name = "csv_stats", description = "Analyze CSV data: show column names, row count, and a sample of values "
            + "per column. Useful for understanding a dataset before writing queries or formulas.")
    public String csvStats(
            @ToolParam(description = "The CSV text to analyze") String csv,
            @ToolParam(description = "Whether the first row is a header (default true)", required = false) Boolean hasHeader) {

        if (csv == null || csv.isBlank()) return "Error: csv is required";

        boolean useHeader = hasHeader == null || hasHeader;

        try (CSVParser parser = new CSVParser(new StringReader(csv), buildFormat(useHeader))) {
            List<CSVRecord> records = parser.getRecords();

            if (records.isEmpty()) return "Empty CSV (no data rows)";

            StringBuilder sb = new StringBuilder();
            sb.append("CSV Analysis\n────────────\n");
            sb.append("Rows: ").append(records.size()).append("\n");

            if (useHeader) {
                List<String> headers = parser.getHeaderNames();
                sb.append("Columns: ").append(headers.size()).append("\n\n");
                for (String header : headers) {
                    sb.append("Column: ").append(header).append("\n");

                    LinkedHashSet<String> seen = new LinkedHashSet<>();
                    for (CSVRecord rec : records) {
                        seen.add(rec.get(header));
                        if (seen.size() >= 5) break;
                    }
                    sb.append("  Sample values: ").append(seen).append("\n");

                    try {
                        DoubleSummaryStatistics stats = records.stream()
                                .mapToDouble(r -> Double.parseDouble(r.get(header)))
                                .summaryStatistics();
                        sb.append("  Type: numeric  min=").append(stats.getMin())
                          .append("  max=").append(stats.getMax())
                          .append("  avg=").append(String.format("%.2f", stats.getAverage())).append("\n");
                    } catch (NumberFormatException e) {
                        sb.append("  Type: text\n");
                    }
                }
            } else {
                sb.append("Columns: ").append(records.get(0).size()).append(" (no header)\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error analyzing CSV: " + e.getMessage();
        }
    }

    private static CSVFormat buildFormat(boolean useHeader) {
        return useHeader
                ? CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
                : CSVFormat.DEFAULT;
    }
}
