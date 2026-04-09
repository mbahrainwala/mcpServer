package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MathToolTest {

    private MathTool tool;

    @BeforeEach
    void setUp() {
        tool = new MathTool();
    }

    @Test
    void calculate_addition() {
        assertThat(tool.calculate("2 + 3")).contains("5");
    }

    @Test
    void calculate_multiplication() {
        assertThat(tool.calculate("4 * 7")).contains("28");
    }

    @Test
    void calculate_power() {
        assertThat(tool.calculate("2 ^ 10")).contains("1024");
    }

    @Test
    void calculate_sqrt() {
        assertThat(tool.calculate("sqrt(144)")).contains("12");
    }

    @Test
    void calculate_nested_expression() {
        assertThat(tool.calculate("(2 + 3) * 4")).contains("20");
    }

    @Test
    void calculate_sin_pi_over_2() {
        String result = tool.calculate("sin(PI / 2)");
        assertThat(result).contains("1");
    }

    @Test
    void calculate_division() {
        assertThat(tool.calculate("10 / 4")).contains("2.5");
    }

    @Test
    void calculate_blank_returns_error() {
        assertThat(tool.calculate("")).containsIgnoringCase("error");
    }

    @Test
    void calculate_null_returns_error() {
        assertThat(tool.calculate(null)).containsIgnoringCase("error");
    }

    @Test
    void expressionParser_basic_arithmetic() {
        assertThat(new MathTool.ExpressionParser("3 + 4 * 2").parse()).isEqualTo(11.0);
    }

    @Test
    void expressionParser_power() {
        assertThat(new MathTool.ExpressionParser("2^8").parse()).isEqualTo(256.0);
    }

    @Test
    void expressionParser_negative_unary() {
        assertThat(new MathTool.ExpressionParser("-5 + 3").parse()).isEqualTo(-2.0);
    }
}
