package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthToolTest {

    private HealthTool tool;

    @BeforeEach
    void setUp() {
        tool = new HealthTool();
    }

    // ── healthBmi ────────────────────────────────────────────────────────────

    @Test
    void healthBmi_normalWeight() {
        // 70 kg, 175 cm → BMI = 70 / (1.75^2) ≈ 22.9
        String result = tool.healthBmi(70, 175);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("Normal weight"), s -> assertThat(s).contains("22"));
    }

    @Test
    void healthBmi_underweight() {
        // 45 kg, 175 cm → BMI ≈ 14.7
        String result = tool.healthBmi(45, 175);
        assertThat(result).containsIgnoringCase("Underweight");
    }

    @Test
    void healthBmi_overweight() {
        // 90 kg, 170 cm → BMI ≈ 31.1
        String result = tool.healthBmi(90, 170);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("Overweight"), s -> assertThat(s).containsIgnoringCase("Obese"));
    }

    @Test
    void healthBmi_includesDisclaimerAndRange() {
        String result = tool.healthBmi(70, 175);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("Disclaimer"), s -> assertThat(s).containsIgnoringCase("⚠"));
        assertThat(result).containsIgnoringCase("Healthy range");
    }

    @Test
    void healthBmi_zeroWeight_returnsError() {
        String result = tool.healthBmi(0, 175);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void healthBmi_zeroHeight_returnsError() {
        String result = tool.healthBmi(70, 0);
        assertThat(result).containsIgnoringCase("error");
    }
}
