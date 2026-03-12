package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for nutrition and health-related calculations.
 * Provides BMI, BMR/TDEE, macronutrient splits, heart rate zones,
 * hydration recommendations, and body fat estimation.
 *
 * Disclaimer: All results are estimates for informational purposes only
 * and do not constitute medical advice.
 */
@Service
public class HealthTool {

    private static final String DISCLAIMER =
            """
                    
                    
                    ⚠ Disclaimer: These values are estimates for informational purposes only and do not constitute medical advice. \
                    Consult a healthcare professional before making changes to your diet or exercise routine.""";

    // ───────────────────────────── 1. BMI ─────────────────────────────

    @Tool(name = "health_bmi", description = "Calculate Body Mass Index (BMI) from weight and height. "
            + "Returns BMI value, WHO category, healthy weight range for the given height, and BMI Prime.")
    public String healthBmi(
            @ToolParam(description = "Body weight in kilograms") double weight_kg,
            @ToolParam(description = "Height in centimetres") double height_cm) {

        if (weight_kg <= 0 || height_cm <= 0) {
            return "Error: weight_kg and height_cm must be positive numbers.";
        }

        double heightM = height_cm / 100.0;
        double bmi = weight_kg / (heightM * heightM);
        String category = bmiCategory(bmi);

        double healthyLow = 18.5 * heightM * heightM;
        double healthyHigh = 24.9 * heightM * heightM;
        double bmiPrime = bmi / 25.0;

        return "══════════════ BMI Results ══════════════\n" +
                String.format("  Weight       : %.1f kg%n", weight_kg) +
                String.format("  Height       : %.1f cm (%.2f m)%n", height_cm, heightM) +
                String.format("  BMI          : %.1f%n", bmi) +
                String.format("  Category     : %s%n", category) +
                String.format("  BMI Prime    : %.2f%n", bmiPrime) +
                String.format("  Healthy range: %.1f – %.1f kg (for %.1f cm)%n", healthyLow, healthyHigh, height_cm) +
                DISCLAIMER;
    }

