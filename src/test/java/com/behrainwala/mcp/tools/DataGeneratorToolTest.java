package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataGeneratorToolTest {

    private DataGeneratorTool tool;

    @BeforeEach
    void setUp() {
        tool = new DataGeneratorTool();
    }

    // ── generateLoremIpsum ───────────────────────────────────────────────────

    @Test
    void generateLoremIpsum_sentences() {
        String result = tool.generateLoremIpsum("sentences", 3);
        assertThat(result).isNotBlank();
    }

    @Test
    void generateLoremIpsum_words() {
        String result = tool.generateLoremIpsum("words", 10);
        assertThat(result).isNotBlank();
    }

    @Test
    void generateLoremIpsum_paragraphs() {
        String result = tool.generateLoremIpsum("paragraphs", 2);
        assertThat(result).isNotBlank();
    }

    @Test
    void generateLoremIpsum_unknownType_returnsError() {
        String result = tool.generateLoremIpsum("haiku", 3);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("error"), s -> assertThat(s).containsIgnoringCase("invalid"));
    }

    @Test
    void generateLoremIpsum_zeroCount_returnsError() {
        String result = tool.generateLoremIpsum("words", 0);
        assertThat(result).containsIgnoringCase("error");
    }

    // ── generateFakeData ─────────────────────────────────────────────────────

    @Test
    void generateFakeData_nameAndEmail() {
        String result = tool.generateFakeData(3, "name,email", "text");
        assertThat(result).isNotBlank();
    }

    @Test
    void generateFakeData_csv() {
        String result = tool.generateFakeData(2, "name,email", "csv");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("name"), s -> assertThat(s).containsIgnoringCase("email"));
    }

    @Test
    void generateFakeData_json() {
        String result = tool.generateFakeData(2, "name", "json");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("{"), s -> assertThat(s).containsIgnoringCase("name"));
    }

    @Test
    void generateFakeData_countOutOfRange_returnsError() {
        String result = tool.generateFakeData(0, "name", "text");
        assertThat(result).containsIgnoringCase("error");
    }

    // ── generateRandom ───────────────────────────────────────────────────────

    @Test
    void generateRandom_integers() {
        String result = tool.generateRandom("int", 5, "1", "100", null);
        assertThat(result).isNotBlank();
    }

    @Test
    void generateRandom_uuid() {
        String result = tool.generateRandom("uuid", 3, null, null, null);
        assertThat(result).isNotBlank();
    }

    @Test
    void generateRandom_hex() {
        String result = tool.generateRandom("hex", 2, null, null, "8");
        assertThat(result).matches("(?s).*[0-9a-fA-F]{8}.*");
    }

    @Test
    void generateRandom_countOutOfRange_returnsError() {
        String result = tool.generateRandom("int", 0, null, null, null);
        assertThat(result).containsIgnoringCase("error");
    }

    // ── generateSequence ─────────────────────────────────────────────────────

    @Test
    void generateSequence_fibonacci() {
        String result = tool.generateSequence("fibonacci", null, null, 8);
        // First 8 fibonacci: 0, 1, 1, 2, 3, 5, 8, 13
        assertThat(result).contains("0").contains("1").contains("13");
    }

    @Test
    void generateSequence_arithmetic() {
        String result = tool.generateSequence("arithmetic", "1", "2", 5);
        // 1, 3, 5, 7, 9
        assertThat(result).contains("1").contains("9");
    }
}
