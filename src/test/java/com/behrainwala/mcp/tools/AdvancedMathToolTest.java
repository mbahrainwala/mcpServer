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

    // ── solveQuadratic ───────────────────────────────────────────────────────

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
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("repeated"), s -> assertThat(s).contains("1"));
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

    @Test
    void solveQuadratic_aZeroBZeroCZero_identity() {
        // 0 = 0 → identity (infinite solutions)
        String result = tool.solveQuadratic(0, 0, 0);
        assertThat(result).containsIgnoringCase("infinite");
    }

    // ── solveLinearSystem ────────────────────────────────────────────────────

    @Test
    void solveLinearSystem_uniqueSolution() {
        // x + y = 3, x - y = 1 → x=2, y=1
        String result = tool.solveLinearSystem(1, 1, 3, 1, -1, 1);
        assertThat(result).contains("2").contains("1");
    }

    @Test
    void solveLinearSystem_infiniteSolutions() {
        // 2x + 4y = 6, x + 2y = 3 → parallel (same line)
        String result = tool.solveLinearSystem(2, 4, 6, 1, 2, 3);
        assertThat(result).containsIgnoringCase("infinite");
    }

    @Test
    void solveLinearSystem_noSolution() {
        // x + 2y = 3, x + 2y = 5 → parallel lines
        String result = tool.solveLinearSystem(1, 2, 3, 1, 2, 5);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("no solution"), s -> assertThat(s).containsIgnoringCase("parallel"));
    }

    // ── computePolynomial ────────────────────────────────────────────────────

    @Test
    void computePolynomial_atX2() {
        // P(x) = x^2, x=3 → 9
        String result = tool.computePolynomial("1,0,0", 3);
        assertThat(result).contains("9");
    }

    @Test
    void computePolynomial_invalidCoefficients() {
        String result = tool.computePolynomial("1,abc,0", 2);
        assertThat(result).containsIgnoringCase("error");
    }

    // ── triangleProperties ───────────────────────────────────────────────────

    @Test
    void triangleProperties_equilateral() {
        // 3-3-3
        String result = tool.triangleProperties(3, 3, 3);
        assertThat(result).contains("Perimeter").contains("Area");
    }

    @Test
    void triangleProperties_rightTriangle() {
        // 3-4-5
        String result = tool.triangleProperties(3, 4, 5);
        assertThat(result).contains("Area").contains("ANGLES");
    }

    @Test
    void triangleProperties_negativeSide() {
        String result = tool.triangleProperties(-1, 3, 3);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void triangleProperties_invalidTriangle() {
        // 1+2 <= 10 → invalid
        String result = tool.triangleProperties(1, 2, 10);
        assertThat(result).containsIgnoringCase("error");
    }

    // ── triangleArea ─────────────────────────────────────────────────────────

    @Test
    void triangleArea_baseHeight() {
        String result = tool.triangleArea("base_height", "10,6");
        assertThat(result).contains("30");
    }

    @Test
    void triangleArea_twoSidesAngle() {
        String result = tool.triangleArea("two_sides_angle", "5,6,90");
        assertThat(result).contains("15");
    }

    @Test
    void triangleArea_coordinates() {
        // Triangle at (0,0),(4,0),(2,3) → area = 6
        String result = tool.triangleArea("coordinates", "0,0,4,0,2,3");
        assertThat(result).contains("6");
    }

    @Test
    void triangleArea_herons() {
        // 3-4-5 right triangle → area = 6
        String result = tool.triangleArea("herons", "3,4,5");
        assertThat(result).contains("6");
    }

    @Test
    void triangleArea_unknownMethod() {
        String result = tool.triangleArea("unknown", "1,2,3");
        assertThat(result).containsIgnoringCase("error");
    }

    // ── circleProperties ─────────────────────────────────────────────────────

    @Test
    void circleProperties_fromRadius() {
        String result = tool.circleProperties("radius", 5);
        assertThat(result).contains("5").contains("Area");
    }

    @Test
    void circleProperties_fromDiameter() {
        String result = tool.circleProperties("diameter", 10);
        assertThat(result).contains("5").contains("Area");
    }

    @Test
    void circleProperties_fromArea() {
        String result = tool.circleProperties("area", Math.PI * 4);
        assertThat(result).contains("2");
    }

    @Test
    void circleProperties_fromCircumference() {
        String result = tool.circleProperties("circumference", 2 * Math.PI);
        assertThat(result).contains("1");
    }

    @Test
    void circleProperties_zeroValue() {
        String result = tool.circleProperties("radius", 0);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void circleProperties_unknownType() {
        String result = tool.circleProperties("hexagon", 5);
        assertThat(result).containsIgnoringCase("error");
    }

    // ── shapeAreaVolume ───────────────────────────────────────────────────────

    @Test
    void shapeAreaVolume_rectangle() {
        String result = tool.shapeAreaVolume("rectangle", "4,5");
        assertThat(result).contains("20");
    }

    @Test
    void shapeAreaVolume_parallelogram() {
        String result = tool.shapeAreaVolume("parallelogram", "6,4");
        assertThat(result).contains("24");
    }

    @Test
    void shapeAreaVolume_trapezoid() {
        String result = tool.shapeAreaVolume("trapezoid", "4,6,5");
        assertThat(result).contains("25");
    }

    @Test
    void shapeAreaVolume_ellipse() {
        String result = tool.shapeAreaVolume("ellipse", "3,4");
        assertThat(result).contains("Area");
    }

    @Test
    void shapeAreaVolume_sphere() {
        String result = tool.shapeAreaVolume("sphere", "5");
        assertThat(result).containsIgnoringCase("Volume");
    }

    @Test
    void shapeAreaVolume_cylinder() {
        String result = tool.shapeAreaVolume("cylinder", "3,10");
        assertThat(result).containsIgnoringCase("Volume");
    }

    @Test
    void shapeAreaVolume_cone() {
        String result = tool.shapeAreaVolume("cone", "3,4");
        assertThat(result).containsIgnoringCase("Volume");
    }

    @Test
    void shapeAreaVolume_cube() {
        String result = tool.shapeAreaVolume("cube", "4");
        assertThat(result).contains("64").containsIgnoringCase("Volume");
    }

    @Test
    void shapeAreaVolume_rectangularPrism() {
        String result = tool.shapeAreaVolume("rectangular_prism", "2,3,4");
        assertThat(result).contains("24").containsIgnoringCase("Volume");
    }

    @Test
    void shapeAreaVolume_pyramid() {
        String result = tool.shapeAreaVolume("pyramid", "4,3,5");
        assertThat(result).containsIgnoringCase("Volume");
    }

    @Test
    void shapeAreaVolume_unknown() {
        String result = tool.shapeAreaVolume("tetrahedron", "3");
        assertThat(result).containsIgnoringCase("error");
    }
}