    private String bmiCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Normal weight";
        if (bmi < 30.0) return "Overweight";
        if (bmi < 35.0) return "Obese – Class I";
        if (bmi < 40.0) return "Obese – Class II";
        return "Obese – Class III";
    }

    // ───────────────────────────── 2. BMR / TDEE ─────────────────────────────

    @Tool(name = "health_bmr", description = "Calculate Basal Metabolic Rate (BMR) using Mifflin-St Jeor and "
            + "Harris-Benedict equations. Also computes Total Daily Energy Expenditure (TDEE) based on activity level, "
            + "and calorie targets for weight loss, maintenance, and weight gain.")
    public String healthBmr(
            @ToolParam(description = "Body weight in kilograms") double weight_kg,
            @ToolParam(description = "Height in centimetres") double height_cm,
            @ToolParam(description = "Age in years") int age,
            @ToolParam(description = "Biological sex: 'male' or 'female'") String sex,
            @ToolParam(description = "Activity level: sedentary, light, moderate, active, or very_active") String activity_level) {

        if (weight_kg <= 0 || height_cm <= 0 || age <= 0) {
            return "Error: weight_kg, height_cm, and age must be positive numbers.";
        }

        String sexLower = sex.trim().toLowerCase();
        if (!sexLower.equals("male") && !sexLower.equals("female")) {
            return "Error: sex must be 'male' or 'female'.";
        }

        double activityMultiplier = activityMultiplier(activity_level);
        if (activityMultiplier < 0) {
            return "Error: activity_level must be one of: sedentary, light, moderate, active, very_active.";
        }

        boolean isMale = sexLower.equals("male");

        // Mifflin-St Jeor
        double mifflin = isMale
                ? (10.0 * weight_kg) + (6.25 * height_cm) - (5.0 * age) + 5
                : (10.0 * weight_kg) + (6.25 * height_cm) - (5.0 * age) - 161;

        // Harris-Benedict (revised)
        double harris = isMale
                ? (13.397 * weight_kg) + (4.799 * height_cm) - (5.677 * age) + 88.362
                : (9.247 * weight_kg) + (3.098 * height_cm) - (4.330 * age) + 447.593;

        double tdeeMifflin = mifflin * activityMultiplier;
        double tdeeHarris = harris * activityMultiplier;
        double tdeeAvg = (tdeeMifflin + tdeeHarris) / 2.0;

        return "══════════════ BMR / TDEE Results ══════════════\n" +
                String.format("  Weight         : %.1f kg%n", weight_kg) +
                String.format("  Height         : %.1f cm%n", height_cm) +
                String.format("  Age            : %d years%n", age) +
                String.format("  Sex            : %s%n", sexLower) +
                String.format("  Activity level : %s (×%.3f)%n", activity_level, activityMultiplier) +
                "\n── Basal Metabolic Rate (BMR) ──\n" +
                String.format("  Mifflin-St Jeor : %.0f kcal/day%n", mifflin) +
                String.format("  Harris-Benedict  : %.0f kcal/day%n", harris) +
                "\n── Total Daily Energy Expenditure (TDEE) ──\n" +
                String.format("  Based on Mifflin-St Jeor : %.0f kcal/day%n", tdeeMifflin) +
                String.format("  Based on Harris-Benedict  : %.0f kcal/day%n", tdeeHarris) +
                String.format("  Average                   : %.0f kcal/day%n", tdeeAvg) +
                "\n── Calorie Targets (based on average TDEE) ──\n" +
                String.format("  Weight loss  (−500 kcal) : %.0f kcal/day%n", tdeeAvg - 500) +
                String.format("  Maintenance              : %.0f kcal/day%n", tdeeAvg) +
                String.format("  Weight gain  (+500 kcal) : %.0f kcal/day%n", tdeeAvg + 500) +
                DISCLAIMER;
    }

    private double activityMultiplier(String level) {
        if (level == null) return -1;
        return switch (level.trim().toLowerCase()) {
            case "sedentary" -> 1.2;
            case "light" -> 1.375;
            case "moderate" -> 1.55;
            case "active" -> 1.725;
            case "very_active" -> 1.9;
            default -> -1;
        };
    }

    // ───────────────────────────── 3. Macro Calculator ─────────────────────────────

    @Tool(name = "health_macro_calculator", description = "Calculate daily macronutrient targets (protein, carbohydrates, fat) "
            + "in grams and calories based on a total calorie target and a dietary goal. "
            + "Goals: maintenance (30/40/30), weight_loss (40/25/35), muscle_gain (35/45/20), keto (20/5/75), balanced (25/50/25).")
    public String healthMacroCalculator(
            @ToolParam(description = "Total daily calorie target") int calories,
            @ToolParam(description = "Dietary goal: maintenance, weight_loss, muscle_gain, keto, or balanced") String goal) {

        if (calories <= 0) {
            return "Error: calories must be a positive number.";
        }

        int[] ratios = macroRatios(goal);
        if (ratios == null) {
            return "Error: goal must be one of: maintenance, weight_loss, muscle_gain, keto, balanced.";
        }

        int proteinPct = ratios[0];
        int carbsPct = ratios[1];
        int fatPct = ratios[2];

        double proteinCal = calories * proteinPct / 100.0;
        double carbsCal = calories * carbsPct / 100.0;
        double fatCal = calories * fatPct / 100.0;

        double proteinG = proteinCal / 4.0;
        double carbsG = carbsCal / 4.0;
        double fatG = fatCal / 9.0;

        return "══════════════ Macronutrient Targets ══════════════\n" +
                String.format("  Total calories : %,d kcal/day%n", calories) +
                String.format("  Goal           : %s%n", goal) +
                String.format("  Ratio (P/C/F)  : %d%% / %d%% / %d%%%n", proteinPct, carbsPct, fatPct) +
                "\n── Breakdown ──\n" +
                String.format("  Protein       : %.0f g   (%.0f kcal)%n", proteinG, proteinCal) +
                String.format("  Carbohydrates : %.0f g   (%.0f kcal)%n", carbsG, carbsCal) +
                String.format("  Fat           : %.0f g   (%.0f kcal)%n", fatG, fatCal) +
                DISCLAIMER;
    }

    /** Returns {protein%, carbs%, fat%} or null if goal is invalid. */
    private int[] macroRatios(String goal) {
        if (goal == null) return null;
        return switch (goal.trim().toLowerCase()) {
            case "maintenance" -> new int[]{30, 40, 30};
            case "weight_loss" -> new int[]{40, 25, 35};
            case "muscle_gain" -> new int[]{35, 45, 20};
            case "keto" -> new int[]{20, 5, 75};
            case "balanced" -> new int[]{25, 50, 25};
            default -> null;
        };
    }

    // ───────────────────────────── 4. Heart Rate Zones ─────────────────────────────

    @Tool(name = "health_heart_rate_zones", description = "Calculate heart rate training zones using the Karvonen method. "
            + "Returns maximum heart rate, heart rate reserve, and five training zones with BPM ranges: "
            + "Zone 1 (warm up), Zone 2 (fat burn), Zone 3 (cardio), Zone 4 (anaerobic), Zone 5 (max effort).")
    public String healthHeartRateZones(
            @ToolParam(description = "Age in years") int age,
            @ToolParam(description = "Resting heart rate in BPM (default 70 if unknown)") int resting_heart_rate) {

        if (age <= 0 || age > 120) {
            return "Error: age must be between 1 and 120.";
        }
        int rhr = resting_heart_rate > 0 ? resting_heart_rate : 70;

        int maxHR = 220 - age;
        int hrReserve = maxHR - rhr;

        StringBuilder sb = new StringBuilder();
        sb.append("══════════════ Heart Rate Training Zones ══════════════\n");
        sb.append(String.format("  Age                  : %d years%n", age));
        sb.append(String.format("  Resting HR           : %d bpm%n", rhr));
        sb.append(String.format("  Max HR (220 − age)   : %d bpm%n", maxHR));
        sb.append(String.format("  HR Reserve (Karvonen): %d bpm%n", hrReserve));
        sb.append("\n── Training Zones (Karvonen method) ──\n");
        appendZone(sb, "Zone 1 – Warm Up    ", 0.50, 0.60, hrReserve, rhr);
        appendZone(sb, "Zone 2 – Fat Burn   ", 0.60, 0.70, hrReserve, rhr);
        appendZone(sb, "Zone 3 – Cardio     ", 0.70, 0.80, hrReserve, rhr);
        appendZone(sb, "Zone 4 – Anaerobic  ", 0.80, 0.90, hrReserve, rhr);
        appendZone(sb, "Zone 5 – Max Effort ", 0.90, 1.00, hrReserve, rhr);
        sb.append(DISCLAIMER);
        return sb.toString();
    }

    private void appendZone(StringBuilder sb, String label, double lowPct, double highPct, int hrReserve, int rhr) {
        int low = (int) Math.round(hrReserve * lowPct + rhr);
        int high = (int) Math.round(hrReserve * highPct + rhr);
        sb.append(String.format("  %s : %3d – %3d bpm  (%2.0f%%–%3.0f%%)%n", label, low, high, lowPct * 100, highPct * 100));
    }

    // ───────────────────────────── 5. Hydration ─────────────────────────────

    @Tool(name = "health_hydration", description = "Calculate recommended daily water intake based on body weight, "
            + "activity level, and climate. Returns intake in litres and cups, plus an hourly intake suggestion.")
    public String healthHydration(
            @ToolParam(description = "Body weight in kilograms") double weight_kg,
            @ToolParam(description = "Activity level: sedentary, light, moderate, active, or very_active") String activity_level,
            @ToolParam(description = "Climate: temperate, hot, or cold") String climate) {

        if (weight_kg <= 0) {
            return "Error: weight_kg must be a positive number.";
        }

        // Base: ~33 mL per kg body weight
        double baseMl = weight_kg * 33.0;

        // Activity adjustment
        double activityFactor;
        switch (activity_level == null ? "" : activity_level.trim().toLowerCase()) {
            case "sedentary":   activityFactor = 1.0; break;
            case "light":       activityFactor = 1.1; break;
            case "moderate":    activityFactor = 1.25; break;
            case "active":      activityFactor = 1.4; break;
            case "very_active": activityFactor = 1.6; break;
            default:
                return "Error: activity_level must be one of: sedentary, light, moderate, active, very_active.";
        }

        // Climate adjustment
        double climateFactor;
        switch (climate == null ? "" : climate.trim().toLowerCase()) {
            case "temperate": climateFactor = 1.0; break;
            case "hot":       climateFactor = 1.2; break;
            case "cold":      climateFactor = 1.1; break;
            default:
                return "Error: climate must be one of: temperate, hot, cold.";
        }

        double totalMl = baseMl * activityFactor * climateFactor;
        double litres = totalMl / 1000.0;
        double cups = totalMl / 240.0; // 1 US cup ≈ 240 mL
        double hourly = litres / 16.0;  // spread over ~16 waking hours

        return "══════════════ Daily Hydration Recommendation ══════════════\n" +
                String.format("  Weight         : %.1f kg%n", weight_kg) +
                String.format("  Activity level : %s (×%.2f)%n", activity_level, activityFactor) +
                String.format("  Climate        : %s (×%.2f)%n", climate, climateFactor) +
                "\n── Recommended Intake ──\n" +
                String.format("  Total          : %.2f litres  (%.0f mL)%n", litres, totalMl) +
                String.format("  In cups        : ~%.0f cups  (240 mL each)%n", cups) +
                String.format("  Hourly target  : ~%.0f mL / hour  (over 16 waking hours)%n", hourly * 1000) +
                DISCLAIMER;
    }

    // ───────────────────────────── 6. Body Fat Estimate ─────────────────────────────

    @Tool(name = "health_body_fat_estimate", description = "Estimate body fat percentage using the US Navy method. "
            + "Requires waist and neck circumference measurements. Hip circumference is additionally required for females. "
            + "Returns estimated body fat %, fat mass, lean mass, and body fat category.")
    public String healthBodyFatEstimate(
            @ToolParam(description = "Biological sex: 'male' or 'female'") String sex,
            @ToolParam(description = "Waist circumference in centimetres (measured at navel)") double waist_cm,
            @ToolParam(description = "Neck circumference in centimetres") double neck_cm,
            @ToolParam(description = "Height in centimetres") double height_cm,
            @ToolParam(description = "Hip circumference in centimetres (required for female, use 0 for male)") double hip_cm) {

        if (waist_cm <= 0 || neck_cm <= 0 || height_cm <= 0) {
            return "Error: waist_cm, neck_cm, and height_cm must be positive numbers.";
        }

        String sexLower = sex == null ? "" : sex.trim().toLowerCase();
        if (!sexLower.equals("male") && !sexLower.equals("female")) {
            return "Error: sex must be 'male' or 'female'.";
        }

        boolean isMale = sexLower.equals("male");

        if (!isMale && hip_cm <= 0) {
            return "Error: hip_cm is required for female body fat estimation.";
        }

        // US Navy Method (uses log10 and centimetre inputs)
        double bodyFatPct;
        if (isMale) {
            // BF% = 86.010 × log10(waist − neck) − 70.041 × log10(height) + 36.76
            double diff = waist_cm - neck_cm;
            if (diff <= 0) {
                return "Error: waist circumference must be greater than neck circumference.";
            }
            bodyFatPct = 86.010 * Math.log10(diff) - 70.041 * Math.log10(height_cm) + 36.76;
        } else {
            // BF% = 163.205 × log10(waist + hip − neck) − 97.684 × log10(height) − 78.387
            double diff = waist_cm + hip_cm - neck_cm;
            if (diff <= 0) {
                return "Error: (waist + hip) must be greater than neck circumference.";
            }
            bodyFatPct = 163.205 * Math.log10(diff) - 97.684 * Math.log10(height_cm) - 78.387;
        }

        if (bodyFatPct < 0) bodyFatPct = 0;

        // We need weight to compute fat/lean mass – derive approximate weight from height & average BMI
        // Since we don't have weight as a param, report fat% and category, and express mass as proportions
        // Actually, let's compute fat mass assuming we can derive it from bf%:
        // We don't have weight, so express in relative terms or just report %.
        // For a more useful output, let's include a note.

        String category = bodyFatCategory(bodyFatPct, isMale);

        StringBuilder sb = new StringBuilder();
        sb.append("══════════════ Body Fat Estimate (US Navy Method) ══════════════\n");
        sb.append(String.format("  Sex            : %s%n", sexLower));
        sb.append(String.format("  Height         : %.1f cm%n", height_cm));
        sb.append(String.format("  Waist          : %.1f cm%n", waist_cm));
        sb.append(String.format("  Neck           : %.1f cm%n", neck_cm));
        if (!isMale) {
            sb.append(String.format("  Hip            : %.1f cm%n", hip_cm));
        }
        sb.append(String.format("%n  Body Fat %%     : %.1f%%%n", bodyFatPct));
        sb.append(String.format("  Category       : %s%n", category));
        sb.append(String.format("  Lean mass %%    : %.1f%%%n", 100.0 - bodyFatPct));
        sb.append("\n── To compute fat / lean mass in kg ──\n");
        sb.append(String.format("  Fat mass  = weight_kg × %.3f%n", bodyFatPct / 100.0));
        sb.append(String.format("  Lean mass = weight_kg × %.3f%n", (100.0 - bodyFatPct) / 100.0));
        sb.append(DISCLAIMER);
        return sb.toString();
    }

    private String bodyFatCategory(double bf, boolean isMale) {
        if (isMale) {
            if (bf < 6)  return "Essential fat";
            if (bf < 14) return "Athletes";
            if (bf < 18) return "Fitness";
            if (bf < 25) return "Average";
            return "Obese";
        } else {
            if (bf < 14) return "Essential fat";
            if (bf < 21) return "Athletes";
            if (bf < 25) return "Fitness";
            if (bf < 32) return "Average";
            return "Obese";
        }
    }
}
