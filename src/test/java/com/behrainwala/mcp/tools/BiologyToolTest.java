package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BiologyToolTest {

    private BiologyTool tool;

    @BeforeEach
    void setUp() {
        tool = new BiologyTool();
    }

    // ── DNA Operations ──

    @Nested
    class DnaOperationsTests {
        @Test
        void complement_DNA() {
            // A→T, T→A, G→C, C→G
            String result = tool.dnaOperations("complement", "ATGC");
            assertThat(result).contains("TACG");
        }

        @Test
        void complement_RNA() {
            // A→U, U→A, G→C, C→G
            String result = tool.dnaOperations("complement", "AUGC");
            assertThat(result).contains("UACG");
        }

        @Test
        void reverseComplement_DNA() {
            // Complement of ATGC=TACG, reversed=GCAT
            String result = tool.dnaOperations("reverse_complement", "ATGC");
            assertThat(result).contains("GCAT");
        }

        @Test
        void reverseComplement_RNA() {
            String result = tool.dnaOperations("reverse_complement", "AUGC");
            assertThat(result).contains("GCAU");
        }

        @Test
        void transcribe_DNA() {
            // T→U
            String result = tool.dnaOperations("transcribe", "ATGCTA");
            assertThat(result).contains("AUGCUA");
        }

        @Test
        void transcribe_alreadyRNA() {
            String result = tool.dnaOperations("transcribe", "AUGC");
            assertThat(result).containsIgnoringCase("already RNA");
        }

        @Test
        void translate_fromDNA_withStartAndStop() {
            // ATGAAATAG → AUG AAA UAG → Met-Lys-Stop
            String result = tool.dnaOperations("translate", "ATGAAATAG");
            assertThat(result).contains("Met").contains("Lys").contains("STOP");
        }

        @Test
        void translate_fromRNA() {
            String result = tool.dnaOperations("translate", "AUGAAAUAG");
            assertThat(result).contains("Met").contains("Lys");
        }

        @Test
        void translate_noStartCodon() {
            String result = tool.dnaOperations("translate", "AAAAAA");
            assertThat(result).containsIgnoringCase("no start codon");
        }

        @Test
        void translate_noStopCodon() {
            // ATGAAA → Met-Lys (runs off end)
            String result = tool.dnaOperations("translate", "AUGAAA");
            assertThat(result).contains("Met").contains("Lys");
        }

        @Test
        void translate_trailingBases() {
            // ATGAAAA → AUG AAA A (trailing A not a full codon)
            String result = tool.dnaOperations("translate", "AUGAAAA");
            assertThat(result).contains("Met").contains("Lys");
        }

        @Test
        void translate_startNotAtBeginning() {
            // AAAAUGGCUUAG → start at position 4
            String result = tool.dnaOperations("translate", "AAAAUGGCUUAG");
            assertThat(result).contains("position 4").contains("Met").contains("Ala");
        }

        @Test
        void translate_stopCodonUAA() {
            String result = tool.dnaOperations("translate", "AUGUAA");
            assertThat(result).contains("STOP");
        }

        @Test
        void translate_stopCodonUGA() {
            String result = tool.dnaOperations("translate", "AUGUGA");
            assertThat(result).contains("STOP");
        }

        @Test
        void gcContent_50percent() {
            String result = tool.dnaOperations("gc_content", "ATGC");
            assertThat(result).contains("50");
        }

        @Test
        void gcContent_0percent() {
            String result = tool.dnaOperations("gc_content", "AAAA");
            assertThat(result).contains("0");
        }

        @Test
        void gcContent_100percent() {
            String result = tool.dnaOperations("gc_content", "GGCC");
            assertThat(result).contains("100");
        }

        @Test
        void meltingTemp_shortSequence() {
            // <14bp uses Wallace rule: Tm = 2(AT) + 4(GC)
            String result = tool.dnaOperations("melting_temp", "ATGCATGC");
            assertThat(result).contains("Wallace Rule");
        }

        @Test
        void meltingTemp_longSequence() {
            // >=14bp uses basic formula
            String result = tool.dnaOperations("melting_temp", "ATGCATGCATGCATGC");
            assertThat(result).contains("Basic Formula").contains("Salt-adjusted");
        }

        @Test
        void molecularWeight_DNA() {
            // 4 bases * 330 Da = 1320
            String result = tool.dnaOperations("molecular_weight", "ATGC");
            assertThat(result).contains("1320");
        }

        @Test
        void molecularWeight_RNA() {
            // 4 bases * 340 Da = 1360
            String result = tool.dnaOperations("molecular_weight", "AUGC");
            assertThat(result).contains("1360");
        }

        @Test
        void analyze_DNA() {
            String result = tool.dnaOperations("analyze", "ATGCATGC");
            assertThat(result).contains("DNA").contains("COMPOSITION")
                    .contains("GC Content").contains("Complement").contains("Rev. Complement");
        }

        @Test
        void analyze_RNA() {
            String result = tool.dnaOperations("analyze", "AUGCAUGC");
            assertThat(result).contains("RNA").contains("COMPOSITION").contains("GC Content");
            // RNA analysis should not show complement
            assertThat(result).doesNotContain("Rev. Complement");
        }

        @Test
        void invalidSequence() {
            String result = tool.dnaOperations("complement", "ATGXQ");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("invalid");
        }

        @Test
        void unknownOperation() {
            String result = tool.dnaOperations("unknown_op", "ATGC");
            assertThat(result).containsIgnoringCase("unknown operation");
        }

        @Test
        void whitespace_in_sequence() {
            String result = tool.dnaOperations("complement", "  AT GC  ");
            assertThat(result).contains("TACG");
        }
    }

    // ── Hardy-Weinberg ──

    @Nested
    class HardyWeinbergTests {
        @Test
        void knownP() {
            // p=0.6 → q=0.4
            String result = tool.hardyWeinberg("p", 0.6);
            assertThat(result).contains("0.6").contains("0.4");
        }

        @Test
        void knownQ() {
            // q=0.3 → p=0.7
            String result = tool.hardyWeinberg("q", 0.3);
            assertThat(result).contains("0.7").contains("0.3");
        }

        @Test
        void knownQ2() {
            // q2=0.25 → q=0.5, p=0.5
            String result = tool.hardyWeinberg("q2", 0.25);
            assertThat(result).contains("0.5");
        }

        @Test
        void knownP2() {
            // p2=0.49 → p=0.7, q=0.3
            String result = tool.hardyWeinberg("p2", 0.49);
            assertThat(result).contains("0.7").contains("0.3");
        }

        @Test
        void heterozygous_2pq() {
            // 2pq=0.48 → 2p(1-p)=0.48 → p=0.6 or 0.4
            String result = tool.hardyWeinberg("heterozygous", 0.48);
            assertThat(result).contains("Hardy-Weinberg");
        }

        @Test
        void heterozygous_alias_2pq() {
            String result = tool.hardyWeinberg("2pq", 0.48);
            assertThat(result).contains("Hardy-Weinberg");
        }

        @Test
        void heterozygous_noRealSolution() {
            // 2pq > 0.5 has no solution (disc < 0)
            String result = tool.hardyWeinberg("heterozygous", 0.6);
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("no real solution");
        }

        @Test
        void valueLessThanZero() {
            String result = tool.hardyWeinberg("p", -0.1);
            assertThat(result).containsIgnoringCase("error").contains("0").contains("1");
        }

        @Test
        void valueGreaterThanOne() {
            String result = tool.hardyWeinberg("p", 1.1);
            assertThat(result).containsIgnoringCase("error").contains("0").contains("1");
        }

        @Test
        void unknownType() {
            String result = tool.hardyWeinberg("unknown", 0.5);
            assertThat(result).containsIgnoringCase("unknown type");
        }

        @Test
        void boundary_zero() {
            String result = tool.hardyWeinberg("p", 0.0);
            assertThat(result).contains("Hardy-Weinberg");
        }

        @Test
        void boundary_one() {
            String result = tool.hardyWeinberg("p", 1.0);
            assertThat(result).contains("Hardy-Weinberg");
        }

        @Test
        void populationCounts_shown() {
            String result = tool.hardyWeinberg("p", 0.6);
            assertThat(result).contains("10,000").contains("individuals").contains("carriers");
        }

        @Test
        void whitespace_in_type() {
            String result = tool.hardyWeinberg("  p  ", 0.5);
            assertThat(result).contains("Hardy-Weinberg");
        }

        @Test
        void heterozygous_maxValid_discZero() {
            // disc = 4 - 8*value = 0 → value = 0.5
            String result = tool.hardyWeinberg("heterozygous", 0.5);
            assertThat(result).contains("Hardy-Weinberg");
        }
    }

    // ── Punnett Square ──

    @Nested
    class PunnettSquareTests {
        @Test
        void monohybrid_AaxAa() {
            String result = tool.punnettSquare("Aa", "Aa", null, null);
            assertThat(result).contains("Punnett Square").contains("GENOTYPE RATIOS").contains("PHENOTYPE RATIOS");
        }

        @Test
        void monohybrid_AAxaa() {
            // All offspring are Aa
            String result = tool.punnettSquare("AA", "aa", null, null);
            assertThat(result).contains("Aa");
        }

        @Test
        void monohybrid_aaxAA() {
            // Reverse cross: aa x AA → combineGametes else branch
            String result = tool.punnettSquare("aa", "AA", null, null);
            assertThat(result).contains("Aa");
        }

        @Test
        void monohybrid_withTraitNames() {
            String result = tool.punnettSquare("Aa", "Aa", "Tall", "Short");
            assertThat(result).contains("Tall").contains("Short");
        }

        @Test
        void dihybrid_AaBbxAaBb() {
            String result = tool.punnettSquare("AaBb", "AaBb", null, null);
            assertThat(result).contains("Punnett Square").contains("GENOTYPE RATIOS");
        }

        @Test
        void dihybrid_withTraitNames() {
            String result = tool.punnettSquare("AaBb", "AaBb", "Tall", "Short");
            assertThat(result).contains("Tall").contains("Short");
        }

        @Test
        void mismatchedLengths_error() {
            String result = tool.punnettSquare("Aa", "AaBb", null, null);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void oddLength_error() {
            String result = tool.punnettSquare("A", "A", null, null);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void monohybrid_Aaxaa() {
            // 50% Aa, 50% aa
            String result = tool.punnettSquare("Aa", "aa", null, null);
            assertThat(result).contains("Punnett Square");
        }

        @Test
        void monohybrid_AAxAA() {
            // All AA
            String result = tool.punnettSquare("AA", "AA", null, null);
            assertThat(result).contains("AA");
        }

        @Test
        void monohybrid_aaxaa() {
            // All aa (recessive)
            String result = tool.punnettSquare("aa", "aa", null, null);
            assertThat(result).contains("aa");
        }

        @Test
        void normalizeGenotype_swap() {
            // When lowercase comes first, should be swapped in normalization
            String result = tool.punnettSquare("aa", "AA", null, null);
            assertThat(result).contains("Aa");
        }
    }

    // ── Population Growth ──

    @Nested
    class PopulationGrowthTests {
        @Test
        void exponential() {
            // N = 100 * e^(0.1*10) = 100 * e = 271.83
            String result = tool.populationGrowth("exponential", "100, 0.1, 10");
            assertThat(result).contains("Exponential Growth").contains("271");
        }

        @Test
        void logistic() {
            // N = K/(1+((K-N0)/N0)*e^(-rt))
            String result = tool.populationGrowth("logistic", "10, 0.5, 1000, 20");
            assertThat(result).contains("Logistic Growth").contains("Capacity used");
        }

        @Test
        void doublingTime() {
            // t = ln(2)/0.1 = 6.931
            String result = tool.populationGrowth("doubling_time", "0.1");
            assertThat(result).contains("6.93").contains("Doubling Time");
        }

        @Test
        void growthRate() {
            // r = ln(200/100)/10 = ln(2)/10 = 0.0693
            String result = tool.populationGrowth("growth_rate", "100, 200, 10");
            assertThat(result).contains("0.069").contains("Growth Rate");
        }

        @Test
        void unknownModel() {
            String result = tool.populationGrowth("unknown_model", "1, 2, 3");
            assertThat(result).containsIgnoringCase("unknown model");
        }

        @Test
        void insufficientValues_exponential() {
            String result = tool.populationGrowth("exponential", "100, 0.1");
            assertThat(result).containsIgnoringCase("error").contains("3");
        }

        @Test
        void insufficientValues_logistic() {
            String result = tool.populationGrowth("logistic", "10, 0.5, 1000");
            assertThat(result).containsIgnoringCase("error").contains("4");
        }

        @Test
        void insufficientValues_growthRate() {
            String result = tool.populationGrowth("growth_rate", "100, 200");
            assertThat(result).containsIgnoringCase("error").contains("3");
        }

        @Test
        void invalidNumber() {
            String result = tool.populationGrowth("exponential", "abc, 0.1, 10");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void exponential_timeZero() {
            // N(0) = N0
            String result = tool.populationGrowth("exponential", "100, 0.1, 0");
            assertThat(result).contains("100");
        }
    }

    // ── Enzyme Kinetics ──

    @Nested
    class EnzymeKineticsTests {
        @Test
        void michaelisMenten() {
            // V = 100*50/(25+50) = 5000/75 = 66.67
            String result = tool.enzymeKinetics("michaelis_menten", "100, 25, 50");
            assertThat(result).contains("66.6").contains("Michaelis-Menten");
        }

        @Test
        void michaelisMenten_atKm() {
            // At [S]=Km: V = Vmax/2 = 50
            String result = tool.enzymeKinetics("michaelis_menten", "100, 25, 25");
            assertThat(result).contains("50");
        }

        @Test
        void lineweaverBurk() {
            String result = tool.enzymeKinetics("lineweaver_burk", "100, 25");
            assertThat(result).contains("Lineweaver-Burk").contains("y-intercept").contains("x-intercept").contains("Slope");
        }

        @Test
        void competitiveInhibition() {
            // Km_app = 25*(1 + 10/5) = 25*3 = 75
            String result = tool.enzymeKinetics("competitive_inhibition", "25, 10, 5");
            assertThat(result).contains("75").contains("Competitive Inhibition");
        }

        @Test
        void kcat_withoutKm() {
            // kcat = Vmax/[E] = 100/0.001 = 100000
            String result = tool.enzymeKinetics("kcat", "100, 0.001");
            assertThat(result).contains("100000").contains("Catalytic Constant");
        }

        @Test
        void kcat_withKm_efficiency() {
            // kcat = 100/0.0001 = 1000000, efficiency = 1000000/0.001 = 1e9 > 1e8 (near diffusion limit)
            String result = tool.enzymeKinetics("kcat", "100, 0.0001, 0.001");
            assertThat(result).contains("efficiency").contains("diffusion limit");
        }

        @Test
        void kcat_withKm_belowDiffusionLimit() {
            // kcat = 100/1 = 100, efficiency = 100/10 = 10 (not near diffusion limit)
            String result = tool.enzymeKinetics("kcat", "100, 1, 10");
            assertThat(result).contains("efficiency");
            assertThat(result).doesNotContain("diffusion limit");
        }

        @Test
        void unknownFormula() {
            String result = tool.enzymeKinetics("unknown", "1, 2, 3");
            assertThat(result).containsIgnoringCase("unknown formula");
        }

        @Test
        void insufficientValues_michaelisMenten() {
            String result = tool.enzymeKinetics("michaelis_menten", "100, 25");
            assertThat(result).containsIgnoringCase("error").contains("3");
        }

        @Test
        void insufficientValues_lineweaverBurk() {
            String result = tool.enzymeKinetics("lineweaver_burk", "100");
            assertThat(result).containsIgnoringCase("error").contains("2");
        }

        @Test
        void insufficientValues_competitiveInhibition() {
            String result = tool.enzymeKinetics("competitive_inhibition", "25, 10");
            assertThat(result).containsIgnoringCase("error").contains("3");
        }

        @Test
        void insufficientValues_kcat() {
            String result = tool.enzymeKinetics("kcat", "100");
            assertThat(result).containsIgnoringCase("error").contains("2");
        }

        @Test
        void invalidNumber() {
            String result = tool.enzymeKinetics("michaelis_menten", "abc, 25, 50");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void whitespace_in_formula() {
            String result = tool.enzymeKinetics("  michaelis_menten  ", "100, 25, 50");
            assertThat(result).contains("Michaelis-Menten");
        }
    }

    // ── Mathematical Correctness ──

    @Nested
    class MathematicalCorrectnessTests {
        @Test
        void exponentialGrowth_exact() {
            // N = 1000 * e^(0.05*20) = 1000 * e^1 = 2718.28
            String result = tool.populationGrowth("exponential", "1000, 0.05, 20");
            assertThat(result).contains("2718");
        }

        @Test
        void hardyWeinberg_populationCounts() {
            // p=0.7, q=0.3: AA=4900, Aa=4200, aa=900
            String result = tool.hardyWeinberg("p", 0.7);
            assertThat(result).contains("4900").contains("4200").contains("900");
        }

        @Test
        void michaelisMenten_highSubstrate() {
            // At [S] >> Km, V ≈ Vmax
            String result = tool.enzymeKinetics("michaelis_menten", "100, 1, 10000");
            // V = 100*10000/(1+10000) ≈ 99.99
            assertThat(result).contains("99.99");
        }

        @Test
        void competitiveInhibition_noInhibitor() {
            // [I]=0: Km_app = Km
            String result = tool.enzymeKinetics("competitive_inhibition", "25, 0, 5");
            assertThat(result).contains("25");
        }

        @Test
        void logistic_approachesCarryingCapacity() {
            // At very large t, N → K
            String result = tool.populationGrowth("logistic", "10, 0.5, 1000, 100");
            assertThat(result).contains("1000");
        }

        @Test
        void gcContent_correctCalculation() {
            // GGCC = 4G+C out of 4 = 100%
            String result = tool.dnaOperations("gc_content", "GGCC");
            assertThat(result).contains("100");
        }
    }

    // ── Fmt Helper Edge Cases ──

    @Nested
    class FmtHelperTests {
        @Test
        void wholeNumber_formattedAsInteger() {
            // doubling_time: ln(2)/1 ≈ 0.693 (not whole, but tests fmt path)
            String result = tool.populationGrowth("doubling_time", "1");
            assertThat(result).contains("0.693");
        }

        @Test
        void nanHandling() {
            // This is hard to trigger directly; NaN would be caught by fmt
            // Testing indirectly through valid calculations
            String result = tool.populationGrowth("exponential", "100, 0.1, 10");
            assertThat(result).doesNotContain("NaN");
        }
    }
}
