package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProbabilityToolTest {

    private ProbabilityTool tool;

    @BeforeEach
    void setUp() {
        tool = new ProbabilityTool();
    }

    // ── probabilityCombinatorics ─────────────────────────────────────────────

    @Test
    void combination_C5_2_equals10() {
        String result = tool.probabilityCombinatorics(5, 2, "combination");
        assertThat(result).contains("10");
    }

    @Test
    void permutation_P4_2_equals12() {
        // P(4,2) = 4!/(4-2)! = 12
        String result = tool.probabilityCombinatorics(4, 2, "permutation");
        assertThat(result).contains("12");
    }

    @Test
    void permutationRepetition_3_pow_2_equals9() {
        // n^r = 3^2 = 9
        String result = tool.probabilityCombinatorics(3, 2, "permutation_repetition");
        assertThat(result).contains("9");
    }

    @Test
    void combination_rGreaterThanN_returnsError() {
        String result = tool.probabilityCombinatorics(3, 5, "combination");
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void permutation_rGreaterThanN_returnsError() {
        String result = tool.probabilityCombinatorics(3, 5, "permutation");
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void negativeN_returnsError() {
        String result = tool.probabilityCombinatorics(-1, 2, "combination");
        assertThat(result).containsIgnoringCase("error");
    }

    // ── probabilityBayes ─────────────────────────────────────────────────────

    @Test
    void bayesTheorem_basicCalc() {
        // P(A|B) = P(B|A) * P(A) / P(B)
        // P(A)=0.3, P(B|A)=0.8, P(B)=0.31 → P(A|B) = 0.24/0.31 ≈ 0.7742
        String result = tool.probabilityBayes(0.3, 0.8, 0.31);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("0.77"), s -> assertThat(s).contains("0.774"));
    }

    // ── probabilityDistribution ──────────────────────────────────────────────

    @Test
    void binomialDistribution() {
        // n=10, p=0.5, k=5
        String result = tool.probabilityDistribution("binomial", "10, 0.5, 5");
        assertThat(result).containsIgnoringCase("binomial").isNotBlank();
    }

    @Test
    void normalDistribution() {
        String result = tool.probabilityDistribution("normal", "0, 1");
        assertThat(result).containsIgnoringCase("normal").isNotBlank();
    }

    @Test
    void unknownDistribution_returnsError() {
        String result = tool.probabilityDistribution("cauchy", "0, 1");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("unknown"), s -> assertThat(s).containsIgnoringCase("error"));
    }
}
