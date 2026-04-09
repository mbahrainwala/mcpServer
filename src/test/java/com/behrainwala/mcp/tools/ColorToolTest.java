package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ColorToolTest {

    private ColorTool tool;

    @BeforeEach
    void setUp() {
        tool = new ColorTool();
    }

    // ── colorConvert ─────────────────────────────────────────────────────────

    @Test
    void colorConvert_hexToAll() {
        String result = tool.colorConvert("#FF0000", "all");
        assertThat(result).containsIgnoringCase("rgb").containsIgnoringCase("hsl");
    }

    @Test
    void colorConvert_hexToRgb() {
        String result = tool.colorConvert("#FF0000", "rgb");
        assertThat(result).contains("255").contains("0");
    }

    @Test
    void colorConvert_rgbToHex() {
        String result = tool.colorConvert("rgb(255,0,0)", "hex");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("FF0000"), s -> assertThat(s).containsIgnoringCase("ff0000"));
    }

    @Test
    void colorConvert_namedColor_white() {
        String result = tool.colorConvert("white", "hex");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("FFFFFF"), s -> assertThat(s).containsIgnoringCase("ffffff"));
    }

    @Test
    void colorConvert_namedColor_black() {
        String result = tool.colorConvert("black", "rgb");
        assertThat(result).contains("0");
    }

    @Test
    void colorConvert_shortHex() {
        String result = tool.colorConvert("#F00", "rgb");
        assertThat(result).contains("255").contains("0");
    }

    // ── colorContrast ────────────────────────────────────────────────────────

    @Test
    void colorContrast_blackOnWhite_highContrast() {
        String result = tool.colorContrast("#000000", "#FFFFFF");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("21"), s -> assertThat(s).containsIgnoringCase("WCAG"));
    }

    @Test
    void colorContrast_sameColor_minimalContrast() {
        String result = tool.colorContrast("#AABBCC", "#AABBCC");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("1.00"), s -> assertThat(s).contains("1:1"));
    }

    // ── colorPalette ─────────────────────────────────────────────────────────

    @Test
    void colorPalette_complementary() {
        String result = tool.colorPalette("#FF0000", "complementary");
        assertThat(result).containsIgnoringCase("complementary");
    }

    @Test
    void colorPalette_triadic() {
        String result = tool.colorPalette("#FF0000", "triadic");
        assertThat(result).containsIgnoringCase("triadic");
    }

    // ── colorBlend ───────────────────────────────────────────────────────────

    @Test
    void colorBlend_redAndBlue() {
        String result = tool.colorBlend("#FF0000", "#0000FF", "0.5");
        assertThat(result).isNotBlank().doesNotContainIgnoringCase("error");
    }
}
