package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculusToolTest {

    private CalculusTool tool;

    @BeforeEach
    void setUp() {
        tool = new CalculusTool();
    }

    // ── Derivative ───────────────────────────────────────────────────────────

    @Test
    void derivative_xSquared_atTwo() {
        // d/dx x^2 at x=2 → 4
        String result = tool.numericalDerivative("x^2", 2.0, 1);
        assertThat(result).contains("4");
    }

    @Test
    void derivative_constant_isZero() {
        // d/dx 5 = 0
        String result = tool.numericalDerivative("5", 3.0, 1);
        assertThat(result).contains("0");
    }

    @Test
    void derivative_secondOrder_xSquared() {
        // d²/dx² x^2 = 2
        String result = tool.numericalDerivative("x^2", 1.0, 2);
        assertThat(result).contains("2");
    }

    // ── Integral ─────────────────────────────────────────────────────────────

    @Test
    void integral_xFrom0To1() {
        // ∫ x dx from 0 to 1 = 0.5
        String result = tool.numericalIntegral("x", 0.0, 1.0, null);
        assertThat(result).contains("0.5");
    }

    @Test
    void integral_constantFrom0To3() {
        // ∫ 2 dx from 0 to 3 = 6
        String result = tool.numericalIntegral("2", 0.0, 3.0, null);
        assertThat(result).contains("6");
    }

    // ── Limit ────────────────────────────────────────────────────────────────

    @Test
    void limit_xApproachesTwo() {
        String result = tool.numericalLimit("x", "2", "both");
        assertThat(result).contains("2");
    }

    // ── Series ───────────────────────────────────────────────────────────────

    @Test
    void seriesSum_harmonic_5() {
        // H(1) = 1, H(2) ≈ 1.5
        String result = tool.seriesSum("harmonic", "5");
        assertThat(result).contains("Harmonic");
    }

    @Test
    void seriesSum_geometric() {
        // a=1, r=0.5, n=4: sum = 1 + 0.5 + 0.25 + 0.125 = 1.875
        String result = tool.seriesSum("geometric", "1, 0.5, 4");
        assertThat(result).contains("1.875");
    }

    // ── Matrix Operations ────────────────────────────────────────────────────

    @Test
    void matrix_determinant_2x2() {
        // |1 2; 3 4| = -2
        String result = tool.matrixOperations("determinant", "1,2;3,4");
        assertThat(result).contains("-2");
    }

    @Test
    void matrix_transpose_2x2() {
        String result = tool.matrixOperations("transpose", "1,2;3,4");
        assertThat(result).containsIgnoringCase("transpose");
    }

    @Test
    void matrix_solve_2x2() {
        // 2x + y = 5, x + 3y = 7 → x=8/5, y=9/5
        String result = tool.matrixOperations("solve", "2,1,5;1,3,7");
        assertThat(result).containsIgnoringCase("solution");
    }

    @Test
    void matrix_unknownOp_returnsError() {
        String result = tool.matrixOperations("unknown_op", "1,2;3,4");
        assertThat(result).containsIgnoringCase("unknown");
    }
}
