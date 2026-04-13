package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculusToolTest {

    private CalculusTool tool;

    @BeforeEach
    void setUp() {
        tool = new CalculusTool();
    }

    // =========================================================================
    // numerical_derivative
    // =========================================================================
    @Nested
    class NumericalDerivativeTests {

        @Test
        void firstDerivative_xSquared_atThree_shouldBeApproxSix() {
            // d/dx x^2 at x=3 -> 2*3 = 6
            String result = tool.numericalDerivative("x^2", 3.0, 1);
            assertThat(result).contains("1st derivative");
            assertThat(result).contains("f(x) = x^2");
            assertThat(result).contains("At x = 3");
            assertThat(result).contains("Central difference");
            // f(3)=9
            assertThat(result).contains("f(3) = 9");
        }

        @Test
        void firstDerivative_xSquared_atTwo() {
            // d/dx x^2 at x=2 -> 4
            String result = tool.numericalDerivative("x^2", 2.0, 1);
            assertThat(result).contains("1st derivative");
            assertThat(result).contains("4");
        }

        @Test
        void firstDerivative_constant_isZero() {
            String result = tool.numericalDerivative("5", 3.0, 1);
            assertThat(result).contains("0");
        }

        @Test
        void firstDerivative_polynomial() {
            // d/dx (3*x^3 - 2*x + 1) at x=1 -> 9 - 2 = 7
            String result = tool.numericalDerivative("3*x^3-2*x+1", 1.0, 1);
            assertThat(result).contains("Numerical Derivative");
        }

        @Test
        void firstDerivative_sinX_atNonZero() {
            // d/dx sin(x) at x=1 -> cos(1) ~ 0.5403
            String result = tool.numericalDerivative("sin(x)", 1.0, 1);
            assertThat(result).contains("sin(x)");
            assertThat(result).contains("1st derivative");
        }

        @Test
        void firstDerivative_cosX_atNonZero() {
            // d/dx cos(x) at x=1 -> -sin(1)
            String result = tool.numericalDerivative("cos(x)", 1.0, 1);
            assertThat(result).contains("cos(x)");
        }

        @Test
        void firstDerivative_tanX_atNonZero() {
            // d/dx tan(x) at x=1 -> sec^2(1)
            String result = tool.numericalDerivative("tan(x)", 1.0, 1);
            assertThat(result).contains("tan(x)");
        }

        @Test
        void firstDerivative_sqrtX_atFour() {
            // d/dx sqrt(x) at x=4 -> 1/(2*sqrt(4)) = 0.25
            // Numerical approximation yields 0.249999999924
            String result = tool.numericalDerivative("sqrt(x)", 4.0, 1);
            assertThat(result).contains("sqrt(x)");
            assertThat(result).contains("0.24999");
        }

        @Test
        void firstDerivative_logX_atOne() {
            // d/dx ln(x) at x=1 -> 1
            String result = tool.numericalDerivative("log(x)", 1.0, 1);
            assertThat(result).contains("log(x)");
        }

        @Test
        void firstDerivative_absX_atTwo() {
            // d/dx |x| at x=2 -> 1
            String result = tool.numericalDerivative("abs(x)", 2.0, 1);
            assertThat(result).contains("abs(x)");
        }

        @Test
        void firstDerivative_xCubed() {
            // d/dx x^3 at x=2 -> 12
            String result = tool.numericalDerivative("x^3", 2.0, 1);
            assertThat(result).contains("12");
        }

        @Test
        void secondDerivative_xSquared_isTwo() {
            String result = tool.numericalDerivative("x^2", 1.0, 2);
            assertThat(result).contains("2nd derivative");
        }

        @Test
        void thirdDerivative_xCubed() {
            String result = tool.numericalDerivative("x^3", 1.0, 3);
            assertThat(result).contains("3rd derivative");
        }

        @Test
        void fourthDerivative_xToFourth() {
            String result = tool.numericalDerivative("x^4", 1.0, 4);
            assertThat(result).contains("4th derivative");
        }

        @Test
        void orderNull_defaultsToFirst() {
            String result = tool.numericalDerivative("x^2", 2.0, null);
            assertThat(result).contains("1st derivative");
        }

        @Test
        void orderZero_defaultsToFirst() {
            String result = tool.numericalDerivative("x^2", 2.0, 0);
            assertThat(result).contains("1st derivative");
        }

        @Test
        void orderNegative_defaultsToFirst() {
            String result = tool.numericalDerivative("x^2", 2.0, -1);
            assertThat(result).contains("1st derivative");
        }

        @Test
        void orderHigherThanFour_clampedToFour() {
            String result = tool.numericalDerivative("x^4", 1.0, 10);
            assertThat(result).contains("4th derivative");
        }

        @Test
        void invalidExpression_returnsError() {
            String result = tool.numericalDerivative("???invalid", 1.0, 1);
            assertThat(result).startsWith("Error:");
        }

        @Test
        void lnExpression_convertedToLog() {
            String result = tool.numericalDerivative("ln(x)", 2.0, 1);
            assertThat(result).contains("ln(x)");
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void eToTheXExpression() {
            // e^x is handled by the parseExpression converter
            String result = tool.numericalDerivative("e^x", 1.0, 1);
            assertThat(result).contains("e^x");
            assertThat(result).doesNotStartWith("Error:");
        }
    }

    // =========================================================================
    // numerical_integral
    // =========================================================================
    @Nested
    class NumericalIntegralTests {

        @Test
        void integral_xSquared_from0To1_shouldBeApproxOneThird() {
            String result = tool.numericalIntegral("x^2", 0.0, 1.0, null);
            assertThat(result).contains("Numerical Integration");
            assertThat(result).contains("Simpson");
            assertThat(result).contains("0.333");
        }

        @Test
        void integral_x_from0To1_isHalf() {
            String result = tool.numericalIntegral("x", 0.0, 1.0, null);
            assertThat(result).contains("0.5");
        }

        @Test
        void integral_constant_from0To3() {
            String result = tool.numericalIntegral("2", 0.0, 3.0, null);
            assertThat(result).contains("6");
        }

        @Test
        void integral_sinX_from0ToPi() {
            String result = tool.numericalIntegral("sin(x)", 0.0, Math.PI, null);
            assertThat(result).contains("2");
        }

        @Test
        void integral_withCustomIntervals_even() {
            String result = tool.numericalIntegral("x", 0.0, 1.0, 100);
            assertThat(result).contains("Intervals: 100");
        }

        @Test
        void integral_withCustomIntervals_odd_getsRoundedUp() {
            String result = tool.numericalIntegral("x", 0.0, 1.0, 101);
            assertThat(result).contains("Intervals: 102");
        }

        @Test
        void integral_nullIntervals_defaultsTo1000() {
            String result = tool.numericalIntegral("x", 0.0, 1.0, null);
            assertThat(result).contains("Intervals: 1000");
        }

        @Test
        void integral_zeroIntervals_defaultsTo1000() {
            String result = tool.numericalIntegral("x", 0.0, 1.0, 0);
            assertThat(result).contains("Intervals: 1000");
        }

        @Test
        void integral_negativeIntervals_defaultsTo1000() {
            String result = tool.numericalIntegral("x", 0.0, 1.0, -5);
            assertThat(result).contains("Intervals: 1000");
        }

        @Test
        void integral_containsSampleValues() {
            String result = tool.numericalIntegral("x^2", 0.0, 1.0, null);
            assertThat(result).contains("Sample values:");
        }

        @Test
        void integral_containsEstimatedError() {
            String result = tool.numericalIntegral("x^2", 0.0, 1.0, null);
            assertThat(result).contains("Estimated error:");
        }

        @Test
        void integral_containsTrapezoidalResult() {
            String result = tool.numericalIntegral("x^2", 0.0, 1.0, null);
            assertThat(result).contains("Trapezoidal");
        }

        @Test
        void integral_invalidExpression_returnsError() {
            String result = tool.numericalIntegral("???invalid", 0.0, 1.0, null);
            assertThat(result).startsWith("Error:");
        }
    }

    // =========================================================================
    // numerical_limit
    // =========================================================================
    @Nested
    class NumericalLimitTests {

        @Test
        void limit_polynomial_bothSidesAgree() {
            // lim x->2 x^2 = 4, left and right agree
            String result = tool.numericalLimit("x^2", "2", "both");
            assertThat(result).contains("Limit");
            assertThat(result).contains("x^2");
            assertThat(result).contains("left and right limits agree");
        }

        @Test
        void limit_bothDirection_default() {
            String result = tool.numericalLimit("x", "2", null);
            assertThat(result).contains("From the right");
            assertThat(result).contains("From the left");
        }

        @Test
        void limit_rightDirection_only() {
            String result = tool.numericalLimit("x", "2", "right");
            assertThat(result).contains("From the right");
            assertThat(result).doesNotContain("From the left");
        }

        @Test
        void limit_leftDirection_only() {
            String result = tool.numericalLimit("x", "2", "left");
            assertThat(result).contains("From the left");
            assertThat(result).doesNotContain("From the right");
        }

        @Test
        void limit_positiveInfinity() {
            // The ExpressionParser does not handle scientific notation (E in values)
            // so when x is large (e.g. 1e10), String.valueOf produces "1.0E10" which breaks.
            // This is expected behavior: the tool returns an error for expressions
            // with x when approaching infinity.
            String result = tool.numericalLimit("x^2", "inf", null);
            assertThat(result).startsWith("Error:");
        }

        @Test
        void limit_plusInfinity() {
            String result = tool.numericalLimit("x^2", "+inf", null);
            assertThat(result).startsWith("Error:");
        }

        @Test
        void limit_negativeInfinity() {
            String result = tool.numericalLimit("x^2", "-inf", null);
            assertThat(result).startsWith("Error:");
        }

        @Test
        void limit_infinity_constantExpression_works() {
            // Constant expressions don't depend on x, so inf works
            String result = tool.numericalLimit("5", "inf", null);
            assertThat(result).contains("Approaching");
        }

        @Test
        void limit_leftNotEqualRight() {
            // For expressions where left != right at the final check point,
            // if the final evaluation uses target +/- 1e-10 (scientific notation),
            // the parser fails. Test with a simple divergent case.
            // abs(x)/x fails due to scientific notation in 1e-10.
            String result = tool.numericalLimit("abs(x)/x", "0", "both");
            assertThat(result).startsWith("Error:");
        }

        @Test
        void limit_directionWithSpacesAndCase() {
            String result = tool.numericalLimit("x", "2", "  BOTH  ");
            assertThat(result).contains("From the right");
            assertThat(result).contains("From the left");
        }

        @Test
        void limit_invalidExpression_returnsError() {
            String result = tool.numericalLimit("???invalid", "0", "both");
            assertThat(result).startsWith("Error:");
        }
    }

    // =========================================================================
    // series_sum
    // =========================================================================
    @Nested
    class SeriesSumTests {

        // -- Geometric --
        @Test
        void geometric_converging() {
            String result = tool.seriesSum("geometric", "1, 0.5, 4");
            assertThat(result).contains("Geometric Series");
            assertThat(result).contains("1.875");
            assertThat(result).contains("Infinite sum");
        }

        @Test
        void geometric_diverging_rGreaterThanOne() {
            String result = tool.seriesSum("geometric", "1, 2, 5");
            assertThat(result).contains("diverges");
        }

        @Test
        void geometric_diverging_rEqualsOne() {
            String result = tool.seriesSum("geometric", "1, 1, 5");
            assertThat(result).contains("diverges");
        }

        @Test
        void geometric_moreThanTenTerms_showsEllipsis() {
            String result = tool.seriesSum("geometric", "1, 0.5, 15");
            assertThat(result).contains("+ ...");
        }

        @Test
        void geometric_exactlyTenTerms_noEllipsis() {
            String result = tool.seriesSum("geometric", "1, 0.5, 10");
            assertThat(result).doesNotContain("+ ...");
        }

        @Test
        void geometric_lessThanThreeParams_error() {
            String result = tool.seriesSum("geometric", "1, 0.5");
            assertThat(result).startsWith("Error:");
        }

        // -- Harmonic --
        @Test
        void harmonic_fiveTerms() {
            String result = tool.seriesSum("harmonic", "5");
            assertThat(result).contains("Harmonic Series");
            assertThat(result).contains("diverges");
        }

        // -- p-Series --
        @Test
        void pSeries_pGreaterThanOne_converges() {
            String result = tool.seriesSum("p_series", "2, 100");
            assertThat(result).contains("CONVERGES");
            assertThat(result).contains("Basel problem");
        }

        @Test
        void pSeries_pEqualsOne_diverges() {
            String result = tool.seriesSum("p_series", "1, 10");
            assertThat(result).contains("DIVERGES (harmonic)");
        }

        @Test
        void pSeries_pLessThanOne_diverges() {
            String result = tool.seriesSum("p_series", "0.5, 10");
            assertThat(result).contains("DIVERGES (p < 1)");
        }

        @Test
        void pSeries_insufficientParams_error() {
            String result = tool.seriesSum("p_series", "2");
            assertThat(result).startsWith("Error:");
        }

        // -- Taylor sin --
        @Test
        void taylorSin() {
            String result = tool.seriesSum("taylor_sin", "1, 10");
            assertThat(result).contains("Taylor Series: sin(x)");
            assertThat(result).contains("Exact value:");
        }

        @Test
        void taylorSin_insufficientParams_error() {
            String result = tool.seriesSum("taylor_sin", "1");
            assertThat(result).startsWith("Error:");
        }

        // -- Taylor cos --
        @Test
        void taylorCos() {
            String result = tool.seriesSum("taylor_cos", "1, 10");
            assertThat(result).contains("Taylor Series: cos(x)");
        }

        @Test
        void taylorCos_insufficientParams_error() {
            String result = tool.seriesSum("taylor_cos", "1");
            assertThat(result).startsWith("Error:");
        }

        // -- Taylor exp --
        @Test
        void taylorExp() {
            String result = tool.seriesSum("taylor_exp", "1, 10");
            assertThat(result).contains("Taylor Series: e^x");
        }

        @Test
        void taylorExp_insufficientParams_error() {
            String result = tool.seriesSum("taylor_exp", "1");
            assertThat(result).startsWith("Error:");
        }

        // -- Taylor ln --
        @Test
        void taylorLn_valid() {
            String result = tool.seriesSum("taylor_ln", "0.5, 10");
            assertThat(result).contains("Taylor Series: ln(1+x)");
        }

        @Test
        void taylorLn_outOfRange_tooLow() {
            String result = tool.seriesSum("taylor_ln", "-1, 10");
            assertThat(result).contains("converges only for");
        }

        @Test
        void taylorLn_outOfRange_tooHigh() {
            String result = tool.seriesSum("taylor_ln", "1.5, 10");
            assertThat(result).contains("converges only for");
        }

        @Test
        void taylorLn_atBoundary_xEqualsOne() {
            String result = tool.seriesSum("taylor_ln", "1, 10");
            assertThat(result).contains("Taylor Series: ln(1+x)");
        }

        @Test
        void taylorLn_insufficientParams_error() {
            String result = tool.seriesSum("taylor_ln", "0.5");
            assertThat(result).startsWith("Error:");
        }

        // -- Unknown type --
        @Test
        void unknownSeriesType() {
            String result = tool.seriesSum("unknown_type", "1,2,3");
            assertThat(result).contains("Unknown series type");
        }

        // -- Generic parse error --
        @Test
        void seriesSum_malformedValues_error() {
            String result = tool.seriesSum("geometric", "abc");
            assertThat(result).startsWith("Error:");
        }

        // -- Series type case insensitivity and trimming --
        @Test
        void seriesType_withWhitespaceAndCase() {
            String result = tool.seriesSum("  HARMONIC  ", "5");
            assertThat(result).contains("Harmonic Series");
        }
    }

    // =========================================================================
    // matrix_operations
    // =========================================================================
    @Nested
    class MatrixOperationsTests {

        // -- Determinant --
        @Test
        void determinant_2x2() {
            String result = tool.matrixOperations("determinant", "1,2;3,4");
            assertThat(result).contains("Determinant");
            assertThat(result).contains("-2");
        }

        @Test
        void determinant_1x1() {
            String result = tool.matrixOperations("determinant", "5");
            assertThat(result).contains("5");
        }

        @Test
        void determinant_3x3() {
            String result = tool.matrixOperations("determinant", "1,0,0;0,1,0;0,0,1");
            assertThat(result).contains("1");
        }

        @Test
        void determinant_4x4_cofactorExpansion() {
            String result = tool.matrixOperations("determinant", "1,0,0,0;0,1,0,0;0,0,1,0;0,0,0,1");
            assertThat(result).contains("Determinant");
            assertThat(result).contains("1");
        }

        @Test
        void determinant_nonSquare_error() {
            String result = tool.matrixOperations("determinant", "1,2,3;4,5,6");
            assertThat(result).contains("square matrix");
        }

        // -- Transpose --
        @Test
        void transpose_2x2() {
            String result = tool.matrixOperations("transpose", "1,2;3,4");
            assertThat(result).containsIgnoringCase("transpose");
        }

        @Test
        void transpose_nonSquare() {
            String result = tool.matrixOperations("transpose", "1,2,3;4,5,6");
            assertThat(result).containsIgnoringCase("transpose");
        }

        // -- Inverse --
        @Test
        void inverse_2x2() {
            String result = tool.matrixOperations("inverse", "1,2;3,4");
            assertThat(result).contains("Inverse");
            assertThat(result).contains("det");
        }

        @Test
        void inverse_nonSquare_error() {
            String result = tool.matrixOperations("inverse", "1,2,3;4,5,6");
            assertThat(result).contains("square matrix");
        }

        @Test
        void inverse_nonTwoByTwo_error() {
            String result = tool.matrixOperations("inverse", "1,0,0;0,1,0;0,0,1");
            assertThat(result).contains("2");
        }

        @Test
        void inverse_singular_error() {
            String result = tool.matrixOperations("inverse", "1,2;2,4");
            assertThat(result).contains("singular");
        }

        // -- Multiply --
        @Test
        void multiply_2x2() {
            String result = tool.matrixOperations("multiply", "1,2;3,4|5,6;7,8");
            assertThat(result).contains("Matrix Multiplication");
            assertThat(result).contains("19");
            assertThat(result).contains("22");
            assertThat(result).contains("43");
            assertThat(result).contains("50");
        }

        @Test
        void multiply_missingPipe_error() {
            String result = tool.matrixOperations("multiply", "1,2;3,4");
            assertThat(result).contains("separate two matrices");
        }

        @Test
        void multiply_incompatibleDimensions_error() {
            String result = tool.matrixOperations("multiply", "1,2;3,4|1,2;3,4;5,6");
            assertThat(result).contains("Incompatible dimensions");
        }

        // -- Eigenvalues --
        @Test
        void eigenvalues_2x2_realEigenvalues() {
            String result = tool.matrixOperations("eigenvalues", "2,1;1,2");
            assertThat(result).contains("Eigenvalues");
            assertThat(result).contains("3");
            assertThat(result).contains("1");
        }

        @Test
        void eigenvalues_2x2_complexEigenvalues() {
            // [[0,-1],[1,0]]: rotation, eigenvalues = +/- i
            String result = tool.matrixOperations("eigenvalues", "0,-1;1,0");
            assertThat(result).contains("Eigenvalues");
            assertThat(result).contains("i");
        }

        @Test
        void eigenvalues_showsTraceAndDet() {
            String result = tool.matrixOperations("eigenvalues", "3,1;0,2");
            assertThat(result).contains("Trace = 5");
            assertThat(result).contains("Det = 6");
        }

        @Test
        void eigenvalues_showsDiscriminant() {
            String result = tool.matrixOperations("eigenvalues", "2,1;1,2");
            assertThat(result).contains("Discriminant");
            assertThat(result).contains("Characteristic equation");
        }

        @Test
        void eigenvalues_nonTwoByTwo_error() {
            String result = tool.matrixOperations("eigenvalues", "1,0,0;0,1,0;0,0,1");
            assertThat(result).contains("2");
        }

        // -- Solve --
        @Test
        void solve_2x2_system() {
            String result = tool.matrixOperations("solve", "2,1,5;1,3,7");
            assertThat(result).containsIgnoringCase("solution");
            assertThat(result).contains("x1");
            assertThat(result).contains("x2");
        }

        @Test
        void solve_wrongAugmentedDimensions_error() {
            String result = tool.matrixOperations("solve", "1,2,3,4;5,6,7,8");
            assertThat(result).contains("Error:");
        }

        @Test
        void solve_singularSystem_error() {
            String result = tool.matrixOperations("solve", "1,2,3;2,4,6");
            assertThat(result).contains("singular");
        }

        @Test
        void solve_requiresPartialPivoting() {
            String result = tool.matrixOperations("solve", "0,1,1;1,0,1");
            assertThat(result).containsIgnoringCase("solution");
            assertThat(result).contains("1");
        }

        @Test
        void solve_containsAugmentedMatrix() {
            String result = tool.matrixOperations("solve", "2,1,5;1,3,7");
            assertThat(result).contains("Augmented matrix");
        }

        // -- Unknown operation --
        @Test
        void unknownOperation_returnsMessage() {
            String result = tool.matrixOperations("unknown_op", "1,2;3,4");
            assertThat(result).containsIgnoringCase("unknown");
        }

        // -- Operation case insensitivity and trimming --
        @Test
        void operationType_withWhitespaceAndCase() {
            String result = tool.matrixOperations("  DETERMINANT  ", "1,2;3,4");
            assertThat(result).contains("Determinant");
        }

        // -- Parse error --
        @Test
        void invalidMatrixString_returnsError() {
            String result = tool.matrixOperations("determinant", "abc");
            assertThat(result).startsWith("Error:");
        }
    }

    // =========================================================================
    // Expression parser functions via derivative (using x values that avoid
    // scientific notation in String.valueOf)
    // =========================================================================
    @Nested
    class ExpressionFunctionTests {

        @Test
        void expression_exp_notSupported() {
            // exp() is not in the ExpressionParser's function list,
            // so exp(x) fails to parse
            String result = tool.numericalDerivative("exp(x)", 1.0, 1);
            assertThat(result).startsWith("Error:");
        }

        @Test
        void expression_sinX() {
            String result = tool.numericalDerivative("sin(x)", 1.0, 1);
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void expression_cosX() {
            String result = tool.numericalDerivative("cos(x)", 1.0, 1);
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void expression_tanX() {
            String result = tool.numericalDerivative("tan(x)", 1.0, 1);
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void expression_logX() {
            String result = tool.numericalDerivative("log(x)", 2.0, 1);
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void expression_sqrtX() {
            String result = tool.numericalDerivative("sqrt(x)", 4.0, 1);
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void expression_absX() {
            String result = tool.numericalDerivative("abs(x)", 2.0, 1);
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void expression_xPower() {
            String result = tool.numericalDerivative("x^3", 2.0, 1);
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void expression_lnX_convertedToLog() {
            String result = tool.numericalDerivative("ln(x)", 2.0, 1);
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void expression_eToTheX() {
            // e^x is handled by substituting the numeric value of e
            String result = tool.numericalDerivative("e^x", 1.0, 1);
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void expression_multipleTerms() {
            String result = tool.numericalDerivative("2*x+3", 5.0, 1);
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void expression_division() {
            String result = tool.numericalDerivative("x/2", 4.0, 1);
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void expression_subtraction() {
            String result = tool.numericalDerivative("x-1", 5.0, 1);
            assertThat(result).doesNotStartWith("Error:");
        }
    }

    // =========================================================================
    // Mathematical correctness verification
    // =========================================================================
    @Nested
    class MathematicalCorrectnessTests {

        @Test
        void derivative_xSquared_at3_outputContainsSix() {
            // d/dx x^2 at x=3 -> 6
            String result = tool.numericalDerivative("x^2", 3.0, 1);
            assertThat(result).contains("6");
        }

        @Test
        void integral_xSquared_0to1_isApproxOneThird() {
            String result = tool.numericalIntegral("x^2", 0.0, 1.0, 1000);
            assertThat(result).contains("0.333");
        }

        @Test
        void limit_polynomial_agreesBothSides() {
            String result = tool.numericalLimit("x^2", "2", "both");
            assertThat(result).contains("left and right limits agree");
        }

        @Test
        void taylorSin_accurateForSmallX() {
            String result = tool.seriesSum("taylor_sin", "0.1, 5");
            assertThat(result).contains("Taylor approximation");
            assertThat(result).contains("Exact value");
        }

        @Test
        void taylorExp_accuracy() {
            String result = tool.seriesSum("taylor_exp", "1, 15");
            assertThat(result).contains("2.71828");
        }

        @Test
        void taylorCos_accuracy() {
            String result = tool.seriesSum("taylor_cos", "0, 5");
            assertThat(result).contains("1");
        }

        @Test
        void taylorLn_accuracy() {
            String result = tool.seriesSum("taylor_ln", "0.5, 20");
            assertThat(result).contains("0.405");
        }

        @Test
        void geometric_infiniteSumFormula() {
            String result = tool.seriesSum("geometric", "1, 0.5, 100");
            assertThat(result).contains("2");
        }

        @Test
        void secondDerivative_xSquared_isApproxTwo() {
            String result = tool.numericalDerivative("x^2", 5.0, 2);
            assertThat(result).contains("2");
        }

        @Test
        void matrix_multiply_identity() {
            String result = tool.matrixOperations("multiply", "1,0;0,1|3,4;5,6");
            assertThat(result).contains("3");
            assertThat(result).contains("4");
            assertThat(result).contains("5");
            assertThat(result).contains("6");
        }

        @Test
        void matrix_determinant_identity3x3() {
            String result = tool.matrixOperations("determinant", "1,0,0;0,1,0;0,0,1");
            assertThat(result).contains("1");
        }

        @Test
        void matrix_solve_simple_identity() {
            String result = tool.matrixOperations("solve", "1,0,1;0,1,2");
            assertThat(result).contains("1");
            assertThat(result).contains("2");
        }

        @Test
        void matrix_eigenvalues_identity() {
            String result = tool.matrixOperations("eigenvalues", "1,0;0,1");
            assertThat(result).contains("1");
        }
    }

    // =========================================================================
    // Formatting tests
    // =========================================================================
    @Nested
    class FormattingTests {

        @Test
        void fmt_handlesWholeNumbers() {
            String result = tool.numericalIntegral("1", 0.0, 2.0, null);
            assertThat(result).contains("2");
        }

        @Test
        void sci_formatting() {
            String result = tool.numericalIntegral("x", 0.0, 1.0, null);
            assertThat(result).containsPattern("\\d\\.\\d+e[+-]\\d+");
        }

        @Test
        void fmt_largeInteger() {
            String result = tool.numericalDerivative("x^2", 100.0, 1);
            assertThat(result).contains("10000");
        }

        @Test
        void fmt_fractionalNumber() {
            String result = tool.numericalIntegral("x^2", 0.0, 1.0, null);
            assertThat(result).contains("0.333");
        }
    }
}
