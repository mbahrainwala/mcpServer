package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelDataToolTest {

    private ExcelDataTool tool;

    @BeforeEach
    void setUp() {
        tool = new ExcelDataTool();
    }

    @Test
    void analyzeData_numericColumn() {
        String csv = "Name,Score\nAlice,95\nBob,82\nCarol,78";
        String result = tool.analyzeData(csv);
        assertThat(result).containsIgnoringCase("Score").containsIgnoringCase("Numeric");
    }

    @Test
    void analyzeData_textColumn() {
        String csv = "Name,Score\nAlice,95\nBob,82";
        String result = tool.analyzeData(csv);
        assertThat(result).containsIgnoringCase("Name");
    }

    @Test
    void analyzeData_rowColumnCount() {
        String csv = "A,B,C\n1,2,3\n4,5,6";
        String result = tool.analyzeData(csv);
        assertThat(result).contains("2").contains("3"); // 2 rows, 3 cols
    }

    @Test
    void analyzeData_statistics() {
        String csv = "Value\n10\n20\n30";
        String result = tool.analyzeData(csv);
        assertThat(result).containsIgnoringCase("Min").containsIgnoringCase("Max")
                .containsIgnoringCase("Mean");
    }

    @Test
    void analyzeData_tooFewRows_returnsError() {
        String result = tool.analyzeData("Header");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("error"), s -> assertThat(s).containsIgnoringCase("need"));
    }
}
