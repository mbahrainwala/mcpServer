package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnitConverterToolTest {

    private UnitConverterTool tool;

    @BeforeEach
    void setUp() {
        tool = new UnitConverterTool();
    }

    // ── Length ───────────────────────────────────────────────────────────────

    @Test
    void convert_kmToMiles() {
        String result = tool.convert(1.0, "km", "miles");
        assertThat(result).contains("0.621");
    }

    @Test
    void convert_metersToFeet() {
        String result = tool.convert(1.0, "m", "ft");
        assertThat(result).contains("3.28");
    }

    @Test
    void convert_inchesToCm() {
        String result = tool.convert(1.0, "in", "cm");
        assertThat(result).contains("2.54");
    }

    // ── Weight ───────────────────────────────────────────────────────────────

    @Test
    void convert_kgToLbs() {
        String result = tool.convert(1.0, "kg", "lb");
        assertThat(result).contains("2.20");
    }

    @Test
    void convert_ozToGrams() {
        String result = tool.convert(1.0, "oz", "g");
        assertThat(result).contains("28.3");
    }

    // ── Temperature ──────────────────────────────────────────────────────────

    @Test
    void convert_celsiusToFahrenheit() {
        String result = tool.convert(100.0, "celsius", "fahrenheit");
        assertThat(result).contains("212.00");
    }

    @Test
    void convert_fahrenheitToCelsius() {
        String result = tool.convert(32.0, "fahrenheit", "celsius");
        assertThat(result).contains("0.00");
    }

    @Test
    void convert_celsiusToKelvin() {
        String result = tool.convert(0.0, "celsius", "kelvin");
        assertThat(result).contains("273.15");
    }

    // ── Volume ───────────────────────────────────────────────────────────────

    @Test
    void convert_litersToGallons() {
        String result = tool.convert(3.78541, "liter", "gallon");
        assertThat(result).contains("1");
    }

    // ── Speed ────────────────────────────────────────────────────────────────

    @Test
    void convert_kphToMph() {
        String result = tool.convert(100.0, "km/h", "mph");
        assertThat(result).contains("62.1");
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    @Test
    void convert_gbToMb() {
        String result = tool.convert(1.0, "gb", "mb");
        assertThat(result).contains("1024");
    }

    // ── Time ─────────────────────────────────────────────────────────────────

    @Test
    void convert_hoursToMinutes() {
        String result = tool.convert(2.0, "hour", "minute");
        assertThat(result).contains("120");
    }

    // ── Error handling ───────────────────────────────────────────────────────

    @Test
    void convert_incompatibleUnits_returnsError() {
        String result = tool.convert(1.0, "km", "kelvin");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("error"), s -> assertThat(s).containsIgnoringCase("cannot"));
    }

    @Test
    void convert_unknownUnit_returnsError() {
        String result = tool.convert(1.0, "parsec", "km");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("error"), s -> assertThat(s).containsIgnoringCase("not recognized"));
    }
}
