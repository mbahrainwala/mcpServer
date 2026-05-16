package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

@Service
public class MathTool {

    @Tool(name = "batch_calculate", description = "Evaluate multiple mathematical expressions in a single call. "
            + "Saves round-trips when you need several calculations. "
            + "Separate expressions with newlines or semicolons. "
            + "Example: '2 + 3 * 4; sqrt(144); 2^10' returns all three results at once.")
    public String batchCalculate(
            @ToolParam(description = "Expressions separated by newlines or semicolons. "
                    + "Each expression uses the same syntax as calculate(): "
                    + "+, -, *, /, ^, sqrt(), sin(), cos(), log(), PI, etc.") String expressions) {

        if (expressions == null || expressions.isBlank()) {
            return "Error: expressions required";
        }

        String[] lines = expressions.split("[;\n]");
        StringBuilder sb = new StringBuilder();
        int idx = 1;
        for (String line : lines) {
            String expr = line.strip();
            if (expr.isEmpty()) continue;
            sb.append(idx++).append(". ").append(expr).append(" = ");
            try {
                sb.append(formatNumber(new ExpressionParser(expr).parse()));
            } catch (Exception e) {
                sb.append("Error: ").append(e.getMessage());
            }
            sb.append("\n");
        }
        return sb.toString().stripTrailing();
    }

    @Tool(name = "calculate", description = "Evaluate a mathematical expression and return the exact result. "
            + "Use this tool for ANY arithmetic, algebra, or numeric computation to avoid calculation errors. "
            + "Supports: +, -, *, /, ^ (power), %, sqrt(), abs(), sin(), cos(), tan(), log(), log10(), "
            + "ceil(), floor(), round(), min(), max(), PI, E. "
            + "Examples: '2 + 3 * 4', 'sqrt(144)', '2 ^ 10', 'sin(PI / 2)', '15% of 200 -> 200 * 0.15'")
    public String calculate(
            @ToolParam(description = "The mathematical expression to evaluate. Use standard math notation.") String expression) {

        if (expression == null || expression.isBlank()) {
            return "Error: expression is required";
        }

        try {
            double result = new ExpressionParser(expression.strip()).parse();
            return "Expression: " + expression + "\nResult: " + formatNumber(result);
        } catch (Exception e) {
            return "Error evaluating '" + expression + "': " + e.getMessage();
        }
    }

    static String formatNumber(double result) {
        if (result == Math.floor(result) && !Double.isInfinite(result)) {
            return String.valueOf((long) result);
        }
        return BigDecimal.valueOf(result)
                .round(new MathContext(15))
                .stripTrailingZeros()
                .toPlainString();
    }

    /**
     * Safe recursive-descent parser supporting: +, -, *, /, %, ^ (power),
     * parentheses, unary minus/plus, and common math functions.
     */
    static class ExpressionParser {
        private static final String[] FUNCTIONS =
                {"sqrt", "abs", "sin", "cos", "tan", "log10", "log", "ceil", "floor", "round", "min", "max"};

        private final String input;
        private int pos;

        ExpressionParser(String input) {
            this.input = input.replaceAll("\\s+", "")
                    .replace("**", "^")
                    .replace("PI", String.valueOf(Math.PI))
                    .replace("pi", String.valueOf(Math.PI))
                    .replace("E", String.valueOf(Math.E));
            this.pos = 0;
        }

        double parse() {
            double result = parseExpression();
            if (pos < input.length()) {
                throw new RuntimeException("Unexpected character at position " + pos + ": '" + input.charAt(pos) + "'");
            }
            return result;
        }

        private double parseExpression() {
            double result = parseTerm();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op == '+') { pos++; result += parseTerm(); }
                else if (op == '-') { pos++; result -= parseTerm(); }
                else break;
            }
            return result;
        }

        private double parseTerm() {
            double result = parsePower();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op == '*') { pos++; result *= parsePower(); }
                else if (op == '/') { pos++; result /= parsePower(); }
                else if (op == '%') { pos++; result %= parsePower(); }
                else break;
            }
            return result;
        }

        private double parsePower() {
            double base = parseUnary();
            if (pos < input.length() && input.charAt(pos) == '^') {
                pos++;
                double exp = parsePower(); // right-associative
                base = Math.pow(base, exp);
            }
            return base;
        }

        private double parseUnary() {
            if (pos < input.length() && input.charAt(pos) == '-') { pos++; return -parseUnary(); }
            if (pos < input.length() && input.charAt(pos) == '+') { pos++; return parseUnary(); }
            return parsePrimary();
        }

        private double parsePrimary() {
            if (pos < input.length() && input.charAt(pos) == '(') {
                pos++;
                double result = parseExpression();
                if (pos < input.length() && input.charAt(pos) == ')') pos++;
                return result;
            }

            for (String func : FUNCTIONS) {
                if (input.startsWith(func + "(", pos) || input.startsWith("Math." + func + "(", pos)) {
                    if (input.startsWith("Math.", pos)) pos += 5;
                    pos += func.length() + 1;

                    double arg1 = parseExpression();
                    double arg2 = Double.NaN;
                    if (pos < input.length() && input.charAt(pos) == ',') {
                        pos++;
                        arg2 = parseExpression();
                    }
                    if (pos < input.length() && input.charAt(pos) == ')') pos++;
                    return applyFunction(func, arg1, arg2);
                }
            }

            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            if (pos == start) throw new RuntimeException("Expected number at position " + pos);
            return Double.parseDouble(input.substring(start, pos));
        }

        private double applyFunction(String func, double arg1, double arg2) {
            return switch (func) {
                case "sqrt"  -> Math.sqrt(arg1);
                case "abs"   -> Math.abs(arg1);
                case "sin"   -> Math.sin(arg1);
                case "cos"   -> Math.cos(arg1);
                case "tan"   -> Math.tan(arg1);
                case "log"   -> Math.log(arg1);
                case "log10" -> Math.log10(arg1);
                case "ceil"  -> Math.ceil(arg1);
                case "floor" -> Math.floor(arg1);
                case "round" -> Math.round(arg1);
                case "min"   -> Math.min(arg1, arg2);
                case "max"   -> Math.max(arg1, arg2);
                default -> throw new RuntimeException("Unknown function: " + func);
            };
        }
    }
}
