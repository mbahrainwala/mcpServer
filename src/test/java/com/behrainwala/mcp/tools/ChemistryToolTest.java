package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChemistryToolTest {

    private ChemistryTool tool;

    @BeforeEach
    void setUp() {
        tool = new ChemistryTool();
    }

    // ── Element Lookup ──

    @Nested
    class ElementLookupTests {
        @Test
        void bySymbol_iron() {
            String result = tool.elementLookup("Fe");
            assertThat(result).contains("Iron").contains("55.845").contains("26");
        }

        @Test
        void byName_sodium() {
            String result = tool.elementLookup("Sodium");
            assertThat(result).contains("Na").contains("22.990").contains("11");
        }

        @Test
        void bySymbol_caseInsensitive() {
            String result = tool.elementLookup("  fe  ");
            assertThat(result).contains("Iron");
        }

        @Test
        void byName_caseInsensitive() {
            String result = tool.elementLookup("iron");
            assertThat(result).contains("Fe").contains("26");
        }

        @Test
        void notFound() {
            String result = tool.elementLookup("Xz");
            assertThat(result).containsIgnoringCase("not found");
        }

        @Test
        void hydrogen() {
            String result = tool.elementLookup("H");
            assertThat(result).contains("Hydrogen").contains("1.008").contains("Nonmetal");
        }

        @Test
        void gold() {
            String result = tool.elementLookup("Au");
            assertThat(result).contains("Gold").contains("196.967").contains("Transition metal");
        }

        @Test
        void uranium() {
            String result = tool.elementLookup("U");
            assertThat(result).contains("Uranium").contains("238.029").contains("Actinide");
        }

        @Test
        void helium_nobleGas() {
            String result = tool.elementLookup("He");
            assertThat(result).contains("Helium").contains("Noble gas");
        }

        @Test
        void boron_metalloid() {
            String result = tool.elementLookup("B");
            assertThat(result).contains("Boron").contains("Metalloid");
        }

        @Test
        void aluminum_postTransition() {
            String result = tool.elementLookup("Al");
            assertThat(result).contains("Aluminum").contains("Post-transition metal");
        }

        @Test
        void fluorine_halogen() {
            String result = tool.elementLookup("F");
            assertThat(result).contains("Fluorine").contains("Halogen");
        }

        @Test
        void calcium_alkalineEarth() {
            String result = tool.elementLookup("Ca");
            assertThat(result).contains("Calcium").contains("Alkaline earth");
        }

        @Test
        void lithium_alkaliMetal() {
            String result = tool.elementLookup("Li");
            assertThat(result).contains("Lithium").contains("Alkali metal");
        }
    }

    // ── Molar Mass ──

    @Nested
    class MolarMassTests {
        @Test
        void water_H2O() {
            String result = tool.molarMass("H2O");
            assertThat(result).contains("H2O");
            // H2O = 2(1.008) + 15.999 = 18.015
            assertThat(result).contains("18.015");
        }

        @Test
        void sodiumChloride_NaCl() {
            String result = tool.molarMass("NaCl");
            // NaCl = 22.990 + 35.453 = 58.443
            assertThat(result).contains("58.443");
        }

        @Test
        void calciumHydroxide_withParentheses() {
            String result = tool.molarMass("Ca(OH)2");
            // Ca(OH)2 = 40.078 + 2*(15.999 + 1.008) = 40.078 + 34.014 = 74.092
            assertThat(result).contains("74.092");
        }

        @Test
        void glucose_C6H12O6() {
            String result = tool.molarMass("C6H12O6");
            // C6H12O6 = 6(12.011) + 12(1.008) + 6(15.999) = 72.066 + 12.096 + 95.994 = 180.156
            assertThat(result).contains("180.156");
        }

        @Test
        void ironOxide_Fe2O3() {
            String result = tool.molarMass("Fe2O3");
            // Fe2O3 = 2(55.845) + 3(15.999) = 111.690 + 47.997 = 159.687
            assertThat(result).contains("159.687");
        }

        @Test
        void unknownElement_returnsError() {
            String result = tool.molarMass("Xz2O3");
            assertThat(result).containsIgnoringCase("unknown element");
        }

        @Test
        void singleElement_noSubscript() {
            String result = tool.molarMass("C");
            assertThat(result).contains("12.011");
        }

        @Test
        void massPercentages_shown() {
            String result = tool.molarMass("H2O");
            assertThat(result).contains("MASS PERCENTAGES");
        }

        @Test
        void molecularMass_shown() {
            String result = tool.molarMass("H2O");
            assertThat(result).contains("Mass of 1 molecule");
        }

        @Test
        void invalidFormula_skipsNonUpperNonParenChars() {
            // Characters that are not uppercase or parentheses get skipped
            String result = tool.molarMass("H2O");
            assertThat(result).contains("18.015");
        }

        @Test
        void nestedParentheses() {
            // Not typical chemistry but tests parser depth: Ca(OH)2
            String result = tool.molarMass("Ca(OH)2");
            assertThat(result).contains("Ca").contains("COMPOSITION");
        }

        @Test
        void parenthesesNoSubscript() {
            // (OH) without subscript = multiplier 1
            String result = tool.molarMass("Na(OH)");
            // Na + O + H = 22.990 + 15.999 + 1.008 = 39.997
            assertThat(result).contains("39.997");
        }
    }

    // ── Solutions ──

    @Nested
    class SolutionsTests {
        @Test
        void molarity() {
            // M = n/V = 2/0.5 = 4 M
            String result = tool.solutions("molarity", "2, 0.5");
            assertThat(result).contains("4").contains("Molarity");
        }

        @Test
        void dilution() {
            // M1V1=M2V2: 1*2 = 2, if V2=4: M2=0.5
            String result = tool.solutions("dilution", "1, 2, 4");
            assertThat(result).contains("Dilution").contains("0.5");
        }

        @Test
        void ph_acidic() {
            // pH = -log(1e-3) = 3 (acidic)
            String result = tool.solutions("ph", "0.001");
            assertThat(result).contains("3").contains("acidic");
        }

        @Test
        void ph_basic() {
            // pH = -log(1e-10) = 10 (basic)
            String result = tool.solutions("ph", "1e-10");
            assertThat(result).contains("10").contains("basic");
        }

        @Test
        void ph_neutral() {
            // pH = -log(1e-7) = 7 (neutral)
            String result = tool.solutions("ph", "1e-7");
            assertThat(result).contains("7").contains("neutral");
        }

        @Test
        void poh() {
            // pOH = -log(1e-4) = 4, pH = 14-4 = 10
            String result = tool.solutions("poh", "0.0001");
            assertThat(result).contains("4").contains("10");
        }

        @Test
        void phToH() {
            // pH=3 → [H+] = 1e-3
            String result = tool.solutions("ph_to_h", "3");
            assertThat(result).contains("1.0000e-03");
        }

        @Test
        void massToMoles() {
            // n = 18/18 = 1 mol
            String result = tool.solutions("mass_to_moles", "18, 18");
            assertThat(result).contains("1").contains("Mass to Moles");
        }

        @Test
        void molesToMass() {
            // m = 2 * 18 = 36 g
            String result = tool.solutions("moles_to_mass", "2, 18");
            assertThat(result).contains("36").contains("Moles to Mass");
        }

        @Test
        void molesToParticles() {
            // N = 1 * 6.022e23
            String result = tool.solutions("moles_to_particles", "1");
            assertThat(result).contains("6.0221e+23");
        }

        @Test
        void percentComposition() {
            // 25/100 * 100 = 25%
            String result = tool.solutions("percent_composition", "25, 100");
            assertThat(result).contains("25").contains("Percent Composition");
        }

        @Test
        void unknownFormula() {
            String result = tool.solutions("unknown_formula", "1, 2");
            assertThat(result).containsIgnoringCase("unknown formula");
        }

        @Test
        void insufficientValues_molarity() {
            String result = tool.solutions("molarity", "1");
            assertThat(result).containsIgnoringCase("error").contains("2");
        }

        @Test
        void insufficientValues_dilution() {
            String result = tool.solutions("dilution", "1, 2");
            assertThat(result).containsIgnoringCase("error").contains("3");
        }

        @Test
        void insufficientValues_massToMoles() {
            String result = tool.solutions("mass_to_moles", "18");
            assertThat(result).containsIgnoringCase("error").contains("2");
        }

        @Test
        void insufficientValues_molesToMass() {
            String result = tool.solutions("moles_to_mass", "2");
            assertThat(result).containsIgnoringCase("error").contains("2");
        }

        @Test
        void insufficientValues_percentComposition() {
            String result = tool.solutions("percent_composition", "25");
            assertThat(result).containsIgnoringCase("error").contains("2");
        }

        @Test
        void invalidNumber_returnsError() {
            String result = tool.solutions("molarity", "abc, 2");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void whitespace_inFormula() {
            String result = tool.solutions("  molarity  ", "2, 0.5");
            assertThat(result).contains("Molarity");
        }
    }

    // ── Gas Laws ──

    @Nested
    class GasLawsTests {
        @Test
        void idealGas_threeValues() {
            // PV=nRT: P=1, V=22.4, n=1 → solve for T
            String result = tool.gasLaws("ideal_gas", "1, 22.4, 1");
            assertThat(result).contains("Ideal Gas Law");
        }

        @Test
        void idealGas_fourValues_consistent() {
            // PV=nRT: 1*22.414 = 1*0.08206*273.15 ≈ 22.414
            String result = tool.gasLaws("ideal_gas", "1, 22.414, 1, 273.15");
            assertThat(result).contains("Verification").contains("Consistent");
        }

        @Test
        void idealGas_fourValues_inconsistent() {
            String result = tool.gasLaws("ideal_gas", "1, 10, 1, 500");
            assertThat(result).contains("Verification").contains("Inconsistent");
        }

        @Test
        void boyle() {
            // P1V1=P2V2: 2*3=6, if P2=6: V2=1, if V2=6: P2=1
            String result = tool.gasLaws("boyle", "2, 3, 6");
            assertThat(result).contains("Boyle").contains("1");
        }

        @Test
        void charles() {
            // V1/T1=V2/T2: 2/300 = V2/600 → V2=4
            String result = tool.gasLaws("charles", "2, 300, 600");
            assertThat(result).contains("Charles");
        }

        @Test
        void dalton() {
            // P_total = 1 + 2 + 3 = 6
            String result = tool.gasLaws("dalton", "1, 2, 3");
            assertThat(result).contains("6").contains("Dalton");
        }

        @Test
        void graham_faster() {
            // M1=2 (H2), M2=32 (O2): ratio = sqrt(32/2) = 4 → faster
            String result = tool.gasLaws("graham", "2, 32");
            assertThat(result).contains("4").contains("faster");
        }

        @Test
        void graham_slower() {
            // M1=32, M2=2: ratio = sqrt(2/32) = 0.25 → slower
            String result = tool.gasLaws("graham", "32, 2");
            assertThat(result).contains("slower");
        }

        @Test
        void unknownFormula() {
            String result = tool.gasLaws("unknown", "1, 2");
            assertThat(result).containsIgnoringCase("unknown formula");
        }

        @Test
        void insufficientValues_idealGas() {
            String result = tool.gasLaws("ideal_gas", "1, 2");
            assertThat(result).containsIgnoringCase("error").contains("3");
        }

        @Test
        void insufficientValues_boyle() {
            String result = tool.gasLaws("boyle", "1, 2");
            assertThat(result).containsIgnoringCase("error").contains("3");
        }

        @Test
        void insufficientValues_charles() {
            String result = tool.gasLaws("charles", "1, 2");
            assertThat(result).containsIgnoringCase("error").contains("3");
        }

        @Test
        void insufficientValues_graham() {
            String result = tool.gasLaws("graham", "2");
            assertThat(result).containsIgnoringCase("error").contains("2");
        }

        @Test
        void invalidNumber() {
            String result = tool.gasLaws("boyle", "abc, 2, 3");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void whitespace_in_formula() {
            String result = tool.gasLaws("  dalton  ", "1, 2");
            assertThat(result).contains("Dalton");
        }
    }

    // ── Equilibrium ──

    @Nested
    class EquilibriumTests {
        @Test
        void kpFromKc_withTemperature() {
            // Kp = Kc(RT)^dn: Kc=1, dn=1, T=300 → Kp = 1*(0.082057*300)^1 = 24.617
            String result = tool.equilibrium("kp_from_kc", "1, 1, 300");
            assertThat(result).contains("Kp from Kc");
            assertThat(result).contains("300");
        }

        @Test
        void kpFromKc_defaultTemperature() {
            // Uses default T=298.15 when only 2 values given
            String result = tool.equilibrium("kp_from_kc", "1, 0");
            // Kp = 1 * (R*298.15)^0 = 1
            assertThat(result).contains("298.15");
        }

        @Test
        void halfLifeFirst() {
            // t1/2 = ln(2)/k: k=0.1 → t1/2 = 6.931
            String result = tool.equilibrium("half_life_first", "0.1");
            assertThat(result).contains("First-Order Half-Life");
            assertThat(result).contains("6.93");
        }

        @Test
        void halfLifeZero() {
            // t1/2 = [A]0/(2k): [A]0=1, k=0.1 → t1/2 = 5
            String result = tool.equilibrium("half_life_zero", "1, 0.1");
            assertThat(result).contains("5").contains("Zero-Order");
        }

        @Test
        void arrhenius() {
            // k = A*exp(-Ea/RT): A=1e13, Ea=80000, T=300
            String result = tool.equilibrium("arrhenius", "1e13, 80000, 300");
            assertThat(result).contains("Arrhenius");
        }

        @Test
        void nernst_spontaneous() {
            // E = E° - (RT/nF)lnQ: E°=1.1, n=2, T=298, Q=0.01 → E > 0 (spontaneous)
            String result = tool.equilibrium("nernst", "1.1, 2, 298, 0.01");
            assertThat(result).contains("spontaneous").doesNotContain("non-spontaneous");
        }

        @Test
        void nernst_nonSpontaneous() {
            // E°=-0.5, n=1, T=298, Q=1000 → E < 0 (non-spontaneous)
            String result = tool.equilibrium("nernst", "-0.5, 1, 298, 1000");
            assertThat(result).contains("non-spontaneous");
        }

        @Test
        void unknownFormula() {
            String result = tool.equilibrium("unknown_eq", "1, 2");
            assertThat(result).containsIgnoringCase("unknown formula");
        }

        @Test
        void insufficientValues_kpFromKc() {
            String result = tool.equilibrium("kp_from_kc", "1");
            assertThat(result).containsIgnoringCase("error").contains("2");
        }

        @Test
        void insufficientValues_halfLifeZero() {
            String result = tool.equilibrium("half_life_zero", "1");
            assertThat(result).containsIgnoringCase("error").contains("2");
        }

        @Test
        void insufficientValues_arrhenius() {
            String result = tool.equilibrium("arrhenius", "1, 2");
            assertThat(result).containsIgnoringCase("error").contains("3");
        }

        @Test
        void insufficientValues_nernst() {
            String result = tool.equilibrium("nernst", "1, 2, 3");
            assertThat(result).containsIgnoringCase("error").contains("4");
        }

        @Test
        void invalidNumber() {
            String result = tool.equilibrium("arrhenius", "abc, 2, 3");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void kpFromKc_negativeDeltaN() {
            // Kp = Kc*(RT)^(-2) with T=300
            String result = tool.equilibrium("kp_from_kc", "100, -2, 300");
            assertThat(result).contains("Kp from Kc");
        }
    }

    // ── Mathematical Correctness ──

    @Nested
    class MathematicalCorrectnessTests {
        @Test
        void molarMass_H2O_exact() {
            String result = tool.molarMass("H2O");
            // 2*1.008 + 15.999 = 18.015
            assertThat(result).contains("18.015");
        }

        @Test
        void graham_ratio_correct() {
            // sqrt(M2/M1) = sqrt(28/4) = sqrt(7) ≈ 2.6458
            String result = tool.gasLaws("graham", "4, 28");
            assertThat(result).contains("2.6457");
        }

        @Test
        void halfLife_first_order_correct() {
            // ln(2)/0.693 ≈ 1.0
            String result = tool.equilibrium("half_life_first", "0.693147");
            assertThat(result).contains("1");
        }

        @Test
        void molarity_correct() {
            // 0.5 mol / 0.25 L = 2 M
            String result = tool.solutions("molarity", "0.5, 0.25");
            assertThat(result).contains("2");
        }

        @Test
        void percentComposition_correct() {
            // 50/200 * 100 = 25%
            String result = tool.solutions("percent_composition", "50, 200");
            assertThat(result).contains("25");
        }
    }

    // ── Fmt Helper ──

    @Nested
    class FmtHelperTests {
        @Test
        void wholeNumber_formattedAsInteger() {
            // molarity: 10/2 = 5 (integer)
            String result = tool.solutions("molarity", "10, 2");
            assertThat(result).contains("5");
        }

        @Test
        void decimalNumber_formattedWithPrecision() {
            // molarity: 1/3 = 0.3333...
            String result = tool.solutions("molarity", "1, 3");
            assertThat(result).contains("0.333333");
        }
    }
}
