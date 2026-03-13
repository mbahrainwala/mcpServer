package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.DoubleUnaryOperator;

/**
 * MCP tool for university-level calculus: numerical differentiation, integration,
 * limits, series, and common calculus operations.
 */
@Service
public class CalculusTool {

    private static final MathContext MC = new MathContext(12);

    @Tool(name = "numerical_derivative", description = "Compute the numerical derivative of a function at a point. "
            + "Uses the central difference method for high accuracy. "
            + "The function is specified as a math expression with 'x' as the variable.")
    public String numericalDerivative(
            @ToolParam(description = "Math expression as a function of x. Examples: 'x^2', 'sin(x)', "
                    + "'3*x^3 - 2*x + 1', 'e^x', 'ln(x)', 'sqrt(x)'") String expression,
            @ToolParam(description = "The x value at which to compute the derivative") double atX,
            @ToolParam(description = "Order of derivative: 1 (first), 2 (second), 3 (third). Default: 1.", required = false) Integer order) {

        int n = (order != null && order > 0) ? Math.min(order, 4) : 1;

        try {
            DoubleUnaryOperator f = parseExpression(expression);

            double h = 1e-6;
            double result;

            // Central difference formulas
            if (n == 1) {
                result = (f.applyAsDouble(atX + h) - f.applyAsDouble(atX - h)) / (2 * h);
            } else if (n == 2) {
                result = (f.applyAsDouble(atX + h) - 2 * f.applyAsDouble(atX) + f.applyAsDouble(atX - h)) / (h * h);
            } else if (n == 3) {
                result = (f.applyAsDouble(atX + 2 * h) - 2 * f.applyAsDouble(atX + h)
                        + 2 * f.applyAsDouble(atX - h) - f.applyAsDouble(atX - 2 * h)) / (2 * h * h * h);
            } else {
                result = (f.applyAsDouble(atX + 2 * h) - 4 * f.applyAsDouble(atX + h)
                        + 6 * f.applyAsDouble(atX) - 4 * f.applyAsDouble(atX - h)
                        + f.applyAsDouble(atX - 2 * h)) / (h * h * h * h);
            }

            String ordinal = switch (n) { case 1 -> "1st"; case 2 -> "2nd"; case 3 -> "3rd"; default -> n + "th"; };

            return "Numerical Derivative\n" +
                    "────────────────────\n" +
                    "f(x) = " + expression + "\n" +
                    "Order: " + ordinal + " derivative\n" +
                    "At x = " + fmt(atX) + "\n\n" +
                    "f(" + fmt(atX) + ") = " + fmt(f.applyAsDouble(atX)) + "\n" +
                    "f" + "'".repeat(n) + "(" + fmt(atX) + ") ≈ " + fmt(result) + "\n\n" +
                    "Method: Central difference (h = " + h + ")\n" +
                    "Note: This is a numerical approximation. For exact symbolic derivatives,\n" +
                    "the LLM should derive the formula analytically and use 'calculate' to evaluate.";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "numerical_integral", description = "Compute the definite integral of a function over an interval [a, b]. "
            + "Uses Simpson's rule for high accuracy. "
            + "The function is specified as a math expression with 'x' as the variable.")
    public String numericalIntegral(
            @ToolParam(description = "Math expression as a function of x (e.g. 'x^2', 'sin(x)', '1/x')") String expression,
            @ToolParam(description = "Lower bound of integration (a)") double a,
            @ToolParam(description = "Upper bound of integration (b)") double b,
            @ToolParam(description = "Number of intervals (higher = more accurate). Default: 1000.", required = false) Integer intervals) {

        int n = (intervals != null && intervals > 0) ? intervals : 1000;
        if (n % 2 != 0) n++; // Simpson's rule needs even n

        try {
            DoubleUnaryOperator f = parseExpression(expression);

            // Simpson's 1/3 rule
            double h = (b - a) / n;
            double sum = f.applyAsDouble(a) + f.applyAsDouble(b);

            for (int i = 1; i < n; i++) {
                double x = a + i * h;
                sum += (i % 2 == 0 ? 2 : 4) * f.applyAsDouble(x);
            }

            double result = sum * h / 3;

            // Also compute with trapezoidal for error estimate
            double trapSum = (f.applyAsDouble(a) + f.applyAsDouble(b)) / 2;
            for (int i = 1; i < n; i++) trapSum += f.applyAsDouble(a + i * h);
            double trapResult = trapSum * h;

            double errorEstimate = Math.abs(result - trapResult);

            StringBuilder sb = new StringBuilder();
            sb.append("Numerical Integration\n");
            sb.append("─────────────────────\n");
            sb.append("∫ f(x) dx from ").append(fmt(a)).append(" to ").append(fmt(b)).append("\n");
            sb.append("f(x) = ").append(expression).append("\n\n");
            sb.append("Result (Simpson's rule): ").append(fmt(result)).append("\n");
            sb.append("Result (Trapezoidal):    ").append(fmt(trapResult)).append("\n");
            sb.append("Estimated error: ~").append(sci(errorEstimate)).append("\n");
            sb.append("Intervals: ").append(n).append("\n\n");
            sb.append("Sample values:\n");

            int samples = 5;
            double step = (b - a) / samples;
            for (int i = 0; i <= samples; i++) {
                double x = a + i * step;
                sb.append("  f(").append(fmt(x)).append(") = ").append(fmt(f.applyAsDouble(x))).append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "numerical_limit", description = "Compute the limit of a function as x approaches a value. "
            + "Evaluates the function at points progressively closer to the target from both sides.")
    public String numericalLimit(
            @ToolParam(description = "Math expression as a function of x") String expression,
            @ToolParam(description = "The value x approaches (use 'inf' or '-inf' for infinity)") String approachValue,
            @ToolParam(description = "Direction: 'both', 'left' (x→a⁻), or 'right' (x→a⁺). Default: both.", required = false) String direction) {

        try {
            DoubleUnaryOperator f = parseExpression(expression);
            String dir = (direction != null) ? direction.strip().toLowerCase() : "both";

            StringBuilder sb = new StringBuilder();
            sb.append("Limit Evaluation\n");
            sb.append("────────────────\n");
            sb.append("lim  f(x)  as x → ").append(approachValue).append("\n");
            sb.append("f(x) = ").append(expression).append("\n\n");

            if ("inf".equalsIgnoreCase(approachValue) || "+inf".equalsIgnoreCase(approachValue)) {
                sb.append("Approaching +∞:\n");
                double[] points = {1e1, 1e2, 1e3, 1e4, 1e6, 1e8, 1e10};
                for (double x : points) {
                    sb.append("  f(").append(sci(x)).append(") = ").append(fmt(f.applyAsDouble(x))).append("\n");
                }
                sb.append("\nLimit ≈ ").append(fmt(f.applyAsDouble(1e12)));
            } else if ("-inf".equalsIgnoreCase(approachValue)) {
                sb.append("Approaching -∞:\n");
                double[] points = {-1e1, -1e2, -1e3, -1e4, -1e6, -1e8, -1e10};
                for (double x : points) {
                    sb.append("  f(").append(sci(x)).append(") = ").append(fmt(f.applyAsDouble(x))).append("\n");
                }
                sb.append("\nLimit ≈ ").append(fmt(f.applyAsDouble(-1e12)));
            } else {
                double target = Double.parseDouble(approachValue);
                double[] offsets = {0.1, 0.01, 0.001, 0.0001, 0.00001, 0.000001};

                if (!"left".equals(dir)) {
                    sb.append("From the right (x → ").append(fmt(target)).append("⁺):\n");
                    for (double h : offsets) {
                        double x = target + h;
                        sb.append("  f(").append(fmt(x)).append(") = ").append(fmt(f.applyAsDouble(x))).append("\n");
                    }
                    sb.append("\n");
                }

                if (!"right".equals(dir)) {
                    sb.append("From the left (x → ").append(fmt(target)).append("⁻):\n");
                    for (double h : offsets) {
                        double x = target - h;
                        sb.append("  f(").append(fmt(x)).append(") = ").append(fmt(f.applyAsDouble(x))).append("\n");
                    }
                    sb.append("\n");
                }

                double rightLimit = f.applyAsDouble(target + 1e-10);
                double leftLimit = f.applyAsDouble(target - 1e-10);

                if (Math.abs(rightLimit - leftLimit) < 1e-4) {
                    sb.append("Limit ≈ ").append(fmt((rightLimit + leftLimit) / 2));
                    sb.append(" (left and right limits agree)");
                } else {
                    sb.append("Left limit ≈ ").append(fmt(leftLimit)).append("\n");
                    sb.append("Right limit ≈ ").append(fmt(rightLimit)).append("\n");
                    sb.append("LIMIT DOES NOT EXIST (left ≠ right)");
                }
            }

            return sb.toString();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "series_sum", description = "Compute partial sums of mathematical series. "
            + "Supports common series: geometric, harmonic, p-series, Taylor series for common functions.")
    public String seriesSum(
            @ToolParam(description = "Series type: 'geometric' (a*r^n), 'harmonic' (1/n), "
                    + "'p_series' (1/n^p), 'taylor_sin' (Taylor series for sin(x)), "
                    + "'taylor_cos' (Taylor series for cos(x)), 'taylor_exp' (Taylor series for e^x), "
                    + "'taylor_ln' (Taylor series for ln(1+x)), 'custom' (user expression with 'n')") String seriesType,
            @ToolParam(description = """
                    Comma-separated parameters:
                      geometric: first_term, common_ratio, num_terms
                      harmonic: num_terms
                      p_series: p, num_terms
                      taylor_*: x_value, num_terms
                      custom: expression_with_n, num_terms""") String values) {

        try {
            return switch (seriesType.strip().toLowerCase()) {
                case "geometric" -> {
                    double[] v = parseValues(values);
                    check(v, 3, "a, r, n");
                    double a = v[0], r = v[1];
                    int n = (int) v[2];

                    StringBuilder terms = new StringBuilder();
                    for (int i = 0; i < n && i < 10; i++) {
                        double term = a * Math.pow(r, i);
                        if (i > 0) terms.append(" + ");
                        terms.append(fmt(term));
                    }
                    if (n > 10) terms.append(" + ...");

                    // Full partial sum
                    double fullSum = 0;
                    for (int i = 0; i < n; i++) fullSum += a * Math.pow(r, i);

                    String infinite;
                    if (Math.abs(r) < 1) {
                        double infSum = a / (1 - r);
                        infinite = "\nInfinite sum (|r|<1): S∞ = a/(1-r) = " + fmt(infSum);
                    } else {
                        infinite = "\nSeries diverges (|r| ≥ 1)";
                    }

                    yield "Geometric Series\n────────────────\n"
                            + "a = " + fmt(a) + ", r = " + fmt(r) + ", n = " + n + "\n"
                            + "Terms: " + terms + "\n\n"
                            + "Partial sum S(" + n + ") = " + fmt(fullSum) + "\n"
                            + "Formula: S(n) = a(1-rⁿ)/(1-r) = " + fmt(a * (1 - Math.pow(r, n)) / (1 - r))
                            + infinite;
                }
                case "harmonic" -> {
                    int n = Integer.parseInt(values.strip());
                    double sum = 0;
                    for (int i = 1; i <= n; i++) sum += 1.0 / i;
                    yield "Harmonic Series\n───────────────\n"
                            + "H(" + n + ") = Σ(1/k) for k=1 to " + n + "\n\n"
                            + "Partial sum = " + fmt(sum) + "\n"
                            + "Approximation: ln(" + n + ") + γ ≈ " + fmt(Math.log(n) + 0.5772156649)
                            + "\nThe harmonic series diverges (grows without bound)";
                }
                case "p_series" -> {
                    double[] v = parseValues(values);
                    check(v, 2, "p, num_terms");
                    double p = v[0];
                    int n = (int) v[1];
                    double sum = 0;
                    for (int i = 1; i <= n; i++) sum += 1.0 / Math.pow(i, p);
                    String convergence = p > 1 ? "CONVERGES (p > 1)" : p == 1 ? "DIVERGES (harmonic)" : "DIVERGES (p < 1)";
                    String known = "";
                    if (p == 2) known = "\nExact value (Basel problem): π²/6 ≈ " + fmt(Math.PI * Math.PI / 6);
                    yield "p-Series\n────────\n"
                            + "Σ(1/n^" + fmt(p) + ") for n=1 to " + n + "\n\n"
                            + "Partial sum = " + fmt(sum) + "\n"
                            + "Convergence: " + convergence + known;
                }
                case "taylor_sin" -> {
                    double[] v = parseValues(values);
                    check(v, 2, "x, num_terms");
                    double x = v[0];
                    int n = (int) v[1];
                    double sum = 0;
                    StringBuilder terms = new StringBuilder();
                    for (int k = 0; k < n; k++) {
                        double term = Math.pow(-1, k) * Math.pow(x, 2 * k + 1) / factorial(2 * k + 1);
                        sum += term;
                        if (k > 0) terms.append(k % 2 == 0 ? " + " : " - ");
                        else if (k == 0) terms.append("");
                        terms.append("x^").append(2 * k + 1).append("/").append(2 * k + 1).append("!");
                    }
                    yield taylorResult("sin(x)", x, n, sum, Math.sin(x), terms.toString());
                }
                case "taylor_cos" -> {
                    double[] v = parseValues(values);
                    check(v, 2, "x, num_terms");
                    double x = v[0];
                    int n = (int) v[1];
                    double sum = 0;
                    for (int k = 0; k < n; k++) {
                        sum += Math.pow(-1, k) * Math.pow(x, 2 * k) / factorial(2 * k);
                    }
                    yield taylorResult("cos(x)", x, n, sum, Math.cos(x), "1 - x²/2! + x⁴/4! - ...");
                }
                case "taylor_exp" -> {
                    double[] v = parseValues(values);
                    check(v, 2, "x, num_terms");
                    double x = v[0];
                    int n = (int) v[1];
                    double sum = 0;
                    for (int k = 0; k < n; k++) {
                        sum += Math.pow(x, k) / factorial(k);
                    }
                    yield taylorResult("e^x", x, n, sum, Math.exp(x), "1 + x + x²/2! + x³/3! + ...");
                }
                case "taylor_ln" -> {
                    double[] v = parseValues(values);
                    check(v, 2, "x, num_terms");
                    double x = v[0];
                    if (x <= -1 || x > 1) yield "Error: Taylor series for ln(1+x) converges only for -1 < x ≤ 1";
                    int n = (int) v[1];
                    double sum = 0;
                    for (int k = 1; k <= n; k++) {
                        sum += Math.pow(-1, k + 1) * Math.pow(x, k) / k;
                    }
                    yield taylorResult("ln(1+x)", x, n, sum, Math.log(1 + x), "x - x²/2 + x³/3 - ...");
                }
                default -> "Unknown series type. Use: geometric, harmonic, p_series, taylor_sin, taylor_cos, taylor_exp, taylor_ln";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "matrix_operations", description = "Perform matrix operations: determinant, inverse, multiplication, "
            + "transpose, eigenvalues (2×2), and solving systems of equations (Ax=b). "
            + "Matrices are entered row by row separated by semicolons.")
    public String matrixOperations(
            @ToolParam(description = "Operation: 'determinant', 'inverse', 'multiply', 'transpose', "
                    + "'eigenvalues' (2×2 only), 'solve' (Ax=b)") String operation,
            @ToolParam(description = "Matrix entered row by row, separated by semicolons. "
                    + "Example: '1,2;3,4' for [[1,2],[3,4]]. "
                    + "For 'multiply': two matrices separated by '|'. Example: '1,2;3,4|5,6;7,8'. "
                    + "For 'solve': augmented matrix [A|b]. Example: '2,1,5;1,3,7' for 2x+y=5, x+3y=7") String matrixStr) {

        try {
            return switch (operation.strip().toLowerCase()) {
                case "determinant" -> {
                    double[][] A = parseMatrix(matrixStr);
                    if (A.length != A[0].length) yield "Error: Determinant requires a square matrix.";
                    double det = determinant(A);
                    yield matrixResult("Determinant", A, "det(A) = " + fmt(det));
                }
                case "transpose" -> {
                    double[][] A = parseMatrix(matrixStr);
                    double[][] T = transpose(A);
                    yield matrixResult("Transpose", A, "Aᵀ =\n" + formatMatrix(T));
                }
                case "inverse" -> {
                    double[][] A = parseMatrix(matrixStr);
                    if (A.length != A[0].length) yield "Error: Inverse requires a square matrix.";
                    if (A.length != 2) yield "Error: Currently supports 2×2 inverse.";
                    double det = determinant(A);
                    if (Math.abs(det) < 1e-12) yield "Matrix is singular (det=0), no inverse exists.";
                    double[][] inv = {
                            {A[1][1] / det, -A[0][1] / det},
                            {-A[1][0] / det, A[0][0] / det}
                    };
                    yield matrixResult("Inverse", A, "det = " + fmt(det) + "\n\nA⁻¹ =\n" + formatMatrix(inv));
                }
                case "multiply" -> {
                    String[] parts = matrixStr.split("\\|");
                    if (parts.length != 2) yield "Error: For multiplication, separate two matrices with '|'.";
                    double[][] A = parseMatrix(parts[0]);
                    double[][] B = parseMatrix(parts[1]);
                    if (A[0].length != B.length) yield "Error: Incompatible dimensions for multiplication. "
                            + "A is " + A.length + "×" + A[0].length + " but B is " + B.length + "×" + B[0].length;
                    double[][] C = multiply(A, B);
                    yield "Matrix Multiplication\n─────────────────────\n"
                            + "A (" + A.length + "×" + A[0].length + ") =\n" + formatMatrix(A) + "\n\n"
                            + "B (" + B.length + "×" + B[0].length + ") =\n" + formatMatrix(B) + "\n\n"
                            + "A × B =\n" + formatMatrix(C);
                }
                case "eigenvalues" -> {
                    double[][] A = parseMatrix(matrixStr);
                    if (A.length != 2 || A[0].length != 2) yield "Error: Eigenvalue computation currently supports 2×2 matrices.";
                    double trace = A[0][0] + A[1][1];
                    double det = determinant(A);
                    double disc = trace * trace - 4 * det;

                    StringBuilder sb = new StringBuilder();
                    sb.append("Eigenvalues (2×2)\n─────────────────\n");
                    sb.append("A =\n").append(formatMatrix(A)).append("\n\n");
                    sb.append("Characteristic equation: λ² - (trace)λ + det = 0\n");
                    sb.append("λ² - ").append(fmt(trace)).append("λ + ").append(fmt(det)).append(" = 0\n\n");
                    sb.append("Trace = ").append(fmt(trace)).append(", Det = ").append(fmt(det)).append("\n");
                    sb.append("Discriminant = ").append(fmt(disc)).append("\n\n");

                    if (disc >= 0) {
                        double l1 = (trace + Math.sqrt(disc)) / 2;
                        double l2 = (trace - Math.sqrt(disc)) / 2;
                        sb.append("λ₁ = ").append(fmt(l1)).append("\nλ₂ = ").append(fmt(l2));
                    } else {
                        double real = trace / 2;
                        double imag = Math.sqrt(-disc) / 2;
                        sb.append("λ₁ = ").append(fmt(real)).append(" + ").append(fmt(imag)).append("i\n");
                        sb.append("λ₂ = ").append(fmt(real)).append(" - ").append(fmt(imag)).append("i");
                    }
                    yield sb.toString();
                }
                case "solve" -> {
                    double[][] aug = parseMatrix(matrixStr);
                    yield solveLinearSystem(aug);
                }
                default -> "Unknown operation. Use: determinant, inverse, multiply, transpose, eigenvalues, solve";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ── Expression Parser (reuses MathTool.ExpressionParser logic) ──

    private DoubleUnaryOperator parseExpression(String expr) {
        return x -> {
            String substituted = expr
                    .replace("e^", "exp(")  // handle e^x → exp(x)
                    .replace("ln(", "log(");

            // Handle e^(...) by adding closing paren if needed
            if (expr.contains("e^") && !expr.contains("exp(")) {
                // simple case: e^x
                substituted = expr.replace("e^x", Math.E + "^x");
            }

            String withX = substituted.replaceAll("(?<![a-zA-Z])x(?![a-zA-Z])", String.valueOf(x));
            return new MathTool.ExpressionParser(withX).parse();
        };
    }

    // ── Matrix Helpers ──

    private double[][] parseMatrix(String str) {
        String[] rows = str.strip().split(";");
        double[][] matrix = new double[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            String[] vals = rows[i].strip().split(",");
            matrix[i] = new double[vals.length];
            for (int j = 0; j < vals.length; j++) {
                matrix[i][j] = Double.parseDouble(vals[j].strip());
            }
        }
        return matrix;
    }

    private double determinant(double[][] m) {
        int n = m.length;
        if (n == 1) return m[0][0];
        if (n == 2) return m[0][0] * m[1][1] - m[0][1] * m[1][0];
        if (n == 3) {
            return m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1])
                    - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
                    + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
        }
        // Cofactor expansion for larger matrices
        double det = 0;
        for (int j = 0; j < n; j++) {
            det += Math.pow(-1, j) * m[0][j] * determinant(minor(m, 0, j));
        }
        return det;
    }

    private double[][] minor(double[][] m, int row, int col) {
        int n = m.length;
        double[][] result = new double[n - 1][n - 1];
        int r = 0;
        for (int i = 0; i < n; i++) {
            if (i == row) continue;
            int c = 0;
            for (int j = 0; j < n; j++) {
                if (j == col) continue;
                result[r][c++] = m[i][j];
            }
            r++;
        }
        return result;
    }

    private double[][] transpose(double[][] m) {
        double[][] t = new double[m[0].length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m[0].length; j++)
                t[j][i] = m[i][j];
        return t;
    }

    private double[][] multiply(double[][] A, double[][] B) {
        int m = A.length, n = B[0].length, p = A[0].length;
        double[][] C = new double[m][n];
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                for (int k = 0; k < p; k++)
                    C[i][j] += A[i][k] * B[k][j];
        return C;
    }

    private String solveLinearSystem(double[][] aug) {
        int n = aug.length;
        int cols = aug[0].length;
        if (cols != n + 1) return "Error: Augmented matrix should have n rows and n+1 columns.";

        // Gaussian elimination
        double[][] m = new double[n][cols];
        for (int i = 0; i < n; i++) System.arraycopy(aug[i], 0, m[i], 0, cols);

        for (int i = 0; i < n; i++) {
            // Partial pivoting
            int maxRow = i;
            for (int k = i + 1; k < n; k++) {
                if (Math.abs(m[k][i]) > Math.abs(m[maxRow][i])) maxRow = k;
            }
            double[] temp = m[i]; m[i] = m[maxRow]; m[maxRow] = temp;

            if (Math.abs(m[i][i]) < 1e-12) return "Error: System has no unique solution (singular matrix).";

            for (int k = i + 1; k < n; k++) {
                double factor = m[k][i] / m[i][i];
                for (int j = i; j < cols; j++) m[k][j] -= factor * m[i][j];
            }
        }

        // Back substitution
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            x[i] = m[i][cols - 1];
            for (int j = i + 1; j < n; j++) x[i] -= m[i][j] * x[j];
            x[i] /= m[i][i];
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Linear System Solution (Gaussian Elimination)\n");
        sb.append("──────────────────────────────────────────────\n");
        sb.append("Augmented matrix [A|b]:\n").append(formatMatrix(aug)).append("\n\n");
        sb.append("Solution:\n");
        for (int i = 0; i < n; i++) {
            sb.append("  x").append(i + 1).append(" = ").append(fmt(x[i])).append("\n");
        }

        return sb.toString();
    }

    private String formatMatrix(double[][] m) {
        StringBuilder sb = new StringBuilder();
        for (double[] row : m) {
            sb.append("  [");
            for (int j = 0; j < row.length; j++) {
                if (j > 0) sb.append(", ");
                sb.append(String.format("%10s", fmt(row[j])));
            }
            sb.append("]\n");
        }
        return sb.toString().stripTrailing();
    }

    private String matrixResult(String title, double[][] A, String result) {
        return title + "\n" + "─".repeat(title.length()) + "\n"
                + "A (" + A.length + "×" + A[0].length + ") =\n" + formatMatrix(A) + "\n\n" + result;
    }

    private String taylorResult(String func, double x, int terms, double approx, double exact, String series) {
        return "Taylor Series: " + func + "\n───────────────────\n"
                + "Series: " + series + "\n"
                + "x = " + fmt(x) + ", terms = " + terms + "\n\n"
                + "Taylor approximation: " + fmt(approx) + "\n"
                + "Exact value:          " + fmt(exact) + "\n"
                + "Error:                " + sci(Math.abs(approx - exact));
    }

    // ── Utility ──

    private double factorial(int n) {
        double result = 1;
        for (int i = 2; i <= n; i++) result *= i;
        return result;
    }

    private double[] parseValues(String values) {
        String[] parts = values.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = Double.parseDouble(parts[i].strip());
        return result;
    }

    private void check(double[] v, int min, String names) {
        if (v.length < min) throw new IllegalArgumentException("Need " + min + " values: " + names);
    }

    private String fmt(double v) {
        if (Double.isNaN(v)) return "NaN";
        if (Double.isInfinite(v)) return v > 0 ? "∞" : "-∞";
        if (v == Math.floor(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return BigDecimal.valueOf(v).round(MC).stripTrailingZeros().toPlainString();
    }

    private String sci(double v) { return String.format("%.6e", v); }
}
