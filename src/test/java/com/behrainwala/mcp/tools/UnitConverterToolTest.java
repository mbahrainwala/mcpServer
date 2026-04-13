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
        assertThat(result).contains("Unit Conversion").contains("0.621");
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

    @Test
    void convert_mmToMeters() {
        String result = tool.convert(1000.0, "mm", "m");
        assertThat(result).contains("1");
    }

    @Test
    void convert_yardsToMeters() {
        String result = tool.convert(1.0, "yd", "m");
        assertThat(result).contains("0.9144");
    }

    @Test
    void convert_nauticalMileToKm() {
        String result = tool.convert(1.0, "nmi", "km");
        assertThat(result).contains("1.85");
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

    @Test
    void convert_tonToKg() {
        String result = tool.convert(1.0, "ton", "kg");
        assertThat(result).contains("907");
    }

    @Test
    void convert_tonneToKg() {
        String result = tool.convert(1.0, "tonne", "kg");
        assertThat(result).contains("1000");
    }

    @Test
    void convert_stoneToKg() {
        String result = tool.convert(1.0, "st", "kg");
        assertThat(result).contains("6.35");
    }

    @Test
    void convert_mgToG() {
        String result = tool.convert(1000.0, "mg", "g");
        assertThat(result).contains("1");
    }

    // ── Temperature ──────────────────────────────────────────────────────────

    @Test
    void convert_celsiusToFahrenheit() {
        String result = tool.convert(100.0, "celsius", "fahrenheit");
        assertThat(result).contains("212.00").contains("Temperature Conversion");
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

    @Test
    void convert_kelvinToCelsius() {
        String result = tool.convert(273.15, "kelvin", "celsius");
        assertThat(result).contains("0.00");
    }

    @Test
    void convert_fahrenheitToKelvin() {
        String result = tool.convert(32.0, "fahrenheit", "kelvin");
        assertThat(result).contains("273.15");
    }

    @Test
    void convert_kelvinToFahrenheit() {
        String result = tool.convert(273.15, "kelvin", "fahrenheit");
        assertThat(result).contains("32.00");
    }

    @Test
    void convert_sameTemperatureUnit() {
        String result = tool.convert(100.0, "celsius", "celsius");
        assertThat(result).contains("same unit");
    }

    @Test
    void convert_tempShortForm_cToF() {
        String result = tool.convert(100.0, "c", "f");
        assertThat(result).contains("212.00");
    }

    @Test
    void convert_tempDegreeSymbol() {
        String result = tool.convert(100.0, "\u00b0c", "\u00b0f");
        assertThat(result).contains("212.00");
    }

    // ── Volume ───────────────────────────────────────────────────────────────

    @Test
    void convert_litersToGallons() {
        String result = tool.convert(3.78541, "liter", "gallon");
        assertThat(result).contains("1");
    }

    @Test
    void convert_mlToL() {
        String result = tool.convert(1000.0, "ml", "l");
        assertThat(result).contains("1");
    }

    @Test
    void convert_cupsToMl() {
        String result = tool.convert(1.0, "cup", "ml");
        assertThat(result).contains("236");
    }

    @Test
    void convert_tbspToTsp() {
        String result = tool.convert(1.0, "tbsp", "tsp");
        assertThat(result).contains("3");
    }

    @Test
    void convert_quartsToLiters() {
        String result = tool.convert(1.0, "qt", "l");
        assertThat(result).contains("0.946");
    }

    @Test
    void convert_pintsToMl() {
        String result = tool.convert(1.0, "pt", "ml");
        assertThat(result).contains("473");
    }

    @Test
    void convert_flOzToMl() {
        String result = tool.convert(1.0, "floz", "ml");
        assertThat(result).contains("29.5");
    }

    // ── Speed ────────────────────────────────────────────────────────────────

    @Test
    void convert_kphToMph() {
        String result = tool.convert(100.0, "km/h", "mph");
        assertThat(result).contains("62.1");
    }

    @Test
    void convert_knotsToKph() {
        String result = tool.convert(1.0, "knot", "km/h");
        assertThat(result).contains("1.85");
    }

    @Test
    void convert_mpsToKph() {
        String result = tool.convert(1.0, "m/s", "km/h");
        assertThat(result).contains("3.5");
    }

    @Test
    void convert_fpsToMps() {
        String result = tool.convert(1.0, "ft/s", "m/s");
        assertThat(result).contains("1");
    }

    // ── Area ─────────────────────────────────────────────────────────────────

    @Test
    void convert_acresToSqM() {
        String result = tool.convert(1.0, "acre", "sq m");
        assertThat(result).contains("4046");
    }

    @Test
    void convert_hectareToAcres() {
        String result = tool.convert(1.0, "hectare", "acre");
        assertThat(result).contains("2.47");
    }

    @Test
    void convert_sqKmToSqMi() {
        String result = tool.convert(1.0, "km2", "sq mi");
        assertThat(result).isNotBlank();
    }

    // ── Time ─────────────────────────────────────────────────────────────────

    @Test
    void convert_hoursToMinutes() {
        String result = tool.convert(2.0, "hour", "minute");
        assertThat(result).contains("120");
    }

    @Test
    void convert_daysToHours() {
        String result = tool.convert(1.0, "day", "hour");
        assertThat(result).contains("24");
    }

    @Test
    void convert_weeksToDay() {
        String result = tool.convert(1.0, "week", "day");
        assertThat(result).contains("7");
    }

    @Test
    void convert_msToSeconds() {
        String result = tool.convert(1000.0, "ms", "s");
        assertThat(result).contains("1");
    }

    @Test
    void convert_yearsToDays() {
        String result = tool.convert(1.0, "year", "day");
        assertThat(result).contains("365");
    }

    @Test
    void convert_monthsToWeeks() {
        String result = tool.convert(1.0, "month", "week");
        assertThat(result).isNotBlank();
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    @Test
    void convert_gbToMb() {
        String result = tool.convert(1.0, "gb", "mb");
        assertThat(result).contains("1024");
    }

    @Test
    void convert_tbToGb() {
        String result = tool.convert(1.0, "tb", "gb");
        assertThat(result).contains("1024");
    }

    @Test
    void convert_kbToBytes() {
        String result = tool.convert(1.0, "kb", "b");
        assertThat(result).contains("1024");
    }

    @Test
    void convert_pbToTb() {
        String result = tool.convert(1.0, "pb", "tb");
        assertThat(result).contains("1024");
    }

    // ── Error handling ───────────────────────────────────────────────────────

    @Test
    void convert_incompatibleUnits_returnsError() {
        String result = tool.convert(1.0, "km", "kelvin");
        assertThat(result).contains("Error:").contains("Cannot convert");
    }

    @Test
    void convert_unknownUnit_returnsError() {
        String result = tool.convert(1.0, "parsec", "km");
        assertThat(result).contains("Error:").contains("Cannot convert");
    }

    @Test
    void convert_tempToNonTemp_returnsError() {
        // celsius is a temp unit but km is not
        String result = tool.convert(1.0, "km", "celsius");
        assertThat(result).contains("Error:");
    }
}
