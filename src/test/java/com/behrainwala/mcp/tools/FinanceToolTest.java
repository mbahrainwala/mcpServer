package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceToolTest {

    private FinanceTool tool;

    @BeforeEach
    void setUp() {
        tool = new FinanceTool();
    }

    // ── compoundInterest ─────────────────────────────────────────────────────

    @Test
    void compoundInterest_containsResultSection() {
        String result = tool.compoundInterest(10000, 5, "12", 10);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("RESULTS"), s -> assertThat(s).containsIgnoringCase("Amount"));
    }

    @Test
    void compoundInterest_negativePrincipal_returnsError() {
        String result = tool.compoundInterest(-1000, 5, null, 5);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void compoundInterest_zeroYears_returnsError() {
        String result = tool.compoundInterest(1000, 5, null, 0);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void compoundInterest_invalidCompounding_returnsError() {
        String result = tool.compoundInterest(1000, 5, "abc", 5);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void compoundInterest_zeroRate_principalUnchanged() {
        String result = tool.compoundInterest(1000, 0, "12", 5);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("1,000"), s -> assertThat(s).contains("1000"));
    }

    // ── loanCalculator ───────────────────────────────────────────────────────

    @Test
    void loanCalculator_basicMortgage() {
        String result = tool.loanCalculator(200000, 5, 30, null);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("monthly"), s -> assertThat(s).containsIgnoringCase("payment"));
    }

    @Test
    void loanCalculator_zeroPrincipal_returnsError() {
        String result = tool.loanCalculator(0, 5, 30, null);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void loanCalculator_zeroRate() {
        // Zero interest loan: monthly payment = principal / (term * 12)
        String result = tool.loanCalculator(12000, 0, 1, null);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("1,000"), s -> assertThat(s).contains("1000")); // $12000 / 12 = $1000/month
    }

    // ── currencyConvert ──────────────────────────────────────────────────────

    @Test
    void currencyConvert_usdToEur() {
        String result = tool.currencyConvert(100, "USD", "EUR", 0.92);
        assertThat(result).contains("92");
    }

    @Test
    void currencyConvert_sameCurrencyRate1() {
        String result = tool.currencyConvert(100, "USD", "USD", 1.0);
        assertThat(result).contains("100");
    }

    @Test
    void currencyConvert_zeroRate_returnsError() {
        String result = tool.currencyConvert(100, "USD", "EUR", 0);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void currencyConvert_blankCurrency_returnsError() {
        String result = tool.currencyConvert(100, "", "EUR", 0.92);
        assertThat(result).containsIgnoringCase("error");
    }
}
