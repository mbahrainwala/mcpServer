package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhysicsToolTest {

    private PhysicsTool tool;

    @BeforeEach
    void setUp() {
        tool = new PhysicsTool();
    }

    // ── physicsConstants ─────────────────────────────────────────────────────

    @Test
    void physicsConstants_all_containsSpeedOfLight() {
        String result = tool.physicsConstants("all");
        assertThat(result).contains("2.997").containsIgnoringCase("speed of light");
    }

    @Test
    void physicsConstants_mechanics() {
        String result = tool.physicsConstants("mechanics");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("Gravity"), s -> assertThat(s).containsIgnoringCase("gravitational"));
    }

    @Test
    void physicsConstants_electromagnetism() {
        String result = tool.physicsConstants("electromagnetism");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("charge"), s -> assertThat(s).containsIgnoringCase("permittivity"));
    }

    @Test
    void physicsConstants_quantum() {
        String result = tool.physicsConstants("quantum");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("planck"), s -> assertThat(s).containsIgnoringCase("electron"));
    }

    @Test
    void physicsConstants_thermodynamics() {
        String result = tool.physicsConstants("thermodynamics");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("boltzmann"), s -> assertThat(s).containsIgnoringCase("gas"));
    }

    @Test
    void physicsConstants_unknown_query_shows_noMatch() {
        String result = tool.physicsConstants("xyzzy_not_a_constant");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("No constants matched"), s -> assertThat(s).containsIgnoringCase("Try"));
    }

    // ── kinematics ───────────────────────────────────────────────────────────

    @Test
    void kinematics_solveForVelocity() {
        // u=0, a=10, t=5 → v=50
        String result = tool.kinematics(null, 0.0, null, 10.0, 5.0);
        assertThat(result).contains("50").contains("SUVAT");
    }

    @Test
    void kinematics_tooFewKnowns() {
        String result = tool.kinematics(null, null, null, null, 5.0);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void kinematics_solveForDisplacement() {
        // u=0, v=10, a=2, t=5 → s = 25
        String result = tool.kinematics(null, 0.0, 10.0, 2.0, 5.0);
        assertThat(result).contains("s =").contains("SUVAT");
    }

    @Test
    void kinematics_solveForTime() {
        // u=0, v=20, a=4 → t=5
        String result = tool.kinematics(null, 0.0, 20.0, 4.0, null);
        assertThat(result).contains("5").contains("SUVAT");
    }

    // ── forces ───────────────────────────────────────────────────────────────

    @Test
    void forces_newton() {
        // F=ma, m=5, a=3 → F=15
        String result = tool.forces("newton", "5,3");
        assertThat(result).contains("15");
    }

    @Test
    void forces_gravity() {
        String result = tool.forces("gravity", "5.972e24,7.342e22,3.844e8");
        assertThat(result).containsIgnoringCase("gravitation");
    }

    @Test
    void forces_weight() {
        // m=10 → W=98.1
        String result = tool.forces("weight", "10");
        assertThat(result).containsIgnoringCase("Weight");
    }

    @Test
    void forces_weightWithCustomG() {
        String result = tool.forces("weight", "10,1.62");
        assertThat(result).contains("16.2");
    }

    @Test
    void forces_friction() {
        // μ=0.5, N=100 → f=50
        String result = tool.forces("friction", "0.5,100");
        assertThat(result).contains("50");
    }

    @Test
    void forces_centripetal() {
        String result = tool.forces("centripetal", "2,10,5");
        assertThat(result).contains("40").containsIgnoringCase("centripetal");
    }

    @Test
    void forces_spring() {
        // k=200, x=0.05 → F=-10
        String result = tool.forces("spring", "200,0.05");
        assertThat(result).containsIgnoringCase("spring");
    }

    @Test
    void forces_work() {
        // F=100, d=5, angle=0 → W=500
        String result = tool.forces("work", "100,5,0");
        assertThat(result).contains("500");
    }

    @Test
    void forces_kineticEnergy() {
        // m=2, v=10 → KE=100
        String result = tool.forces("kinetic_energy", "2,10");
        assertThat(result).contains("100");
    }

    @Test
    void forces_potentialEnergy() {
        // m=5, h=10 → PE = 5*9.81*10 ≈ 490.5
        String result = tool.forces("potential_energy", "5,10");
        assertThat(result).containsIgnoringCase("potential");
    }

    @Test
    void forces_momentum() {
        // m=3, v=4 → p=12
        String result = tool.forces("momentum", "3,4");
        assertThat(result).contains("12");
    }

    @Test
    void forces_impulse() {
        // F=10, t=3 → J=30
        String result = tool.forces("impulse", "10,3");
        assertThat(result).contains("30");
    }

    @Test
    void forces_power() {
        // W=100, t=5 → P=500
        String result = tool.forces("power", "100,5");
        assertThat(result).containsIgnoringCase("power");
    }

    @Test
    void forces_unknownFormula() {
        String result = tool.forces("telekinesis", "1,2");
        assertThat(result).containsIgnoringCase("unknown");
    }

    // ── waves ────────────────────────────────────────────────────────────────

    @Test
    void waves_waveSpeed() {
        // f=440, λ=0.78 → v≈343
        String result = tool.waves("wave_speed", "440,0.78");
        assertThat(result).containsIgnoringCase("wave");
    }

    @Test
    void waves_photonEnergy_fromFrequency() {
        // frequency ~6e14 Hz (visible light)
        String result = tool.waves("photon_energy", "6e14");
        assertThat(result).containsIgnoringCase("photon");
    }

    @Test
    void waves_photonEnergy_fromWavelength() {
        // wavelength ~500e-9 m
        String result = tool.waves("photon_energy", "5e-7");
        assertThat(result).containsIgnoringCase("photon");
    }

    @Test
    void waves_deBroglie() {
        String result = tool.waves("de_broglie", "9.109e-31,1e6");
        assertThat(result).containsIgnoringCase("broglie");
    }

    @Test
    void waves_doppler() {
        String result = tool.waves("doppler", "440,343,0,0");
        assertThat(result).containsIgnoringCase("doppler");
    }

    @Test
    void waves_snellsLaw() {
        // n1=1, n2=1.5, theta1=30 → theta2 ≈ 19.47
        String result = tool.waves("snells_law", "1,1.5,30");
        assertThat(result).containsIgnoringCase("snell");
    }

    @Test
    void waves_snellsLaw_totalInternalReflection() {
        // n1=1.5, n2=1, theta1=60 → total internal reflection
        String result = tool.waves("snells_law", "1.5,1,60");
        assertThat(result).containsIgnoringCase("total internal reflection");
    }

    @Test
    void waves_thinLens() {
        String result = tool.waves("thin_lens", "0.1,0.2");
        assertThat(result).containsIgnoringCase("lens");
    }

    @Test
    void waves_unknownFormula() {
        String result = tool.waves("telepathy", "1,2");
        assertThat(result).containsIgnoringCase("unknown");
    }

    // ── electricity ──────────────────────────────────────────────────────────

    @Test
    void electricity_ohm() {
        // V=12, R=4 → I=3
        String result = tool.electricity("ohm", "12,4");
        assertThat(result).containsIgnoringCase("ohm");
    }

    @Test
    void electricity_power() {
        String result = tool.electricity("power", "12,3");
        assertThat(result).containsIgnoringCase("power");
    }

    @Test
    void electricity_resistorsSeries() {
        String result = tool.electricity("resistors_series", "10,20,30");
        assertThat(result).contains("60");
    }

    @Test
    void electricity_resistorsParallel() {
        String result = tool.electricity("resistors_parallel", "10,10");
        assertThat(result).contains("5");
    }

    @Test
    void electricity_coulomb() {
        String result = tool.electricity("coulomb", "1e-6,2e-6,0.1");
        assertThat(result).containsIgnoringCase("coulomb");
    }

    @Test
    void electricity_unknownFormula() {
        String result = tool.electricity("magic", "1,2");
        assertThat(result).containsIgnoringCase("unknown");
    }
}
