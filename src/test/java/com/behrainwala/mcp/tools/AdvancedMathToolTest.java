package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdvancedMathToolTest {

    private AdvancedMathTool tool;

    @BeforeEach
    void setUp() {
        tool = new AdvancedMathTool();
    }

    @Test
    void solveQuadratic_twoRealRoots() {
        // x^2 - 5x + 6 = 0 → roots 3 and 2
        String result = tool.solveQuadratic(1, -5, 6);
        assertThat(result).contains("3").contains("2");
    }

    @Test
    void solveQuadratic_doubleRoot() {
        // x^2 - 2x + 1 = 0 → x = 1 (double root)
        String result = tool.solveQuadratic(1, -2, 1);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("double"), s -> assertThat(s).contains("1"));
    }

    @Test
    void solveQuadratic_complexRoots() {
        // x^2 + 1 = 0 → complex roots
        String result = tool.solveQuadratic(1, 0, 1);
        assertThat(result).containsAnyOf("complex", "imaginary", "i");
    }

    @Test
    void solveQuadratic_aZero_linearCase() {
        // 0x^2 + 2x - 4 = 0 → x = 2
        String result = tool.solveQuadratic(0, 2, -4);
        assertThat(result).contains("2");
    }

    @Test
    void solveQuadratic_aZeroBZero_noSolution() {
        // 0 = 0x + 1 → no solution
        String result = tool.solveQuadratic(0, 0, 1);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("no solution"), s -> assertThat(s).containsIgnoringCase("error"));
    }
}
