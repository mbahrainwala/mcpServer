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
    void physicsConstants_all_containsAllCategories() {
        String result = tool.physicsConstants("all");
        assertThat(result)
                .contains("MECHANICS & RELATIVITY")
                .contains("ELECTROMAGNETISM")
                .contains("QUANTUM & ATOMIC")
                .contains("THERMODYNAMICS")
                .contains("Speed of light");
    }

    @Test
    void physicsConstants_mechanics() {
        String result = tool.physicsConstants("mechanics");
        assertThat(result).contains("MECHANICS & RELATIVITY").contains("Gravitational constant");
    }

    @Test
    void physicsConstants_gravit() {
        String result = tool.physicsConstants("gravit");
        assertThat(result).contains("MECHANICS & RELATIVITY");
    }

    @Test
    void physicsConstants_speed() {
        String result = tool.physicsConstants("speed");
        assertThat(result).contains("Speed of light");
    }

    @Test
    void physicsConstants_light() {
        String result = tool.physicsConstants("light");
        assertThat(result).contains("Speed of light");
    }

    @Test
    void physicsConstants_electromagnetism() {
        String result = tool.physicsConstants("electromagnetism");
        assertThat(result).contains("ELECTROMAGNETISM").contains("Elementary charge");
    }

    @Test
    void physicsConstants_charge() {
        String result = tool.physicsConstants("charge");
        assertThat(result).contains("ELECTROMAGNETISM");
    }

    @Test
    void physicsConstants_permit() {
        String result = tool.physicsConstants("permit");
        assertThat(result).contains("Vacuum permittivity");
    }

    @Test
    void physicsConstants_coulomb() {
        String result = tool.physicsConstants("coulomb");
        assertThat(result).contains("Coulomb's constant");
    }

    @Test
    void physicsConstants_quantum() {
        String result = tool.physicsConstants("quantum");
        assertThat(result).contains("QUANTUM & ATOMIC").contains("Planck constant");
    }

    @Test
    void physicsConstants_planck() {
        String result = tool.physicsConstants("planck");
        assertThat(result).contains("QUANTUM & ATOMIC");
    }

    @Test
    void physicsConstants_electron() {
        String result = tool.physicsConstants("electron");
        assertThat(result).contains("Electron mass");
    }

    @Test
    void physicsConstants_proton() {
        String result = tool.physicsConstants("proton");
        assertThat(result).contains("Proton mass");
    }

    @Test
    void physicsConstants_thermodynamics() {
        String result = tool.physicsConstants("thermodynamics");
        assertThat(result).contains("THERMODYNAMICS").contains("Boltzmann constant");
    }

    @Test
    void physicsConstants_boltzmann() {
        String result = tool.physicsConstants("boltzmann");
        assertThat(result).contains("Boltzmann constant");
    }

    @Test
    void physicsConstants_stefan() {
        String result = tool.physicsConstants("stefan");
        assertThat(result).contains("Stefan");
    }

    @Test
    void physicsConstants_gas() {
        String result = tool.physicsConstants("gas");
        assertThat(result).contains("Ideal gas constant");
    }

    @Test
    void physicsConstants_unknownQuery_noMatch() {
        String result = tool.physicsConstants("xyzzy_not_a_constant");
        assertThat(result).contains("No constants matched");
    }

    // ── kinematics ───────────────────────────────────────────────────────────

    @Test
    void kinematics_solveForV_givenUAT() {
        // v = u + at = 0 + 10*5 = 50
        String result = tool.kinematics(null, 0.0, null, 10.0, 5.0);
        assertThat(result).contains("50").contains("SUVAT");
    }

    @Test
    void kinematics_solveForU_givenVAT() {
        // u = v - at = 50 - 10*5 = 0
        String result = tool.kinematics(null, null, 50.0, 10.0, 5.0);
        assertThat(result).contains("u = 0");
    }

    @Test
    void kinematics_solveForA_givenUVT() {
        // a = (v - u)/t = (20 - 0)/4 = 5
        String result = tool.kinematics(null, 0.0, 20.0, null, 4.0);
        assertThat(result).contains("a = 5");
    }

    @Test
    void kinematics_solveForT_givenUVA() {
        // t = (v - u)/a = (20 - 0)/4 = 5
        String result = tool.kinematics(null, 0.0, 20.0, 4.0, null);
        assertThat(result).contains("t = 5");
    }

    @Test
    void kinematics_solveForS_givenUAT() {
        // s = ut + 0.5*a*t^2 = 0*5 + 0.5*10*25 = 125
        String result = tool.kinematics(null, 0.0, null, 10.0, 5.0);
        assertThat(result).contains("125");
    }

    @Test
    void kinematics_solveForU_givenSAT() {
        // u = (s - 0.5*a*t^2)/t = (125 - 0.5*10*25)/5 = 0
        String result = tool.kinematics(125.0, null, null, 10.0, 5.0);
        assertThat(result).contains("u = 0");
    }

    @Test
    void kinematics_solveForV_givenUAS() {
        // v^2 = u^2 + 2as = 0 + 2*10*125 = 2500, v=50
        String result = tool.kinematics(125.0, 0.0, null, 10.0, null);
        assertThat(result).contains("50");
    }

    @Test
    void kinematics_solveForS_givenUVA() {
        // s = (v^2 - u^2) / (2a) = (2500 - 0) / 20 = 125
        String result = tool.kinematics(null, 0.0, 50.0, 10.0, null);
        assertThat(result).contains("125");
    }

    @Test
    void kinematics_solveForT_givenUVS() {
        // t = 2s/(u+v) = 2*125/(0+50) = 5
        String result = tool.kinematics(125.0, 0.0, 50.0, null, null);
        assertThat(result).contains("5");
    }

    @Test
    void kinematics_solveForS_givenUVT() {
        // s = 0.5*(u+v)*t = 0.5*(0+50)*5 = 125
        String result = tool.kinematics(null, 0.0, 50.0, null, 5.0);
        assertThat(result).contains("125");
    }

    @Test
    void kinematics_tooFewKnowns_returnsError() {
        String result = tool.kinematics(null, null, null, null, 5.0);
        assertThat(result).contains("Error: Need at least 3 known variables");
    }

    @Test
    void kinematics_allFiveKnown() {
        String result = tool.kinematics(125.0, 0.0, 50.0, 10.0, 5.0);
        assertThat(result).contains("SOLVED:");
    }

    @Test
    void kinematics_negativeV2_givenDeceleration() {
        // u=0, a=-10, s=5 => v^2 = 0 + 2*(-10)*5 = -100 => v = -sqrt(100)
        String result = tool.kinematics(5.0, 0.0, null, -10.0, null);
        assertThat(result).contains("v =");
    }

    // ── forces ───────────────────────────────────────────────────────────────

    @Test
    void forces_newton_Fma() {
        String result = tool.forces("newton", "5,3");
        assertThat(result).contains("15").contains("Newton's Second Law");
    }

    @Test
    void forces_gravity() {
        String result = tool.forces("gravity", "5.972e24,7.342e22,3.844e8");
        assertThat(result).contains("Gravitation");
    }

    @Test
    void forces_weight_defaultG() {
        String result = tool.forces("weight", "10");
        assertThat(result).contains("Weight").contains("9.80665");
    }

    @Test
    void forces_weight_customG() {
        String result = tool.forces("weight", "10,1.62");
        assertThat(result).contains("16.2");
    }

    @Test
    void forces_friction() {
        String result = tool.forces("friction", "0.5,100");
        assertThat(result).contains("50").contains("Friction");
    }

    @Test
    void forces_centripetal() {
        // F = mv^2/r = 2*100/5 = 40
        String result = tool.forces("centripetal", "2,10,5");
        assertThat(result).contains("40").contains("Centripetal");
    }

    @Test
    void forces_spring() {
        // F = -kx = -200*0.05 = -10, PE = 0.5*200*0.0025 = 0.25
        String result = tool.forces("spring", "200,0.05");
        assertThat(result).contains("-10").contains("Hooke");
    }

    @Test
    void forces_work() {
        // W = Fd cos(0) = 100*5*1 = 500
        String result = tool.forces("work", "100,5,0");
        assertThat(result).contains("500").contains("Work");
    }

    @Test
    void forces_kineticEnergy() {
        // KE = 0.5*2*100 = 100
        String result = tool.forces("kinetic_energy", "2,10");
        assertThat(result).contains("100").contains("Kinetic Energy");
    }

    @Test
    void forces_ke_alias() {
        String result = tool.forces("ke", "2,10");
        assertThat(result).contains("100").contains("Kinetic Energy");
    }

    @Test
    void forces_potentialEnergy_defaultG() {
        // PE = mgh = 5*9.80665*10
        String result = tool.forces("potential_energy", "5,10");
        assertThat(result).contains("Potential Energy");
    }

    @Test
    void forces_potentialEnergy_customG() {
        // PE = mgh = 5*1.62*10 = 81
        String result = tool.forces("pe", "5,10,1.62");
        assertThat(result).contains("81").contains("Potential Energy");
    }

    @Test
    void forces_momentum() {
        String result = tool.forces("momentum", "3,4");
        assertThat(result).contains("12").contains("Momentum");
    }

    @Test
    void forces_impulse() {
        String result = tool.forces("impulse", "10,3");
        assertThat(result).contains("30").contains("Impulse");
    }

    @Test
    void forces_power() {
        String result = tool.forces("power", "100,5");
        assertThat(result).contains("500").contains("Power");
    }

    @Test
    void forces_unknownFormula() {
        String result = tool.forces("telekinesis", "1,2");
        assertThat(result).contains("Unknown formula");
    }

    @Test
    void forces_tooFewValues_returnsError() {
        String result = tool.forces("newton", "5");
        assertThat(result).contains("Error:");
    }

    // ── waves ────────────────────────────────────────────────────────────────

    @Test
    void waves_waveSpeed() {
        // v = f*lambda = 440*0.78 = 343.2
        String result = tool.waves("wave_speed", "440,0.78");
        assertThat(result).contains("Wave Speed").contains("343.2");
    }

    @Test
    void waves_photonEnergy_fromFrequency() {
        String result = tool.waves("photon_energy", "6e14");
        assertThat(result).contains("Photon Energy").contains("eV");
    }

    @Test
    void waves_photonEnergy_fromWavelength() {
        String result = tool.waves("photon_energy", "5e-7");
        assertThat(result).contains("Photon Energy");
    }

    @Test
    void waves_deBroglie() {
        String result = tool.waves("de_broglie", "9.109e-31,1e6");
        assertThat(result).contains("de Broglie");
    }

    @Test
    void waves_doppler() {
        String result = tool.waves("doppler", "440,343,30,0");
        assertThat(result).contains("Doppler").contains("Approaching").contains("Receding");
    }

    @Test
    void waves_snellsLaw_normal() {
        String result = tool.waves("snells_law", "1,1.5,30");
        assertThat(result).contains("Snell's Law");
    }

    @Test
    void waves_snell_alias() {
        String result = tool.waves("snell", "1,1.5,30");
        assertThat(result).contains("Snell's Law");
    }

    @Test
    void waves_snellsLaw_totalInternalReflection() {
        String result = tool.waves("snells_law", "1.5,1,60");
        assertThat(result).contains("Total Internal Reflection").contains("Critical angle");
    }

    @Test
    void waves_thinLens() {
        // f=0.1, do=0.2 => di = 1/(1/0.1 - 1/0.2) = 1/(10-5) = 0.2
        String result = tool.waves("thin_lens", "0.1,0.2");
        assertThat(result).contains("Thin Lens").contains("Image distance");
    }

    @Test
    void waves_thinLens_virtualImage() {
        // f=0.2, do=0.1 => di = 1/(1/0.2 - 1/0.1) = 1/(5-10) = -0.2 (virtual)
        String result = tool.waves("thin_lens", "0.2,0.1");
        assertThat(result).contains("Virtual");
    }

    @Test
    void waves_unknownFormula() {
        String result = tool.waves("telepathy", "1,2");
        assertThat(result).contains("Unknown formula");
    }

    @Test
    void waves_tooFewValues_returnsError() {
        String result = tool.waves("doppler", "440");
        assertThat(result).contains("Error:");
    }

    // ── electricity ──────────────────────────────────────────────────────────

    @Test
    void electricity_ohm() {
        String result = tool.electricity("ohm", "12,4");
        assertThat(result).contains("Ohm's Law").contains("3");
    }

    @Test
    void electricity_power() {
        // P = IV = 2*5 = 10
        String result = tool.electricity("power", "2,5");
        assertThat(result).contains("10").contains("Electrical Power");
    }

    @Test
    void electricity_resistorsSeries() {
        String result = tool.electricity("resistors_series", "10,20,30");
        assertThat(result).contains("60").contains("Series");
    }

    @Test
    void electricity_resistorsParallel() {
        // 1/R = 1/10 + 1/10 = 0.2, R = 5
        String result = tool.electricity("resistors_parallel", "10,10");
        assertThat(result).contains("5").contains("Parallel");
    }

    @Test
    void electricity_coulomb() {
        String result = tool.electricity("coulomb", "1e-6,2e-6,0.1");
        assertThat(result).contains("Coulomb");
    }

    @Test
    void electricity_electricField() {
        String result = tool.electricity("electric_field", "1e-6,0.1");
        assertThat(result).contains("Electric Field");
    }

    @Test
    void electricity_capacitance() {
        // C = Q/V = 0.001/5 = 0.0002
        String result = tool.electricity("capacitance", "0.001,5");
        assertThat(result).contains("Capacitance");
    }

    @Test
    void electricity_capacitorsSeries() {
        String result = tool.electricity("capacitors_series", "1e-6,2e-6");
        assertThat(result).contains("Capacitors in Series");
    }

    @Test
    void electricity_capacitorsParallel() {
        String result = tool.electricity("capacitors_parallel", "1e-6,2e-6");
        assertThat(result).contains("Capacitors in Parallel");
    }

    @Test
    void electricity_rcCircuit() {
        // tau = RC = 1000*0.001 = 1
        String result = tool.electricity("rc_circuit", "1000,0.001");
        assertThat(result).contains("RC Circuit");
    }

    @Test
    void electricity_rlCircuit() {
        // tau = L/R = 0.1/100 = 0.001
        String result = tool.electricity("rl_circuit", "0.1,100");
        assertThat(result).contains("RL Circuit");
    }

    @Test
    void electricity_energyCapacitor() {
        // U = 0.5*C*V^2 = 0.5*0.001*100 = 0.05
        String result = tool.electricity("energy_capacitor", "0.001,10");
        assertThat(result).contains("Energy Stored");
    }

    @Test
    void electricity_unknownFormula() {
        String result = tool.electricity("magic", "1,2");
        assertThat(result).contains("Unknown formula");
    }

    @Test
    void electricity_tooFewValues_returnsError() {
        String result = tool.electricity("ohm", "5");
        assertThat(result).contains("Error:");
    }

    // ── thermodynamics ───────────────────────────────────────────────────────

    @Test
    void thermodynamics_idealGas() {
        // PV = nRT, given P=101325, V=0.0224, T=273.15 => n ~= 1
        String result = tool.thermodynamics("ideal_gas", "101325,0.0224,273.15");
        assertThat(result).contains("Ideal Gas Law");
    }

    @Test
    void thermodynamics_idealGas_tooFewValues() {
        String result = tool.thermodynamics("ideal_gas", "101325,0.0224");
        assertThat(result).contains("Error:");
    }

    @Test
    void thermodynamics_heatTransfer() {
        // Q = mcDT = 1*4186*10 = 41860
        String result = tool.thermodynamics("heat_transfer", "1,4186,10");
        assertThat(result).contains("41860").contains("Heat Transfer");
    }

    @Test
    void thermodynamics_thermalExpansion() {
        String result = tool.thermodynamics("thermal_expansion", "1.2e-5,1,100");
        assertThat(result).contains("Thermal Expansion");
    }

    @Test
    void thermodynamics_carnot() {
        // eta = 1 - Tc/Th = 1 - 300/600 = 0.5 = 50%
        String result = tool.thermodynamics("carnot", "300,600");
        assertThat(result).contains("50").contains("Carnot");
    }

    @Test
    void thermodynamics_stefanBoltzmann() {
        String result = tool.thermodynamics("stefan_boltzmann", "1,1,5778");
        assertThat(result).contains("Stefan");
    }

    @Test
    void thermodynamics_entropy() {
        // DS = Q/T = 1000/300
        String result = tool.thermodynamics("entropy", "1000,300");
        assertThat(result).contains("Entropy");
    }

    @Test
    void thermodynamics_latentHeat() {
        // Q = mL = 2*334000 = 668000
        String result = tool.thermodynamics("latent_heat", "2,334000");
        assertThat(result).contains("668000").contains("Latent Heat");
    }

    @Test
    void thermodynamics_unknownFormula() {
        String result = tool.thermodynamics("alchemy", "1,2");
        assertThat(result).contains("Unknown formula");
    }

    @Test
    void thermodynamics_tooFewValues_returnsError() {
        String result = tool.thermodynamics("heat_transfer", "1,2");
        assertThat(result).contains("Error:");
    }

    // ── fmt edge cases ───────────────────────────────────────────────────────

    @Test
    void forces_worksWithNaN_inOutput() {
        // Divide by zero: centripetal with radius 0
        // This would produce NaN or Infinity
        String result = tool.forces("centripetal", "1,1,0");
        assertThat(result).isNotBlank();
    }
}
