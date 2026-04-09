package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChemistryToolTest {

    private ChemistryTool tool;

    @BeforeEach
    void setUp() {
        tool = new ChemistryTool();
    }

    @Test
    void elementLookup_bySymbol_iron() {
        String result = tool.elementLookup("Fe");
        assertThat(result).containsIgnoringCase("Iron").containsIgnoringCase("26");
    }

    @Test
    void elementLookup_byName_sodium() {
        String result = tool.elementLookup("Sodium");
        assertThat(result).containsIgnoringCase("Na").containsIgnoringCase("11");
    }

    @Test
    void elementLookup_hydrogen() {
        String result = tool.elementLookup("H");
        assertThat(result).containsIgnoringCase("Hydrogen").contains("1");
    }

    @Test
    void elementLookup_unknown_returnsError() {
        String result = tool.elementLookup("Xq");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("not found"), s -> assertThat(s).containsIgnoringCase("error"));
    }
}
