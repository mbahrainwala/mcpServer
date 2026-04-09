package com.behrainwala.mcp.tools;

import com.behrainwala.mcp.config.McpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DictionaryToolTest {

    private DictionaryTool tool;

    @BeforeEach
    void setUp() {
        tool = new DictionaryTool(new McpProperties());
    }

    @Test
    void define_blankWord_returnsError() {
        String result = tool.define("");
        assertThat(result).containsIgnoringCase("error").containsIgnoringCase("required");
    }

    @Test
    void define_nullWord_returnsError() {
        String result = tool.define(null);
        assertThat(result).containsIgnoringCase("error").containsIgnoringCase("required");
    }
}
