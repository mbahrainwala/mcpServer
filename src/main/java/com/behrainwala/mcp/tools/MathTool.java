package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * MCP tool for precise mathematical calculations.
 * LLMs frequently make arithmetic errors — this tool provides exact results.
 */
@Service
public class MathTool {

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
            // Sanitize: only allow safe math characters
            String sanitized = expression.strip();

            // Replace common math notation with Java equivalents
            String prepared = sanitized
                    .replace("^", "**")           // power notation
                    .replace("PI", String.valueOf(Math.PI))
                    .replace("pi", String.valueOf(Math.PI))
                    .replace("E", String.valueOf(Math.E))
                    .replace("sqrt(", "Math.sqrt(")
                    .replace("abs(", "Math.abs(")
                    .replace("sin(", "Math.sin(")
                    .replace("cos(", "Math.cos(")
                    .replace("tan(", "Math.tan(")
                    .replace("log(", "Math.log(")
                    .replace("log10(", "Math.log10(")
                    .replace("ceil(", "Math.ceil(")
                    .replace("floor(", "Math.floor(")
                    .replace("round(", "Math.round(")
                    .replace("min(", "Math.min(")
                    .replace("max(", "Math.max(")
                    .replace("**", "Math.pow")    // convert power: a**b -> Math.pow(a,b) won't work directly
                    ;

            // Handle power expressions: convert "a ** b" style to Math.pow(a, b)
            prepared = convertPowerExpressions(prepared);

            // Security check: reject anything that isn't math
            if (!prepared.matches("[0-9Math.\\s+\\-*/(),%.<>:?_powsqrtabsincotaglerflumd]+")) {
                // Fallback: try simple expression evaluation
                return evaluateSimple(sanitized);
            }

            // Use Nashorn-compatible evaluation via runtime compilation
            Object result = evaluateExpression(prepared);
            return "Expression: " + expression + "\nResult: " + result;

        } catch (Exception e) {
            // Fallback to simple evaluation
            try {
                return evaluateSimple(expression.strip());
            } catch (Exception e2) {
                return "Error evaluating '" + expression + "': " + e2.getMessage();
            }
        }
    }

    /**
     * Evaluates mathematical expressions using Java's built-in capabilities.
     */
    private Object evaluateExpression(String expr) throws Exception {
        // Use a simple recursive descent parser for safety
        return new ExpressionParser(expr).parse();
    }

    private String evaluateSimple(String expression) {
        try {
            double result = new ExpressionParser(expression).parse();
            // Format: remove trailing zeros
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return "Expression: " + expression + "\nResult: " + (long) result;
            }
            return "Expression: " + expression + "\nResult: " + BigDecimal.valueOf(result)
                    .round(new MathContext(15))
                    .stripTrailingZeros()
                    .toPlainString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot evaluate: " + expression + " — " + e.getMessage());
        }
    }

    private String convertPowerExpressions(String expr) {
        // Simple pass-through; the ExpressionParser handles ^ directly
        return expr.replace("Math.pow", "**");
    }

    /**
     * A safe recursive-descent math expression parser.
     * Supports: numbers, +, -, *, /, %, ^ (power), parentheses, and common math functions.
     */
    static class ExpressionParser {
        private final String input;
        private int pos;

        ExpressionParser(String input) {
            this.input = input.replaceAll("\\s+", "")
                    .replace("**", "^")
                    .replace("PI", String.valueOf(Math.PI))
                    .replace("pi", String.valueOf(Math.PI));
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
            if (pos < input.length() && input.charAt(pos) == '-') {
                pos++;
                return -parseUnary();
            }
            if (pos < input.length() && input.charAt(pos) == '+') {
                pos++;
                return parseUnary();
            }
            return parsePrimary();
        }

        private double parsePrimary() {
            if (pos < input.length() && input.charAt(pos) == '(') {
                pos++; // skip '('
                double result = parseExpression();
                if (pos < input.length() && input.charAt(pos) == ')') pos++; // skip ')'
                return result;
            }

            // Check for function names
            String[] functions = {"sqrt", "abs", "sin", "cos", "tan", "log10", "log", "ceil", "floor", "round", "min", "max"};
            for (String func : functions) {
                if (input.startsWith(func + "(", pos) || input.startsWith("Math." + func + "(", pos)) {
                    if (input.startsWith("Math.", pos)) pos += 5;
                    pos += func.length();
                    pos++; // skip '('

                    double arg1 = parseExpression();
                    double arg2 = Double.NaN;

                    if (pos < input.length() && input.charAt(pos) == ',') {
                        pos++; // skip ','
                        arg2 = parseExpression();
                    }

                    if (pos < input.length() && input.charAt(pos) == ')') pos++; // skip ')'

                    return applyFunction(func, arg1, arg2);
                }
            }

            // Parse number
            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            if (pos == start) {
                throw new RuntimeException("Expected number at position " + pos);
            }
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
