package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceToolTest {

    private FinanceTool tool;

    @BeforeEach
    void setUp() {
        tool = new FinanceTool();
    }

    // =====================================================================
    //  1. Compound Interest
    // =====================================================================
    @Nested
    class CompoundInterestTests {

        @Test
        void basicMonthlyCompounding() {
            // A = 10000 * (1 + 0.05/12)^(12*10) = 16470.09
            String result = tool.compoundInterest(10000, 5, "12", 10);
            assertThat(result).contains("COMPOUND INTEREST CALCULATOR");
            assertThat(result).contains("RESULTS");
            assertThat(result).contains("16,470.09");
            assertThat(result).contains("6,470.09");
            assertThat(result).contains("YEAR-BY-YEAR BREAKDOWN");
        }

        @Test
        void yearlyCompounding() {
            // A = 1000 * (1 + 0.10)^3 = 1331.00
            String result = tool.compoundInterest(1000, 10, "1", 3);
            assertThat(result).contains("1,331.00");
        }

        @Test
        void quarterlyCompounding() {
            // A = 5000 * (1 + 0.08/4)^(4*2) = 5858.30
            String result = tool.compoundInterest(5000, 8, "4", 2);
            assertThat(result).contains("5,858.30");
        }

        @Test
        void nullCompoundingDefaultsTo12() {
            // A = 1000 * (1 + 0.06/12)^(12*1) = 1061.68
            String result = tool.compoundInterest(1000, 6, null, 1);
            assertThat(result).contains("1,061.68");
            assertThat(result).contains("12 times/year");
        }

        @Test
        void emptyCompoundingDefaultsTo12() {
            String result = tool.compoundInterest(1000, 6, "", 1);
            assertThat(result).contains("12 times/year");
        }

        @Test
        void blankCompoundingDefaultsTo12() {
            String result = tool.compoundInterest(1000, 6, "   ", 1);
            assertThat(result).contains("12 times/year");
        }

        @Test
        void zeroRatePrincipalUnchanged() {
            String result = tool.compoundInterest(1000, 0, "12", 5);
            assertThat(result).contains("1,000.00");
        }

        @Test
        void zeroPrincipalReturnsZeroAmount() {
            String result = tool.compoundInterest(0, 5, "12", 5);
            assertThat(result).doesNotContainIgnoringCase("error");
            assertThat(result).contains("$0.00");
        }

        @Test
        void effectiveAnnualRate() {
            // EAR = (1 + 0.12/12)^12 - 1 = 12.6825%
            String result = tool.compoundInterest(1000, 12, "12", 1);
            assertThat(result).contains("12.6825%");
        }

        @Test
        void negativePrincipalReturnsError() {
            assertThat(tool.compoundInterest(-1000, 5, null, 5))
                    .isEqualTo("Error: principal must be non-negative");
        }

        @Test
        void zeroYearsReturnsError() {
            assertThat(tool.compoundInterest(1000, 5, null, 0))
                    .isEqualTo("Error: years must be positive");
        }

        @Test
        void negativeYearsReturnsError() {
            assertThat(tool.compoundInterest(1000, 5, null, -1))
                    .isEqualTo("Error: years must be positive");
        }

        @Test
        void invalidCompoundingNonNumericReturnsError() {
            assertThat(tool.compoundInterest(1000, 5, "abc", 5))
                    .isEqualTo("Error: compounding_per_year must be a valid integer");
        }

        @Test
        void zeroCompoundingReturnsError() {
            assertThat(tool.compoundInterest(1000, 5, "0", 5))
                    .isEqualTo("Error: compounding_per_year must be positive");
        }

        @Test
        void negativeCompoundingReturnsError() {
            assertThat(tool.compoundInterest(1000, 5, "-2", 5))
                    .isEqualTo("Error: compounding_per_year must be positive");
        }

        @Test
        void compoundingWithWhitespaceParsed() {
            String result = tool.compoundInterest(1000, 10, " 4 ", 1);
            assertThat(result).contains("4 times/year");
        }

        @Test
        void fractionalYearsBreakdown() {
            // 2.5 years => ceil = 3 rows in breakdown
            String result = tool.compoundInterest(1000, 10, "1", 2.5);
            assertThat(result).contains("2.5 years");
        }
    }

    // =====================================================================
    //  2. Loan / Mortgage Calculator
    // =====================================================================
    @Nested
    class LoanCalculatorTests {

        @Test
        void basicMortgage() {
            String result = tool.loanCalculator(200000, 6, 30, null);
            assertThat(result).contains("LOAN / MORTGAGE CALCULATOR");
            assertThat(result).contains("PAYMENT SUMMARY");
            assertThat(result).contains("FIRST 12 MONTHS AMORTIZATION");
            // Monthly payment for 200k at 6% for 30 years = 1199.10
            assertThat(result).contains("1,199.10");
        }

        @Test
        void zeroRateSimplePayment() {
            // monthly = 12000 / (1*12) = 1000
            String result = tool.loanCalculator(12000, 0, 1, null);
            assertThat(result).contains("1,000.00");
        }

        @Test
        void withExtraPayment() {
            String result = tool.loanCalculator(200000, 6, 30, "200");
            assertThat(result).contains("Extra Payment");
            assertThat(result).contains("incl. extra");
            assertThat(result).contains("Time Saved");
        }

        @Test
        void extraPaymentWithWhitespace() {
            String result = tool.loanCalculator(10000, 5, 1, " 100 ");
            assertThat(result).contains("Extra Payment");
        }

        @Test
        void zeroPrincipalReturnsError() {
            assertThat(tool.loanCalculator(0, 5, 30, null))
                    .isEqualTo("Error: principal must be positive");
        }

        @Test
        void negativePrincipalReturnsError() {
            assertThat(tool.loanCalculator(-1, 5, 30, null))
                    .isEqualTo("Error: principal must be positive");
        }

        @Test
        void zeroTermReturnsError() {
            assertThat(tool.loanCalculator(10000, 5, 0, null))
                    .isEqualTo("Error: term_years must be positive");
        }

        @Test
        void negativeTermReturnsError() {
            assertThat(tool.loanCalculator(10000, 5, -1, null))
                    .isEqualTo("Error: term_years must be positive");
        }

        @Test
        void negativeRateReturnsError() {
            assertThat(tool.loanCalculator(10000, -1, 30, null))
                    .isEqualTo("Error: annual_rate must be non-negative");
        }

        @Test
        void negativeExtraPaymentReturnsError() {
            assertThat(tool.loanCalculator(10000, 5, 1, "-50"))
                    .isEqualTo("Error: extra_payment must be non-negative");
        }

        @Test
        void invalidExtraPaymentReturnsError() {
            assertThat(tool.loanCalculator(10000, 5, 1, "xyz"))
                    .isEqualTo("Error: extra_payment must be a valid number");
        }

        @Test
        void emptyExtraPaymentTreatedAsZero() {
            String result = tool.loanCalculator(10000, 5, 1, "");
            assertThat(result).doesNotContain("Extra Payment");
        }

        @Test
        void blankExtraPaymentTreatedAsZero() {
            String result = tool.loanCalculator(10000, 5, 1, "   ");
            assertThat(result).doesNotContain("Extra Payment");
        }

        @Test
        void nullExtraPaymentTreatedAsZero() {
            String result = tool.loanCalculator(10000, 5, 1, null);
            assertThat(result).doesNotContain("Extra Payment");
        }

        @Test
        void largeExtraPaymentPaysOffEarly() {
            // Small loan with huge extra => pays off in 1 month
            String result = tool.loanCalculator(1000, 5, 30, "5000");
            assertThat(result).contains("Time Saved");
            assertThat(result).contains("1 months");
        }

        @Test
        void zeroExtraPaymentNoTimeSaved() {
            String result = tool.loanCalculator(10000, 5, 1, "0");
            assertThat(result).doesNotContain("Time Saved");
        }
    }

    // =====================================================================
    //  3. Investment Analysis
    // =====================================================================
    @Nested
    class InvestmentAnalysisTests {

        @Test
        void basicPositiveNpv() {
            String result = tool.investmentAnalysis(50000, "15000,18000,20000,22000", 10);
            assertThat(result).contains("INVESTMENT ANALYSIS");
            assertThat(result).contains("ACCEPT (NPV >= 0)");
            assertThat(result).contains("Internal Rate of Return");
            assertThat(result).contains("Profitability Index");
        }

        @Test
        void npvRejectCase() {
            // NPV = -10000 + 5500/1.1 + 5500/1.21 = -10000 + 5000 + 4545.45 = -454.55
            String result = tool.investmentAnalysis(10000, "5500,5500", 10);
            assertThat(result).contains("REJECT (NPV < 0)");
            assertThat(result).contains("-454.55");
        }

        @Test
        void npvAcceptCase() {
            // NPV = -1000 + 600/1.05 + 600/1.1025 = 115.65
            String result = tool.investmentAnalysis(1000, "600,600", 5);
            assertThat(result).contains("ACCEPT (NPV >= 0)");
            assertThat(result).contains("115.65");
        }

        @Test
        void paybackWithinFirstYear() {
            // initial=1000, flow=2000 -> payback = 0 + 1000/2000 = 0.50
            String result = tool.investmentAnalysis(1000, "2000", 10);
            assertThat(result).contains("0.50 years");
        }

        @Test
        void paybackNeverRecovered() {
            String result = tool.investmentAnalysis(100000, "100,100", 10);
            assertThat(result).contains("Investment not recovered");
        }

        @Test
        void irrConverges() {
            // 10000 invested, gets 11000 in year 1 => IRR ~10%
            String result = tool.investmentAnalysis(10000, "11000", 5);
            assertThat(result).contains("10.00");
        }

        @Test
        void irrCannotConverge() {
            // All negative cash flows
            String result = tool.investmentAnalysis(10000, "-5000,-5000,-5000", 10);
            assertThat(result).contains("Could not converge");
        }

        @Test
        void profitabilityIndexAccept() {
            String result = tool.investmentAnalysis(1000, "600,600", 5);
            assertThat(result).contains("ACCEPT (PI >= 1)");
        }

        @Test
        void profitabilityIndexReject() {
            String result = tool.investmentAnalysis(10000, "100,100", 10);
            assertThat(result).contains("REJECT (PI < 1)");
        }

        @Test
        void zeroInvestmentReturnsError() {
            assertThat(tool.investmentAnalysis(0, "1000", 10))
                    .isEqualTo("Error: initial_investment must be positive");
        }

        @Test
        void negativeInvestmentReturnsError() {
            assertThat(tool.investmentAnalysis(-5000, "1000", 10))
                    .isEqualTo("Error: initial_investment must be positive");
        }

        @Test
        void nullCashFlowsReturnsError() {
            assertThat(tool.investmentAnalysis(1000, null, 10))
                    .isEqualTo("Error: cash_flows is required");
        }

        @Test
        void emptyCashFlowsReturnsError() {
            assertThat(tool.investmentAnalysis(1000, "", 10))
                    .isEqualTo("Error: cash_flows is required");
        }

        @Test
        void blankCashFlowsReturnsError() {
            assertThat(tool.investmentAnalysis(1000, "   ", 10))
                    .isEqualTo("Error: cash_flows is required");
        }

        @Test
        void invalidCashFlowsReturnsError() {
            assertThat(tool.investmentAnalysis(1000, "abc,def", 10))
                    .isEqualTo("Error: cash_flows must be comma-separated numbers");
        }

        @Test
        void cashFlowsWithSpacesParsed() {
            String result = tool.investmentAnalysis(1000, " 500 , 600 ", 5);
            assertThat(result).contains("INVESTMENT ANALYSIS");
        }

        @Test
        void singleCashFlow() {
            String result = tool.investmentAnalysis(1000, "1200", 10);
            assertThat(result).contains("Year 1");
        }
    }

    // =====================================================================
    //  4. Break-Even Analysis
    // =====================================================================
    @Nested
    class BreakEvenTests {

        @Test
        void basicBreakEven() {
            // CM=50, BE units=50000/50=1000, BE rev=75000
            String result = tool.breakEvenAnalysis(50000, 25, 75);
            assertThat(result).contains("BREAK-EVEN ANALYSIS");
            assertThat(result).contains("1,000.00 units");
            assertThat(result).contains("75,000.00");
            assertThat(result).contains("$50.00 per unit");
            assertThat(result).contains("PROFIT AT VARIOUS VOLUMES");
        }

        @Test
        void contributionMarginRatio() {
            // CMR = 50/75 = 0.6667
            String result = tool.breakEvenAnalysis(50000, 25, 75);
            assertThat(result).contains("0.6667");
            assertThat(result).contains("66.67%");
        }

        @Test
        void zeroFixedCosts() {
            String result = tool.breakEvenAnalysis(0, 50, 100);
            assertThat(result).contains("0.00 units");
        }

        @Test
        void negativeFixedCostsReturnsError() {
            assertThat(tool.breakEvenAnalysis(-100, 25, 75))
                    .isEqualTo("Error: fixed_costs must be non-negative");
        }

        @Test
        void negativeVariableCostReturnsError() {
            assertThat(tool.breakEvenAnalysis(50000, -1, 75))
                    .isEqualTo("Error: variable_cost_per_unit must be non-negative");
        }

        @Test
        void zeroSellingPriceReturnsError() {
            assertThat(tool.breakEvenAnalysis(50000, 25, 0))
                    .isEqualTo("Error: selling_price_per_unit must be positive");
        }

        @Test
        void negativeSellingPriceReturnsError() {
            assertThat(tool.breakEvenAnalysis(50000, 25, -10))
                    .isEqualTo("Error: selling_price_per_unit must be positive");
        }

        @Test
        void zeroContributionMarginReturnsError() {
            String result = tool.breakEvenAnalysis(50000, 50, 50);
            assertThat(result).contains("Error: selling price must be greater than variable cost");
            assertThat(result).contains("$0.00");
        }

        @Test
        void negativeContributionMarginReturnsError() {
            String result = tool.breakEvenAnalysis(50000, 75, 25);
            assertThat(result).containsIgnoringCase("error");
            
        }

        @Test
        void zeroVariableCost() {
            // CM=100, BE=10000/100=100 units
            String result = tool.breakEvenAnalysis(10000, 0, 100);
            assertThat(result).contains("100.00 units");
            assertThat(result).contains("$100.00 per unit");
        }
    }

    // =====================================================================
    //  5. Depreciation
    // =====================================================================
    @Nested
    class DepreciationTests {

        @Test
        void straightLineBasic() {
            // annual = 90000/5 = 18000
            String result = tool.depreciation(100000, 10000, 5, "straight_line");
            assertThat(result).contains("DEPRECIATION SCHEDULE");
            assertThat(result).contains("Straight-Line");
            assertThat(result).contains("18,000.00");
            assertThat(result).contains("10,000.00");
        }

        @Test
        void straightLineYearByYear() {
            // cost=10000, salvage=0, life=2 => annual=5000
            String result = tool.depreciation(10000, 0, 2, "straight_line");
            assertThat(result).contains("5,000.00");
        }

        @Test
        void decliningBalanceBasic() {
            // rate = 2/5 = 0.4; Year 1: 100000*0.4=40000
            String result = tool.depreciation(100000, 10000, 5, "declining_balance");
            assertThat(result).contains("Double Declining Balance");
            assertThat(result).contains("DDB Rate");
            assertThat(result).contains("40,000.00");
            assertThat(result).contains("24,000.00");
        }

        @Test
        void decliningBalanceClampsToSalvage() {
            // cost=10000, salvage=5000, life=2 => rate=1.0
            // Year 1: dep=5000 (clamped), BV=5000
            // Year 2: dep=0 (already at salvage)
            String result = tool.depreciation(10000, 5000, 2, "declining_balance");
            assertThat(result).contains("5,000.00");
        }

        @Test
        void decliningBalanceDepBecomesZero() {
            // cost=1000, salvage=900, life=1 => rate=2.0
            // dep=1000*2=2000, clamped to 100
            String result = tool.depreciation(1000, 900, 1, "declining_balance");
            assertThat(result).contains("100.00");
            assertThat(result).contains("900.00");
        }

        @Test
        void sumOfYearsBasic() {
            // SYD=15; Year1=30000, Year2=24000, Year3=18000, Year4=12000, Year5=6000
            String result = tool.depreciation(100000, 10000, 5, "sum_of_years");
            assertThat(result).contains("Sum-of-Years-Digits");
            assertThat(result).contains("Sum of Years Digits: 15");
            assertThat(result).contains("30,000.00");
            assertThat(result).contains("24,000.00");
            assertThat(result).contains("18,000.00");
            assertThat(result).contains("12,000.00");
            assertThat(result).contains("6,000.00");
        }

        @Test
        void sumOfYearsFinalBookValueEqualsSalvage() {
            String result = tool.depreciation(10000, 2000, 3, "sum_of_years");
            assertThat(result).contains("2,000.00");
        }

        @Test
        void zeroCostReturnsError() {
            assertThat(tool.depreciation(0, 0, 5, "straight_line"))
                    .isEqualTo("Error: asset_cost must be positive");
        }

        @Test
        void negativeCostReturnsError() {
            assertThat(tool.depreciation(-100, 0, 5, "straight_line"))
                    .isEqualTo("Error: asset_cost must be positive");
        }

        @Test
        void negativeSalvageReturnsError() {
            assertThat(tool.depreciation(10000, -1, 5, "straight_line"))
                    .isEqualTo("Error: salvage_value must be non-negative");
        }

        @Test
        void salvageGreaterThanCostReturnsError() {
            assertThat(tool.depreciation(10000, 20000, 5, "straight_line"))
                    .isEqualTo("Error: salvage_value must be less than asset_cost");
        }

        @Test
        void salvageEqualsCostReturnsError() {
            assertThat(tool.depreciation(10000, 10000, 5, "straight_line"))
                    .isEqualTo("Error: salvage_value must be less than asset_cost");
        }

        @Test
        void zeroUsefulLifeReturnsError() {
            assertThat(tool.depreciation(10000, 0, 0, "straight_line"))
                    .isEqualTo("Error: useful_life_years must be positive");
        }

        @Test
        void negativeUsefulLifeReturnsError() {
            assertThat(tool.depreciation(10000, 0, -1, "straight_line"))
                    .isEqualTo("Error: useful_life_years must be positive");
        }

        @Test
        void nullMethodReturnsError() {
            assertThat(tool.depreciation(10000, 0, 5, null))
                    .isEqualTo("Error: method is required");
        }

        @Test
        void emptyMethodReturnsError() {
            assertThat(tool.depreciation(10000, 0, 5, ""))
                    .isEqualTo("Error: method is required");
        }

        @Test
        void blankMethodReturnsError() {
            assertThat(tool.depreciation(10000, 0, 5, "   "))
                    .isEqualTo("Error: method is required");
        }

        @Test
        void unknownMethodReturnsError() {
            String result = tool.depreciation(10000, 0, 5, "FIFO");
            assertThat(result).contains("Error: unknown method 'FIFO'");
            assertThat(result).contains("straight_line, declining_balance, or sum_of_years");
        }

        @Test
        void methodIsCaseInsensitive() {
            String result = tool.depreciation(10000, 0, 5, "STRAIGHT_LINE");
            assertThat(result).contains("Straight-Line");
        }

        @Test
        void methodWithWhitespace() {
            String result = tool.depreciation(10000, 0, 5, " declining_balance ");
            assertThat(result).contains("Double Declining Balance");
        }
    }

    // =====================================================================
    //  6. Retirement Calculator
    // =====================================================================
    @Nested
    class RetirementCalculatorTests {

        @Test
        void basicProjection() {
            String result = tool.retirementCalculator(30, 65, 50000, 500, 7, 3);
            assertThat(result).contains("RETIREMENT PLANNING CALCULATOR");
            assertThat(result).contains("PROJECTION AT RETIREMENT");
            assertThat(result).contains("SAVINGS MILESTONES");
            assertThat(result).contains("Years to Retirement:    35");
        }

        @Test
        void fourPercentRule() {
            // savings=1200000, 0% return, 0 contribution, 0 inflation
            // annual=48000, monthly=4000
            String result = tool.retirementCalculator(60, 61, 1200000, 0, 0, 0);
            assertThat(result).contains("48,000.00");
            assertThat(result).contains("4,000.00");
        }

        @Test
        void zeroReturnRateLinearGrowth() {
            // fvContributions = 100 * 120 = 12000
            String result = tool.retirementCalculator(55, 65, 0, 100, 0, 0);
            assertThat(result).contains("12,000.00");
        }

        @Test
        void milestonesEvery5Years() {
            String result = tool.retirementCalculator(30, 65, 50000, 500, 7, 3);
            assertThat(result).contains("35  ");
            assertThat(result).contains("40  ");
            assertThat(result).contains("65  ");
        }

        @Test
        void milestonesNonDivisibleBy5ShowsRetirement() {
            // 30 to 63 = 33 years, not divisible by 5
            String result = tool.retirementCalculator(30, 63, 50000, 500, 7, 3);
            assertThat(result).contains("63  ");
        }

        @Test
        void milestonesZeroReturnRate() {
            // At year 5: 100*60=6000; year 10: 100*120=12000; year 15: 100*180=18000
            String result = tool.retirementCalculator(30, 45, 0, 100, 0, 0);
            assertThat(result).contains("6,000.00");
            assertThat(result).contains("12,000.00");
            assertThat(result).contains("18,000.00");
        }

        @Test
        void inflationAdjustment() {
            // 10 years, 0% return, 0 contribution, 100000 savings, 3% inflation
            // real = 100000 / (1.03)^10 = 74409.39
            String result = tool.retirementCalculator(55, 65, 100000, 0, 0, 3);
            assertThat(result).contains("74,409.39");
        }

        @Test
        void retirementIncomeSection() {
            String result = tool.retirementCalculator(30, 65, 50000, 500, 7, 3);
            // sb.append uses literal "%%", so output contains "4%%"
            assertThat(result).contains("RETIREMENT INCOME (4%% RULE)");
        }

        @Test
        void negativeAgeReturnsError() {
            assertThat(tool.retirementCalculator(-1, 65, 50000, 500, 7, 3))
                    .isEqualTo("Error: current_age must be non-negative");
        }

        @Test
        void retirementAgeLessThanCurrentReturnsError() {
            assertThat(tool.retirementCalculator(65, 60, 50000, 500, 7, 3))
                    .isEqualTo("Error: retirement_age must be greater than current_age");
        }

        @Test
        void retirementAgeEqualsCurrentReturnsError() {
            assertThat(tool.retirementCalculator(65, 65, 50000, 500, 7, 3))
                    .isEqualTo("Error: retirement_age must be greater than current_age");
        }

        @Test
        void negativeSavingsReturnsError() {
            assertThat(tool.retirementCalculator(30, 65, -100, 500, 7, 3))
                    .isEqualTo("Error: current_savings must be non-negative");
        }

        @Test
        void negativeContributionReturnsError() {
            assertThat(tool.retirementCalculator(30, 65, 50000, -500, 7, 3))
                    .isEqualTo("Error: monthly_contribution must be non-negative");
        }

        @Test
        void zeroSavingsAndContribution() {
            String result = tool.retirementCalculator(30, 65, 0, 0, 7, 3);
            assertThat(result).contains("$0.00");
        }

        @Test
        void shortTimeframeLessThan5Years() {
            // 2 years, no 5-year milestones, but retirement year shown
            String result = tool.retirementCalculator(60, 62, 100000, 0, 5, 2);
            assertThat(result).contains("62  ");
        }
    }

    // =====================================================================
    //  7. Currency Conversion
    // =====================================================================
    @Nested
    class CurrencyConvertTests {

        @Test
        void usdToEur() {
            String result = tool.currencyConvert(100, "USD", "EUR", 0.92);
            assertThat(result).contains("CURRENCY CONVERSION");
            assertThat(result).contains("92.0000 EUR");
            assertThat(result).contains("DETAILS");
            assertThat(result).contains("QUICK REFERENCE");
        }

        @Test
        void inverseRate() {
            // 1/0.92 = 1.086957
            String result = tool.currencyConvert(100, "USD", "EUR", 0.92);
            assertThat(result).contains("1.086957");
        }

        @Test
        void sameCurrency() {
            String result = tool.currencyConvert(100, "USD", "USD", 1.0);
            assertThat(result).contains("100.0000 USD");
        }

        @Test
        void negativeAmount() {
            String result = tool.currencyConvert(-100, "USD", "EUR", 0.92);
            assertThat(result).contains("-92.0000 EUR");
        }

        @Test
        void zeroAmount() {
            String result = tool.currencyConvert(0, "USD", "EUR", 0.92);
            assertThat(result).contains("0.0000 EUR");
        }

        @Test
        void quickReferenceTable() {
            String result = tool.currencyConvert(100, "USD", "EUR", 2.0);
            assertThat(result).contains("2.00");
            assertThat(result).contains("20.00");
            assertThat(result).contains("200.00");
            assertThat(result).contains("2,000.00");
            assertThat(result).contains("20,000.00");
        }

        @Test
        void currencyCodesUppercased() {
            String result = tool.currencyConvert(100, "usd", "eur", 0.92);
            assertThat(result).contains("USD");
            assertThat(result).contains("EUR");
        }

        @Test
        void currencyCodesWithWhitespace() {
            String result = tool.currencyConvert(100, " usd ", " eur ", 0.92);
            assertThat(result).contains("USD");
            assertThat(result).contains("EUR");
        }

        @Test
        void nullFromCurrencyReturnsError() {
            assertThat(tool.currencyConvert(100, null, "EUR", 0.92))
                    .isEqualTo("Error: from_currency is required");
        }

        @Test
        void emptyFromCurrencyReturnsError() {
            assertThat(tool.currencyConvert(100, "", "EUR", 0.92))
                    .isEqualTo("Error: from_currency is required");
        }

        @Test
        void blankFromCurrencyReturnsError() {
            assertThat(tool.currencyConvert(100, "   ", "EUR", 0.92))
                    .isEqualTo("Error: from_currency is required");
        }

        @Test
        void nullToCurrencyReturnsError() {
            assertThat(tool.currencyConvert(100, "USD", null, 0.92))
                    .isEqualTo("Error: to_currency is required");
        }

        @Test
        void emptyToCurrencyReturnsError() {
            assertThat(tool.currencyConvert(100, "USD", "", 0.92))
                    .isEqualTo("Error: to_currency is required");
        }

        @Test
        void blankToCurrencyReturnsError() {
            assertThat(tool.currencyConvert(100, "USD", "   ", 0.92))
                    .isEqualTo("Error: to_currency is required");
        }

        @Test
        void zeroExchangeRateReturnsError() {
            assertThat(tool.currencyConvert(100, "USD", "EUR", 0))
                    .isEqualTo("Error: exchange_rate must be positive");
        }

        @Test
        void negativeExchangeRateReturnsError() {
            assertThat(tool.currencyConvert(100, "USD", "EUR", -1))
                    .isEqualTo("Error: exchange_rate must be positive");
        }

        @Test
        void largeExchangeRate() {
            String result = tool.currencyConvert(1, "USD", "VND", 24000);
            assertThat(result).contains("24,000.0000 VND");
        }

        @Test
        void verySmallExchangeRate() {
            String result = tool.currencyConvert(1000000, "VND", "USD", 0.000042);
            assertThat(result).contains("42.0000 USD");
        }
    }
}
