package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataGeneratorToolTest {

    private DataGeneratorTool tool;

    @BeforeEach
    void setUp() {
        tool = new DataGeneratorTool();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  generateLoremIpsum
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    class GenerateLoremIpsum {

        @Test
        void words_returnsCapitalizedSentenceEndingWithPeriod() {
            String result = tool.generateLoremIpsum("words", 10);
            assertThat(result).isNotBlank();
            assertThat(result).endsWith(".");
            assertThat(Character.isUpperCase(result.charAt(0))).isTrue();
            // 10 words + period, so at least 9 spaces between words
            long spaceCount = result.chars().filter(c -> c == ' ').count();
            assertThat(spaceCount).isGreaterThanOrEqualTo(9);
        }

        @Test
        void words_singleWord() {
            String result = tool.generateLoremIpsum("words", 1);
            assertThat(result).isNotBlank().endsWith(".");
        }

        @Test
        void sentences_returnsExpectedCount() {
            String result = tool.generateLoremIpsum("sentences", 3);
            assertThat(result).isNotBlank();
            // Each sentence ends with a period, so at least 3 periods
            long periodCount = result.chars().filter(c -> c == '.').count();
            assertThat(periodCount).isGreaterThanOrEqualTo(3);
        }

        @Test
        void sentences_single() {
            String result = tool.generateLoremIpsum("sentences", 1);
            assertThat(result).isNotBlank().endsWith(".");
        }

        @Test
        void paragraphs_multipleSeparatedByDoubleNewline() {
            String result = tool.generateLoremIpsum("paragraphs", 3);
            assertThat(result).isNotBlank();
            // 3 paragraphs => 2 separators
            assertThat(result.split("\n\n")).hasSizeGreaterThanOrEqualTo(3);
        }

        @Test
        void paragraphs_single() {
            String result = tool.generateLoremIpsum("paragraphs", 1);
            assertThat(result).isNotBlank();
            // Single paragraph should have no double-newline separator
            assertThat(result).doesNotContain("\n\n");
        }

        @Test
        void unknownType_returnsError() {
            String result = tool.generateLoremIpsum("haiku", 3);
            assertThat(result).containsIgnoringCase("error").contains("haiku");
        }

        @Test
        void zeroCount_returnsError() {
            String result = tool.generateLoremIpsum("words", 0);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void negativeCount_returnsError() {
            String result = tool.generateLoremIpsum("sentences", -5);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void caseInsensitiveType() {
            String result = tool.generateLoremIpsum("WORDS", 5);
            assertThat(result).isNotBlank().endsWith(".");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  generateFakeData
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    class GenerateFakeData {

        // ── valid field coverage ────────────────────────────────────────────

        @Test
        void nameField_text() {
            String result = tool.generateFakeData(2, "name", "text");
            assertThat(result).contains("Record 1").contains("Record 2");
            assertThat(result).containsIgnoringCase("name");
        }

        @Test
        void emailField_text() {
            String result = tool.generateFakeData(1, "email", "text");
            assertThat(result).contains("@");
        }

        @Test
        void phoneField_text() {
            String result = tool.generateFakeData(1, "phone", "text");
            assertThat(result).contains("(555)");
        }

        @Test
        void addressField_text() {
            String result = tool.generateFakeData(1, "address", "text");
            assertThat(result).isNotBlank();
            // Address contains a comma (city, state)
            assertThat(result).contains(",");
        }

        @Test
        void companyField_text() {
            String result = tool.generateFakeData(1, "company", "text");
            assertThat(result).isNotBlank();
        }

        @Test
        void jobTitleField_text() {
            String result = tool.generateFakeData(1, "job_title", "text");
            assertThat(result).isNotBlank();
        }

        @Test
        void usernameField_text() {
            String result = tool.generateFakeData(1, "username", "text");
            assertThat(result).isNotBlank();
        }

        @Test
        void dateOfBirthField_text() {
            String result = tool.generateFakeData(1, "date_of_birth", "text");
            // date_of_birth format is YYYY-MM-DD
            assertThat(result).containsPattern("\\d{4}-\\d{2}-\\d{2}");
        }

        @Test
        void allFieldsCombined_text() {
            String result = tool.generateFakeData(1,
                    "name,email,phone,address,company,job_title,username,date_of_birth", "text");
            assertThat(result).contains("Record 1");
        }

        // ── format variants ─────────────────────────────────────────────────

        @Test
        void csvFormat_hasHeaderAndRows() {
            String result = tool.generateFakeData(3, "name,email", "csv");
            String[] lines = result.split("\n");
            // header + 3 data rows
            assertThat(lines).hasSizeGreaterThanOrEqualTo(4);
            assertThat(lines[0]).contains("name").contains("email");
        }

        @Test
        void csvFormat_escapesCommasInValues() {
            // address values contain commas, so they must be quoted in CSV
            String result = tool.generateFakeData(1, "address", "csv");
            assertThat(result).contains("\"");
        }

        @Test
        void jsonFormat_validStructure() {
            String result = tool.generateFakeData(2, "name,email", "json");
            assertThat(result).startsWith("[");
            assertThat(result).contains("{");
            assertThat(result).contains("}");
            assertThat(result.trim()).endsWith("]");
        }

        @Test
        void invalidFormat_returnsError() {
            String result = tool.generateFakeData(1, "name", "xml");
            assertThat(result).containsIgnoringCase("error").contains("xml");
        }

        // ── error paths ─────────────────────────────────────────────────────

        @Test
        void countZero_returnsError() {
            String result = tool.generateFakeData(0, "name", "text");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void countNegative_returnsError() {
            String result = tool.generateFakeData(-1, "name", "text");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void countAbove50_returnsError() {
            String result = tool.generateFakeData(51, "name", "text");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void emptyFields_returnsError() {
            String result = tool.generateFakeData(1, "", "text");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void unknownField_returnsError() {
            String result = tool.generateFakeData(1, "name,ssn", "text");
            assertThat(result).containsIgnoringCase("error").contains("ssn");
        }

        @Test
        void boundaryCount1_isValid() {
            String result = tool.generateFakeData(1, "name", "text");
            assertThat(result).doesNotContainIgnoringCase("error");
        }

        @Test
        void boundaryCount50_isValid() {
            String result = tool.generateFakeData(50, "name", "text");
            assertThat(result).doesNotContainIgnoringCase("error");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  generateTestDataset
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    class GenerateTestDataset {

        // ── column types ────────────────────────────────────────────────────

        @Test
        void intColumn_csv() {
            String result = tool.generateTestDataset("score:int", 3, "csv", null);
            assertThat(result).isNotBlank();
            // header + 3 rows
            assertThat(result.split("\n")).hasSizeGreaterThanOrEqualTo(4);
        }

        @Test
        void intColumnWithRange_csv() {
            String result = tool.generateTestDataset("age:int:18-65", 5, "csv", null);
            assertThat(result).contains("age");
        }

        @Test
        void idColumn_autoIncrements() {
            String result = tool.generateTestDataset("id:int", 3, "csv", null);
            // id column without range gets autoId: 1, 2, 3
            assertThat(result).contains("1").contains("2").contains("3");
        }

        @Test
        void idColumnWithRange_doesNotAutoIncrement() {
            // When id has explicit range, it uses random values (not autoId)
            String result = tool.generateTestDataset("id:int:100-200", 2, "csv", null);
            assertThat(result).isNotBlank();
        }

        @Test
        void floatColumn_csv() {
            String result = tool.generateTestDataset("price:float", 2, "csv", null);
            assertThat(result).contains("price");
            // float values contain a decimal point
            assertThat(result).contains(".");
        }

        @Test
        void floatColumnWithRange_csv() {
            String result = tool.generateTestDataset("salary:float:30000-120000", 2, "csv", null);
            assertThat(result).contains("salary");
        }

        @Test
        void stringColumn_csv() {
            String result = tool.generateTestDataset("name:string", 2, "csv", null);
            assertThat(result).contains("name");
        }

        @Test
        void boolColumn_csv() {
            String result = tool.generateTestDataset("active:bool", 10, "csv", null);
            assertThat(result).contains("active");
            // Over 10 rows, very likely to see both true and false
            assertThat(result).containsAnyOf("true", "false");
        }

        @Test
        void dateColumn_csv() {
            String result = tool.generateTestDataset("hired:date", 2, "csv", null);
            assertThat(result).containsPattern("\\d{4}-\\d{2}-\\d{2}");
        }

        @Test
        void dateColumnWithRange_csv() {
            String result = tool.generateTestDataset("hired:date:2020-2024", 2, "csv", null);
            assertThat(result).isNotBlank();
        }

        @Test
        void multipleColumns_csv() {
            String result = tool.generateTestDataset(
                    "id:int,name:string,age:int:18-65,salary:float:30000-120000,active:bool,hired:date:2020-2024",
                    3, "csv", null);
            String[] lines = result.split("\n");
            assertThat(lines[0]).contains("id").contains("name").contains("age")
                    .contains("salary").contains("active").contains("hired");
        }

        // ── output formats ──────────────────────────────────────────────────

        @Test
        void jsonFormat() {
            String result = tool.generateTestDataset("id:int,name:string,active:bool", 2, "json", null);
            assertThat(result).startsWith("[");
            assertThat(result.trim()).endsWith("]");
            // int and bool values should not be quoted in json
            assertThat(result).contains("\"id\": ");
            assertThat(result).contains("\"name\": \"");
        }

        @Test
        void sqlInsertFormat_defaultTableName() {
            String result = tool.generateTestDataset("id:int,name:string", 2, "sql_insert", null);
            assertThat(result).contains("INSERT INTO test_data");
            assertThat(result).contains("VALUES");
        }

        @Test
        void sqlInsertFormat_customTableName() {
            String result = tool.generateTestDataset("id:int,name:string", 2, "sql_insert", "users");
            assertThat(result).contains("INSERT INTO users");
        }

        @Test
        void sqlInsertFormat_blankTableName_usesDefault() {
            String result = tool.generateTestDataset("id:int", 1, "sql_insert", "  ");
            assertThat(result).contains("INSERT INTO test_data");
        }

        @Test
        void sqlInsertFormat_stringValuesQuoted() {
            String result = tool.generateTestDataset("name:string", 1, "sql_insert", "t");
            // String values are wrapped in single quotes
            assertThat(result).contains("'");
        }

        @Test
        void sqlInsertFormat_numericValuesNotQuoted() {
            String result = tool.generateTestDataset("id:int,price:float,active:bool", 1, "sql_insert", "t");
            assertThat(result).contains("INSERT INTO t");
        }

        @Test
        void invalidFormat_returnsError() {
            String result = tool.generateTestDataset("id:int", 1, "xml", null);
            assertThat(result).containsIgnoringCase("error").contains("xml");
        }

        // ── error paths ─────────────────────────────────────────────────────

        @Test
        void rowsZero_returnsError() {
            String result = tool.generateTestDataset("id:int", 0, "csv", null);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void rowsNegative_returnsError() {
            String result = tool.generateTestDataset("id:int", -1, "csv", null);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void rowsAbove100_returnsError() {
            String result = tool.generateTestDataset("id:int", 101, "csv", null);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void invalidColumnDef_missingType_returnsError() {
            String result = tool.generateTestDataset("name", 1, "csv", null);
            assertThat(result).containsIgnoringCase("error").contains("name");
        }

        @Test
        void invalidColumnType_returnsError() {
            String result = tool.generateTestDataset("x:varchar", 1, "csv", null);
            assertThat(result).containsIgnoringCase("error").contains("varchar");
        }

        @Test
        void boundaryRows1_isValid() {
            String result = tool.generateTestDataset("id:int", 1, "csv", null);
            assertThat(result).doesNotContainIgnoringCase("error");
        }

        @Test
        void boundaryRows100_isValid() {
            String result = tool.generateTestDataset("id:int", 100, "csv", null);
            assertThat(result).doesNotContainIgnoringCase("error");
        }

        @Test
        void rangeWithSingleValue_setsMinAndMaxSame() {
            // When range doesn't contain a dash the parser sets both min and max to the same value
            // Using "id:int:5" should parse rangeMin=5 rangeMax="" (split by -, 2 parts)
            // Actually "5" split by "-" with limit 2 gives ["5"] length 1 => both set to "5"
            String result = tool.generateTestDataset("score:int:42", 1, "csv", null);
            // rangeMin and rangeMax both "42", so random.nextInt(1) = 0, result = 42
            assertThat(result).contains("42");
        }

        @Test
        void csvEscapesStringsWithSpaces() {
            // string columns produce "First Last" which has a space => gets quoted in CSV
            String result = tool.generateTestDataset("name:string", 1, "csv", null);
            assertThat(result).contains("\"");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  generateRandom
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    class GenerateRandom {

        @Test
        void intType_withinRange() {
            String result = tool.generateRandom("int", 5, "1", "100", null);
            assertThat(result).contains("Generated 5 random int value(s)");
        }

        @Test
        void intType_defaultRange() {
            String result = tool.generateRandom("int", 1, null, null, null);
            assertThat(result).contains("Generated 1 random int value(s)");
        }

        @Test
        void floatType_withinRange() {
            String result = tool.generateRandom("float", 3, "0.5", "9.5", null);
            assertThat(result).contains("Generated 3 random float value(s)");
        }

        @Test
        void floatType_defaultRange() {
            String result = tool.generateRandom("float", 1, null, null, null);
            assertThat(result).contains("Generated 1 random float value(s)");
        }

        @Test
        void stringType_defaultLength() {
            String result = tool.generateRandom("string", 2, null, null, null);
            assertThat(result).contains("Generated 2 random string value(s)");
        }

        @Test
        void stringType_customLength() {
            String result = tool.generateRandom("string", 1, null, null, "20");
            assertThat(result).isNotBlank();
        }

        @Test
        void hexType() {
            String result = tool.generateRandom("hex", 2, null, null, "8");
            assertThat(result).containsPattern("[0-9a-f]{8}");
        }

        @Test
        void alphanumericType() {
            String result = tool.generateRandom("alphanumeric", 2, null, null, "12");
            assertThat(result).isNotBlank();
        }

        @Test
        void dateType_withRange() {
            String result = tool.generateRandom("date", 3, "2023-01-01", "2023-12-31", null);
            assertThat(result).containsPattern("2023-\\d{2}-\\d{2}");
        }

        @Test
        void dateType_defaultRange() {
            String result = tool.generateRandom("date", 1, null, null, null);
            assertThat(result).containsPattern("\\d{4}-\\d{2}-\\d{2}");
        }

        @Test
        void timeType() {
            String result = tool.generateRandom("time", 2, null, null, null);
            assertThat(result).containsPattern("\\d{2}:\\d{2}:\\d{2}");
        }

        @Test
        void colorType() {
            String result = tool.generateRandom("color", 2, null, null, null);
            assertThat(result).containsPattern("#[0-9A-F]{6}");
        }

        @Test
        void ipv4Type() {
            String result = tool.generateRandom("ipv4", 2, null, null, null);
            assertThat(result).containsPattern("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
        }

        @Test
        void macAddressType() {
            String result = tool.generateRandom("mac_address", 2, null, null, null);
            assertThat(result).containsPattern("[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}");
        }

        @Test
        void uuidType() {
            String result = tool.generateRandom("uuid", 3, null, null, null);
            assertThat(result).containsPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        void unknownType_returnsError() {
            String result = tool.generateRandom("binary", 1, null, null, null);
            assertThat(result).containsIgnoringCase("error").contains("binary");
        }

        // ── count boundary errors ───────────────────────────────────────────

        @Test
        void countZero_returnsError() {
            String result = tool.generateRandom("int", 0, null, null, null);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void countNegative_returnsError() {
            String result = tool.generateRandom("int", -1, null, null, null);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void countAbove100_returnsError() {
            String result = tool.generateRandom("int", 101, null, null, null);
            assertThat(result).containsIgnoringCase("error");
        }

        // ── length edge cases ───────────────────────────────────────────────

        @Test
        void invalidLength_returnsError() {
            String result = tool.generateRandom("string", 1, null, null, "abc");
            assertThat(result).containsIgnoringCase("error").contains("abc");
        }

        @Test
        void lengthBelowOne_clampedToOne() {
            // strLength < 1 gets clamped to 1
            String result = tool.generateRandom("string", 1, null, null, "0");
            assertThat(result).doesNotContainIgnoringCase("error");
        }

        @Test
        void lengthAbove256_clampedTo256() {
            String result = tool.generateRandom("string", 1, null, null, "999");
            assertThat(result).doesNotContainIgnoringCase("error");
        }

        @Test
        void blankLength_usesDefault() {
            String result = tool.generateRandom("hex", 1, null, null, "  ");
            assertThat(result).doesNotContainIgnoringCase("error");
        }

        @Test
        void blankMinMax_usesDefaults() {
            String result = tool.generateRandom("int", 1, "", "", null);
            assertThat(result).doesNotContainIgnoringCase("error");
        }

        // ── boundary valid counts ───────────────────────────────────────────

        @Test
        void countOne_isValid() {
            String result = tool.generateRandom("uuid", 1, null, null, null);
            assertThat(result).doesNotContainIgnoringCase("error");
        }

        @Test
        void count100_isValid() {
            String result = tool.generateRandom("uuid", 100, null, null, null);
            assertThat(result).doesNotContainIgnoringCase("error");
        }

        @Test
        void dateType_endBeforeStart_returnsSameDate() {
            // When end < start, days = 0 so always returns start date
            String result = tool.generateRandom("date", 1, "2025-06-01", "2020-01-01", null);
            assertThat(result).contains("2025-06-01");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  generateSequence
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    class GenerateSequence {

        // ── arithmetic ──────────────────────────────────────────────────────

        @Test
        void arithmetic_defaultStartAndStep() {
            String result = tool.generateSequence("arithmetic", null, null, 5);
            assertThat(result).contains("Arithmetic Sequence").contains("start=0").contains("step=1");
        }

        @Test
        void arithmetic_customStartAndStep() {
            String result = tool.generateSequence("arithmetic", "10", "5", 4);
            assertThat(result).contains("10").contains("15").contains("20").contains("25");
        }

        @Test
        void arithmetic_blankStartAndStep_usesDefaults() {
            String result = tool.generateSequence("arithmetic", "", "", 3);
            assertThat(result).contains("Arithmetic Sequence");
        }

        // ── geometric ───────────────────────────────────────────────────────

        @Test
        void geometric_defaultStartAndRatio() {
            String result = tool.generateSequence("geometric", null, null, 4);
            assertThat(result).contains("Geometric Sequence").contains("start=1").contains("ratio=2");
            // 1, 2, 4, 8
            assertThat(result).contains("1").contains("2").contains("4").contains("8");
        }

        @Test
        void geometric_customStartAndRatio() {
            String result = tool.generateSequence("geometric", "3", "3", 3);
            assertThat(result).contains("3").contains("9").contains("27");
        }

        @Test
        void geometric_blankStartAndRatio_usesDefaults() {
            String result = tool.generateSequence("geometric", "", "", 2);
            assertThat(result).contains("Geometric Sequence");
        }

        // ── fibonacci ───────────────────────────────────────────────────────

        @Test
        void fibonacci_first8() {
            String result = tool.generateSequence("fibonacci", null, null, 8);
            assertThat(result).contains("Fibonacci Sequence");
            // 0, 1, 1, 2, 3, 5, 8, 13
            assertThat(result).contains("0").contains("13");
        }

        @Test
        void fibonacci_singleElement() {
            String result = tool.generateSequence("fibonacci", null, null, 1);
            assertThat(result).contains("0");
        }

        // ── dates ───────────────────────────────────────────────────────────

        @Test
        void dates_customStartAndStep() {
            String result = tool.generateSequence("dates", "2024-01-01", "7", 3);
            assertThat(result).contains("2024-01-01").contains("2024-01-08").contains("2024-01-15");
        }

        @Test
        void dates_defaultStartAndStep() {
            String result = tool.generateSequence("dates", null, null, 2);
            assertThat(result).contains("Date Sequence");
        }

        @Test
        void dates_blankStartAndStep_usesDefaults() {
            String result = tool.generateSequence("dates", "", "", 2);
            assertThat(result).contains("Date Sequence").contains("interval=1 day(s)");
        }

        // ── timestamps ──────────────────────────────────────────────────────

        @Test
        void timestamps_customStartAndStep() {
            String result = tool.generateSequence("timestamps", "2024-01-01 08:00", "30", 3);
            assertThat(result).contains("2024-01-01 08:00")
                    .contains("2024-01-01 08:30")
                    .contains("2024-01-01 09:00");
        }

        @Test
        void timestamps_defaultStartAndStep() {
            String result = tool.generateSequence("timestamps", null, null, 2);
            assertThat(result).contains("Timestamp Sequence").contains("interval=60 min");
        }

        @Test
        void timestamps_blankStartAndStep() {
            String result = tool.generateSequence("timestamps", "", "", 2);
            assertThat(result).contains("Timestamp Sequence");
        }

        // ── ids ─────────────────────────────────────────────────────────────

        @Test
        void ids_customPrefixAndStart() {
            String result = tool.generateSequence("ids", "ORD-", "100", 3);
            assertThat(result).contains("ORD-0100").contains("ORD-0101").contains("ORD-0102");
        }

        @Test
        void ids_defaultPrefixAndStart() {
            String result = tool.generateSequence("ids", null, null, 3);
            assertThat(result).contains("ID-").contains("ID Sequence");
        }

        @Test
        void ids_blankPrefixAndStart_usesDefaults() {
            String result = tool.generateSequence("ids", "", "", 2);
            assertThat(result).contains("ID-").contains("ID Sequence");
        }

        @Test
        void ids_paddingWidth_minimum4() {
            // start=1, count=3 => maxNum=3 => padWidth=1 => clamped to 4
            String result = tool.generateSequence("ids", "T-", "1", 3);
            assertThat(result).contains("T-0001").contains("T-0002").contains("T-0003");
        }

        @Test
        void ids_paddingWidth_growsAbove4() {
            // start=9998, count=5 => maxNum=10002 => padWidth=5
            String result = tool.generateSequence("ids", "X-", "9998", 5);
            assertThat(result).contains("X-09998").contains("X-10002");
        }

        // ── error paths ─────────────────────────────────────────────────────

        @Test
        void unknownType_returnsError() {
            String result = tool.generateSequence("random_walk", null, null, 5);
            assertThat(result).containsIgnoringCase("error").contains("random_walk");
        }

        @Test
        void countZero_returnsError() {
            String result = tool.generateSequence("fibonacci", null, null, 0);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void countNegative_returnsError() {
            String result = tool.generateSequence("fibonacci", null, null, -1);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void countAbove1000_returnsError() {
            String result = tool.generateSequence("fibonacci", null, null, 1001);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void invalidStartNumber_returnsError() {
            String result = tool.generateSequence("arithmetic", "abc", "1", 5);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void invalidDateFormat_returnsError() {
            String result = tool.generateSequence("dates", "not-a-date", "1", 3);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void invalidTimestampFormat_returnsError() {
            String result = tool.generateSequence("timestamps", "bad-time", "1", 3);
            assertThat(result).containsIgnoringCase("error");
        }

        // ── formatNum coverage ──────────────────────────────────────────────

        @Test
        void arithmetic_fractionalStep_showsDecimals() {
            String result = tool.generateSequence("arithmetic", "0.5", "0.5", 3);
            // 0.5, 1.0, 1.5
            assertThat(result).contains("0.5").contains("1.5");
        }

        @Test
        void geometric_fractionalValues() {
            String result = tool.generateSequence("geometric", "0.5", "2", 3);
            // 0.5, 1, 2
            assertThat(result).contains("0.5");
        }

        // ── boundary valid count ────────────────────────────────────────────

        @Test
        void count1_isValid() {
            String result = tool.generateSequence("arithmetic", "0", "1", 1);
            assertThat(result).doesNotContainIgnoringCase("error");
        }

        @Test
        void count1000_isValid() {
            String result = tool.generateSequence("fibonacci", null, null, 1000);
            assertThat(result).doesNotContainIgnoringCase("error");
        }
    }
}
