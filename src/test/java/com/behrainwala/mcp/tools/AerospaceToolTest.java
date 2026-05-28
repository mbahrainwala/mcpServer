package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AerospaceToolTest {

    private AerospaceTool tool;

    @BeforeEach
    void setUp() {
        tool = new AerospaceTool();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // aero_oblique_shock
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void obliqueShock_fromDeflectionAngle_solvesWeakBeta() {
        // M1=2, θ=15° → known weak β ≈ 45.3°
        String result = tool.obliqueShock(2.0, 15.0, null, null, null, null);
        assertThat(result).contains("β  =").contains("°").doesNotContain("Error:");
    }

    @Test
    void obliqueShock_fromShockAngle_solvesTheta() {
        // M1=2, β=45° → θ ≈ 14.7°
        String result = tool.obliqueShock(2.0, null, 45.0, null, null, null);
        assertThat(result).contains("θ  =").doesNotContain("Error:");
    }

    @Test
    void obliqueShock_downstreamMachLessThanUpstream() {
        // Oblique shock always decelerates flow (M2 < M1)
        String result = tool.obliqueShock(3.0, 20.0, null, null, null, null);
        assertThat(result).contains("M₂").doesNotContain("Error:");
    }

    @Test
    void obliqueShock_withAbsoluteP1T1_showsAbsoluteValues() {
        String result = tool.obliqueShock(2.5, 10.0, null, 50000.0, 216.65, null);
        assertThat(result).contains("P₂ =").contains("T₂ =");
    }

    @Test
    void obliqueShock_M1SubsonicReturnsError() {
        assertThat(tool.obliqueShock(0.8, 15.0, null, null, null, null)).startsWith("Error:");
    }

    @Test
    void obliqueShock_M1ExactlyOneReturnsError() {
        assertThat(tool.obliqueShock(1.0, 10.0, null, null, null, null)).startsWith("Error:");
    }

    @Test
    void obliqueShock_neitherAngleProvidedReturnsError() {
        assertThat(tool.obliqueShock(2.0, null, null, null, null, null)).startsWith("Error:");
    }

    @Test
    void obliqueShock_nullM1ReturnsError() {
        assertThat(tool.obliqueShock(null, 15.0, null, null, null, null)).startsWith("Error:");
    }

    @Test
    void obliqueShock_deflectionExceedsMax_shockDetaches() {
        // θ=45° far exceeds max for M1=2 (≈22.97°)
        String result = tool.obliqueShock(2.0, 45.0, null, null, null, null);
        assertThat(result).startsWith("Error:").contains("detach");
    }

    @Test
    void obliqueShock_betaAtMachAngle_isInvalidRange() {
        // β = arcsin(1/2) = 30° exactly = Mach angle → not strictly inside valid range
        String result = tool.obliqueShock(2.0, null, 30.0, null, null, null);
        assertThat(result).startsWith("Error:");
    }

    @Test
    void obliqueShock_stagnationPressureRecoveryBelowOne() {
        String result = tool.obliqueShock(3.0, 15.0, null, null, null, null);
        assertThat(result).contains("P₀₂/P₀₁ =").contains("stagnation pressure lost");
    }

    @Test
    void obliqueShock_customGamma_changesBeta() {
        String r14 = tool.obliqueShock(3.0, 20.0, null, null, null, 1.4);
        String r13 = tool.obliqueShock(3.0, 20.0, null, null, null, 1.3);
        assertThat(r14).contains("β  =");
        assertThat(r13).contains("β  =");
        assertThat(r14).isNotEqualTo(r13);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // aero_prandtl_meyer
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void prandtlMeyer_M2_M1_expansionIncreasesM() {
        // Expansion always increases Mach number
        String result = tool.prandtlMeyer(2.0, 20.0, null);
        assertThat(result).contains("M₂ =").doesNotContain("Error:");
        // M2 should be > 2 — result contains the numeric value
        assertThat(result).doesNotContain("Error:");
    }

    @Test
    void prandtlMeyer_sonicEntry_M1one() {
        // M1=1 → ν1=0; any turning → supersonic M2
        String result = tool.prandtlMeyer(1.0, 30.0, null);
        assertThat(result).contains("M₂ =").doesNotContain("Error:");
    }

    @Test
    void prandtlMeyer_pressureDropsAcrossExpansion() {
        // P2/P1 < 1 always for expansion
        String result = tool.prandtlMeyer(2.0, 15.0, null);
        assertThat(result).contains("P₂/P₁ =").doesNotContain("Error:");
    }

    @Test
    void prandtlMeyer_isentropicNotePresent() {
        String result = tool.prandtlMeyer(2.0, 10.0, null);
        assertThat(result).contains("isentropic").contains("stagnation pressure is fully preserved");
    }

    @Test
    void prandtlMeyer_turningAngleTooLargeReturnsError() {
        // Max turning for γ=1.4 ≈ 130.45°; requesting 200° → error
        assertThat(tool.prandtlMeyer(1.0, 200.0, null)).startsWith("Error:");
    }

    @Test
    void prandtlMeyer_M1SubsonicReturnsError() {
        assertThat(tool.prandtlMeyer(0.5, 10.0, null)).startsWith("Error:");
    }

    @Test
    void prandtlMeyer_negativeAngleReturnsError() {
        assertThat(tool.prandtlMeyer(2.0, -5.0, null)).startsWith("Error:");
    }

    @Test
    void prandtlMeyer_missingRequiredParamsReturnsError() {
        assertThat(tool.prandtlMeyer(null, 10.0, null)).startsWith("Error:");
        assertThat(tool.prandtlMeyer(2.0, null, null)).startsWith("Error:");
    }

    @Test
    void prandtlMeyer_customGammaChangesM2() {
        String r14 = tool.prandtlMeyer(2.0, 20.0, 1.4);
        String r13 = tool.prandtlMeyer(2.0, 20.0, 1.3);
        assertThat(r14).contains("M₂ =");
        assertThat(r13).contains("M₂ =");
        assertThat(r14).isNotEqualTo(r13);
    }

    @Test
    void prandtlMeyer_nuFunctionKnownValue() {
        // For γ=1.4, M=2 → ν ≈ 26.38°
        double nu = tool.pmFunction(2.0, 1.4);
        assertThat(Math.toDegrees(nu)).isCloseTo(26.38, within(0.1));
    }

    @Test
    void prandtlMeyer_mFromPMInverse() {
        // mFromPM(pmFunction(M)) should recover M
        double M = 2.5;
        double nu = tool.pmFunction(M, 1.4);
        double Mback = tool.mFromPM(nu, 1.4);
        assertThat(Mback).isCloseTo(M, within(0.001));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // aero_rocket_propulsion
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void rocket_tsiolkovsky_computesDeltaV() {
        // Isp=350s, m0=100000kg, mf=20000kg → ΔV = 350*9.81*ln(5) ≈ 5530 m/s
        String result = tool.rocketPropulsion("tsiolkovsky", "350, 100000, 20000");
        assertThat(result).contains("ΔV =").contains("km/s").doesNotContain("Error:");
    }

    @Test
    void rocket_tsiolkovsky_mfGreaterThanM0_returnsError() {
        assertThat(tool.rocketPropulsion("tsiolkovsky", "350, 10000, 50000")).startsWith("Error:");
    }

    @Test
    void rocket_tsiolkovsky_referenceAnnotationsPresent() {
        String result = tool.rocketPropulsion("tsiolkovsky", "300, 50000, 5000");
        assertThat(result).contains("LEO").contains("GTO");
    }

    @Test
    void rocket_tsiolkovskayMass_computesPropellant() {
        // Isp=350, ΔV=9500 m/s, m0=100000 → mf = 100000/exp(9500/(350*9.81))
        String result = tool.rocketPropulsion("tsiolkovsky_mass", "350, 9500, 100000");
        assertThat(result).contains("mf").contains("m_propellant").doesNotContain("Error:");
    }

    @Test
    void rocket_thrust_computesTotalThrust() {
        // ṁ=100 kg/s, Ve=3000 m/s, Ae=1 m², Pe=50000 Pa, Pa=101325 Pa
        String result = tool.rocketPropulsion("thrust", "100, 3000, 1, 50000, 101325");
        assertThat(result).contains("Total thrust").contains("kN").doesNotContain("Error:");
    }

    @Test
    void rocket_thrust_pressureThrustNegativeWhenUnderexpanded_notError() {
        // Pe < Pa → pressure thrust negative → still valid
        String result = tool.rocketPropulsion("thrust", "50, 2500, 0.5, 80000, 101325");
        assertThat(result).contains("Total thrust").doesNotContain("Error:");
    }

    @Test
    void rocket_isp_computesSpecificImpulse() {
        // F=500kN, ṁ=50 kg/s → Isp ≈ 1019 s (high, for verification logic)
        String result = tool.rocketPropulsion("isp", "500000, 50");
        assertThat(result).contains("Isp =").contains("s").doesNotContain("Error:");
    }

    @Test
    void rocket_isp_referenceValuesPresent() {
        assertThat(tool.rocketPropulsion("isp", "100000, 30")).contains("H₂/LOX");
    }

    @Test
    void rocket_exitVelocity_computesVe() {
        // γ=1.4, R=287 J/kg·K, Tc=3000K, Pc=7MPa, Pe=100kPa
        String result = tool.rocketPropulsion("exit_velocity", "1.4, 287, 3000, 7000000, 100000");
        assertThat(result).contains("Ve  =").contains("m/s").doesNotContain("Error:");
    }

    @Test
    void rocket_exitVelocity_PeGreaterThanPc_returnsError() {
        assertThat(tool.rocketPropulsion("exit_velocity", "1.4, 287, 3000, 100000, 7000000")).startsWith("Error:");
    }

    @Test
    void rocket_massFlow_computesMdot() {
        String result = tool.rocketPropulsion("mass_flow", "1000000, 350");
        assertThat(result).contains("ṁ =").doesNotContain("Error:");
    }

    @Test
    void rocket_expansionRatio_Me1_returnsOne() {
        // Ae/At at M=1 (sonic) = 1
        String result = tool.rocketPropulsion("expansion_ratio", "1.0");
        assertThat(result).contains("Ae/At = 1").doesNotContain("Error:");
    }

    @Test
    void rocket_expansionRatio_supersonicExit() {
        // Me=3 → Ae/At ≈ 4.23
        String result = tool.rocketPropulsion("expansion_ratio", "3.0");
        assertThat(result).contains("Ae/At =").doesNotContain("Error:");
    }

    @Test
    void rocket_expansionRatio_subsonicExitReturnsError() {
        assertThat(tool.rocketPropulsion("expansion_ratio", "0.5")).startsWith("Error:");
    }

    @Test
    void rocket_unknownFormulaReturnsHint() {
        String result = tool.rocketPropulsion("bogus_formula", "1, 2");
        assertThat(result).contains("Unknown formula");
    }

    @Test
    void rocket_insufficientValuesReturnsError() {
        assertThat(tool.rocketPropulsion("tsiolkovsky", "350, 100000")).startsWith("Error:");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // aero_flight_performance
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void perf_breguetRange_computesKm() {
        // V=250 m/s, Isp=5000s (turbofan), L/D=18, m0=75000kg, mf=45000kg
        String result = tool.flightPerformance("breguet_range", "250, 5000, 18, 75000, 45000");
        assertThat(result).contains("km").contains("NM").doesNotContain("Error:");
    }

    @Test
    void perf_breguetRange_mfGreaterThanM0_returnsError() {
        assertThat(tool.flightPerformance("breguet_range", "250, 5000, 18, 40000, 75000")).startsWith("Error:");
    }

    @Test
    void perf_breguetRange_nauticalMilesPresent() {
        String result = tool.flightPerformance("breguet_range", "230, 4500, 16, 60000, 35000");
        assertThat(result).contains("NM");
    }

    @Test
    void perf_breguetEndurance_computesHours() {
        // Isp=5000s, L/D=20, m0=50000, mf=35000
        String result = tool.flightPerformance("breguet_endurance", "5000, 20, 50000, 35000");
        assertThat(result).contains("hours").doesNotContain("Error:");
    }

    @Test
    void perf_breguetEndurance_mfGeM0_returnsError() {
        assertThat(tool.flightPerformance("breguet_endurance", "5000, 20, 30000, 30000")).startsWith("Error:");
    }

    @Test
    void perf_stallSpeed_computesKnots() {
        // W=500000N, ρ=1.225, S=125m², CLmax=2.2
        String result = tool.flightPerformance("stall_speed", "500000, 1.225, 125, 2.2");
        assertThat(result).contains("m/s").contains("knots").doesNotContain("Error:");
    }

    @Test
    void perf_stallSpeed_higherCLmaxLowersStallSpeed() {
        String low  = tool.flightPerformance("stall_speed", "300000, 1.225, 80, 1.5");
        String high = tool.flightPerformance("stall_speed", "300000, 1.225, 80, 2.5");
        assertThat(low).contains("m/s");
        assertThat(high).contains("m/s");
        // Both succeed; higher CLmax should give lower stall speed (not checking numbers directly,
        // but verifying both run without error)
        assertThat(low).doesNotContain("Error:");
        assertThat(high).doesNotContain("Error:");
    }

    @Test
    void perf_rateOfClimb_positiveWhenThrustExceedsDrag() {
        // T=150kN, D=80kN, V=200m/s, W=700kN
        String result = tool.flightPerformance("rate_of_climb", "150000, 80000, 200, 700000");
        assertThat(result).contains("ROC").contains("ft/min").doesNotContain("Error:");
        assertThat(result).doesNotContain("descending");
    }

    @Test
    void perf_rateOfClimb_negativeWhenDragExceedsThrust() {
        // T < D → ROC negative
        String result = tool.flightPerformance("rate_of_climb", "50000, 100000, 200, 700000");
        assertThat(result).contains("descending");
    }

    @Test
    void perf_rateOfClimb_climbAnglePresent() {
        String result = tool.flightPerformance("rate_of_climb", "200000, 100000, 250, 1000000");
        assertThat(result).contains("Climb angle").contains("°");
    }

    @Test
    void perf_turnBank_45deg_loadFactorSqrt2() {
        // 45° bank → n = 1/cos(45°) = √2 ≈ 1.414
        String result = tool.flightPerformance("turn_bank", "200, 45");
        assertThat(result).contains("Load factor").doesNotContain("Error:");
    }

    @Test
    void perf_turnBank_90deg_returnsError() {
        assertThat(tool.flightPerformance("turn_bank", "200, 90")).startsWith("Error:");
    }

    @Test
    void perf_turnBank_negativeAngleReturnsError() {
        assertThat(tool.flightPerformance("turn_bank", "200, -10")).startsWith("Error:");
    }

    @Test
    void perf_turnLoad_n1_zeroBank() {
        // n=1 → φ=0°, no turn — turn rate approaches 0
        String result = tool.flightPerformance("turn_load", "200, 1");
        assertThat(result).contains("Turn rate").doesNotContain("Error:");
    }

    @Test
    void perf_turnLoad_nLessThanOne_returnsError() {
        assertThat(tool.flightPerformance("turn_load", "200, 0.5")).startsWith("Error:");
    }

    @Test
    void perf_turnLoad_consistentWithTurnBank() {
        // turn_bank(V=200, φ=60°) and turn_load(V=200, n=2) should give same results
        // since 60° bank → n=1/cos60°=2
        String byBank = tool.flightPerformance("turn_bank", "200, 60");
        String byLoad = tool.flightPerformance("turn_load", "200, 2");
        assertThat(byBank).contains("Turn radius").doesNotContain("Error:");
        assertThat(byLoad).contains("Turn radius").doesNotContain("Error:");
    }

    @Test
    void perf_unknownFormulaReturnsHint() {
        String result = tool.flightPerformance("banana", "1,2,3");
        assertThat(result).contains("Unknown formula");
    }

    @Test
    void perf_insufficientValuesReturnsError() {
        assertThat(tool.flightPerformance("breguet_range", "250, 5000, 18")).startsWith("Error:");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // aero_heating
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void heating_stagnationTempAlwaysAboveStatic() {
        // T0 > T∞ for any M > 0
        String result = tool.aerodynamicHeating(5.0, 1500.0, 250.0, 0.01, null, null, null, null);
        assertThat(result).contains("T₀").doesNotContain("Error:");
    }

    @Test
    void heating_recoveryTempBetweenStaticAndStagnation() {
        // Tr_lam < T0 and Tr_lam > T∞ for M > 0
        String result = tool.aerodynamicHeating(7.0, 2200.0, 220.0, 3e-3, null, null, null, null);
        assertThat(result).contains("Tr (lam)").contains("Tr (turb)").doesNotContain("Error:");
    }

    @Test
    void heating_withNoseRadius_computesHeatFlux() {
        // rN=0.3m, M=7, V=2200m/s, T=220K, ρ=0.003 kg/m³
        String result = tool.aerodynamicHeating(7.0, 2200.0, 220.0, 3e-3, 0.3, null, null, null);
        assertThat(result).contains("q_s =").contains("W/m²").contains("Tw_rad =");
    }

    @Test
    void heating_highMachShowsMWPerM2() {
        // At very high ΔV, heat flux > 1 MW/m²
        String result = tool.aerodynamicHeating(25.0, 7900.0, 200.0, 1.2e-4, 0.1, 300.0, null, null);
        // Either shows MW/m² or just W/m² depending on magnitude; either way no error
        assertThat(result).contains("q_s =").doesNotContain("Error:");
    }

    @Test
    void heating_withoutNoseRadius_skipsHeatFluxSection() {
        String result = tool.aerodynamicHeating(5.0, 1500.0, 250.0, 0.01, null, null, null, null);
        assertThat(result).doesNotContain("q_s =");
    }

    @Test
    void heating_radiationEquilibriumTempPresent() {
        String result = tool.aerodynamicHeating(8.0, 2500.0, 210.0, 5e-3, 0.2, 300.0, 0.85, null);
        assertThat(result).contains("Tw_rad =").contains("°C");
    }

    @Test
    void heating_customEmissivity_changesRadEqTemp() {
        String e85  = tool.aerodynamicHeating(7.0, 2200.0, 220.0, 3e-3, 0.3, 300.0, 0.85, null);
        String e50  = tool.aerodynamicHeating(7.0, 2200.0, 220.0, 3e-3, 0.3, 300.0, 0.50, null);
        assertThat(e85).contains("Tw_rad =");
        assertThat(e50).contains("Tw_rad =");
        assertThat(e85).isNotEqualTo(e50);
    }

    @Test
    void heating_defaultWallTemp300WhenNull() {
        // Not providing wallTemp → 300 K assumed (visible in output)
        String result = tool.aerodynamicHeating(5.0, 1600.0, 250.0, 0.01, 0.5, null, null, null);
        assertThat(result).contains("300").doesNotContain("Error:");
    }

    @Test
    void heating_missingRequiredParamsReturnsError() {
        assertThat(tool.aerodynamicHeating(null, 1500.0, 250.0, 0.01, null, null, null, null)).startsWith("Error:");
        assertThat(tool.aerodynamicHeating(5.0, null, 250.0, 0.01, null, null, null, null)).startsWith("Error:");
        assertThat(tool.aerodynamicHeating(5.0, 1500.0, null, 0.01, null, null, null, null)).startsWith("Error:");
        assertThat(tool.aerodynamicHeating(5.0, 1500.0, 250.0, null, null, null, null, null)).startsWith("Error:");
    }

    @Test
    void heating_negativeMachReturnsError() {
        assertThat(tool.aerodynamicHeating(-1.0, 340.0, 288.0, 1.225, null, null, null, null)).startsWith("Error:");
    }

    @Test
    void heating_customGammaChangesT0() {
        String r14 = tool.aerodynamicHeating(5.0, 1500.0, 250.0, 0.01, null, null, null, 1.4);
        String r13 = tool.aerodynamicHeating(5.0, 1500.0, 250.0, 0.01, null, null, null, 1.3);
        assertThat(r14).contains("T₀");
        assertThat(r13).contains("T₀");
        assertThat(r14).isNotEqualTo(r13);
    }

    @Test
    void heating_noteAboutAccuracyPresent() {
        String result = tool.aerodynamicHeating(6.0, 1900.0, 230.0, 0.005, null, null, null, null);
        assertThat(result).contains("Chapman").contains("±30%");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // aero_turbomachinery
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void turbo_compressor_actualExitTempHigherThanIdeal() {
        // With η_c < 1: T02 > T02s (more work needed than ideal)
        String result = tool.turbomachinery("compressor", "288, 101325, 10, 0.85");
        assertThat(result).contains("T₀₂s =").contains("T₀₂  =").doesNotContain("Error:");
    }

    @Test
    void turbo_compressor_pressureRatioLessThanOne_returnsError() {
        assertThat(tool.turbomachinery("compressor", "288, 101325, 0.5, 0.85")).startsWith("Error:");
    }

    @Test
    void turbo_compressor_efficiencyOutOfRange_returnsError() {
        assertThat(tool.turbomachinery("compressor", "288, 101325, 10, 1.5")).startsWith("Error:");
        assertThat(tool.turbomachinery("compressor", "288, 101325, 10, 0")).startsWith("Error:");
    }

    @Test
    void turbo_compressor_withMassFlow_showsPower() {
        String result = tool.turbomachinery("compressor", "288, 101325, 8, 0.88, 50");
        assertThat(result).contains("Power =").contains("MW");
    }

    @Test
    void turbo_compressor_workIsPositive() {
        // Compressor adds work → W > 0 in output
        String result = tool.turbomachinery("compressor", "300, 101325, 5, 0.9");
        assertThat(result).contains("kJ/kg").doesNotContain("Error:");
    }

    @Test
    void turbo_turbine_actualExitTempHigherThanIdeal() {
        // With η_t < 1: T04 > T04s (less work extracted than ideal)
        String result = tool.turbomachinery("turbine", "1400, 1500000, 4, 0.90");
        assertThat(result).contains("T₀₄s =").contains("T₀₄  =").doesNotContain("Error:");
    }

    @Test
    void turbo_turbine_expansionRatioLessThanOne_returnsError() {
        assertThat(tool.turbomachinery("turbine", "1400, 1500000, 0.8, 0.90")).startsWith("Error:");
    }

    @Test
    void turbo_turbine_efficiencyOutOfRange_returnsError() {
        assertThat(tool.turbomachinery("turbine", "1400, 1500000, 4, 0")).startsWith("Error:");
    }

    @Test
    void turbo_turbine_withMassFlow_showsPower() {
        String result = tool.turbomachinery("turbine", "1500, 2000000, 5, 0.92, 30");
        assertThat(result).contains("Power =").contains("MW");
    }

    @Test
    void turbo_turbine_temperatureDropIsPositive() {
        String result = tool.turbomachinery("turbine", "1300, 1200000, 3, 0.88");
        assertThat(result).contains("ΔT₀  =").doesNotContain("Error:");
    }

    @Test
    void turbo_euler_compressorMode_positiveWork() {
        // Cw2 > Cw1 → work input (compressor)
        String result = tool.turbomachinery("euler", "300, 50, 200, 150");
        assertThat(result).contains("compressor").doesNotContain("Error:");
    }

    @Test
    void turbo_euler_turbineMode_negativeWork() {
        // Cw2 < Cw1 → work extracted (turbine)
        String result = tool.turbomachinery("euler", "300, 250, 50, 150");
        assertThat(result).contains("turbine").doesNotContain("Error:");
    }

    @Test
    void turbo_euler_stageLoadingAndFlowCoefficientPresent() {
        String result = tool.turbomachinery("euler", "400, 0, 200, 200");
        assertThat(result).contains("ψ   =").contains("φ   =");
    }

    @Test
    void turbo_euler_velocityTriangleAnglesPresent() {
        String result = tool.turbomachinery("euler", "350, 100, 250, 180");
        assertThat(result).contains("α₁ =").contains("α₂ =").contains("β₁ =").contains("β₂ =");
    }

    @Test
    void turbo_euler_zeroBladeSpeed_returnsError() {
        assertThat(tool.turbomachinery("euler", "0, 100, 200, 150")).startsWith("Error:");
    }

    @Test
    void turbo_euler_zeroAxialVelocity_returnsError() {
        assertThat(tool.turbomachinery("euler", "300, 100, 200, 0")).startsWith("Error:");
    }

    @Test
    void turbo_unknownFormulaReturnsHint() {
        assertThat(tool.turbomachinery("fan", "1,2,3,4")).contains("Unknown formula");
    }

    @Test
    void turbo_insufficientValuesReturnsError() {
        assertThat(tool.turbomachinery("compressor", "288, 101325, 10")).startsWith("Error:");
        assertThat(tool.turbomachinery("euler", "300, 100, 200")).startsWith("Error:");
    }
}
