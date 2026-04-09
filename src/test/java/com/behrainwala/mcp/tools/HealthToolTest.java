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
        String result = tool.healthBmi(70, 175);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("Normal weight"), s -> assertThat(s).contains("22"));
    }

    @Test
    void healthBmi_underweight() {
        String result = tool.healthBmi(45, 175);
        assertThat(result).containsIgnoringCase("Underweight");
    }

    @Test
    void healthBmi_overweightClass1() {
        // BMI ~27 → Overweight
        String result = tool.healthBmi(82, 175);
        assertThat(result).containsIgnoringCase("Overweight");
    }

    @Test
    void healthBmi_obeseClass1() {
        // BMI ~31 → Obese Class I
        String result = tool.healthBmi(95, 175);
        assertThat(result).containsIgnoringCase("Obese");
    }

    @Test
    void healthBmi_obeseClass2() {
        // BMI ~36 → Obese Class II
        String result = tool.healthBmi(110, 175);
        assertThat(result).containsIgnoringCase("Obese");
    }

    @Test
    void healthBmi_obeseClass3() {
        // BMI ~45 → Obese Class III
        String result = tool.healthBmi(140, 175);
        assertThat(result).containsIgnoringCase("Obese");
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

    // ── healthBmr ────────────────────────────────────────────────────────────

    @Test
    void healthBmr_male_sedentary() {
        String result = tool.healthBmr(80, 180, 30, "male", "sedentary");
        assertThat(result).containsIgnoringCase("BMR").containsIgnoringCase("sedentary");
    }

    @Test
    void healthBmr_female_active() {
        String result = tool.healthBmr(60, 165, 25, "female", "active");
        assertThat(result).containsIgnoringCase("BMR");
    }

    @Test
    void healthBmr_male_light() {
        String result = tool.healthBmr(75, 175, 35, "male", "light");
        assertThat(result).containsIgnoringCase("TDEE");
    }

    @Test
    void healthBmr_female_moderate() {
        String result = tool.healthBmr(65, 160, 28, "female", "moderate");
        assertThat(result).containsIgnoringCase("TDEE");
    }

    @Test
    void healthBmr_veryActive() {
        String result = tool.healthBmr(70, 170, 22, "male", "very_active");
        assertThat(result).containsIgnoringCase("TDEE");
    }

    @Test
    void healthBmr_invalidSex_returnsError() {
        String result = tool.healthBmr(70, 175, 30, "other", "sedentary");
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void healthBmr_invalidActivity_returnsError() {
        String result = tool.healthBmr(70, 175, 30, "male", "superman");
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void healthBmr_zeroAge_returnsError() {
        String result = tool.healthBmr(70, 175, 0, "male", "sedentary");
        assertThat(result).containsIgnoringCase("error");
    }

    // ── healthMacroCalculator ────────────────────────────────────────────────

    @Test
    void healthMacro_maintenance() {
        String result = tool.healthMacroCalculator(2000, "maintenance");
        assertThat(result).containsIgnoringCase("Protein").containsIgnoringCase("Carbohydrates");
    }

    @Test
    void healthMacro_weightLoss() {
        String result = tool.healthMacroCalculator(1800, "weight_loss");
        assertThat(result).containsIgnoringCase("Protein");
    }

    @Test
    void healthMacro_muscleGain() {
        String result = tool.healthMacroCalculator(2500, "muscle_gain");
        assertThat(result).containsIgnoringCase("Protein");
    }

    @Test
    void healthMacro_keto() {
        String result = tool.healthMacroCalculator(2000, "keto");
        assertThat(result).containsIgnoringCase("Fat");
    }

    @Test
    void healthMacro_balanced() {
        String result = tool.healthMacroCalculator(2000, "balanced");
        assertThat(result).containsIgnoringCase("Protein");
    }

    @Test
    void healthMacro_invalidGoal_returnsError() {
        String result = tool.healthMacroCalculator(2000, "paleo");
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void healthMacro_zeroCalories_returnsError() {
        String result = tool.healthMacroCalculator(0, "maintenance");
        assertThat(result).containsIgnoringCase("error");
    }

    // ── healthHeartRateZones ─────────────────────────────────────────────────

    @Test
    void healthHeartRate_age30_rhr60() {
        String result = tool.healthHeartRateZones(30, 60);
        assertThat(result).containsIgnoringCase("Zone").contains("bpm");
    }

    @Test
    void healthHeartRate_defaultRhr_whenZero() {
        // resting HR = 0 → defaults to 70
        String result = tool.healthHeartRateZones(25, 0);
        assertThat(result).containsIgnoringCase("Zone");
    }

    @Test
    void healthHeartRate_invalidAge_returnsError() {
        String result = tool.healthHeartRateZones(0, 60);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void healthHeartRate_tooOldAge_returnsError() {
        String result = tool.healthHeartRateZones(150, 60);
        assertThat(result).containsIgnoringCase("error");
    }

    // ── healthHydration ──────────────────────────────────────────────────────

    @Test
    void healthHydration_sedentary_temperate() {
        String result = tool.healthHydration(70, "sedentary", "temperate");
        assertThat(result).containsIgnoringCase("litres");
    }

    @Test
    void healthHydration_active_hot() {
        String result = tool.healthHydration(80, "active", "hot");
        assertThat(result).containsIgnoringCase("litres");
    }

    @Test
    void healthHydration_moderate_cold() {
        String result = tool.healthHydration(65, "moderate", "cold");
        assertThat(result).containsIgnoringCase("litres");
    }

    @Test
    void healthHydration_veryActive() {
        String result = tool.healthHydration(90, "very_active", "temperate");
        assertThat(result).containsIgnoringCase("litres");
    }

    @Test
    void healthHydration_light_activity() {
        String result = tool.healthHydration(60, "light", "temperate");
        assertThat(result).containsIgnoringCase("litres");
    }

    @Test
    void healthHydration_invalidActivity_returnsError() {
        String result = tool.healthHydration(70, "turbo", "temperate");
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void healthHydration_invalidClimate_returnsError() {
        String result = tool.healthHydration(70, "sedentary", "tropical");
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void healthHydration_zeroWeight_returnsError() {
        String result = tool.healthHydration(0, "sedentary", "temperate");
        assertThat(result).containsIgnoringCase("error");
    }

    // ── healthBodyFatEstimate ────────────────────────────────────────────────

    @Test
    void healthBodyFat_male() {
        String result = tool.healthBodyFatEstimate("male", 85, 38, 175, 0);
        assertThat(result).containsIgnoringCase("Body Fat");
    }

    @Test
    void healthBodyFat_female() {
        String result = tool.healthBodyFatEstimate("female", 75, 34, 165, 95);
        assertThat(result).containsIgnoringCase("Body Fat");
    }

    @Test
    void healthBodyFat_invalidSex_returnsError() {
        String result = tool.healthBodyFatEstimate("robot", 80, 35, 175, 0);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void healthBodyFat_femaleWithoutHip_returnsError() {
        String result = tool.healthBodyFatEstimate("female", 75, 34, 165, 0);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void healthBodyFat_waistSmallerThanNeck_returnsError() {
        // waist=30, neck=40 → invalid
        String result = tool.healthBodyFatEstimate("male", 30, 40, 175, 0);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void healthBodyFat_zeroMeasurement_returnsError() {
        String result = tool.healthBodyFatEstimate("male", 0, 35, 175, 0);
        assertThat(result).containsIgnoringCase("error");
    }
}
