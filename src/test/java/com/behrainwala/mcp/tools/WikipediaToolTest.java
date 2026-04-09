package com.behrainwala.mcp.tools;

import com.behrainwala.mcp.config.McpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WikipediaToolTest {

    private WikipediaTool tool;

    @BeforeEach
    void setUp() {
        tool = new WikipediaTool(new McpProperties());
    }

    @Test
    void lookup_blankTopic_returnsError() {
        String result = tool.lookup("");
        assertThat(result).containsIgnoringCase("error").containsIgnoringCase("required");
    }

    @Test
    void lookup_nullTopic_returnsError() {
        String result = tool.lookup(null);
        assertThat(result).containsIgnoringCase("error").containsIgnoringCase("required");
    }
}
