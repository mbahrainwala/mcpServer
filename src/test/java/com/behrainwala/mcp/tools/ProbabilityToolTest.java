package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProbabilityToolTest {

    private ProbabilityTool tool;

    @BeforeEach
    void setUp() {
        tool = new ProbabilityTool();
    }

    // ── probabilityCombinatorics ────────────────────────────────────────────

    @Nested
    class Combinatorics {

        @Test
        void permutation_P5_2_equals20() {
            String r = tool.probabilityCombinatorics(5, 2, "permutation");
            assertThat(r).contains("=== Combinatorics ===");
            assertThat(r).contains("Result: 20");
            assertThat(r).contains("Formula: P(n, r) = n! / (n - r)!");
        }

        @Test
        void permutation_P4_2_equals12() {
            String r = tool.probabilityCombinatorics(4, 2, "permutation");
            assertThat(r).contains("Result: 12");
        }

        @Test
        void permutation_P0_0_equals1() {
            String r = tool.probabilityCombinatorics(0, 0, "permutation");
            assertThat(r).contains("Result: 1");
        }

        @Test
        void permutation_rGreaterThanN_returnsError() {
            String r = tool.probabilityCombinatorics(2, 5, "permutation");
            assertThat(r).isEqualTo("Error: r cannot be greater than n for permutations without repetition.");
        }

        @Test
        void combination_C5_2_equals10() {
            String r = tool.probabilityCombinatorics(5, 2, "combination");
            assertThat(r).contains("Result: 10");
            assertThat(r).contains("Formula: C(n, r) = n! / (r! * (n - r)!)");
        }

        @Test
        void combination_C10_3_equals120() {
            String r = tool.probabilityCombinatorics(10, 3, "combination");
            assertThat(r).contains("Result: 120");
        }

        @Test
        void combination_C5_0_equals1() {
            String r = tool.probabilityCombinatorics(5, 0, "combination");
            assertThat(r).contains("Result: 1");
        }

        @Test
        void combination_C5_5_equals1() {
            String r = tool.probabilityCombinatorics(5, 5, "combination");
            assertThat(r).contains("Result: 1");
        }

        @Test
        void combination_rGreaterThanN_returnsError() {
            String r = tool.probabilityCombinatorics(2, 5, "combination");
            assertThat(r).isEqualTo("Error: r cannot be greater than n for combinations without repetition.");
        }

        @Test
        void permutationRepetition_3pow2_equals9() {
            String r = tool.probabilityCombinatorics(3, 2, "permutation_repetition");
            assertThat(r).contains("Result: 9");
            assertThat(r).contains("Formula: n^r");
        }

        @Test
        void permutationRepetition_2pow10_equals1024() {
            String r = tool.probabilityCombinatorics(2, 10, "permutation_repetition");
            assertThat(r).contains("Result: 1024");
        }

        @Test
        void combinationRepetition_n3_r2() {
            // C(3+2-1, 2) = C(4,2) = 6
            String r = tool.probabilityCombinatorics(3, 2, "combination_repetition");
            assertThat(r).contains("Result: 6");
            assertThat(r).contains("Formula: C(n + r - 1, r)");
        }

        @Test
        void combinationRepetition_n5_r2() {
            // C(5+2-1, 2) = C(6,2) = 15
            String r = tool.probabilityCombinatorics(5, 2, "combination_repetition");
            assertThat(r).contains("Result: 15");
        }

        @Test
        void negativeN_returnsError() {
            String r = tool.probabilityCombinatorics(-1, 2, "permutation");
            assertThat(r).isEqualTo("Error: n and r must be non-negative integers.");
        }

        @Test
        void negativeR_returnsError() {
            String r = tool.probabilityCombinatorics(5, -1, "permutation");
            assertThat(r).isEqualTo("Error: n and r must be non-negative integers.");
        }

        @Test
        void bothNegative_returnsError() {
            String r = tool.probabilityCombinatorics(-1, -2, "combination");
            assertThat(r).isEqualTo("Error: n and r must be non-negative integers.");
        }

        @Test
        void unknownType_returnsError() {
            String r = tool.probabilityCombinatorics(5, 2, "unknown");
            assertThat(r).startsWith("Error: Unknown type 'unknown'");
        }

        @Test
        void typeCaseInsensitiveAndTrimmed() {
            String r = tool.probabilityCombinatorics(5, 2, "  COMBINATION  ");
            assertThat(r).contains("Result: 10");
        }

        @Test
        void largePermutation_P20_3_equals6840() {
            // P(20,3) = 20*19*18 = 6840
            String r = tool.probabilityCombinatorics(20, 3, "permutation");
            assertThat(r).contains("Result: 6840");
        }

        @Test
        void permutation_nEqualsR() {
            // P(5,5) = 5! = 120
            String r = tool.probabilityCombinatorics(5, 5, "permutation");
            assertThat(r).contains("Result: 120");
        }
    }

    // ── probabilityDistribution ─────────────────────────────────────────────

    @Nested
    class Distribution {

        // --- null / blank ---
        @Test
        void nullDistribution_returnsError() {
            String r = tool.probabilityDistribution(null, "1,2,3");
            assertThat(r).isEqualTo("Error: distribution type is required.");
        }

        @Test
        void blankDistribution_returnsError() {
            String r = tool.probabilityDistribution("  ", "1,2,3");
            assertThat(r).isEqualTo("Error: distribution type is required.");
        }

        @Test
        void nullParams_returnsError() {
            String r = tool.probabilityDistribution("binomial", null);
            assertThat(r).isEqualTo("Error: params are required.");
        }

        @Test
        void blankParams_returnsError() {
            String r = tool.probabilityDistribution("binomial", "  ");
            assertThat(r).isEqualTo("Error: params are required.");
        }

        @Test
        void unknownDistribution_returnsError() {
            String r = tool.probabilityDistribution("cauchy", "0,1");
            assertThat(r).startsWith("Error: Unknown distribution 'cauchy'");
        }

        @Test
        void invalidNumberInParams_returnsError() {
            String r = tool.probabilityDistribution("binomial", "abc,0.5,3");
            assertThat(r).startsWith("Error: Invalid number in params.");
        }

        // --- binomial ---
        @Test
        void binomial_n10_p05_k5() {
            // C(10,5)*0.5^5*0.5^5 = 252/1024 = 0.24609375
            String r = tool.probabilityDistribution("binomial", "10,0.5,5");
            assertThat(r).contains("=== Binomial Distribution ===");
            assertThat(r).contains("P(X = 5) = 0.24609375");
            assertThat(r).contains("Mean: E(X) = n*p = 5");
        }

        @Test
        void binomial_tooFewParams() {
            assertThat(tool.probabilityDistribution("binomial", "10,0.5"))
                    .isEqualTo("Error: Binomial distribution requires 3 params: n, p, k");
        }

        @Test
        void binomial_negativeN() {
            assertThat(tool.probabilityDistribution("binomial", "-1,0.5,0"))
                    .isEqualTo("Error: n (trials) must be non-negative.");
        }

        @Test
        void binomial_pTooHigh() {
            assertThat(tool.probabilityDistribution("binomial", "10,1.5,3"))
                    .isEqualTo("Error: p (probability) must be between 0 and 1.");
        }

        @Test
        void binomial_pNegative() {
            assertThat(tool.probabilityDistribution("binomial", "10,-0.1,3"))
                    .isEqualTo("Error: p (probability) must be between 0 and 1.");
        }

        @Test
        void binomial_kGreaterThanN() {
            assertThat(tool.probabilityDistribution("binomial", "5,0.5,6"))
                    .isEqualTo("Error: k (successes) must be between 0 and n.");
        }

        @Test
        void binomial_kNegative() {
            assertThat(tool.probabilityDistribution("binomial", "5,0.5,-1"))
                    .isEqualTo("Error: k (successes) must be between 0 and n.");
        }

        // --- normal ---
        @Test
        void normal_standardNormal_x0() {
            String r = tool.probabilityDistribution("normal", "0,1,0");
            assertThat(r).contains("=== Normal Distribution ===");
            assertThat(r).contains("z-score = 0");
            // CDF(0) ~ 0.5
            assertThat(r).contains("0.5");
        }

        @Test
        void normal_tooFewParams() {
            assertThat(tool.probabilityDistribution("normal", "0,1"))
                    .isEqualTo("Error: Normal distribution requires 3 params: mean, std_dev, x");
        }

        @Test
        void normal_stdDevZero() {
            assertThat(tool.probabilityDistribution("normal", "0,0,1"))
                    .isEqualTo("Error: std_dev must be positive.");
        }

        @Test
        void normal_stdDevNegative() {
            assertThat(tool.probabilityDistribution("normal", "0,-1,1"))
                    .isEqualTo("Error: std_dev must be positive.");
        }

        @Test
        void normal_negativeZ() {
            // x=-1, mean=0, std=1 -> z=-1 triggers negative branch of normalCdf
            String r = tool.probabilityDistribution("normal", "0,1,-1");
            assertThat(r).contains("z-score = -1");
        }

        @Test
        void normal_veryLargePositiveZ() {
            // z > 8 branch
            String r = tool.probabilityDistribution("normal", "0,1,10");
            assertThat(r).contains("=== Normal Distribution ===");
        }

        @Test
        void normal_veryLargeNegativeZ() {
            // z < -8 branch
            String r = tool.probabilityDistribution("normal", "0,1,-10");
            assertThat(r).contains("=== Normal Distribution ===");
        }

        // --- poisson ---
        @Test
        void poisson_lambda3_k2() {
            String r = tool.probabilityDistribution("poisson", "3,2");
            assertThat(r).contains("=== Poisson Distribution ===");
            assertThat(r).contains("0.22404");
        }

        @Test
        void poisson_tooFewParams() {
            assertThat(tool.probabilityDistribution("poisson", "3"))
                    .isEqualTo("Error: Poisson distribution requires 2 params: lambda, k");
        }

        @Test
        void poisson_lambdaZero() {
            assertThat(tool.probabilityDistribution("poisson", "0,2"))
                    .isEqualTo("Error: lambda must be positive.");
        }

        @Test
        void poisson_lambdaNegative() {
            assertThat(tool.probabilityDistribution("poisson", "-1,2"))
                    .isEqualTo("Error: lambda must be positive.");
        }

        @Test
        void poisson_kNegative() {
            assertThat(tool.probabilityDistribution("poisson", "3,-1"))
                    .isEqualTo("Error: k must be non-negative.");
        }

        // --- geometric ---
        @Test
        void geometric_p05_k3() {
            // P(X=3) = 0.5^2 * 0.5 = 0.125
            String r = tool.probabilityDistribution("geometric", "0.5,3");
            assertThat(r).contains("=== Geometric Distribution ===");
            assertThat(r).contains("P(X = 3) = 0.125");
        }

        @Test
        void geometric_tooFewParams() {
            assertThat(tool.probabilityDistribution("geometric", "0.5"))
                    .isEqualTo("Error: Geometric distribution requires 2 params: p, k");
        }

        @Test
        void geometric_pZero() {
            assertThat(tool.probabilityDistribution("geometric", "0,3"))
                    .isEqualTo("Error: p must be in (0, 1].");
        }

        @Test
        void geometric_pNegative() {
            assertThat(tool.probabilityDistribution("geometric", "-0.5,3"))
                    .isEqualTo("Error: p must be in (0, 1].");
        }

        @Test
        void geometric_pGreaterThan1() {
            assertThat(tool.probabilityDistribution("geometric", "1.5,3"))
                    .isEqualTo("Error: p must be in (0, 1].");
        }

        @Test
        void geometric_kLessThan1() {
            assertThat(tool.probabilityDistribution("geometric", "0.5,0"))
                    .isEqualTo("Error: k must be at least 1 (trial number of first success).");
        }

        @Test
        void geometric_pEquals1() {
            // p=1, k=1 -> P(X=1) = 1
            String r = tool.probabilityDistribution("geometric", "1,1");
            assertThat(r).contains("P(X = 1) = 1");
        }

        // --- uniform ---
        @Test
        void uniform_xBetweenAB() {
            // a=0, b=10, x=5 -> CDF = 0.5
            String r = tool.probabilityDistribution("uniform", "0,10,5");
            assertThat(r).contains("=== Continuous Uniform Distribution ===");
            assertThat(r).contains("P(X <= 5) = 0.5");
        }

        @Test
        void uniform_xBelowA() {
            String r = tool.probabilityDistribution("uniform", "0,10,-1");
            assertThat(r).contains("P(X <= -1) = 0");
        }

        @Test
        void uniform_xAboveB() {
            String r = tool.probabilityDistribution("uniform", "0,10,15");
            assertThat(r).contains("P(X <= 15) = 1");
        }

        @Test
        void uniform_tooFewParams() {
            assertThat(tool.probabilityDistribution("uniform", "0,10"))
                    .isEqualTo("Error: Uniform distribution requires 3 params: a, b, x");
        }

        @Test
        void uniform_aEqualsB() {
            assertThat(tool.probabilityDistribution("uniform", "10,10,5"))
                    .isEqualTo("Error: a must be less than b.");
        }

        @Test
        void uniform_aGreaterThanB() {
            assertThat(tool.probabilityDistribution("uniform", "10,5,7"))
                    .isEqualTo("Error: a must be less than b.");
        }

        // --- distribution type trimming / case ---
        @Test
        void distributionCaseInsensitiveAndTrimmed() {
            String r = tool.probabilityDistribution("  BINOMIAL  ", "10,0.5,5");
            assertThat(r).contains("=== Binomial Distribution ===");
        }
    }

    // ── probabilityBayes ────────────────────────────────────────────────────

    @Nested
    class Bayes {

        @Test
        void bayes_classicExample() {
            // P(A|B) = 0.9*0.01/0.05 = 0.18
            String r = tool.probabilityBayes(0.01, 0.9, 0.05);
            assertThat(r).contains("=== Bayes' Theorem ===");
            assertThat(r).contains("Posterior Probability: P(A|B) = 0.18");
        }

        @Test
        void bayes_posteriorExactlyOne() {
            // P(A|B) = 1.0*0.5/0.5 = 1.0
            String r = tool.probabilityBayes(0.5, 1.0, 0.5);
            assertThat(r).contains("Posterior Probability: P(A|B) = 1");
            assertThat(r).doesNotContain("Warning");
        }

        @Test
        void bayes_posteriorGreaterThanOne_warning() {
            // P(A|B) = 0.9*0.8/0.5 = 1.44
            String r = tool.probabilityBayes(0.8, 0.9, 0.5);
            assertThat(r).contains("Warning: Posterior > 1.0");
        }

        @Test
        void bayes_priorNegative() {
            assertThat(tool.probabilityBayes(-0.1, 0.5, 0.5))
                    .isEqualTo("Error: prior_probability P(A) must be between 0 and 1.");
        }

        @Test
        void bayes_priorGreaterThan1() {
            assertThat(tool.probabilityBayes(1.1, 0.5, 0.5))
                    .isEqualTo("Error: prior_probability P(A) must be between 0 and 1.");
        }

        @Test
        void bayes_likelihoodNegative() {
            assertThat(tool.probabilityBayes(0.5, -0.1, 0.5))
                    .isEqualTo("Error: likelihood P(B|A) must be between 0 and 1.");
        }

        @Test
        void bayes_likelihoodGreaterThan1() {
            assertThat(tool.probabilityBayes(0.5, 1.1, 0.5))
                    .isEqualTo("Error: likelihood P(B|A) must be between 0 and 1.");
        }

        @Test
        void bayes_marginalZero() {
            assertThat(tool.probabilityBayes(0.5, 0.5, 0.0))
                    .isEqualTo("Error: marginal_probability P(B) must be between 0 (exclusive) and 1.");
        }

        @Test
        void bayes_marginalNegative() {
            assertThat(tool.probabilityBayes(0.5, 0.5, -0.1))
                    .isEqualTo("Error: marginal_probability P(B) must be between 0 (exclusive) and 1.");
        }

        @Test
        void bayes_marginalGreaterThan1() {
            assertThat(tool.probabilityBayes(0.5, 0.5, 1.1))
                    .isEqualTo("Error: marginal_probability P(B) must be between 0 (exclusive) and 1.");
        }

        @Test
        void bayes_priorZero_isValid() {
            String r = tool.probabilityBayes(0.0, 0.5, 0.5);
            assertThat(r).contains("Posterior Probability: P(A|B) = 0");
        }

        @Test
        void bayes_likelihoodZero_isValid() {
            String r = tool.probabilityBayes(0.5, 0.0, 0.5);
            assertThat(r).contains("Posterior Probability: P(A|B) = 0");
        }
    }

    // ── probabilityExpectedValue ────────────────────────────────────────────

    @Nested
    class ExpectedValue {

        @Test
        void expectedValue_knownValues() {
            // values: 1,2 probs: 0.3,0.7 -> E(X) = 0.3+1.4 = 1.7
            String r = tool.probabilityExpectedValue("1,2", "0.3,0.7");
            assertThat(r).contains("=== Expected Value & Variance ===");
            assertThat(r).contains("1.7");
        }

        @Test
        void expectedValue_probabilitiesSumTo1_noWarning() {
            String r = tool.probabilityExpectedValue("1,2", "0.5,0.5");
            assertThat(r).doesNotContain("Warning");
        }

        @Test
        void expectedValue_probabilitiesDontSumTo1_warning() {
            String r = tool.probabilityExpectedValue("1,2,3", "0.1,0.2,0.3");
            assertThat(r).contains("Warning: Probabilities sum to 0.6");
        }

        @Test
        void expectedValue_nullValues() {
            assertThat(tool.probabilityExpectedValue(null, "0.5,0.5"))
                    .isEqualTo("Error: Both values and probabilities are required.");
        }

        @Test
        void expectedValue_blankValues() {
            assertThat(tool.probabilityExpectedValue("  ", "0.5,0.5"))
                    .isEqualTo("Error: Both values and probabilities are required.");
        }

        @Test
        void expectedValue_nullProbabilities() {
            assertThat(tool.probabilityExpectedValue("1,2", null))
                    .isEqualTo("Error: Both values and probabilities are required.");
        }

        @Test
        void expectedValue_blankProbabilities() {
            assertThat(tool.probabilityExpectedValue("1,2", ""))
                    .isEqualTo("Error: Both values and probabilities are required.");
        }

        @Test
        void expectedValue_mismatchedLengths() {
            assertThat(tool.probabilityExpectedValue("1,2,3", "0.5,0.5"))
                    .startsWith("Error: The number of values (3) must match the number of probabilities (2).");
        }

        @Test
        void expectedValue_negativeProbability() {
            assertThat(tool.probabilityExpectedValue("1,2", "-0.5,1.5"))
                    .isEqualTo("Error: Probabilities must be non-negative. Found: -0.5");
        }

        @Test
        void expectedValue_invalidNumber() {
            assertThat(tool.probabilityExpectedValue("1,abc", "0.5,0.5"))
                    .startsWith("Error: Invalid number in input.");
        }
    }

    // ── probabilityMarkovChain ──────────────────────────────────────────────

    @Nested
    class MarkovChain {

        @Test
        void markov_twoState_oneStep() {
            // initial [1,0], matrix [[0.7,0.3],[0.4,0.6]] -> [0.7, 0.3]
            String r = tool.probabilityMarkovChain("0.7,0.3;0.4,0.6", "1,0", 1);
            assertThat(r).contains("=== Markov Chain ===");
            assertThat(r).contains("State after 1 step(s):");
            assertThat(r).contains("0.7");
            assertThat(r).contains("0.3");
        }

        @Test
        void markov_twoState_twoSteps() {
            // step1: [0.7, 0.3], step2: [0.7*0.7+0.3*0.4, 0.7*0.3+0.3*0.6] = [0.61, 0.39]
            String r = tool.probabilityMarkovChain("0.7,0.3;0.4,0.6", "1,0", 2);
            assertThat(r).contains("0.61");
            assertThat(r).contains("0.39");
        }

        @Test
        void markov_zeroSteps() {
            String r = tool.probabilityMarkovChain("0.7,0.3;0.4,0.6", "1,0", 0);
            assertThat(r).contains("State after 0 step(s): [ 1, 0 ]");
        }

        @Test
        void markov_probabilityInterpretation() {
            // state sums to 1 -> shows probability interpretation
            String r = tool.probabilityMarkovChain("0.7,0.3;0.4,0.6", "0.5,0.5", 1);
            assertThat(r).contains("Probability interpretation:");
            assertThat(r).contains("State 0:");
            assertThat(r).contains("State 1:");
        }

        @Test
        void markov_stateDoesNotSumToOne_noProbabilityInterpretation() {
            String r = tool.probabilityMarkovChain("0.7,0.3;0.4,0.6", "2,0", 1);
            assertThat(r).doesNotContain("Probability interpretation:");
        }

        @Test
        void markov_nullTransitionMatrix() {
            assertThat(tool.probabilityMarkovChain(null, "1,0", 1))
                    .isEqualTo("Error: transition_matrix is required.");
        }

        @Test
        void markov_blankTransitionMatrix() {
            assertThat(tool.probabilityMarkovChain("  ", "1,0", 1))
                    .isEqualTo("Error: transition_matrix is required.");
        }

        @Test
        void markov_nullInitialState() {
            assertThat(tool.probabilityMarkovChain("0.7,0.3;0.4,0.6", null, 1))
                    .isEqualTo("Error: initial_state is required.");
        }

        @Test
        void markov_blankInitialState() {
            assertThat(tool.probabilityMarkovChain("0.7,0.3;0.4,0.6", "", 1))
                    .isEqualTo("Error: initial_state is required.");
        }

        @Test
        void markov_negativeSteps() {
            assertThat(tool.probabilityMarkovChain("0.7,0.3;0.4,0.6", "1,0", -1))
                    .isEqualTo("Error: steps must be non-negative.");
        }

        @Test
        void markov_nonSquareMatrix() {
            assertThat(tool.probabilityMarkovChain("0.5,0.3,0.2;0.4,0.6", "1,0", 1))
                    .startsWith("Error: Transition matrix must be square.");
        }

        @Test
        void markov_stateLengthMismatch() {
            assertThat(tool.probabilityMarkovChain("0.7,0.3;0.4,0.6", "1,0,0", 1))
                    .startsWith("Error: Initial state vector length (3) must match matrix dimension (2).");
        }

        @Test
        void markov_rowNotStochastic_showsWarning() {
            String r = tool.probabilityMarkovChain("0.5,0.3;0.4,0.4", "1,0", 1);
            assertThat(r).contains("Warning: Row 0 sums to 0.8");
            assertThat(r).contains("Warning: Row 1 sums to 0.8");
        }

        @Test
        void markov_invalidNumberInMatrix() {
            assertThat(tool.probabilityMarkovChain("0.7,abc;0.4,0.6", "1,0", 1))
                    .startsWith("Error: Invalid number in input.");
        }

        @Test
        void markov_invalidNumberInState() {
            assertThat(tool.probabilityMarkovChain("0.7,0.3;0.4,0.6", "1,xyz", 1))
                    .startsWith("Error: Invalid number in input.");
        }

        @Test
        void markov_3x3_matrix() {
            // [1,0,0] * [[0.5,0.3,0.2],[0.1,0.6,0.3],[0.2,0.2,0.6]] = [0.5, 0.3, 0.2]
            String r = tool.probabilityMarkovChain(
                    "0.5,0.3,0.2;0.1,0.6,0.3;0.2,0.2,0.6", "1,0,0", 1);
            assertThat(r).contains("0.5");
            assertThat(r).contains("0.3");
            assertThat(r).contains("0.2");
            assertThat(r).contains("State 2:");
        }

        @Test
        void markov_multipleSteps() {
            String r = tool.probabilityMarkovChain("0.7,0.3;0.4,0.6", "1,0", 3);
            assertThat(r).contains("State after 3 step(s):");
        }
    }
}
