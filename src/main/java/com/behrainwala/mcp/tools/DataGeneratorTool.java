package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP tool for generating test/fake data.
 * Provides lorem ipsum text, fake identities, tabular datasets, random values, and sequences.
 */
@Service
public class DataGeneratorTool {

    private final Random random = new Random();

    // ── Lorem Ipsum Sentences ──────────────────────────────────────────────

    private static final String[] LOREM_SENTENCES = {
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
            "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris.",
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore.",
            "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt.",
            "Curabitur pretium tincidunt lacus, nec gravida arcu fermentum vel.",
            "Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia.",
            "Praesent commodo cursus magna, vel scelerisque nisl consectetur et.",
            "Nullam quis risus eget urna mollis ornare vel eu leo.",
            "Maecenas faucibus mollis interdum, sed posuere consectetur est at lobortis.",
            "Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh.",
            "Donec ullamcorper nulla non metus auctor fringilla.",
            "Integer posuere erat a ante venenatis dapibus posuere velit aliquet.",
            "Aenean lacinia bibendum nulla sed consectetur.",
            "Vivamus sagittis lacus vel augue laoreet rutrum faucibus dolor auctor.",
            "Morbi leo risus, porta ac consectetur ac, vestibulum at eros.",
            "Cras mattis consectetur purus sit amet fermentum.",
            "Etiam porta sem malesuada magna mollis euismod.",
            "Nulla vitae elit libero, a pharetra augue mollis interdum.",
            "Pellentesque habitant morbi tristique senectus et netus et malesuada fames."
    };

    // ── Fake Data Pools ────────────────────────────────────────────────────

    private static final String[] FIRST_NAMES = {
            "James", "Mary", "Robert", "Patricia", "John", "Jennifer", "Michael", "Linda",
            "David", "Elizabeth", "William", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
            "Thomas", "Sarah", "Christopher", "Karen", "Charles", "Lisa", "Daniel", "Nancy",
            "Matthew", "Betty", "Anthony", "Margaret", "Mark", "Sandra", "Donald", "Ashley",
            "Steven", "Kimberly", "Paul", "Emily", "Andrew", "Donna", "Joshua", "Michelle",
            "Kenneth", "Carol", "Kevin", "Amanda", "Brian", "Dorothy", "George", "Melissa",
            "Timothy", "Deborah"
    };

