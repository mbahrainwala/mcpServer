package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MathToolTest {

    private MathTool tool;

    @BeforeEach
    void setUp() {
        tool = new MathTool();
    }

    // ── calculate() method-level tests ──────────────────────────────────────

    @Nested
    class CalculateInputValidation {

        @Test
        void nullExpression_returnsError() {
            assertThat(tool.calculate(null)).isEqualTo("Error: expression is required");
        }

        @Test
        void emptyExpression_returnsError() {
            assertThat(tool.calculate("")).isEqualTo("Error: expression is required");
        }

        @Test
        void blankExpression_returnsError() {
            assertThat(tool.calculate("   ")).isEqualTo("Error: expression is required");
        }
    }

    @Nested
    class CalculateBasicArithmetic {

        @Test
        void addition() {
            String result = tool.calculate("2 + 3");
            assertThat(result).contains("Result:").contains("5");
        }

        @Test
        void subtraction() {
            String result = tool.calculate("10 - 4");
            assertThat(result).contains("Result:").contains("6");
        }

        @Test
        void multiplication() {
            String result = tool.calculate("4 * 7");
            assertThat(result).contains("Result:").contains("28");
        }

        @Test
        void division() {
            String result = tool.calculate("10 / 4");
            assertThat(result).contains("Result:").contains("2.5");
        }

        @Test
        void modulus() {
            String result = tool.calculate("17 % 5");
            assertThat(result).contains("Result:").contains("2");
        }

        @Test
        void power() {
            String result = tool.calculate("2 ^ 10");
            assertThat(result).contains("Result:").contains("1024");
        }

        @Test
        void divisionByZero_returnsInfinity() {
            // IEEE 754: double division by zero gives Infinity, not an exception
            String result = tool.calculate("5 / 0");
            assertThat(result).containsAnyOf("Infinity", "∞", "Error");
        }
    }

    @Nested
    class CalculateFunctions {

        @Test
        void sqrt() {
            assertThat(tool.calculate("sqrt(144)")).contains("12");
        }

        @Test
        void abs_positive() {
            assertThat(tool.calculate("abs(5)")).contains("5");
        }

        @Test
        void abs_negative() {
            assertThat(tool.calculate("abs(-7)")).contains("7");
        }

        @Test
        void sin() {
            // sin(PI/2) = 1
            String result = tool.calculate("sin(PI / 2)");
            assertThat(result).contains("1");
        }

        @Test
        void cos() {
            // cos(0) = 1
            String result = tool.calculate("cos(0)");
            assertThat(result).contains("1");
        }

        @Test
        void tan() {
            // tan(0) = 0
            String result = tool.calculate("tan(0)");
            assertThat(result).contains("0");
        }

        @Test
        void log() {
            // ln(1) = 0
            String result = tool.calculate("log(1)");
            assertThat(result).contains("0");
        }

        @Test
        void log10() {
            // log10(100) = 2
            String result = tool.calculate("log10(100)");
            assertThat(result).contains("2");
        }

        @Test
        void ceil() {
            assertThat(tool.calculate("ceil(2.3)")).contains("3");
        }

        @Test
        void floor() {
            assertThat(tool.calculate("floor(2.9)")).contains("2");
        }

        @Test
        void round() {
            assertThat(tool.calculate("round(2.5)")).contains("3");
        }

        @Test
        void min() {
            assertThat(tool.calculate("min(3, 7)")).contains("3");
        }

        @Test
        void max() {
            assertThat(tool.calculate("max(3, 7)")).contains("7");
        }
    }

    @Nested
    class CalculateConstants {

        @Test
        void piConstant() {
            // PI alone should yield ~3.14159...
            String result = tool.calculate("PI");
            assertThat(result).contains("3.14159");
        }

        @Test
        void piLowercase() {
            String result = tool.calculate("pi");
            assertThat(result).contains("3.14159");
        }

        @Test
        void eConstant() {
            // E alone should yield ~2.71828...
            String result = tool.calculate("E");
            assertThat(result).contains("2.71828");
        }
    }

    @Nested
    class CalculateComplexExpressions {

        @Test
        void nestedParentheses() {
            // (2 + 3) * 4 = 20
            assertThat(tool.calculate("(2 + 3) * 4")).contains("20");
        }

        @Test
        void deeplyNestedParentheses() {
            // ((2 + 3) * (4 - 1)) = 15
            assertThat(tool.calculate("((2 + 3) * (4 - 1))")).contains("15");
        }

        @Test
        void orderOfOperations() {
            // 3 + 4 * 2 = 11 (not 14)
            assertThat(tool.calculate("3 + 4 * 2")).contains("11");
        }

        @Test
        void combinedFunctionsAndOperators() {
            // sqrt(9) + 2^3 = 3 + 8 = 11
            assertThat(tool.calculate("sqrt(9) + 2^3")).contains("11");
        }

        @Test
        void multipleOperators() {
            // 10 - 2 * 3 + 1 = 10 - 6 + 1 = 5
            assertThat(tool.calculate("10 - 2 * 3 + 1")).contains("5");
        }

        @Test
        void rightAssociativePower() {
            // 2^3^2 = 2^(3^2) = 2^9 = 512 (right-associative)
            assertThat(tool.calculate("2^3^2")).contains("512");
        }
    }

    @Nested
    class CalculateErrorHandling {

        @Test
        void invalidExpression_returnsError() {
            String result = tool.calculate("abc xyz");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void unknownFunction_fallsBack() {
            // "foo(5)" is not a recognized function; should result in error via fallback
            String result = tool.calculate("foo(5)");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void expressionWithWhitespace_succeeds() {
            // Leading/trailing whitespace should be stripped
            assertThat(tool.calculate("  2 + 3  ")).contains("5");
        }

        @Test
        void securityCheck_rejectsUnsafeInput() {
            // Characters that fail the regex cause a fallback to evaluateSimple
            // which will also fail for truly invalid input
            String result = tool.calculate("System.exit(0)");
            // The regex check will fail; it falls to evaluateSimple which also fails
            assertThat(result).containsIgnoringCase("error");
        }
    }

    // ── ExpressionParser direct tests ───────────────────────────────────────

    @Nested
    class ExpressionParserTests {

        @Test
        void basicAddition() {
            assertThat(new MathTool.ExpressionParser("3 + 4").parse()).isEqualTo(7.0);
        }

        @Test
        void basicSubtraction() {
            assertThat(new MathTool.ExpressionParser("10 - 3").parse()).isEqualTo(7.0);
        }

        @Test
        void basicMultiplication() {
            assertThat(new MathTool.ExpressionParser("6 * 7").parse()).isEqualTo(42.0);
        }

        @Test
        void basicDivision() {
            assertThat(new MathTool.ExpressionParser("20 / 4").parse()).isEqualTo(5.0);
        }

        @Test
        void basicModulus() {
            assertThat(new MathTool.ExpressionParser("17 % 5").parse()).isEqualTo(2.0);
        }

        @Test
        void basicPower() {
            assertThat(new MathTool.ExpressionParser("2^8").parse()).isEqualTo(256.0);
        }

        @Test
        void doubleStar_power() {
            // ** is converted to ^ internally
            assertThat(new MathTool.ExpressionParser("3**2").parse()).isEqualTo(9.0);
        }

        @Test
        void unaryMinus() {
            assertThat(new MathTool.ExpressionParser("-5 + 3").parse()).isEqualTo(-2.0);
        }

        @Test
        void unaryPlus() {
            assertThat(new MathTool.ExpressionParser("+5 + 3").parse()).isEqualTo(8.0);
        }

        @Test
        void doubleUnaryMinus() {
            // --5 => -(-5) = 5
            assertThat(new MathTool.ExpressionParser("--5").parse()).isEqualTo(5.0);
        }

        @Test
        void parentheses() {
            assertThat(new MathTool.ExpressionParser("(2 + 3) * 4").parse()).isEqualTo(20.0);
        }

        @Test
        void nestedParentheses() {
            assertThat(new MathTool.ExpressionParser("((1 + 2) * (3 + 4))").parse()).isEqualTo(21.0);
        }

        @Test
        void sqrt_function() {
            assertThat(new MathTool.ExpressionParser("sqrt(25)").parse()).isEqualTo(5.0);
        }

        @Test
        void abs_function() {
            assertThat(new MathTool.ExpressionParser("abs(-10)").parse()).isEqualTo(10.0);
        }

        @Test
        void sin_function() {
            assertThat(new MathTool.ExpressionParser("sin(0)").parse()).isEqualTo(0.0);
        }

        @Test
        void cos_function() {
            assertThat(new MathTool.ExpressionParser("cos(0)").parse()).isEqualTo(1.0);
        }

        @Test
        void tan_function() {
            assertThat(new MathTool.ExpressionParser("tan(0)").parse()).isEqualTo(0.0);
        }

        @Test
        void log_function() {
            // ln(e) = 1; e ~ 2.718281828...
            // We use Math.E directly
            assertThat(new MathTool.ExpressionParser("log(1)").parse()).isEqualTo(0.0);
        }

        @Test
        void log10_function() {
            assertThat(new MathTool.ExpressionParser("log10(1000)").parse()).isCloseTo(3.0, within(1e-10));
        }

        @Test
        void ceil_function() {
            assertThat(new MathTool.ExpressionParser("ceil(4.1)").parse()).isEqualTo(5.0);
        }

        @Test
        void floor_function() {
            assertThat(new MathTool.ExpressionParser("floor(4.9)").parse()).isEqualTo(4.0);
        }

        @Test
        void round_function() {
            assertThat(new MathTool.ExpressionParser("round(4.5)").parse()).isEqualTo(5.0);
        }

        @Test
        void min_function() {
            assertThat(new MathTool.ExpressionParser("min(3, 7)").parse()).isEqualTo(3.0);
        }

        @Test
        void max_function() {
            assertThat(new MathTool.ExpressionParser("max(3, 7)").parse()).isEqualTo(7.0);
        }

        @Test
        void mathDot_prefix_sqrt() {
            // The parser should also handle Math.sqrt(...)
            assertThat(new MathTool.ExpressionParser("Math.sqrt(16)").parse()).isEqualTo(4.0);
        }

        @Test
        void mathDot_prefix_abs() {
            assertThat(new MathTool.ExpressionParser("Math.abs(-3)").parse()).isEqualTo(3.0);
        }

        @Test
        void piConstant() {
            assertThat(new MathTool.ExpressionParser("PI").parse()).isCloseTo(Math.PI, within(1e-10));
        }

        @Test
        void piLowercase() {
            assertThat(new MathTool.ExpressionParser("pi").parse()).isCloseTo(Math.PI, within(1e-10));
        }

        @Test
        void functionWithExpression() {
            // sqrt(4 + 5) = sqrt(9) = 3
            assertThat(new MathTool.ExpressionParser("sqrt(4 + 5)").parse()).isEqualTo(3.0);
        }

        @Test
        void nestedFunctions() {
            // abs(sqrt(16) - 5) = abs(4 - 5) = abs(-1) = 1
            assertThat(new MathTool.ExpressionParser("abs(sqrt(16) - 5)").parse()).isEqualTo(1.0);
        }

        @Test
        void unexpectedCharacter_throwsException() {
            assertThatThrownBy(() -> new MathTool.ExpressionParser("2 + 3 @").parse())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unexpected character");
        }

        @Test
        void expectedNumber_throwsException() {
            assertThatThrownBy(() -> new MathTool.ExpressionParser("*5").parse())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Expected number");
        }

        @Test
        void emptyInput_throwsException() {
            assertThatThrownBy(() -> new MathTool.ExpressionParser("").parse())
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void decimalNumbers() {
            assertThat(new MathTool.ExpressionParser("1.5 + 2.5").parse()).isEqualTo(4.0);
        }

        @Test
        void multipleAdditionsAndSubtractions() {
            // 1 + 2 - 3 + 4 = 4
            assertThat(new MathTool.ExpressionParser("1 + 2 - 3 + 4").parse()).isEqualTo(4.0);
        }

        @Test
        void multipleMultiplicationsAndDivisions() {
            // 12 / 3 * 2 = 8
            assertThat(new MathTool.ExpressionParser("12 / 3 * 2").parse()).isEqualTo(8.0);
        }

        @Test
        void precedence_multiplication_before_addition() {
            // 2 + 3 * 4 = 14
            assertThat(new MathTool.ExpressionParser("2 + 3 * 4").parse()).isEqualTo(14.0);
        }

        @Test
        void precedence_power_before_multiplication() {
            // 2 * 3^2 = 2 * 9 = 18
            assertThat(new MathTool.ExpressionParser("2 * 3^2").parse()).isEqualTo(18.0);
        }
    }

    @Nested
    class EvaluateSimplePath {

        @Test
        void integerResult_formatsWithoutDecimal() {
            // An integer result like 6 should be formatted as "6" (long)
            String result = tool.calculate("2 + 4");
            assertThat(result).contains("Result: 6");
        }

        @Test
        void decimalResult_formatsWithDecimal() {
            // A non-integer result preserves decimal
            String result = tool.calculate("7 / 2");
            assertThat(result).contains("Result: 3.5");
        }
    }

    @Nested
    class RegexFallbackAndEdgeCases {

        @Test
        void regexFails_fallsToEvaluateSimple() {
            // Characters like '$' fail the regex → evaluateSimple is called
            // But evaluateSimple also fails for '$' → error
            String result = tool.calculate("$100");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void expressionWithOnlyNumber() {
            String result = tool.calculate("42");
            assertThat(result).contains("42");
        }

        @Test
        void powerExpression_convertPower() {
            // Tests convertPowerExpressions path
            String result = tool.calculate("3 ^ 3");
            assertThat(result).contains("27");
        }

        @Test
        void infiniteResult_formatting() {
            // Division by 0 yields Infinity (not floor-able)
            String result = tool.calculate("1 / 0");
            assertThat(result).containsAnyOf("Infinity", "Error", "∞");
        }

        @Test
        void eConstant_inExpression() {
            // E gets replaced with Math.E value
            String result = tool.calculate("E * 2");
            assertThat(result).contains("5.4365");
        }

        @Test
        void preparedExpressionPassesRegex() {
            // Simple expression that should pass regex and use evaluateExpression
            String result = tool.calculate("2 + 3 * 4");
            assertThat(result).contains("14");
        }

        @Test
        void evaluateSimple_largeInteger() {
            String result = tool.calculate("1000 * 1000");
            assertThat(result).contains("1000000");
        }

        @Test
        void evaluateSimple_negativeResult() {
            String result = tool.calculate("3 - 10");
            assertThat(result).contains("-7");
        }

        @Test
        void multipleSpaces() {
            String result = tool.calculate("  2   +   3  ");
            assertThat(result).contains("5");
        }
    }
}
