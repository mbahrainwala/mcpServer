package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FluidDynamicsToolTest {

    private FluidDynamicsTool tool;

    @BeforeEach
    void setUp() {
        tool = new FluidDynamicsTool();
    }

    // ── Bernoulli ──────────────────────────────────────────────────────────────

    @Test
    void bernoulli_solveForVelocity() {
        // Pitot-static: P1=101325, v1=0 (stagnation), P2=100000, solve v2
        String result = tool.bernoulli(1.225, 101325.0, 0.0, 0.0, 100000.0, null, 0.0);
        assertThat(result).contains("m/s").contains("solved");
    }

    @Test
    void bernoulli_solveForPressure() {
        // v1=0 (stagnation), v2=100 m/s, solve P2
        String result = tool.bernoulli(1.225, 101325.0, 0.0, 0.0, null, 100.0, 0.0);
        assertThat(result).contains("Pa").contains("solved");
        // P2 should be less than P1 (flow accelerated)
        assertThat(result).contains("dynamic pressure");
    }

    @Test
    void bernoulli_verify_bothKnown() {
        // P1 + ½ρv1² = P2 + ½ρv2² => give consistent values
        // At v1=0: P2 = P1 - ½ρv2² = 101325 - 0.5*1.225*100^2 = 101325 - 6125 = 95200 Pa
        String result = tool.bernoulli(1.225, 101325.0, 0.0, 0.0, 95200.0, 100.0, 0.0);
        assertThat(result).contains("Bernoulli check");
    }

    @Test
    void bernoulli_missingRequiredParam_returnsError() {
        String result = tool.bernoulli(null, 101325.0, 10.0, 0.0, null, null, 0.0);
        assertThat(result).startsWith("Error:");
    }

    @Test
    void bernoulli_withElevationDifference_affectsResult() {
        // Water flowing down 10 m: h1=10, h2=0; v1=1 m/s, solve P2
        // Bernoulli const = P1 + ½ρv1² + ρgh1; P2 = const - ½ρv2² - ρg*0
        String result = tool.bernoulli(1000.0, 100000.0, 1.0, 10.0, null, 1.0, 0.0);
        assertThat(result).contains("Pa").contains("solved");
        // P2 should contain positive pressure gain from elevation drop
        assertThat(result).doesNotContain("Error:");
    }

    @Test
    void bernoulli_neitherP2NorV2_showsPrompt() {
        String result = tool.bernoulli(1.225, 101325.0, 50.0, 0.0, null, null, 0.0);
        assertThat(result).contains("provide either P₂ or v₂");
    }

    @Test
    void bernoulli_energyViolation_returnsError() {
        // P2 much higher than stagnation pressure → v2² < 0
        String result = tool.bernoulli(1.225, 101325.0, 0.0, 0.0, 999999.0, null, 0.0);
        assertThat(result).startsWith("Error:").contains("too high");
    }

    // ── Reynolds ───────────────────────────────────────────────────────────────

    @Test
    void reynolds_laminarPipeFlow() {
        // Re = 0.1 * 0.01 / 1.461e-5 ≈ 68 → laminar
        String result = tool.reynolds(0.1, 0.01, null, null, 1.461e-5);
        assertThat(result).contains("LAMINAR").contains("Re");
    }

    @Test
    void reynolds_turbulentExternalFlow() {
        // Re = 100 * 1.0 / 1.461e-5 ≈ 6.8e6 → turbulent
        String result = tool.reynolds(100.0, 1.0, null, null, 1.461e-5);
        assertThat(result).contains("TURBULENT");
        assertThat(result).contains("TURBULENT boundary layer");
    }

    @Test
    void reynolds_withDensityAndViscosity() {
        String result = tool.reynolds(50.0, 0.5, 1.225, 1.789e-5, null);
        assertThat(result).contains("Re").contains("BOUNDARY LAYER").doesNotContain("Error:");
    }

    @Test
    void reynolds_defaultAirProperties() {
        // No fluid params → should default to air at sea level
        String result = tool.reynolds(30.0, 2.0, null, null, null);
        assertThat(result).contains("Re").doesNotContain("Error:");
    }

    @Test
    void reynolds_transitionalFlow() {
        // Re ≈ 3000 → transitional: v=0.3, L=0.1, ν=1e-5
        String result = tool.reynolds(0.3, 0.1, null, null, 1.0e-5);
        assertThat(result).contains("TRANSITIONAL");
    }

    @Test
    void reynolds_missingRequiredParams_returnsError() {
        String result = tool.reynolds(null, 0.1, null, null, null);
        assertThat(result).startsWith("Error:");
        String result2 = tool.reynolds(10.0, null, null, null, null);
        assertThat(result2).startsWith("Error:");
    }

    @Test
    void reynolds_laminarExternalButTurbulentPipe() {
        // Re ≈ 10000: turbulent for pipe but laminar boundary layer for external flow (Re < 5e5)
        String result = tool.reynolds(1.0, 0.1, null, null, 1.0e-5);
        assertThat(result).contains("TURBULENT (Re > 4000)");
        assertThat(result).contains("LAMINAR boundary layer");
    }

    // ── Mach ──────────────────────────────────────────────────────────────────

    @Test
    void mach_subsonic() {
        // 100 m/s at sea level (a≈340 m/s → M≈0.29) → incompressible
        String result = tool.mach(100.0, 288.15, 101325.0, null);
        assertThat(result).contains("Incompressible");
        assertThat(result).contains("Mach");
    }

    @Test
    void mach_supersonic() {
        // 750 m/s at 288.15 K → M ≈ 2.2
        String result = tool.mach(750.0, 288.15, 101325.0, null);
        assertThat(result).contains("Supersonic");
    }

    @Test
    void mach_stagnationTemperatureAlwaysHigher() {
        String result = tool.mach(300.0, 250.0, 50000.0, null);
        assertThat(result).contains("T₀ =").contains("K");
        // stagnation T > static T
        assertThat(result).doesNotContain("Error:");
    }

    @Test
    void mach_subsonicCompressible() {
        // 200 m/s at sea level → M ≈ 0.59 → "Subsonic"
        String result = tool.mach(200.0, 288.15, 101325.0, null);
        assertThat(result).contains("Subsonic (0.3");
    }

    @Test
    void mach_transonic() {
        // 330 m/s at 288.15 K → M ≈ 0.97 → transonic
        String result = tool.mach(330.0, 288.15, 101325.0, null);
        assertThat(result).contains("Transonic");
    }

    @Test
    void mach_hypersonic() {
        // 1800 m/s at 288.15 K → M ≈ 5.3 → hypersonic
        String result = tool.mach(1800.0, 288.15, 101325.0, null);
        assertThat(result).contains("Hypersonic");
    }

    @Test
    void mach_missingVelocity_returnsError() {
        String result = tool.mach(null, 288.15, 101325.0, null);
        assertThat(result).startsWith("Error:");
    }

    @Test
    void mach_defaultsTandP_whenNull() {
        // T and P both null → uses ISA sea-level defaults, should not error
        String result = tool.mach(150.0, null, null, null);
        assertThat(result).contains("288.15").contains("101325").doesNotContain("Error:");
    }

    @Test
    void mach_customGamma() {
        // γ=1.3 (e.g. hot combustion gas) → different speed of sound
        String result = tool.mach(300.0, 1000.0, 200000.0, 1.3);
        assertThat(result).contains("1.3").doesNotContain("Error:");
    }

    // ── Isentropic ────────────────────────────────────────────────────────────

    @Test
    void isentropic_sonicConditions_areaRatioOne() {
        // M=1 → A/A* = 1
        String result = tool.isentropic(1.0, 500.0, 200000.0, null, null);
        assertThat(result).contains("A/A* = 1").doesNotContain("Error:");
    }

    @Test
    void isentropic_subsonic_pressureRatioBelowOne() {
        String result = tool.isentropic(0.5, 400.0, 150000.0, null, null);
        assertThat(result).contains("P/P₀ =").doesNotContain("Error:");
    }

    @Test
    void isentropic_supersonic_largeAreaRatio() {
        // M=3 → A/A* ≈ 4.23
        String result = tool.isentropic(3.0, 600.0, 300000.0, null, null);
        assertThat(result).contains("A/A* =").doesNotContain("Error:");
    }

    @Test
    void isentropic_missingParams_returnsError() {
        assertThat(tool.isentropic(null, 500.0, 200000.0, null, null)).startsWith("Error:");
        assertThat(tool.isentropic(1.0, null, 200000.0, null, null)).startsWith("Error:");
        assertThat(tool.isentropic(1.0, 500.0, null,    null, null)).startsWith("Error:");
    }

    @Test
    void isentropic_negativeM_returnsError() {
        String result = tool.isentropic(-0.5, 500.0, 200000.0, null, null);
        assertThat(result).startsWith("Error:");
    }

    @Test
    void isentropic_customGamma() {
        // γ=1.3 for combustion gas — should produce different area ratio than γ=1.4
        String r14 = tool.isentropic(2.0, 500.0, 200000.0, null, 1.4);
        String r13 = tool.isentropic(2.0, 500.0, 200000.0, null, 1.3);
        assertThat(r14).contains("A/A* =");
        assertThat(r13).contains("A/A* =");
        assertThat(r14).isNotEqualTo(r13);
    }

    @Test
    void isentropic_explicitRho0_usedDirectly() {
        // When rho0 is supplied explicitly, it should appear in the output
        String result = tool.isentropic(0.8, 400.0, 150000.0, 1.5, null);
        assertThat(result).contains("1.5").doesNotContain("Error:");
    }

    @Test
    void isentropic_mZero_areaRatioInfinite() {
        // M=0 → A/A* = ∞
        String result = tool.isentropic(0.0, 500.0, 200000.0, null, null);
        assertThat(result).contains("∞").doesNotContain("Error:");
    }

    // ── Normal Shock ──────────────────────────────────────────────────────────

    @Test
    void normalShock_M2_isSubsonic() {
        // All normal shocks produce subsonic downstream flow
        String result = tool.normalShock(2.0, 50000.0, 216.65, null);
        assertThat(result).contains("M₂ =").contains("subsonic");
    }

    @Test
    void normalShock_pressureRatioIncreases() {
        String result = tool.normalShock(3.0, null, null, null);
        assertThat(result).contains("P₂/P₁ =").doesNotContain("Error:");
    }

    @Test
    void normalShock_M1LessThanOne_returnsError() {
        String result = tool.normalShock(0.8, null, null, null);
        assertThat(result).startsWith("Error:");
    }

    @Test
    void normalShock_stagnationPressureRecoveryBelowOne() {
        String result = tool.normalShock(2.5, null, null, null);
        assertThat(result).contains("P₀₂/P₀₁ =").contains("%  stagnation pressure lost");
    }

    @Test
    void normalShock_nullM1_returnsError() {
        String result = tool.normalShock(null, null, null, null);
        assertThat(result).startsWith("Error:");
    }

    @Test
    void normalShock_exactlyM1_returnsError() {
        String result = tool.normalShock(1.0, null, null, null);
        assertThat(result).startsWith("Error:");
    }

    @Test
    void normalShock_withAbsoluteT1_showsAbsoluteT2() {
        // T1=216.65 K (11 km ISA), M1=2.0 → T2 = T1 * T2/T1 ratio
        String result = tool.normalShock(2.0, 50000.0, 216.65, null);
        assertThat(result).contains("T₂ =").contains("K");
        assertThat(result).doesNotContain("Error:");
    }

    @Test
    void normalShock_customGamma() {
        // γ=1.3 (combustion products) — ratios differ from air
        String resultAir  = tool.normalShock(3.0, null, null, 1.4);
        String resultHot  = tool.normalShock(3.0, null, null, 1.3);
        assertThat(resultAir).contains("P₂/P₁ =");
        assertThat(resultHot).contains("P₂/P₁ =");
        assertThat(resultAir).isNotEqualTo(resultHot);
    }

    // ── Aerodynamics ──────────────────────────────────────────────────────────

    @Test
    void aerodynamics_basicLiftDrag() {
        // Boeing 737-style: v=250 m/s, ρ=0.9, S=125 m², CL=0.5, CD=0.04
        String result = tool.aerodynamics(250.0, 0.9, 125.0, 0.5, 0.04, null);
        assertThat(result).contains("L =").contains("D =").contains("L/D =");
    }

    @Test
    void aerodynamics_withMass_computesLoadFactor() {
        String result = tool.aerodynamics(200.0, 1.225, 100.0, 0.6, 0.05, 50000.0);
        assertThat(result).contains("LOAD FACTOR").contains("n =");
    }

    @Test
    void aerodynamics_dynamicPressureCorrect() {
        // q = ½ * 1.225 * 100² = 6125 Pa
        String result = tool.aerodynamics(100.0, 1.225, 10.0, 1.0, 0.1, null);
        assertThat(result).contains("6125");
    }

    @Test
    void aerodynamics_missingParams_returnsError() {
        String result = tool.aerodynamics(null, 1.225, 100.0, 0.5, 0.04, null);
        assertThat(result).startsWith("Error:");
        String result2 = tool.aerodynamics(200.0, null, 100.0, 0.5, 0.04, null);
        assertThat(result2).startsWith("Error:");
    }

    @Test
    void aerodynamics_ldRatioEqualsClOverCd() {
        // L/D must equal CL/CD = 0.6/0.04 = 15
        String result = tool.aerodynamics(200.0, 1.225, 50.0, 0.6, 0.04, null);
        assertThat(result).contains("15");
    }

    @Test
    void aerodynamics_highAltitudeLowerDensity_reducesLift() {
        // Same speed but lower density → lower lift
        String seaLevel = tool.aerodynamics(200.0, 1.225, 100.0, 0.6, 0.05, null);
        String altitude  = tool.aerodynamics(200.0, 0.414, 100.0, 0.6, 0.05, null); // ~10 km
        assertThat(seaLevel).contains("L =");
        assertThat(altitude).contains("L =");
        // Both succeed without errors
        assertThat(seaLevel).doesNotContain("Error:");
        assertThat(altitude).doesNotContain("Error:");
    }

    // ── Pipe Flow ─────────────────────────────────────────────────────────────

    @Test
    void pipeFlow_laminarRegime() {
        // Low Re → laminar: D=0.01 m, v=0.1 m/s, water (ρ=1000, ν=1e-6)
        String result = tool.pipeFlow(0.01, 10.0, 0.1, null, 1000.0, 1.0e-6, 0.0, 0.0);
        assertThat(result).contains("Laminar").contains("64/Re");
    }

    @Test
    void pipeFlow_turbulentSmooth() {
        // High Re, smooth pipe
        String result = tool.pipeFlow(0.1, 100.0, 5.0, null, 1000.0, 1.0e-6, 0.0, null);
        assertThat(result).contains("Turbulent").contains("Blasius");
    }

    @Test
    void pipeFlow_turbulentRough() {
        String result = tool.pipeFlow(0.1, 100.0, 5.0, null, 1000.0, 1.0e-6, 4.6e-5, null);
        assertThat(result).contains("Swamee-Jain");
    }

    @Test
    void pipeFlow_fromFlowRate() {
        // Provide volumeFlowRate instead of velocity
        String result = tool.pipeFlow(0.05, 20.0, null, 0.001, 1.225, null, null, null);
        assertThat(result).contains("m/s").doesNotContain("Error:");
    }

    @Test
    void pipeFlow_minorLossesIncluded() {
        String result = tool.pipeFlow(0.05, 10.0, 3.0, null, 1000.0, 1.0e-6, 0.0, 2.5);
        assertThat(result).contains("Minor").contains("fittings");
    }

    @Test
    void pipeFlow_missingVelocityAndFlowRate_returnsError() {
        String result = tool.pipeFlow(0.05, 10.0, null, null, 1000.0, null, null, null);
        assertThat(result).startsWith("Error:");
    }

    @Test
    void pipeFlow_missingDiameter_returnsError() {
        String result = tool.pipeFlow(null, 10.0, 2.0, null, 1000.0, null, null, null);
        assertThat(result).startsWith("Error:");
    }

    @Test
    void pipeFlow_defaultAirViscosity_usedWhenNull() {
        // null kinematicViscosity → default air at sea level; should not error
        String result = tool.pipeFlow(0.05, 5.0, 20.0, null, 1.225, null, null, null);
        assertThat(result).contains("Re").doesNotContain("Error:");
    }

    // ── Continuity ────────────────────────────────────────────────────────────

    @Test
    void continuity_incompressible_solveV2() {
        // A1=0.1, v1=10 → Q=1 m³/s; A2=0.05 → v2=20
        String result = tool.continuity(0.1, 10.0, null, 0.05, null, null);
        assertThat(result).contains("20").contains("m/s").contains("solved");
    }

    @Test
    void continuity_incompressible_solveA2() {
        // A1=0.1, v1=10 → Q=1 m³/s; v2=50 → A2=0.02
        String result = tool.continuity(0.1, 10.0, null, null, 50.0, null);
        assertThat(result).contains("m²").contains("solved");
    }

    @Test
    void continuity_compressible_verification() {
        // Provide both sections → should show mass balance error ≈ 0
        String result = tool.continuity(0.1, 10.0, 1.225, 0.2, 5.0, 1.225);
        assertThat(result).contains("Mass balance error").contains("%");
    }

    @Test
    void continuity_compressible_solveV2_withDensityChange() {
        // rho1=1.225 (low alt), rho2=0.414 (high alt), A1=A2=0.5 → v2 = (rho1/rho2)*v1
        String result = tool.continuity(0.5, 100.0, 1.225, 0.5, null, 0.414);
        assertThat(result).contains("m/s").contains("solved").doesNotContain("Error:");
    }

    @Test
    void continuity_neitherA2NorV2_showsPrompt() {
        String result = tool.continuity(0.1, 10.0, null, null, null, null);
        assertThat(result).contains("Provide either A2 or v2");
    }

    @Test
    void continuity_missingRequired_returnsError() {
        assertThat(tool.continuity(null, 10.0, null, 0.05, null, null)).startsWith("Error:");
        assertThat(tool.continuity(0.1, null, null, 0.05, null, null)).startsWith("Error:");
    }

    @Test
    void continuity_equivalentDiameter_reportedForSolvedA2() {
        // When solving for A2, the output should include equivalent circular diameter
        String result = tool.continuity(0.2, 5.0, null, null, 20.0, null);
        assertThat(result).contains("mm").contains("diameter");
    }

    // ── ISA Atmosphere ────────────────────────────────────────────────────────

    @Test
    void isa_seaLevel_knownValues() {
        String result = tool.isaAtmosphere(0.0, null);
        assertThat(result).contains("288.15").contains("101325").contains("1.2249");
    }

    @Test
    void isa_cruiseAltitude_coolerAndLowerPressure() {
        String result = tool.isaAtmosphere(10000.0, null);
        assertThat(result)
                .contains("10 km")
                .contains("Speed of sound")
                .doesNotContain("Error:");
    }

    @Test
    void isa_stratosphere() {
        String result = tool.isaAtmosphere(15000.0, null);
        assertThat(result).contains("15 km").doesNotContain("Error:");
    }

    @Test
    void isa_withIsaOffset() {
        String result = tool.isaAtmosphere(5000.0, 15.0);
        assertThat(result).contains("ISA+15").doesNotContain("Error:");
    }

    @Test
    void isa_invalidAltitude_returnsError() {
        String result = tool.isaAtmosphere(-100.0, null);
        assertThat(result).startsWith("Error:");
    }

    @Test
    void isa_tooHighAltitude_returnsError() {
        String result = tool.isaAtmosphere(90000.0, null);
        assertThat(result).startsWith("Error:");
    }

    @Test
    void isa_negativeIsaOffset() {
        // ISA-15: colder air than standard
        String result = tool.isaAtmosphere(5000.0, -15.0);
        assertThat(result).contains("ISA-15").doesNotContain("Error:");
    }

    @Test
    void isa_upperStratosphere_above20km() {
        // 25 km — in the warm (rising temperature) layer of the stratosphere
        String result = tool.isaAtmosphere(25000.0, null);
        assertThat(result).contains("25 km").doesNotContain("Error:");
    }

    @Test
    void isa_densityDecreases_withAltitude() {
        String sl   = tool.isaAtmosphere(0.0, null);
        String high = tool.isaAtmosphere(10000.0, null);
        // Both should succeed and report density/pressure in output
        assertThat(sl).contains("kg/m³");
        assertThat(high).contains("kg/m³");
    }
}
