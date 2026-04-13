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
        // BMI = 70 / (1.75^2) = 22.86
        String result = tool.healthBmi(70, 175);
        assertThat(result)
                .contains("Normal weight")
                .contains("BMI Results")
                .contains("BMI Prime");
    }

    @Test
    void healthBmi_underweight() {
        String result = tool.healthBmi(45, 175);
        assertThat(result).contains("Underweight");
    }

    @Test
    void healthBmi_overweight() {
        // BMI ~27
        String result = tool.healthBmi(82, 175);
        assertThat(result).contains("Overweight");
    }

    @Test
    void healthBmi_obeseClass1() {
        // BMI ~31
        String result = tool.healthBmi(95, 175);
        assertThat(result).contains("Obese").contains("Class I");
    }

    @Test
    void healthBmi_obeseClass2() {
        // BMI ~36
        String result = tool.healthBmi(110, 175);
        assertThat(result).contains("Obese").contains("Class II");
    }

    @Test
    void healthBmi_obeseClass3() {
        // BMI ~45
        String result = tool.healthBmi(140, 175);
        assertThat(result).contains("Obese").contains("Class III");
    }

    @Test
    void healthBmi_containsHealthyRange() {
        String result = tool.healthBmi(70, 175);
        assertThat(result).contains("Healthy range:");
    }

    @Test
    void healthBmi_containsDisclaimer() {
        String result = tool.healthBmi(70, 175);
        assertThat(result).contains("Disclaimer");
    }

    @Test
    void healthBmi_zeroWeight_returnsError() {
        String result = tool.healthBmi(0, 175);
        assertThat(result).contains("Error:");
    }

    @Test
    void healthBmi_negativeWeight_returnsError() {
        String result = tool.healthBmi(-70, 175);
        assertThat(result).contains("Error:");
    }

    @Test
    void healthBmi_zeroHeight_returnsError() {
        String result = tool.healthBmi(70, 0);
        assertThat(result).contains("Error:");
    }

    @Test
    void healthBmi_negativeHeight_returnsError() {
        String result = tool.healthBmi(70, -175);
        assertThat(result).contains("Error:");
    }

    // ── healthBmr ────────────────────────────────────────────────────────────

    @Test
    void healthBmr_male_sedentary() {
        String result = tool.healthBmr(80, 180, 30, "male", "sedentary");
        assertThat(result)
                .contains("Mifflin-St Jeor")
                .contains("Harris-Benedict")
                .contains("TDEE")
                .contains("Weight loss")
                .contains("Maintenance")
                .contains("Weight gain");
    }

    @Test
    void healthBmr_female_active() {
        String result = tool.healthBmr(60, 165, 25, "female", "active");
        assertThat(result).contains("BMR");
    }

    @Test
    void healthBmr_male_light() {
        String result = tool.healthBmr(75, 175, 35, "male", "light");
        assertThat(result).contains("1.375");
    }

    @Test
    void healthBmr_female_moderate() {
        String result = tool.healthBmr(65, 160, 28, "female", "moderate");
        assertThat(result).contains("1.550");
    }

    @Test
    void healthBmr_veryActive() {
        String result = tool.healthBmr(70, 170, 22, "male", "very_active");
        assertThat(result).contains("1.900");
    }

    @Test
    void healthBmr_invalidSex_returnsError() {
        String result = tool.healthBmr(70, 175, 30, "other", "sedentary");
        assertThat(result).contains("Error: sex must be 'male' or 'female'.");
    }

    @Test
    void healthBmr_invalidActivity_returnsError() {
        String result = tool.healthBmr(70, 175, 30, "male", "superman");
        assertThat(result).contains("Error: activity_level must be one of:");
    }

    @Test
    void healthBmr_nullActivity_returnsError() {
        String result = tool.healthBmr(70, 175, 30, "male", null);
        assertThat(result).contains("Error:");
    }

    @Test
    void healthBmr_zeroWeight_returnsError() {
        String result = tool.healthBmr(0, 175, 30, "male", "sedentary");
        assertThat(result).contains("Error:");
    }

    @Test
    void healthBmr_zeroHeight_returnsError() {
        String result = tool.healthBmr(70, 0, 30, "male", "sedentary");
        assertThat(result).contains("Error:");
    }

    @Test
    void healthBmr_zeroAge_returnsError() {
        String result = tool.healthBmr(70, 175, 0, "male", "sedentary");
        assertThat(result).contains("Error:");
    }

    // ── healthMacroCalculator ────────────────────────────────────────────────

    @Test
    void healthMacro_maintenance() {
        String result = tool.healthMacroCalculator(2000, "maintenance");
        assertThat(result)
                .contains("Protein")
                .contains("Carbohydrates")
                .contains("Fat")
                .contains("30% / 40% / 30%");
    }

    @Test
    void healthMacro_weightLoss() {
        String result = tool.healthMacroCalculator(1800, "weight_loss");
        assertThat(result).contains("40% / 25% / 35%");
    }

    @Test
    void healthMacro_muscleGain() {
        String result = tool.healthMacroCalculator(2500, "muscle_gain");
        assertThat(result).contains("35% / 45% / 20%");
    }

    @Test
    void healthMacro_keto() {
        String result = tool.healthMacroCalculator(2000, "keto");
        assertThat(result).contains("20% / 5% / 75%");
    }

    @Test
    void healthMacro_balanced() {
        String result = tool.healthMacroCalculator(2000, "balanced");
        assertThat(result).contains("25% / 50% / 25%");
    }

    @Test
    void healthMacro_invalidGoal_returnsError() {
        String result = tool.healthMacroCalculator(2000, "paleo");
        assertThat(result).contains("Error: goal must be one of:");
    }

    @Test
    void healthMacro_nullGoal_returnsError() {
        String result = tool.healthMacroCalculator(2000, null);
        assertThat(result).contains("Error:");
    }

    @Test
    void healthMacro_zeroCalories_returnsError() {
        String result = tool.healthMacroCalculator(0, "maintenance");
        assertThat(result).contains("Error: calories must be a positive number.");
    }

    @Test
    void healthMacro_negativeCalories_returnsError() {
        String result = tool.healthMacroCalculator(-100, "maintenance");
        assertThat(result).contains("Error:");
    }

    @Test
    void healthMacro_containsDisclaimer() {
        String result = tool.healthMacroCalculator(2000, "maintenance");
        assertThat(result).contains("Disclaimer");
    }

    @Test
    void healthMacro_mathematicalCorrectness() {
        // 2000 cal maintenance: 30/40/30
        // Protein: 2000*0.3=600 cal / 4 = 150g
        // Carbs: 2000*0.4=800 cal / 4 = 200g
        // Fat: 2000*0.3=600 cal / 9 = 66.7g
        String result = tool.healthMacroCalculator(2000, "maintenance");
        assertThat(result).contains("150 g").contains("200 g");
    }

    // ── healthHeartRateZones ─────────────────────────────────────────────────

    @Test
    void healthHeartRate_age30_rhr60() {
        // maxHR = 220-30 = 190, hrReserve = 190-60 = 130
        // Zone 1: 0.5*130+60=125 to 0.6*130+60=138
        String result = tool.healthHeartRateZones(30, 60);
        assertThat(result)
                .contains("Zone 1")
                .contains("Zone 2")
                .contains("Zone 3")
                .contains("Zone 4")
                .contains("Zone 5")
                .contains("Max HR (220")
                .contains("190 bpm");
    }

    @Test
    void healthHeartRate_defaultRhr_whenZero() {
        String result = tool.healthHeartRateZones(25, 0);
        assertThat(result).contains("Resting HR           : 70 bpm");
    }

    @Test
    void healthHeartRate_negativeRhr_defaultsTo70() {
        String result = tool.healthHeartRateZones(25, -10);
        assertThat(result).contains("Resting HR           : 70 bpm");
    }

    @Test
    void healthHeartRate_invalidAge_zero_returnsError() {
        String result = tool.healthHeartRateZones(0, 60);
        assertThat(result).contains("Error: age must be between 1 and 120.");
    }

    @Test
    void healthHeartRate_invalidAge_tooOld_returnsError() {
        String result = tool.healthHeartRateZones(121, 60);
        assertThat(result).contains("Error: age must be between 1 and 120.");
    }

    @Test
    void healthHeartRate_invalidAge_negative_returnsError() {
        String result = tool.healthHeartRateZones(-5, 60);
        assertThat(result).contains("Error:");
    }

    @Test
    void healthHeartRate_containsDisclaimer() {
        String result = tool.healthHeartRateZones(30, 60);
        assertThat(result).contains("Disclaimer");
    }

    // ── healthHydration ──────────────────────────────────────────────────────

    @Test
    void healthHydration_sedentary_temperate() {
        String result = tool.healthHydration(70, "sedentary", "temperate");
        assertThat(result)
                .contains("litres")
                .contains("cups")
                .contains("Hourly target");
    }

    @Test
    void healthHydration_light_temperate() {
        String result = tool.healthHydration(60, "light", "temperate");
        assertThat(result).contains("1.10");
    }

    @Test
    void healthHydration_moderate_cold() {
        String result = tool.healthHydration(65, "moderate", "cold");
        assertThat(result).contains("1.10");
    }

    @Test
    void healthHydration_active_hot() {
        String result = tool.healthHydration(80, "active", "hot");
        assertThat(result).contains("1.40").contains("1.20");
    }

    @Test
    void healthHydration_veryActive() {
        String result = tool.healthHydration(90, "very_active", "temperate");
        assertThat(result).contains("1.60");
    }

    @Test
    void healthHydration_invalidActivity_returnsError() {
        String result = tool.healthHydration(70, "turbo", "temperate");
        assertThat(result).contains("Error: activity_level must be one of:");
    }

    @Test
    void healthHydration_nullActivity_returnsError() {
        String result = tool.healthHydration(70, null, "temperate");
        assertThat(result).contains("Error:");
    }

    @Test
    void healthHydration_invalidClimate_returnsError() {
        String result = tool.healthHydration(70, "sedentary", "tropical");
        assertThat(result).contains("Error: climate must be one of:");
    }

    @Test
    void healthHydration_nullClimate_returnsError() {
        String result = tool.healthHydration(70, "sedentary", null);
        assertThat(result).contains("Error:");
    }

    @Test
    void healthHydration_zeroWeight_returnsError() {
        String result = tool.healthHydration(0, "sedentary", "temperate");
        assertThat(result).contains("Error: weight_kg must be a positive number.");
    }

    @Test
    void healthHydration_negativeWeight_returnsError() {
        String result = tool.healthHydration(-10, "sedentary", "temperate");
        assertThat(result).contains("Error:");
    }

    @Test
    void healthHydration_containsDisclaimer() {
        String result = tool.healthHydration(70, "sedentary", "temperate");
        assertThat(result).contains("Disclaimer");
    }

    // ── healthBodyFatEstimate ────────────────────────────────────────────────

    @Test
    void healthBodyFat_male_averageCategory() {
        String result = tool.healthBodyFatEstimate("male", 85, 38, 175, 0);
        assertThat(result).contains("Body Fat");
    }

    @Test
    void healthBodyFat_male_essentialFat() {
        // Very low body fat (waist barely larger than neck)
        String result = tool.healthBodyFatEstimate("male", 70, 39, 175, 0);
        assertThat(result).contains("Body Fat");
    }

    @Test
    void healthBodyFat_male_athletes() {
        String result = tool.healthBodyFatEstimate("male", 78, 38, 180, 0);
        assertThat(result).contains("Body Fat");
    }

    @Test
    void healthBodyFat_male_fitness() {
        String result = tool.healthBodyFatEstimate("male", 82, 38, 178, 0);
        assertThat(result).contains("Body Fat");
    }

    @Test
    void healthBodyFat_male_obese() {
        String result = tool.healthBodyFatEstimate("male", 120, 38, 170, 0);
        assertThat(result).contains("Obese");
    }

    @Test
    void healthBodyFat_female() {
        String result = tool.healthBodyFatEstimate("female", 75, 34, 165, 95);
        assertThat(result).contains("Body Fat").contains("Hip");
    }

    @Test
    void healthBodyFat_female_athletes() {
        String result = tool.healthBodyFatEstimate("female", 65, 33, 170, 85);
        assertThat(result).contains("Body Fat");
    }

    @Test
    void healthBodyFat_female_essential() {
        // Very tight measurements
        String result = tool.healthBodyFatEstimate("female", 60, 33, 175, 80);
        assertThat(result).contains("Body Fat");
    }

    @Test
    void healthBodyFat_female_obese() {
        String result = tool.healthBodyFatEstimate("female", 100, 34, 155, 120);
        assertThat(result).contains("Body Fat");
    }

    @Test
    void healthBodyFat_invalidSex_returnsError() {
        String result = tool.healthBodyFatEstimate("robot", 80, 35, 175, 0);
        assertThat(result).contains("Error: sex must be 'male' or 'female'.");
    }

    @Test
    void healthBodyFat_nullSex_returnsError() {
        String result = tool.healthBodyFatEstimate(null, 80, 35, 175, 0);
        assertThat(result).contains("Error:");
    }

    @Test
    void healthBodyFat_femaleWithoutHip_returnsError() {
        String result = tool.healthBodyFatEstimate("female", 75, 34, 165, 0);
        assertThat(result).contains("Error: hip_cm is required for female");
    }

    @Test
    void healthBodyFat_maleWaistSmallerThanNeck_returnsError() {
        String result = tool.healthBodyFatEstimate("male", 30, 40, 175, 0);
        assertThat(result).contains("Error: waist circumference must be greater than neck");
    }

    @Test
    void healthBodyFat_femaleNegativeDiff_returnsError() {
        // waist + hip - neck < 0
        String result = tool.healthBodyFatEstimate("female", 30, 100, 165, 30);
        assertThat(result).contains("Error: (waist + hip) must be greater than neck");
    }

    @Test
    void healthBodyFat_zeroWaist_returnsError() {
        String result = tool.healthBodyFatEstimate("male", 0, 35, 175, 0);
        assertThat(result).contains("Error:");
    }

    @Test
    void healthBodyFat_zeroNeck_returnsError() {
        String result = tool.healthBodyFatEstimate("male", 80, 0, 175, 0);
        assertThat(result).contains("Error:");
    }

    @Test
    void healthBodyFat_zeroHeight_returnsError() {
        String result = tool.healthBodyFatEstimate("male", 80, 35, 0, 0);
        assertThat(result).contains("Error:");
    }

    @Test
    void healthBodyFat_containsLeanMass() {
        String result = tool.healthBodyFatEstimate("male", 85, 38, 175, 0);
        assertThat(result).contains("Lean mass %");
    }

    @Test
    void healthBodyFat_containsDisclaimer() {
        String result = tool.healthBodyFatEstimate("male", 85, 38, 175, 0);
        assertThat(result).contains("Disclaimer");
    }
}