    private static final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
            "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
            "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker",
            "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
            "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell",
            "Carter", "Roberts"
    };

    private static final String[] STREET_NAMES = {
            "Main St", "Oak Ave", "Maple Dr", "Cedar Ln", "Elm St", "Pine Rd",
            "Washington Blvd", "Park Ave", "Lake Dr", "Hill St", "River Rd", "Sunset Blvd",
            "Broadway", "Highland Ave", "Meadow Ln", "Forest Dr", "Spring St",
            "Valley Rd", "Church St", "Academy Ave"
    };

    private static final String[] CITIES = {
            "Springfield", "Riverside", "Fairview", "Madison", "Georgetown",
            "Clinton", "Arlington", "Salem", "Franklin", "Greenville",
            "Bristol", "Oxford", "Manchester", "Burlington", "Milton",
            "Chester", "Newport", "Ashland", "Lakewood", "Centerville"
    };

    private static final String[] STATES = {
            "CA", "TX", "FL", "NY", "PA", "IL", "OH", "GA", "NC", "MI"
    };

    private static final String[] COMPANIES = {
            "Acme Corp", "Globex Industries", "Initech Solutions", "Umbrella Inc",
            "Stark Technologies", "Wayne Enterprises", "Cyberdyne Systems", "Soylent Corp",
            "Hooli Technologies", "Pied Piper Inc"
    };

    private static final String[] JOB_TITLES = {
            "Software Engineer", "Product Manager", "Data Analyst", "Marketing Director",
            "Sales Representative", "HR Coordinator", "Financial Analyst", "UX Designer",
            "Project Manager", "Systems Administrator", "Business Analyst", "QA Engineer",
            "Technical Writer", "Account Executive", "Operations Manager",
            "DevOps Engineer", "Customer Success Manager", "Research Scientist",
            "Graphic Designer", "Database Administrator"
    };

    private static final String[] EMAIL_DOMAINS = {
            "example.com", "testmail.org", "fakeinbox.net", "mailinator.com", "sample.io",
            "demo.org", "placeholder.net", "fakedata.com", "testsite.org", "mockmail.io"
    };

    // ── Tool 1: Lorem Ipsum ────────────────────────────────────────────────

    @Tool(name = "generate_lorem_ipsum", description = "Generate placeholder lorem ipsum text. "
            + "Useful for creating dummy content for UI mockups, testing layouts, or filling templates.")
    public String generateLoremIpsum(
            @ToolParam(description = "Type of text to generate: 'words', 'sentences', or 'paragraphs'.") String type,
            @ToolParam(description = "Number of units to generate (e.g., 50 words, 5 sentences, 3 paragraphs).") int count) {

        if (count < 1) {
            return "Error: count must be at least 1.";
        }

        return switch (type.toLowerCase()) {
            case "words" -> generateLoremWords(count);
            case "sentences" -> generateLoremSentences(count);
            case "paragraphs" -> generateLoremParagraphs(count);
            default -> "Error: Invalid type '" + type + "'. Use 'words', 'sentences', or 'paragraphs'.";
        };
    }

    private String generateLoremWords(int count) {
        List<String> allWords = new ArrayList<>();
        for (String sentence : LOREM_SENTENCES) {
            String cleaned = sentence.replaceAll("[.,]", "").toLowerCase();
            Collections.addAll(allWords, cleaned.split("\\s+"));
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(" ");
            sb.append(allWords.get(random.nextInt(allWords.size())));
        }
        // Capitalize first word and add period at end
        String result = sb.toString();
        return Character.toUpperCase(result.charAt(0)) + result.substring(1) + ".";
    }

    private String generateLoremSentences(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(" ");
            sb.append(LOREM_SENTENCES[random.nextInt(LOREM_SENTENCES.length)]);
        }
        return sb.toString();
    }

    private String generateLoremParagraphs(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append("\n\n");
            int sentencesInParagraph = 3 + random.nextInt(5); // 3-7 sentences
            for (int j = 0; j < sentencesInParagraph; j++) {
                if (j > 0) sb.append(" ");
                sb.append(LOREM_SENTENCES[random.nextInt(LOREM_SENTENCES.length)]);
            }
        }
        return sb.toString();
    }

    // ── Tool 2: Fake Identity Data ─────────────────────────────────────────

    @Tool(name = "generate_fake_data", description = "Generate fake identity data for testing. "
            + "Produces realistic-looking but obviously fake personal records. "
            + "Useful for populating test databases, creating sample user lists, or UI testing.")
    public String generateFakeData(
            @ToolParam(description = "Number of records to generate (1-50).") int count,
            @ToolParam(description = "Comma-separated list of fields to include. Available: "
                    + "name, email, phone, address, company, job_title, username, date_of_birth.") String fields,
            @ToolParam(description = "Output format: 'text', 'csv', or 'json'.") String format) {

        if (count < 1 || count > 50) {
            return "Error: count must be between 1 and 50.";
        }

        String[] requestedFields = Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        if (requestedFields.length == 0) {
            return "Error: At least one field must be specified.";
        }

        List<String> validFields = Arrays.asList(
                "name", "email", "phone", "address", "company", "job_title", "username", "date_of_birth");
        for (String f : requestedFields) {
            if (!validFields.contains(f.toLowerCase())) {
                return "Error: Unknown field '" + f + "'. Valid fields: " + String.join(", ", validFields);
            }
        }

        List<Map<String, String>> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            records.add(generateRecord(firstName, lastName, requestedFields));
        }

        return switch (format.toLowerCase()) {
            case "text" -> formatAsText(records, requestedFields);
            case "csv" -> formatAsCsv(records, requestedFields);
            case "json" -> formatAsJson(records, requestedFields);
            default -> "Error: Invalid format '" + format + "'. Use 'text', 'csv', or 'json'.";
        };
    }

    private Map<String, String> generateRecord(String firstName, String lastName, String[] fields) {
        Map<String, String> record = new LinkedHashMap<>();
        for (String field : fields) {
            switch (field.toLowerCase()) {
                case "name":
                    record.put("name", firstName + " " + lastName);
                    break;
                case "email":
                    String domain = EMAIL_DOMAINS[random.nextInt(EMAIL_DOMAINS.length)];
                    record.put("email", firstName.toLowerCase() + "." + lastName.toLowerCase()
                            + random.nextInt(100) + "@" + domain);
                    break;
                case "phone":
                    record.put("phone", String.format("(555) %03d-%04d",
                            100 + random.nextInt(900), random.nextInt(10000)));
                    break;
                case "address":
                    record.put("address", String.format("%d %s, %s, %s %05d",
                            100 + random.nextInt(9900),
                            STREET_NAMES[random.nextInt(STREET_NAMES.length)],
                            CITIES[random.nextInt(CITIES.length)],
                            STATES[random.nextInt(STATES.length)],
                            10000 + random.nextInt(90000)));
                    break;
                case "company":
                    record.put("company", COMPANIES[random.nextInt(COMPANIES.length)]);
                    break;
                case "job_title":
                    record.put("job_title", JOB_TITLES[random.nextInt(JOB_TITLES.length)]);
                    break;
                case "username":
                    record.put("username", firstName.toLowerCase()
                            + lastName.toLowerCase().charAt(0)
                            + (10 + random.nextInt(90)));
                    break;
                case "date_of_birth":
                    int year = 1960 + random.nextInt(40); // 1960-1999
                    int month = 1 + random.nextInt(12);
                    int day = 1 + random.nextInt(28);
                    record.put("date_of_birth", String.format("%04d-%02d-%02d", year, month, day));
                    break;
            }
        }
        return record;
    }

    private String formatAsText(List<Map<String, String>> records, String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < records.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append("--- Record ").append(i + 1).append(" ---\n");
            Map<String, String> record = records.get(i);
            for (String field : fields) {
                String label = field.substring(0, 1).toUpperCase() + field.substring(1).replace("_", " ");
                sb.append(String.format("  %-15s: %s%n", label, record.get(field)));
            }
        }
        return sb.toString();
    }

    private String formatAsCsv(List<Map<String, String>> records, String[] fields) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", fields)).append("\n");
        for (Map<String, String> record : records) {
            List<String> values = new ArrayList<>();
            for (String field : fields) {
                String value = record.get(field);
                if (value.contains(",") || value.contains("\"")) {
                    value = "\"" + value.replace("\"", "\"\"") + "\"";
                }
                values.add(value);
            }
            sb.append(String.join(",", values)).append("\n");
        }
        return sb.toString();
    }

    private String formatAsJson(List<Map<String, String>> records, String[] fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < records.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append("  {");
            Map<String, String> record = records.get(i);
            int fieldIdx = 0;
            for (String field : fields) {
                if (fieldIdx > 0) sb.append(", ");
                sb.append("\"").append(field).append("\": \"")
                        .append(escapeJson(record.get(field))).append("\"");
                fieldIdx++;
            }
            sb.append("}");
        }
        sb.append("\n]");
        return sb.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\t", "\\t");
    }

    // ── Tool 3: Test Dataset ───────────────────────────────────────────────

    @Tool(name = "generate_test_dataset", description = "Generate tabular test data with typed columns. "
            + "Supports column types: int, float, string, bool, date. "
            + "Ranges can be specified for numeric and date types. "
            + "Column definition format: 'col_name:type' or 'col_name:type:min-max'. "
            + "Example: 'id:int,name:string,age:int:18-65,salary:float:30000-120000,active:bool,date:date:2020-2024'.")
    public String generateTestDataset(
            @ToolParam(description = "Comma-separated column definitions. Format: 'name:type' or 'name:type:min-max'. "
                    + "Types: int, float, string, bool, date. "
                    + "Examples: 'id:int', 'age:int:18-65', 'salary:float:30000-120000', 'hired:date:2020-2024'.") String columns,
            @ToolParam(description = "Number of rows to generate (1-100).") int rows,
            @ToolParam(description = "Output format: 'csv', 'json', or 'sql_insert'.") String format,
            @ToolParam(description = "Table name for sql_insert format. Ignored for other formats.", required = false) String tableName) {

        if (rows < 1 || rows > 100) {
            return "Error: rows must be between 1 and 100.";
        }

        List<ColumnDef> columnDefs;
        try {
            columnDefs = parseColumnDefinitions(columns);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }

        List<List<String>> data = new ArrayList<>();
        int autoId = 1;
        for (int r = 0; r < rows; r++) {
            List<String> row = new ArrayList<>();
            for (ColumnDef col : columnDefs) {
                row.add(generateColumnValue(col, autoId));
            }
            data.add(row);
            autoId++;
        }

        return switch (format.toLowerCase()) {
            case "csv" -> datasetToCsv(columnDefs, data);
            case "json" -> datasetToJson(columnDefs, data);
            case "sql_insert" -> {
                String table = (tableName != null && !tableName.isBlank()) ? tableName : "test_data";
                yield datasetToSql(columnDefs, data, table);
            }
            default -> "Error: Invalid format '" + format + "'. Use 'csv', 'json', or 'sql_insert'.";
        };
    }

    private static class ColumnDef {
        String name;
        String type;
        String rangeMin;
        String rangeMax;

        ColumnDef(String name, String type, String rangeMin, String rangeMax) {
            this.name = name;
            this.type = type;
            this.rangeMin = rangeMin;
            this.rangeMax = rangeMax;
        }
    }

    private List<ColumnDef> parseColumnDefinitions(String columns) {
        List<ColumnDef> defs = new ArrayList<>();
        String[] parts = columns.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            String[] segments = trimmed.split(":");
            if (segments.length < 2) {
                throw new IllegalArgumentException(
                        "Invalid column definition '" + trimmed + "'. Expected format: 'name:type' or 'name:type:min-max'.");
            }
            String name = segments[0].trim();
            String type = segments[1].trim().toLowerCase();
            String rangeMin = null;
            String rangeMax = null;

            if (!Arrays.asList("int", "float", "string", "bool", "date").contains(type)) {
                throw new IllegalArgumentException(
                        "Unknown column type '" + type + "'. Valid types: int, float, string, bool, date.");
            }

            if (segments.length >= 3) {
                String rangeStr = segments[2].trim();
                String[] rangeParts = rangeStr.split("-", 2);
                if (rangeParts.length == 2) {
                    rangeMin = rangeParts[0].trim();
                    rangeMax = rangeParts[1].trim();
                } else {
                    rangeMin = rangeStr;
                    rangeMax = rangeStr;
                }
            }

            defs.add(new ColumnDef(name, type, rangeMin, rangeMax));
        }
        return defs;
    }

    private String generateColumnValue(ColumnDef col, int autoId) {
        switch (col.type) {
            case "int": {
                int min = col.rangeMin != null ? Integer.parseInt(col.rangeMin) : 1;
                int max = col.rangeMax != null ? Integer.parseInt(col.rangeMax) : 10000;
                if (col.name.equalsIgnoreCase("id") && col.rangeMin == null) {
                    return String.valueOf(autoId);
                }
                return String.valueOf(min + random.nextInt(max - min + 1));
            }
            case "float": {
                double min = col.rangeMin != null ? Double.parseDouble(col.rangeMin) : 0.0;
                double max = col.rangeMax != null ? Double.parseDouble(col.rangeMax) : 10000.0;
                double value = min + random.nextDouble() * (max - min);
                return String.format("%.2f", value);
            }
            case "string": {
                String first = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                String last = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                return first + " " + last;
            }
            case "bool": {
                return random.nextBoolean() ? "true" : "false";
            }
            case "date": {
                int minYear = col.rangeMin != null ? Integer.parseInt(col.rangeMin) : 2000;
                int maxYear = col.rangeMax != null ? Integer.parseInt(col.rangeMax) : 2025;
                int year = minYear + random.nextInt(maxYear - minYear + 1);
                int month = 1 + random.nextInt(12);
                int day = 1 + random.nextInt(28);
                return String.format("%04d-%02d-%02d", year, month, day);
            }
            default:
                return "";
        }
    }

    private String datasetToCsv(List<ColumnDef> cols, List<List<String>> data) {
        StringBuilder sb = new StringBuilder();
        sb.append(cols.stream().map(c -> c.name).collect(Collectors.joining(","))).append("\n");
        for (List<String> row : data) {
            List<String> escaped = new ArrayList<>();
            for (String val : row) {
                if (val.contains(",") || val.contains("\"") || val.contains(" ")) {
                    escaped.add("\"" + val.replace("\"", "\"\"") + "\"");
                } else {
                    escaped.add(val);
                }
            }
            sb.append(String.join(",", escaped)).append("\n");
        }
        return sb.toString();
    }

    private String datasetToJson(List<ColumnDef> cols, List<List<String>> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int r = 0; r < data.size(); r++) {
            if (r > 0) sb.append(",\n");
            sb.append("  {");
            List<String> row = data.get(r);
            for (int c = 0; c < cols.size(); c++) {
                if (c > 0) sb.append(", ");
                ColumnDef col = cols.get(c);
                String val = row.get(c);
                sb.append("\"").append(col.name).append("\": ");
                switch (col.type) {
                    case "int" -> sb.append(val);
                    case "float" -> sb.append(val);
                    case "bool" -> sb.append(val);
                    case null, default -> sb.append("\"").append(escapeJson(val)).append("\"");
                }
            }
            sb.append("}");
        }
        sb.append("\n]");
        return sb.toString();
    }

    private String datasetToSql(List<ColumnDef> cols, List<List<String>> data, String tableName) {
        StringBuilder sb = new StringBuilder();
        String colNames = cols.stream().map(c -> c.name).collect(Collectors.joining(", "));
        for (List<String> row : data) {
            sb.append("INSERT INTO ").append(tableName).append(" (").append(colNames).append(") VALUES (");
            for (int c = 0; c < cols.size(); c++) {
                if (c > 0) sb.append(", ");
                ColumnDef col = cols.get(c);
                String val = row.get(c);
                if ("int".equals(col.type) || "float".equals(col.type) || "bool".equals(col.type)) {
                    sb.append(val);
                } else {
                    sb.append("'").append(val.replace("'", "''")).append("'");
                }
            }
            sb.append(");\n");
        }
        return sb.toString();
    }

    // ── Tool 4: Random Values ──────────────────────────────────────────────

    @Tool(name = "generate_random", description = "Generate random values of various types. "
            + "Supports integers, floats, strings, hex, alphanumeric, dates, times, colors, "
            + "IPv4 addresses, MAC addresses, and UUIDs.")
    public String generateRandom(
            @ToolParam(description = "Type of random value: 'int', 'float', 'string', 'hex', 'alphanumeric', "
                    + "'date', 'time', 'color', 'ipv4', 'mac_address', or 'uuid'.") String type,
            @ToolParam(description = "Number of values to generate (1-100).") int count,
            @ToolParam(description = "Minimum value (for int/float) or start date (for date, format YYYY-MM-DD).",
                    required = false) String min,
            @ToolParam(description = "Maximum value (for int/float) or end date (for date, format YYYY-MM-DD).",
                    required = false) String max,
            @ToolParam(description = "Length of generated strings (for string, hex, alphanumeric types). Default: 10.",
                    required = false) String length) {

        if (count < 1 || count > 100) {
            return "Error: count must be between 1 and 100.";
        }

        int strLength = 10;
        if (length != null && !length.isBlank()) {
            try {
                strLength = Integer.parseInt(length);
                if (strLength < 1) strLength = 1;
                if (strLength > 256) strLength = 256;
            } catch (NumberFormatException e) {
                return "Error: Invalid length value '" + length + "'.";
            }
        }

        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < count; i++) {
                results.add(generateSingleRandom(type.toLowerCase(), min, max, strLength));
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Generated ").append(count).append(" random ").append(type).append(" value(s):\n\n");
        for (int i = 0; i < results.size(); i++) {
            sb.append(String.format("%3d. %s%n", i + 1, results.get(i)));
        }
        return sb.toString();
    }

    private String generateSingleRandom(String type, String min, String max, int strLength) {
        switch (type) {
            case "int": {
                int lo = min != null && !min.isBlank() ? Integer.parseInt(min) : 0;
                int hi = max != null && !max.isBlank() ? Integer.parseInt(max) : 1000000;
                return String.valueOf(lo + random.nextInt(hi - lo + 1));
            }
            case "float": {
                double lo = min != null && !min.isBlank() ? Double.parseDouble(min) : 0.0;
                double hi = max != null && !max.isBlank() ? Double.parseDouble(max) : 1000000.0;
                return String.format("%.4f", lo + random.nextDouble() * (hi - lo));
            }
            case "string": {
                String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
                return randomString(chars, strLength);
            }
            case "hex": {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < strLength; i++) {
                    sb.append(Integer.toHexString(random.nextInt(16)));
                }
                return sb.toString();
            }
            case "alphanumeric": {
                String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                return randomString(chars, strLength);
            }
            case "date": {
                LocalDate start = min != null && !min.isBlank()
                        ? LocalDate.parse(min, DateTimeFormatter.ISO_LOCAL_DATE)
                        : LocalDate.of(2000, 1, 1);
                LocalDate end = max != null && !max.isBlank()
                        ? LocalDate.parse(max, DateTimeFormatter.ISO_LOCAL_DATE)
                        : LocalDate.of(2025, 12, 31);
                long days = end.toEpochDay() - start.toEpochDay();
                if (days < 0) days = 0;
                return start.plusDays(random.nextInt((int) days + 1))
                        .format(DateTimeFormatter.ISO_LOCAL_DATE);
            }
            case "time": {
                return String.format("%02d:%02d:%02d",
                        random.nextInt(24), random.nextInt(60), random.nextInt(60));
            }
            case "color": {
                return String.format("#%06X", random.nextInt(0xFFFFFF + 1));
            }
            case "ipv4": {
                return String.format("%d.%d.%d.%d",
                        1 + random.nextInt(254), random.nextInt(256),
                        random.nextInt(256), 1 + random.nextInt(254));
            }
            case "mac_address": {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 6; i++) {
                    if (i > 0) sb.append(":");
                    sb.append(String.format("%02X", random.nextInt(256)));
                }
                return sb.toString();
            }
            case "uuid": {
                return UUID.randomUUID().toString();
            }
            default:
                throw new IllegalArgumentException("Unknown type '" + type
                        + "'. Valid types: int, float, string, hex, alphanumeric, date, time, color, ipv4, mac_address, uuid.");
        }
    }

    private String randomString(String charset, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(charset.charAt(random.nextInt(charset.length())));
        }
        return sb.toString();
    }

    // ── Tool 5: Sequences ──────────────────────────────────────────────────

    @Tool(name = "generate_sequence", description = "Generate numeric, date, or ID sequences. "
            + "Supports arithmetic, geometric, Fibonacci, date, timestamp, and ID sequences. "
            + "Useful for generating primary keys, time series data, or mathematical sequences.")
    public String generateSequence(
            @ToolParam(description = "Sequence type: 'arithmetic', 'geometric', 'fibonacci', "
                    + "'dates', 'timestamps', or 'ids'.") String type,
            @ToolParam(description = "Starting value. For arithmetic/geometric: a number. "
                    + "For dates: a date in YYYY-MM-DD format. For timestamps: a datetime in 'YYYY-MM-DD HH:mm' format. "
                    + "For IDs: a prefix string (e.g., 'ORD-'). For fibonacci: ignored (always starts 0, 1).",
                    required = false) String start,
            @ToolParam(description = "Step (arithmetic), ratio (geometric), interval in days (dates), "
                    + "interval in minutes (timestamps), or starting number (IDs). "
                    + "For fibonacci: ignored.", required = false) String step,
            @ToolParam(description = "Number of elements to generate.") int count) {

        if (count < 1) {
            return "Error: count must be at least 1.";
        }
        if (count > 1000) {
            return "Error: count must be at most 1000.";
        }

        try {
            return switch (type.toLowerCase()) {
                case "arithmetic" -> generateArithmetic(start, step, count);
                case "geometric" -> generateGeometric(start, step, count);
                case "fibonacci" -> generateFibonacci(count);
                case "dates" -> generateDateSequence(start, step, count);
                case "timestamps" -> generateTimestampSequence(start, step, count);
                case "ids" -> generateIdSequence(start, step, count);
                default -> "Error: Unknown sequence type '" + type
                        + "'. Valid types: arithmetic, geometric, fibonacci, dates, timestamps, ids.";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String generateArithmetic(String start, String step, int count) {
        double s = start != null && !start.isBlank() ? Double.parseDouble(start) : 0;
        double d = step != null && !step.isBlank() ? Double.parseDouble(step) : 1;

        StringBuilder sb = new StringBuilder();
        sb.append("Arithmetic Sequence (start=").append(formatNum(s))
                .append(", step=").append(formatNum(d)).append("):\n\n");
        for (int i = 0; i < count; i++) {
            double val = s + i * d;
            sb.append(String.format("%4d. %s%n", i + 1, formatNum(val)));
        }
        return sb.toString();
    }

    private String generateGeometric(String start, String step, int count) {
        double s = start != null && !start.isBlank() ? Double.parseDouble(start) : 1;
        double r = step != null && !step.isBlank() ? Double.parseDouble(step) : 2;

        StringBuilder sb = new StringBuilder();
        sb.append("Geometric Sequence (start=").append(formatNum(s))
                .append(", ratio=").append(formatNum(r)).append("):\n\n");
        double val = s;
        for (int i = 0; i < count; i++) {
            sb.append(String.format("%4d. %s%n", i + 1, formatNum(val)));
            val *= r;
        }
        return sb.toString();
    }

    private String generateFibonacci(int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fibonacci Sequence:\n\n");
        long a = 0, b = 1;
        for (int i = 0; i < count; i++) {
            sb.append(String.format("%4d. %d%n", i + 1, a));
            long next = a + b;
            a = b;
            b = next;
        }
        return sb.toString();
    }

    private String generateDateSequence(String start, String step, int count) {
        LocalDate date = start != null && !start.isBlank()
                ? LocalDate.parse(start, DateTimeFormatter.ISO_LOCAL_DATE)
                : LocalDate.now();
        int intervalDays = step != null && !step.isBlank() ? Integer.parseInt(step) : 1;

        StringBuilder sb = new StringBuilder();
        sb.append("Date Sequence (start=").append(date).append(", interval=")
                .append(intervalDays).append(" day(s)):\n\n");
        for (int i = 0; i < count; i++) {
            sb.append(String.format("%4d. %s%n", i + 1, date.format(DateTimeFormatter.ISO_LOCAL_DATE)));
            date = date.plusDays(intervalDays);
        }
        return sb.toString();
    }

    private String generateTimestampSequence(String start, String step, int count) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        java.time.LocalDateTime dt;
        if (start != null && !start.isBlank()) {
            dt = java.time.LocalDateTime.parse(start, dtf);
        } else {
            dt = java.time.LocalDateTime.now().withSecond(0).withNano(0);
        }
        int intervalMinutes = step != null && !step.isBlank() ? Integer.parseInt(step) : 60;

        StringBuilder sb = new StringBuilder();
        sb.append("Timestamp Sequence (start=").append(dt.format(dtf))
                .append(", interval=").append(intervalMinutes).append(" min):\n\n");
        for (int i = 0; i < count; i++) {
            sb.append(String.format("%4d. %s%n", i + 1, dt.format(dtf)));
            dt = dt.plusMinutes(intervalMinutes);
        }
        return sb.toString();
    }

    private String generateIdSequence(String start, String step, int count) {
        String prefix = start != null && !start.isBlank() ? start : "ID-";
        int startNum = step != null && !step.isBlank() ? Integer.parseInt(step) : 1;

        // Determine padding width based on total count
        int maxNum = startNum + count - 1;
        int padWidth = String.valueOf(maxNum).length();
        if (padWidth < 4) padWidth = 4;

        StringBuilder sb = new StringBuilder();
        sb.append("ID Sequence (prefix='").append(prefix).append("', start=")
                .append(startNum).append("):\n\n");
        for (int i = 0; i < count; i++) {
            sb.append(String.format("%4d. %s%s%n", i + 1, prefix,
                    String.format("%0" + padWidth + "d", startNum + i)));
        }
        return sb.toString();
    }

    private String formatNum(double val) {
        if (val == (long) val) {
            return String.valueOf((long) val);
        }
        return String.valueOf(val);
    }
}
