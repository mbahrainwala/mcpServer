package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelFormulaToolTest {

    private ExcelFormulaTool tool;

    @BeforeEach
    void setUp() {
        tool = new ExcelFormulaTool();
    }

    @Test
    void functionLookup_vlookup() {
        String result = tool.functionLookup("VLOOKUP");
        assertThat(result).containsIgnoringCase("VLOOKUP").containsIgnoringCase("lookup");
    }

    @Test
    void functionLookup_caseInsensitive() {
        String result = tool.functionLookup("vlookup");
        assertThat(result).containsIgnoringCase("VLOOKUP");
    }

    @Test
    void functionLookup_sumif() {
        String result = tool.functionLookup("SUMIF");
        assertThat(result).containsIgnoringCase("SUMIF").containsIgnoringCase("criteria");
    }

    @Test
    void functionLookup_xlookup() {
        String result = tool.functionLookup("XLOOKUP");
        assertThat(result).containsIgnoringCase("XLOOKUP");
    }

    @Test
    void functionLookup_index() {
        String result = tool.functionLookup("INDEX");
        assertThat(result).containsIgnoringCase("INDEX");
    }

    @Test
    void functionLookup_unknown_returns_noMatch() {
        String result = tool.functionLookup("NOTAFUNCTION");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("not found"), s -> assertThat(s).containsIgnoringCase("no function"))
                ;
    }

    @Test
    void functionLookup_categorySearch() {
        String result = tool.functionLookup("math");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("SUMIF"), s -> assertThat(s).containsIgnoringCase("ROUND"));
    }
}
