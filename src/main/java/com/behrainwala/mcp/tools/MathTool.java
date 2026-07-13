package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

@Service
public class MathTool {

    // Previous result, exposed to expressions as the `ans` constant. Shared across calls
    // (the MCP service is a singleton), mirroring a desk-calculator's answer memory.
    private volatile double lastResult = Double.NaN;

    @Tool(name = "batch_calculate", description = "Evaluate multiple mathematical expressions in a single call. "
            + "Saves round-trips when you need several calculations. "
            + "Separate expressions with newlines or semicolons; each line may reference the previous line's "
            + "result via `ans`. Example: '2 + 3 * 4; sqrt(144); ans + 1' evaluates all three in order.")
    public String batchCalculate(
            @ToolParam(description = "Expressions separated by newlines or semicolons. "
                    + "Each expression uses the same syntax as calculate(): "
                    + "+, -, *, /, ^, %, sqrt(), cbrt(), pow(), sin()/cos()/tan() in DEGREES, "
                    + "ln()/log()/log2()/logn(), factorial()/n!, constants pi/e/inf/ans, etc.") String expressions) {

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
                double result = new ExpressionParser(expr, lastResult).parse();
                lastResult = result;
                sb.append(formatNumber(result));
            } catch (Exception e) {
                sb.append("Error: ").append(e.getMessage());
            }
            sb.append("\n");
        }
        return sb.toString().stripTrailing();
    }

    @Tool(name = "calculate", description = "Evaluate a mathematical expression and return the exact result. "
            + "Use this tool for ANY arithmetic, algebra, or numeric computation to avoid calculation errors. "
            + "Operators: + - * / % ^ (power) and parentheses. Postfix factorial with '!'. "
            + "Functions: sqrt, cbrt, pow(x,y), abs, sign, ceil, floor, round, trunc, "
            + "sin, cos, tan, asin, acos, atan (ANGLES IN DEGREES), sinh, cosh, tanh, "
            + "ln (natural), log (base 10), log2, logn(base,x), log10, factorial(n), min(a,b), max(a,b). "
            + "Constants: pi, e, inf, and ans (the previous result). "
            + "Examples: '2 + 3 * 4', 'sqrt(144)', '2 ^ 10', 'sin(90)', 'log(1000)', '5!', 'logn(2, 8)'")
    public String calculate(
            @ToolParam(description = "The mathematical expression to evaluate. Use standard math notation. "
                    + "Trigonometric functions take degrees (e.g. sin(30) = 0.5).") String expression) {

        if (expression == null || expression.isBlank()) {
            return "Error: expression is required";
        }

        try {
            double result = new ExpressionParser(expression.strip(), lastResult).parse();
            lastResult = result;
            return "Expression: " + expression + "\nResult: " + formatNumber(result);
        } catch (Exception e) {
            return "Error evaluating '" + expression + "': " + e.getMessage();
        }
    }

    static String formatNumber(double result) {
        if (Double.isNaN(result)) return "NaN";
        if (Double.isInfinite(result)) return result > 0 ? "Infinity" : "-Infinity";
        if (result == Math.floor(result) && Math.abs(result) < 9.007199254740992e15) {
            return String.valueOf((long) result);
        }
        return BigDecimal.valueOf(result)
                .round(new MathContext(15))
                .stripTrailingZeros()
                .toPlainString();
    }

    /**
     * Safe recursive-descent parser.
     * <p>
     * Grammar (lowest to highest precedence):
     * expression → term (('+'|'-') term)*
     * term       → power (('*'|'/'|'%') power)*
     * power      → unary ('^' power)?            (right-associative)
     * unary      → ('-'|'+') unary | postfix
     * postfix    → primary ('!')*                (factorial)
     * primary    → '(' expression ')' | function '(' args ')' | constant | number
     * <p>
     * Trigonometric functions operate in DEGREES. `log` is base-10, `ln` is natural.
     */
    static class ExpressionParser {

        private final String input;
        private int pos;
        private final double ans;

        ExpressionParser(String input) {
            this(input, Double.NaN);
        }

        ExpressionParser(String input, double ans) {
            this.input = input.replaceAll("\\s+", "").replace("**", "^");
            this.pos = 0;
            this.ans = ans;
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
            return parsePostfix();
        }

        private double parsePostfix() {
            double value = parsePrimary();
            while (pos < input.length() && input.charAt(pos) == '!') {
                pos++;
                value = factorial(value);
            }
            return value;
        }

        private double parsePrimary() {
            // Parenthesised sub-expression
            if (pos < input.length() && input.charAt(pos) == '(') {
                pos++;
                double result = parseExpression();
                if (pos < input.length() && input.charAt(pos) == ')') pos++;
                else throw new RuntimeException("Missing closing parenthesis");
                return result;
            }

            // Optional Math. prefix (e.g. Math.sqrt(9))
            if (input.startsWith("Math.", pos)) pos += 5;

            // Identifier → function call or constant
            if (pos < input.length() && isIdentStart(input.charAt(pos))) {
                int start = pos;
                while (pos < input.length() && isIdentPart(input.charAt(pos))) pos++;
                String name = input.substring(start, pos).toLowerCase();

                if (pos < input.length() && input.charAt(pos) == '(') {
                    pos++; // consume '('
                    List<Double> args = new ArrayList<>();
                    if (!(pos < input.length() && input.charAt(pos) == ')')) {
                        args.add(parseExpression());
                        while (pos < input.length() && input.charAt(pos) == ',') {
                            pos++;
                            args.add(parseExpression());
                        }
                    }
                    if (pos < input.length() && input.charAt(pos) == ')') pos++;
                    else throw new RuntimeException("Missing closing parenthesis for " + name + "()");
                    return applyFunction(name, args);
                }
                return resolveConstant(name);
            }

            // Numeric literal
            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            if (pos == start) throw new RuntimeException("Expected number at position " + pos);
            return Double.parseDouble(input.substring(start, pos));
        }

        private static boolean isIdentStart(char c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        }

        private static boolean isIdentPart(char c) {
            return isIdentStart(c) || (c >= '0' && c <= '9');
        }

        private double resolveConstant(String name) {
            return switch (name) {
                case "pi" -> Math.PI;
                case "e" -> Math.E;
                case "inf", "infinity" -> Double.POSITIVE_INFINITY;
                case "ans" -> {
                    if (Double.isNaN(ans)) throw new RuntimeException("No previous result available for 'ans'");
                    yield ans;
                }
                default -> throw new RuntimeException("Unknown constant or function: " + name);
            };
        }

        private double applyFunction(String f, List<Double> a) {
            return switch (f) {
                // roots & powers
                case "sqrt" -> Math.sqrt(one(f, a));
                case "cbrt" -> Math.cbrt(one(f, a));
                case "pow" -> { double[] t = two(f, a); yield Math.pow(t[0], t[1]); }
                // numeric helpers
                case "abs" -> Math.abs(one(f, a));
                case "sign", "signum" -> Math.signum(one(f, a));
                case "ceil" -> Math.ceil(one(f, a));
                case "floor" -> Math.floor(one(f, a));
                case "round" -> (double) Math.round(one(f, a));
                case "trunc" -> { double x = one(f, a); yield x < 0 ? Math.ceil(x) : Math.floor(x); }
                // trigonometry — arguments in DEGREES
                case "sin" -> Math.sin(Math.toRadians(one(f, a)));
                case "cos" -> Math.cos(Math.toRadians(one(f, a)));
                case "tan" -> Math.tan(Math.toRadians(one(f, a)));
                case "asin" -> Math.toDegrees(Math.asin(one(f, a)));
                case "acos" -> Math.toDegrees(Math.acos(one(f, a)));
                case "atan" -> Math.toDegrees(Math.atan(one(f, a)));
                // hyperbolic
                case "sinh" -> Math.sinh(one(f, a));
                case "cosh" -> Math.cosh(one(f, a));
                case "tanh" -> Math.tanh(one(f, a));
                // logarithms — log is base 10, ln is natural
                case "ln" -> Math.log(one(f, a));
                case "log", "log10" -> Math.log10(one(f, a));
                case "log2" -> Math.log(one(f, a)) / Math.log(2);
                case "logn" -> { double[] t = two(f, a); yield Math.log(t[1]) / Math.log(t[0]); }
                // misc
                case "factorial" -> factorial(one(f, a));
                case "min" -> { double[] t = two(f, a); yield Math.min(t[0], t[1]); }
                case "max" -> { double[] t = two(f, a); yield Math.max(t[0], t[1]); }
                default -> throw new RuntimeException("Unknown function: " + f);
            };
        }

        private static double one(String f, List<Double> a) {
            if (a.size() != 1) throw new RuntimeException(f + "() expects 1 argument, got " + a.size());
            return a.get(0);
        }

        private static double[] two(String f, List<Double> a) {
            if (a.size() != 2) throw new RuntimeException(f + "() expects 2 arguments, got " + a.size());
            return new double[]{a.get(0), a.get(1)};
        }

        /** Factorial via exact product for non-negative integers, Gamma elsewhere. */
        static double factorial(double x) {
            if (Double.isNaN(x)) return Double.NaN;
            if (x == Math.floor(x) && x < 0) {
                throw new RuntimeException("factorial undefined for negative integers");
            }
            if (x == Math.floor(x) && x <= 170) {
                double r = 1;
                for (int i = 2; i <= (int) x; i++) r *= i;
                return r;
            }
            return gamma(x + 1);
        }

        // Lanczos approximation (g=7, n=9) for the Gamma function — supports non-integer factorials.
        private static final double[] LANCZOS = {
                0.99999999999980993,
                676.5203681218851,
                -1259.1392167224028,
                771.32342877765313,
                -176.61502916214059,
                12.507343278686905,
                -0.13857109526572012,
                9.9843695780195716e-6,
                1.5056327351493116e-7
        };

        static double gamma(double x) {
            if (x < 0.5) {
                // Reflection formula for the left half-plane
                return Math.PI / (Math.sin(Math.PI * x) * gamma(1 - x));
            }
            x -= 1;
            double a = LANCZOS[0];
            double t = x + 7.5;
            for (int i = 1; i < LANCZOS.length; i++) {
                a += LANCZOS[i] / (x + i);
            }
            return Math.sqrt(2 * Math.PI) * Math.pow(t, x + 0.5) * Math.exp(-t) * a;
        }
    }
}
