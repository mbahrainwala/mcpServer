package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AstronomyToolTest {

    private AstronomyTool tool;

    @BeforeEach
    void setUp() {
        tool = new AstronomyTool();
    }

    // ── planetInfo ───────────────────────────────────────────────────────────

    @Test
    void planetInfo_earth() {
        String result = tool.planetInfo("earth");
        assertThat(result).containsIgnoringCase("Earth").containsIgnoringCase("Terrestrial");
    }

    @Test
    void planetInfo_mars() {
        String result = tool.planetInfo("Mars");
        assertThat(result).containsIgnoringCase("Mars");
    }

    @Test
    void planetInfo_caseInsensitive() {
        String result = tool.planetInfo("JUPITER");
        assertThat(result).containsIgnoringCase("Jupiter").containsIgnoringCase("Gas Giant");
    }

    @Test
    void planetInfo_unknown_returnsError() {
        String result = tool.planetInfo("Xenu");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("error"), s -> assertThat(s).containsIgnoringCase("unknown"));
    }

    @Test
    void planetInfo_blank_returnsError() {
        String result = tool.planetInfo("");
        assertThat(result).containsIgnoringCase("error");
    }

    // ── moonPhase ────────────────────────────────────────────────────────────

    @Test
    void moonPhase_validDate() {
        String result = tool.moonPhase("2024-01-01");
        assertThat(result).isNotBlank().doesNotContainIgnoringCase("error");
    }

    @Test
    void moonPhase_invalidDate_returnsError() {
        String result = tool.moonPhase("not-a-date");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("error"), s -> assertThat(s).containsIgnoringCase("invalid"));
    }
}
