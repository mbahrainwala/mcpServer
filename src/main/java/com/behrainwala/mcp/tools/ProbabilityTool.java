package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for probability and combinatorics calculations.
 * Covers permutations, combinations, probability distributions,
 * Bayes' theorem, expected value, and Markov chains.
 */
@Service
public class ProbabilityTool {

    // ─────────────────────────────────────────────
    // 1. Permutations & Combinations
    // ─────────────────────────────────────────────

    @Tool(name = "probability_combinatorics",
            description = "Calculate permutations and combinations. "
                    + "Supports: permutation P(n,r) = n!/(n-r)!, "
                    + "combination C(n,r) = n!/(r!(n-r)!), "
                    + "permutation_repetition n^r, "
                    + "combination_repetition C(n+r-1,r) = (n+r-1)!/(r!(n-1)!).")
    public String probabilityCombinatorics(
            @ToolParam(description = "Total number of items (n)") int n,
            @ToolParam(description = "Number of items chosen (r)") int r,
            @ToolParam(description = "Type of calculation: permutation, combination, permutation_repetition, or combination_repetition") String type) {

        if (n < 0 || r < 0) {
            return "Error: n and r must be non-negative integers.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Combinatorics ===\n");
        sb.append("n = ").append(n).append(", r = ").append(r).append("\n");
        sb.append("Type: ").append(type).append("\n\n");

        try {
            switch (type.toLowerCase().trim()) {
                case "permutation" -> {
                    if (r > n) {
                        return "Error: r cannot be greater than n for permutations without repetition.";
                    }
                    double result = factorial(n) / factorial(n - r);
                    sb.append("Formula: P(n, r) = n! / (n - r)!\n");
                    sb.append("P(").append(n).append(", ").append(r).append(") = ")
                            .append(n).append("! / ").append(n - r).append("!\n");
                    sb.append("Result: ").append(formatNumber(result)).append("\n");
                }
                case "combination" -> {
                    if (r > n) {
                        return "Error: r cannot be greater than n for combinations without repetition.";
                    }
                    double result = factorial(n) / (factorial(r) * factorial(n - r));
                    sb.append("Formula: C(n, r) = n! / (r! * (n - r)!)\n");
                    sb.append("C(").append(n).append(", ").append(r).append(") = ")
                            .append(n).append("! / (").append(r).append("! * ").append(n - r).append("!)\n");
                    sb.append("Result: ").append(formatNumber(result)).append("\n");
                }
                case "permutation_repetition" -> {
                    double result = Math.pow(n, r);
                    sb.append("Formula: n^r\n");
                    sb.append(n).append("^").append(r).append("\n");
                    sb.append("Result: ").append(formatNumber(result)).append("\n");
                }
                case "combination_repetition" -> {
                    int total = n + r - 1;
                    double result = factorial(total) / (factorial(r) * factorial(total - r));
                    sb.append("Formula: C(n + r - 1, r) = (n + r - 1)! / (r! * (n - 1)!)\n");
                    sb.append("C(").append(total).append(", ").append(r).append(") = ")
                            .append(total).append("! / (").append(r).append("! * ").append(n - 1).append("!)\n");
                    sb.append("Result: ").append(formatNumber(result)).append("\n");
                }
                default -> {
                    return "Error: Unknown type '" + type + "'. Use: permutation, combination, permutation_repetition, or combination_repetition.";
                }
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────────
    // 2. Probability Distributions
    // ─────────────────────────────────────────────

    @Tool(name = "probability_distribution",
            description = "Calculate common probability distributions. "
                    + "Supported distributions: binomial, normal, poisson, geometric, uniform. "
                    + "Pass distribution-specific parameters as a comma-separated string: "
                    + "binomial → 'n,p,k' (trials,probability,successes); "
                    + "normal → 'mean,std_dev,x'; "
                    + "poisson → 'lambda,k'; "
                    + "geometric → 'p,k'; "
                    + "uniform → 'a,b,x'.")
    public String probabilityDistribution(
            @ToolParam(description = "Distribution type: binomial, normal, poisson, geometric, or uniform") String distribution,
            @ToolParam(description = "Comma-separated parameters specific to the distribution") String params) {

        if (distribution == null || distribution.isBlank()) {
            return "Error: distribution type is required.";
        }
        if (params == null || params.isBlank()) {
            return "Error: params are required.";
        }

        String[] parts = params.split(",");
        try {
            return switch (distribution.toLowerCase().trim()) {
                case "binomial" -> binomialDistribution(parts);
                case "normal" -> normalDistribution(parts);
                case "poisson" -> poissonDistribution(parts);
                case "geometric" -> geometricDistribution(parts);
                case "uniform" -> uniformDistribution(parts);
                default -> "Error: Unknown distribution '" + distribution
                        + "'. Use: binomial, normal, poisson, geometric, or uniform.";
            };
        } catch (NumberFormatException e) {
            return "Error: Invalid number in params. " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String binomialDistribution(String[] parts) {
        if (parts.length < 3) {
            return "Error: Binomial distribution requires 3 params: n, p, k";
        }
        int n = Integer.parseInt(parts[0].trim());
        double p = Double.parseDouble(parts[1].trim());
        int k = Integer.parseInt(parts[2].trim());

        if (n < 0) return "Error: n (trials) must be non-negative.";
        if (p < 0 || p > 1) return "Error: p (probability) must be between 0 and 1.";
        if (k < 0 || k > n) return "Error: k (successes) must be between 0 and n.";

        double q = 1.0 - p;
        double comb = factorial(n) / (factorial(k) * factorial(n - k));
        double pmf = comb * Math.pow(p, k) * Math.pow(q, n - k);

        double cdf = 0.0;
        for (int i = 0; i <= k; i++) {
            double c = factorial(n) / (factorial(i) * factorial(n - i));
            cdf += c * Math.pow(p, i) * Math.pow(q, n - i);
        }

        double mean = n * p;
        double variance = n * p * q;

        return "=== Binomial Distribution ===\n" +
                "Parameters: n = " + n + ", p = " + p + ", k = " + k + "\n\n" +
                "Formula: P(X = k) = C(n,k) * p^k * (1-p)^(n-k)\n" +
                "C(" + n + ", " + k + ") = " + formatNumber(comb) + "\n\n" +
                "P(X = " + k + ") = " + formatNumber(pmf) + "\n" +
                "P(X <= " + k + ") = " + formatNumber(cdf) + "\n" +
                "Mean: E(X) = n*p = " + formatNumber(mean) + "\n" +
                "Variance: Var(X) = n*p*(1-p) = " + formatNumber(variance) + "\n" +
                "Std Dev: σ = " + formatNumber(Math.sqrt(variance)) + "\n";
    }

    private String normalDistribution(String[] parts) {
        if (parts.length < 3) {
            return "Error: Normal distribution requires 3 params: mean, std_dev, x";
        }
        double mean = Double.parseDouble(parts[0].trim());
        double stdDev = Double.parseDouble(parts[1].trim());
        double x = Double.parseDouble(parts[2].trim());

        if (stdDev <= 0) return "Error: std_dev must be positive.";

        double z = (x - mean) / stdDev;
        double cdf = normalCdf(z);

        return "=== Normal Distribution ===\n" +
                "Parameters: mean (μ) = " + formatNumber(mean) +
                ", std_dev (σ) = " + formatNumber(stdDev) +
                ", x = " + formatNumber(x) + "\n\n" +
                "Formula: z = (x - μ) / σ\n" +
                "z = (" + formatNumber(x) + " - " + formatNumber(mean) +
                ") / " + formatNumber(stdDev) + "\n" +
                "z-score = " + formatNumber(z) + "\n\n" +
                "P(X <= " + formatNumber(x) + ") = Φ(" + formatNumber(z) +
                ") ≈ " + formatNumber(cdf) + "\n" +
                "P(X > " + formatNumber(x) + ") ≈ " + formatNumber(1.0 - cdf) + "\n" +
                "\n(Normal CDF computed via Abramowitz & Stegun approximation)\n";
    }

    private String poissonDistribution(String[] parts) {
        if (parts.length < 2) {
            return "Error: Poisson distribution requires 2 params: lambda, k";
        }
        double lambda = Double.parseDouble(parts[0].trim());
        int k = Integer.parseInt(parts[1].trim());

        if (lambda <= 0) return "Error: lambda must be positive.";
        if (k < 0) return "Error: k must be non-negative.";

        double pmf = Math.pow(lambda, k) * Math.exp(-lambda) / factorial(k);

        double cdf = 0.0;
        for (int i = 0; i <= k; i++) {
            cdf += Math.pow(lambda, i) * Math.exp(-lambda) / factorial(i);
        }

        return "=== Poisson Distribution ===\n" +
                "Parameters: λ = " + formatNumber(lambda) + ", k = " + k + "\n\n" +
                "Formula: P(X = k) = (λ^k * e^(-λ)) / k!\n" +
                "P(X = " + k + ") = (" + formatNumber(lambda) + "^" +
                k + " * e^(-" + formatNumber(lambda) + ")) / " + k + "!\n" +
                "P(X = " + k + ") = " + formatNumber(pmf) + "\n" +
                "P(X <= " + k + ") = " + formatNumber(cdf) + "\n" +
                "Mean: E(X) = λ = " + formatNumber(lambda) + "\n" +
                "Variance: Var(X) = λ = " + formatNumber(lambda) + "\n" +
                "Std Dev: σ = " + formatNumber(Math.sqrt(lambda)) + "\n";
    }

    private String geometricDistribution(String[] parts) {
        if (parts.length < 2) {
            return "Error: Geometric distribution requires 2 params: p, k";
        }
        double p = Double.parseDouble(parts[0].trim());
        int k = Integer.parseInt(parts[1].trim());

        if (p <= 0 || p > 1) return "Error: p must be in (0, 1].";
        if (k < 1) return "Error: k must be at least 1 (trial number of first success).";

        double q = 1.0 - p;
        double pmf = Math.pow(q, k - 1) * p;
        double cdf = 1.0 - Math.pow(q, k);
        double mean = 1.0 / p;
        double variance = q / (p * p);

        return "=== Geometric Distribution ===\n" +
                "Parameters: p = " + formatNumber(p) + ", k = " + k + "\n\n" +
                "Formula: P(X = k) = (1-p)^(k-1) * p\n" +
                "P(X = " + k + ") = (1-" + formatNumber(p) + ")^" +
                (k - 1) + " * " + formatNumber(p) + "\n" +
                "P(X = " + k + ") = " + formatNumber(pmf) + "\n" +
                "P(X <= " + k + ") = 1 - (1-p)^k = " + formatNumber(cdf) + "\n" +
                "Mean: E(X) = 1/p = " + formatNumber(mean) + "\n" +
                "Variance: Var(X) = (1-p)/p² = " + formatNumber(variance) + "\n" +
                "Std Dev: σ = " + formatNumber(Math.sqrt(variance)) + "\n";
    }

    private String uniformDistribution(String[] parts) {
        if (parts.length < 3) {
            return "Error: Uniform distribution requires 3 params: a, b, x";
        }
        double a = Double.parseDouble(parts[0].trim());
        double b = Double.parseDouble(parts[1].trim());
        double x = Double.parseDouble(parts[2].trim());

        if (a >= b) return "Error: a must be less than b.";

        double cdf;
        if (x < a) {
            cdf = 0.0;
        } else if (x > b) {
            cdf = 1.0;
        } else {
            cdf = (x - a) / (b - a);
        }

        double mean = (a + b) / 2.0;
        double variance = Math.pow(b - a, 2) / 12.0;

        return "=== Continuous Uniform Distribution ===\n" +
                "Parameters: a = " + formatNumber(a) + ", b = " + formatNumber(b) +
                ", x = " + formatNumber(x) + "\n\n" +
                "PDF: f(x) = 1/(b-a) = " + formatNumber(1.0 / (b - a)) + " for a <= x <= b\n" +
                "CDF: P(X <= x) = (x - a) / (b - a)\n" +
                "P(X <= " + formatNumber(x) + ") = " + formatNumber(cdf) + "\n" +
                "Mean: E(X) = (a+b)/2 = " + formatNumber(mean) + "\n" +
                "Variance: Var(X) = (b-a)²/12 = " + formatNumber(variance) + "\n" +
                "Std Dev: σ = " + formatNumber(Math.sqrt(variance)) + "\n";
    }

    // ─────────────────────────────────────────────
    // 3. Bayes' Theorem
    // ─────────────────────────────────────────────

    @Tool(name = "probability_bayes",
            description = "Apply Bayes' theorem to calculate the posterior probability. "
                    + "P(A|B) = P(B|A) * P(A) / P(B). "
                    + "Provide: prior probability P(A), likelihood P(B|A), and marginal probability P(B).")
    public String probabilityBayes(
            @ToolParam(description = "Prior probability P(A), between 0 and 1") double prior_probability,
            @ToolParam(description = "Likelihood P(B|A), between 0 and 1") double likelihood,
            @ToolParam(description = "Marginal probability P(B), between 0 and 1, must be > 0") double marginal_probability) {

        if (prior_probability < 0 || prior_probability > 1) {
            return "Error: prior_probability P(A) must be between 0 and 1.";
        }
        if (likelihood < 0 || likelihood > 1) {
            return "Error: likelihood P(B|A) must be between 0 and 1.";
        }
        if (marginal_probability <= 0 || marginal_probability > 1) {
            return "Error: marginal_probability P(B) must be between 0 (exclusive) and 1.";
        }

        double posterior = (likelihood * prior_probability) / marginal_probability;

        StringBuilder sb = new StringBuilder();
        sb.append("=== Bayes' Theorem ===\n\n");
        sb.append("Formula: P(A|B) = P(B|A) * P(A) / P(B)\n\n");
        sb.append("Given:\n");
        sb.append("  P(A)   = ").append(formatNumber(prior_probability)).append("  (prior probability)\n");
        sb.append("  P(B|A) = ").append(formatNumber(likelihood)).append("  (likelihood)\n");
        sb.append("  P(B)   = ").append(formatNumber(marginal_probability)).append("  (marginal probability)\n\n");
        sb.append("Calculation:\n");
        sb.append("  P(A|B) = ").append(formatNumber(likelihood)).append(" * ")
                .append(formatNumber(prior_probability)).append(" / ")
                .append(formatNumber(marginal_probability)).append("\n");
        sb.append("  P(A|B) = ").append(formatNumber(likelihood * prior_probability))
                .append(" / ").append(formatNumber(marginal_probability)).append("\n\n");
        sb.append("Posterior Probability: P(A|B) = ").append(formatNumber(posterior)).append("\n");

        if (posterior > 1.0) {
            sb.append("\nWarning: Posterior > 1.0 — check that your inputs are consistent.\n");
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────────
    // 4. Expected Value & Variance
    // ─────────────────────────────────────────────

    @Tool(name = "probability_expected_value",
            description = "Calculate the expected value E(X), variance Var(X), and standard deviation "
                    + "for a discrete probability distribution. "
                    + "Provide values and their corresponding probabilities as comma-separated strings. "
                    + "The probabilities should sum to 1.")
    public String probabilityExpectedValue(
            @ToolParam(description = "Comma-separated values of the random variable X (e.g. '1,2,3,4')") String values,
            @ToolParam(description = "Comma-separated probabilities corresponding to each value (e.g. '0.1,0.2,0.3,0.4')") String probabilities) {

        if (values == null || values.isBlank() || probabilities == null || probabilities.isBlank()) {
            return "Error: Both values and probabilities are required.";
        }

        String[] valParts = values.split(",");
        String[] probParts = probabilities.split(",");

        if (valParts.length != probParts.length) {
            return "Error: The number of values (" + valParts.length
                    + ") must match the number of probabilities (" + probParts.length + ").";
        }

        int n = valParts.length;
        double[] vals = new double[n];
        double[] probs = new double[n];

        try {
            for (int i = 0; i < n; i++) {
                vals[i] = Double.parseDouble(valParts[i].trim());
                probs[i] = Double.parseDouble(probParts[i].trim());
                if (probs[i] < 0) {
                    return "Error: Probabilities must be non-negative. Found: " + probs[i];
                }
            }
        } catch (NumberFormatException e) {
            return "Error: Invalid number in input. " + e.getMessage();
        }

        double probSum = 0;
        for (double p : probs) probSum += p;

        // Expected value E(X)
        double ex = 0;
        for (int i = 0; i < n; i++) {
            ex += vals[i] * probs[i];
        }

        // E(X^2)
        double ex2 = 0;
        for (int i = 0; i < n; i++) {
            ex2 += vals[i] * vals[i] * probs[i];
        }

        double variance = ex2 - ex * ex;
        double stdDev = Math.sqrt(Math.abs(variance));

        StringBuilder sb = new StringBuilder();
        sb.append("=== Expected Value & Variance ===\n\n");

        // Probability distribution table
        sb.append("Probability Distribution Table:\n");
        sb.append(String.format("%-12s | %-12s | %-14s\n", "X", "P(X)", "X * P(X)"));
        sb.append("-".repeat(42)).append("\n");
        for (int i = 0; i < n; i++) {
            sb.append(String.format("%-12s | %-12s | %-14s\n",
                    formatNumber(vals[i]),
                    formatNumber(probs[i]),
                    formatNumber(vals[i] * probs[i])));
        }
        sb.append("\n");

        if (Math.abs(probSum - 1.0) > 1e-9) {
            sb.append("Warning: Probabilities sum to ").append(formatNumber(probSum))
                    .append(" (expected 1.0)\n\n");
        }

        sb.append("E(X)   = Σ [x * P(x)] = ").append(formatNumber(ex)).append("\n");
        sb.append("E(X²)  = Σ [x² * P(x)] = ").append(formatNumber(ex2)).append("\n");
        sb.append("Var(X) = E(X²) - [E(X)]² = ").append(formatNumber(ex2))
                .append(" - ").append(formatNumber(ex)).append("² = ").append(formatNumber(variance)).append("\n");
        sb.append("Std Dev (σ) = √Var(X) = ").append(formatNumber(stdDev)).append("\n");

        return sb.toString();
    }

    // ─────────────────────────────────────────────
    // 5. Markov Chain
    // ─────────────────────────────────────────────

    @Tool(name = "probability_markov_chain",
            description = "Compute the state of a Markov chain after n steps. "
                    + "Provide the transition matrix as semicolon-separated rows with comma-separated values "
                    + "(e.g. '0.7,0.3;0.4,0.6'), an initial state vector as comma-separated values "
                    + "(e.g. '1,0'), and the number of steps.")
    public String probabilityMarkovChain(
            @ToolParam(description = "Transition matrix: rows separated by semicolons, values by commas (e.g. '0.7,0.3;0.4,0.6')") String transition_matrix,
            @ToolParam(description = "Initial state vector, comma-separated (e.g. '1,0' or '0.5,0.5')") String initial_state,
            @ToolParam(description = "Number of steps to simulate") int steps) {

        if (transition_matrix == null || transition_matrix.isBlank()) {
            return "Error: transition_matrix is required.";
        }
        if (initial_state == null || initial_state.isBlank()) {
            return "Error: initial_state is required.";
        }
        if (steps < 0) {
            return "Error: steps must be non-negative.";
        }

        try {
            // Parse transition matrix
            String[] rows = transition_matrix.split(";");
            int size = rows.length;
            double[][] matrix = new double[size][size];
            for (int i = 0; i < size; i++) {
                String[] cols = rows[i].trim().split(",");
                if (cols.length != size) {
                    return "Error: Transition matrix must be square. Row " + i
                            + " has " + cols.length + " columns, expected " + size + ".";
                }
                for (int j = 0; j < size; j++) {
                    matrix[i][j] = Double.parseDouble(cols[j].trim());
                }
            }

            // Parse initial state
            String[] stateParts = initial_state.split(",");
            if (stateParts.length != size) {
                return "Error: Initial state vector length (" + stateParts.length
                        + ") must match matrix dimension (" + size + ").";
            }
            double[] state = new double[size];
            for (int i = 0; i < size; i++) {
                state[i] = Double.parseDouble(stateParts[i].trim());
            }

            // Validate row stochastic
            StringBuilder warnings = new StringBuilder();
            for (int i = 0; i < size; i++) {
                double rowSum = 0;
                for (int j = 0; j < size; j++) rowSum += matrix[i][j];
                if (Math.abs(rowSum - 1.0) > 1e-9) {
                    warnings.append("Warning: Row ").append(i).append(" sums to ")
                            .append(formatNumber(rowSum)).append(" (expected 1.0)\n");
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== Markov Chain ===\n\n");
            sb.append("Transition Matrix:\n");
            for (int i = 0; i < size; i++) {
                sb.append("  [ ");
                for (int j = 0; j < size; j++) {
                    if (j > 0) sb.append(", ");
                    sb.append(formatNumber(matrix[i][j]));
                }
                sb.append(" ]\n");
            }
            sb.append("\nInitial State: [ ");
            for (int i = 0; i < size; i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatNumber(state[i]));
            }
            sb.append(" ]\n");
            sb.append("Steps: ").append(steps).append("\n");

            if (!warnings.isEmpty()) {
                sb.append("\n").append(warnings);
            }

            // Multiply: state * matrix^steps
            // state(t+1) = state(t) * matrix  (row vector times matrix)
            double[] current = state.clone();
            for (int step = 0; step < steps; step++) {
                double[] next = new double[size];
                for (int j = 0; j < size; j++) {
                    for (int i = 0; i < size; i++) {
                        next[j] += current[i] * matrix[i][j];
                    }
                }
                current = next;
            }

            sb.append("\nState after ").append(steps).append(" step(s): [ ");
            for (int i = 0; i < size; i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatNumber(current[i]));
            }
            sb.append(" ]\n");

            // Show probability interpretation
            double stateSum = 0;
            for (double v : current) stateSum += v;
            if (Math.abs(stateSum - 1.0) < 1e-6) {
                sb.append("\nProbability interpretation:\n");
                for (int i = 0; i < size; i++) {
                    sb.append("  State ").append(i).append(": ")
                            .append(formatNumber(current[i] * 100)).append("%\n");
                }
            }

            return sb.toString();

        } catch (NumberFormatException e) {
            return "Error: Invalid number in input. " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────

    /**
     * Compute factorial of n. Returns as double to handle large values.
     * For n > 170, factorial overflows double — use Stirling's approximation conceptually,
     * but in practice we cap at 170.
     */
    private double factorial(int n) {
        if (n < 0) throw new IllegalArgumentException("Factorial is not defined for negative numbers.");
        if (n == 0 || n == 1) return 1.0;
        if (n > 170) throw new ArithmeticException("Factorial overflow: n = " + n + " exceeds double precision range.");
        double result = 1.0;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    /**
     * Standard normal CDF approximation using Abramowitz and Stegun formula 26.2.17.
     * Accurate to about 1e-5.
     */
    private double normalCdf(double z) {
        if (z < -8.0) return 0.0;
        if (z > 8.0) return 1.0;

        boolean negative = z < 0;
        if (negative) z = -z;

        // Abramowitz and Stegun constants
        double p = 0.2316419;
        double b1 = 0.319381530;
        double b2 = -0.356563782;
        double b3 = 1.781477937;
        double b4 = -1.821255978;
        double b5 = 1.330274429;

        double t = 1.0 / (1.0 + p * z);
        double phi = (1.0 / Math.sqrt(2.0 * Math.PI)) * Math.exp(-0.5 * z * z);

        double cdf = 1.0 - phi * (b1 * t + b2 * t * t + b3 * Math.pow(t, 3)
                + b4 * Math.pow(t, 4) + b5 * Math.pow(t, 5));

        return negative ? 1.0 - cdf : cdf;
    }

    /**
     * Format a number for display: show integers without decimals, otherwise up to 10 significant digits.
     */
    private String formatNumber(double value) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        if (value == Math.floor(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        // Remove trailing zeros
        String formatted = String.format("%.10g", value);
        if (formatted.contains(".")) {
            formatted = formatted.replaceAll("0+$", "");
            formatted = formatted.replaceAll("\\.$", "");
        }
        return formatted;
    }
}
