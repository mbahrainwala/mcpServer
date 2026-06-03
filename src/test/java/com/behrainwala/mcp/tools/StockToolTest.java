package com.behrainwala.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StockToolTest {

    private StockTool tool;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        tool = new StockTool();
    }

    // =========================================================================
    //  stock_quote — input validation
    // =========================================================================

    @Nested
    class StockQuoteValidation {

        @Test
        void nullSymbolsReturnsError() {
            assertThat(tool.stockQuote(null)).startsWith("Error:");
        }

        @Test
        void blankSymbolsReturnsError() {
            assertThat(tool.stockQuote("   ")).startsWith("Error:");
        }

        @Test
        void commasOnlyReturnsError() {
            assertThat(tool.stockQuote(",,,")).startsWith("Error:");
        }

        @ParameterizedTest
        @ValueSource(strings = {"AAPL", "AAPL,MSFT", "aapl,msft,googl", " AAPL , MSFT "})
        void validSymbolsDoNotImmediatelyError(String input) {
            // Non-error result means validation passed — API call may or may not succeed
            String result = tool.stockQuote(input);
            assertThat(result).isNotNull().isNotBlank();
            // Should not be a validation error (blank/null check errors)
            assertThat(result).doesNotStartWith("Error: symbols");
        }

        @Test
        void moreThanFiveSymbolsAreCapped() {
            // No "Error: too many" — extra tickers are silently capped at 5
            String result = tool.stockQuote("A,B,C,D,E,F,G");
            assertThat(result).doesNotStartWith("Error: symbols");
        }

        @Test
        void lowercaseSymbolsAccepted() {
            // Symbols are normalized to uppercase internally
            String result = tool.stockQuote("aapl");
            assertThat(result).doesNotStartWith("Error:");
        }
    }

    // =========================================================================
    //  stock_analyze — input validation
    // =========================================================================

    @Nested
    class StockAnalyzeValidation {

        @Test
        void nullSymbolReturnsError() {
            assertThat(tool.stockAnalyze(null)).startsWith("Error:");
        }

        @Test
        void blankSymbolReturnsError() {
            assertThat(tool.stockAnalyze("  ")).startsWith("Error:");
        }

        @Test
        void emptyStringReturnsError() {
            assertThat(tool.stockAnalyze("")).startsWith("Error:");
        }

        @Test
        void validSymbolPassesValidation() {
            String result = tool.stockAnalyze("AAPL");
            // Should not be a local validation error
            assertThat(result).doesNotStartWith("Error: symbol is required");
        }

        @Test
        void lowercaseSymbolNormalized() {
            String result = tool.stockAnalyze("msft");
            assertThat(result).doesNotStartWith("Error: symbol is required");
        }
    }

    // =========================================================================
    //  stock_history — input validation
    // =========================================================================

    @Nested
    class StockHistoryValidation {

        @Test
        void nullSymbolReturnsError() {
            assertThat(tool.stockHistory(null, null)).startsWith("Error:");
        }

        @Test
        void blankSymbolReturnsError() {
            assertThat(tool.stockHistory("  ", null)).startsWith("Error:");
        }

        @ParameterizedTest
        @ValueSource(strings = {"10y", "1d", "max", "week", "monthly", "quarterly"})
        void invalidPeriodsReturnError(String period) {
            assertThat(tool.stockHistory("AAPL", period)).startsWith("Error: invalid period");
        }

        @ParameterizedTest
        @ValueSource(strings = {"5d", "1mo", "3mo", "6mo", "1y", "2y", "5y", "ytd"})
        void allValidPeriodsPassValidation(String period) {
            String result = tool.stockHistory("AAPL", period);
            // Should not be a period validation error
            assertThat(result).doesNotStartWith("Error: invalid period");
        }

        @Test
        void nullPeriodDefaultsTo3mo() {
            // Passes validation; may succeed or fail at the HTTP call
            String result = tool.stockHistory("AAPL", null);
            assertThat(result).doesNotStartWith("Error: invalid period");
        }

        @Test
        void blankPeriodDefaultsTo3mo() {
            String result = tool.stockHistory("AAPL", "   ");
            assertThat(result).doesNotStartWith("Error: invalid period");
        }

        @Test
        void uppercasePeriodNormalizedAndAccepted() {
            // Periods are normalized to lowercase, so "3MO" → "3mo" (valid)
            String result = tool.stockHistory("AAPL", "3MO");
            assertThat(result).doesNotStartWith("Error: invalid period");
        }
    }

    // =========================================================================
    //  portfolio_analyze — input validation
    // =========================================================================

    @Nested
    class PortfolioAnalyzeValidation {

        @Test
        void nullHoldingsReturnsError() {
            assertThat(tool.portfolioAnalyze(null)).startsWith("Error:");
        }

        @Test
        void blankHoldingsReturnsError() {
            assertThat(tool.portfolioAnalyze("   ")).startsWith("Error:");
        }

        @Test
        void invalidJsonReturnsError() {
            assertThat(tool.portfolioAnalyze("not-json")).startsWith("Error:");
        }

        @Test
        void jsonObjectReturnsError() {
            // Must be an array, not an object
            assertThat(tool.portfolioAnalyze("{\"symbol\":\"AAPL\"}")).startsWith("Error:");
        }

        @Test
        void emptyArrayReturnsError() {
            assertThat(tool.portfolioAnalyze("[]")).startsWith("Error:");
        }

        @Test
        void allEntriesMissingSymbolReturnsError() {
            assertThat(tool.portfolioAnalyze("[{\"shares\":10,\"avgCost\":150}]"))
                    .startsWith("Error:");
        }

        @Test
        void allEntriesHaveZeroSharesReturnsError() {
            assertThat(tool.portfolioAnalyze(
                    "[{\"symbol\":\"AAPL\",\"shares\":0,\"avgCost\":150}]"))
                    .startsWith("Error:");
        }

        @Test
        void allEntriesHaveNegativeSharesReturnsError() {
            // Negative shares treated same as zero — skipped
            assertThat(tool.portfolioAnalyze(
                    "[{\"symbol\":\"AAPL\",\"shares\":-5,\"avgCost\":150}]"))
                    .startsWith("Error:");
        }

        @Test
        void emptySymbolStringSkipped() {
            assertThat(tool.portfolioAnalyze(
                    "[{\"symbol\":\"\",\"shares\":10,\"avgCost\":150}]"))
                    .startsWith("Error:");
        }

        @Test
        void mixedValidAndInvalidEntriesKeepsValidOnes() {
            // One valid entry + one with zero shares → valid entry should be processed
            String result = tool.portfolioAnalyze(
                    "[{\"symbol\":\"AAPL\",\"shares\":5,\"avgCost\":150},"
                    + "{\"symbol\":\"MSFT\",\"shares\":0,\"avgCost\":300}]");
            // Should NOT be a "no valid holdings" error — AAPL is valid
            assertThat(result).doesNotStartWith("Error: no valid holdings");
        }

        @Test
        void symbolNormalizedToUpperCase() {
            // lowercase symbol passes validation
            String result = tool.portfolioAnalyze(
                    "[{\"symbol\":\"aapl\",\"shares\":10,\"avgCost\":150}]");
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void fractionalSharesAccepted() {
            String result = tool.portfolioAnalyze(
                    "[{\"symbol\":\"AAPL\",\"shares\":0.5,\"avgCost\":150}]");
            assertThat(result).doesNotStartWith("Error:");
        }

        @Test
        void zeroAvgCostAccepted() {
            // avgCost=0 is allowed (e.g. gifted shares); P&L will show N/A
            String result = tool.portfolioAnalyze(
                    "[{\"symbol\":\"AAPL\",\"shares\":5,\"avgCost\":0}]");
            assertThat(result).doesNotStartWith("Error:");
        }
    }

    // =========================================================================
    //  portfolio_suggest — input validation
    // =========================================================================

    @Nested
    class PortfolioSuggestValidation {

        private static final String ONE_HOLDING =
                "[{\"symbol\":\"AAPL\",\"shares\":5,\"avgCost\":150}]";

        @Test
        void nullHoldingsReturnsError() {
            assertThat(tool.portfolioSuggest(null, null, null)).startsWith("Error:");
        }

        @Test
        void blankHoldingsReturnsError() {
            assertThat(tool.portfolioSuggest("", null, null)).startsWith("Error:");
        }

        @Test
        void invalidJsonReturnsError() {
            assertThat(tool.portfolioSuggest("{bad json", null, null)).startsWith("Error:");
        }

        @ParameterizedTest
        @ValueSource(strings = {"ultra-aggressive", "safe", "low", "high", "none"})
        void invalidRiskProfilesReturnError(String risk) {
            assertThat(tool.portfolioSuggest(ONE_HOLDING, null, risk)).startsWith("Error:");
        }

        @ParameterizedTest
        @ValueSource(strings = {"conservative", "moderate", "aggressive"})
        void allValidRiskProfilesPassValidation(String risk) {
            String result = tool.portfolioSuggest(ONE_HOLDING, null, risk);
            assertThat(result).doesNotStartWith("Error: riskProfile");
        }

        @ParameterizedTest
        @ValueSource(strings = {"MODERATE", "Conservative", "AGGRESSIVE"})
        void uppercaseRiskProfileNormalizedAndAccepted(String risk) {
            // riskProfile is lowercased internally, so mixed-case input is valid
            String result = tool.portfolioSuggest(ONE_HOLDING, null, risk);
            assertThat(result).doesNotStartWith("Error: riskProfile");
        }

        @Test
        void nullRiskProfileDefaultsToModerate() {
            String result = tool.portfolioSuggest(ONE_HOLDING, null, null);
            assertThat(result).doesNotStartWith("Error: riskProfile");
        }

        @Test
        void blankRiskProfileDefaultsToModerate() {
            String result = tool.portfolioSuggest(ONE_HOLDING, "1000", "  ");
            assertThat(result).doesNotStartWith("Error: riskProfile");
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-a-number", "abc", "$1000", "1,000", "1e3x"})
        void invalidInvestmentAmountsReturnError(String amount) {
            assertThat(tool.portfolioSuggest(ONE_HOLDING, amount, null)).startsWith("Error:");
        }

        @ParameterizedTest
        @ValueSource(strings = {"1000", "0", "5000.50", "1e6", "0.01"})
        void validInvestmentAmountsPassValidation(String amount) {
            String result = tool.portfolioSuggest(ONE_HOLDING, amount, null);
            assertThat(result).doesNotStartWith("Error: investmentAmount");
        }

        @Test
        void nullInvestmentAmountDefaultsToZero() {
            String result = tool.portfolioSuggest(ONE_HOLDING, null, "moderate");
            assertThat(result).doesNotStartWith("Error:");
        }
    }

    // =========================================================================
    //  fmtPrice
    // =========================================================================

    @Nested
    class FmtPriceTests {

        @Test
        void nanReturnsNA() {
            assertThat(StockTool.fmtPrice(Double.NaN)).isEqualTo("N/A");
        }

        @Test
        void zeroFormattedWithDollarSign() {
            assertThat(StockTool.fmtPrice(0)).isEqualTo("$0.00");
        }

        @Test
        void positiveValueFormatted() {
            assertThat(StockTool.fmtPrice(185.5)).isEqualTo("$185.50");
        }

        @Test
        void largeValueHasThousandsSeparator() {
            assertThat(StockTool.fmtPrice(1234567.89)).isEqualTo("$1,234,567.89");
        }

        @Test
        void negativeValueFormatted() {
            assertThat(StockTool.fmtPrice(-50.0)).isEqualTo("$-50.00");
        }

        @Test
        void twoDecimalPlaces() {
            assertThat(StockTool.fmtPrice(9.999)).isEqualTo("$10.00");
        }
    }

    // =========================================================================
    //  fmtPct
    // =========================================================================

    @Nested
    class FmtPctTests {

        @Test
        void nanReturnsNA() {
            assertThat(StockTool.fmtPct(Double.NaN)).isEqualTo("N/A");
        }

        @Test
        void zeroDecimalReturnsZeroPct() {
            assertThat(StockTool.fmtPct(0.0)).isEqualTo("0.00%");
        }

        @Test
        void fivePercentDecimalConverted() {
            assertThat(StockTool.fmtPct(0.05)).isEqualTo("5.00%");
        }

        @Test
        void hundredPercent() {
            assertThat(StockTool.fmtPct(1.0)).isEqualTo("100.00%");
        }

        @Test
        void negativePercentage() {
            assertThat(StockTool.fmtPct(-0.10)).isEqualTo("-10.00%");
        }

        @Test
        void fractionalRoundedToTwoDecimals() {
            // 0.1234 → 12.34%
            assertThat(StockTool.fmtPct(0.1234)).isEqualTo("12.34%");
        }

        @Test
        void verySmallPct() {
            assertThat(StockTool.fmtPct(0.001)).isEqualTo("0.10%");
        }
    }

    // =========================================================================
    //  fmtLarge
    // =========================================================================

    @Nested
    class FmtLargeTests {

        @Test
        void nanReturnsNA() {
            assertThat(StockTool.fmtLarge(Double.NaN)).isEqualTo("N/A");
        }

        @Test
        void zeroReturnsNA() {
            assertThat(StockTool.fmtLarge(0)).isEqualTo("N/A");
        }

        @Test
        void millionRange() {
            assertThat(StockTool.fmtLarge(2_500_000)).isEqualTo("$2.50M");
        }

        @Test
        void billionRange() {
            assertThat(StockTool.fmtLarge(3_000_000_000.0)).isEqualTo("$3.00B");
        }

        @Test
        void trillionRange() {
            assertThat(StockTool.fmtLarge(2_850_000_000_000.0)).isEqualTo("$2.85T");
        }

        @Test
        void belowMillionUsesRawFormat() {
            assertThat(StockTool.fmtLarge(500_000)).isEqualTo("$500,000");
        }

        @Test
        void negativeBillionRange() {
            assertThat(StockTool.fmtLarge(-1_000_000_000.0)).isEqualTo("$-1.00B");
        }

        @Test
        void exactlyOneBillion() {
            assertThat(StockTool.fmtLarge(1_000_000_000.0)).isEqualTo("$1.00B");
        }

        @Test
        void exactlyOneTrillion() {
            assertThat(StockTool.fmtLarge(1_000_000_000_000.0)).isEqualTo("$1.00T");
        }
    }

    // =========================================================================
    //  fmtRatio
    // =========================================================================

    @Nested
    class FmtRatioTests {

        @Test
        void nanReturnsNA() {
            assertThat(StockTool.fmtRatio(Double.NaN)).isEqualTo("N/A");
        }

        @Test
        void zeroReturnsNA() {
            assertThat(StockTool.fmtRatio(0)).isEqualTo("N/A");
        }

        @Test
        void negativeReturnsNA() {
            assertThat(StockTool.fmtRatio(-5.0)).isEqualTo("N/A");
        }

        @Test
        void positiveRatioFormatted() {
            assertThat(StockTool.fmtRatio(28.5)).isEqualTo("28.50");
        }

        @Test
        void twoDecimalPlacesAlways() {
            assertThat(StockTool.fmtRatio(1.0)).isEqualTo("1.00");
        }

        @Test
        void smallRatio() {
            assertThat(StockTool.fmtRatio(0.001)).isEqualTo("0.00");
        }
    }

    // =========================================================================
    //  sma
    // =========================================================================

    @Nested
    class SmaTests {

        @Test
        void emptyListReturnsNaN() {
            assertThat(StockTool.sma(List.of(), 20)).isNaN();
        }

        @Test
        void fewerPointsThanPeriodReturnsNaN() {
            assertThat(StockTool.sma(List.of(1.0, 2.0, 3.0), 5)).isNaN();
        }

        @Test
        void exactlyPeriodPointsUsesAllData() {
            List<Double> data = List.of(10.0, 20.0, 30.0);
            assertThat(StockTool.sma(data, 3)).isCloseTo(20.0, within(0.001));
        }

        @Test
        void usesOnlyLastNPoints() {
            // SMA(3) of [1,2,3,10,20,30] = average of [10,20,30] = 20
            List<Double> data = List.of(1.0, 2.0, 3.0, 10.0, 20.0, 30.0);
            assertThat(StockTool.sma(data, 3)).isCloseTo(20.0, within(0.001));
        }

        @Test
        void smaOfOne() {
            assertThat(StockTool.sma(List.of(42.5), 1)).isCloseTo(42.5, within(0.001));
        }

        @Test
        void sma20WithExactData() {
            List<Double> data = new java.util.ArrayList<>();
            for (int i = 1; i <= 20; i++) data.add((double) i);
            // Average of 1..20 = 10.5
            assertThat(StockTool.sma(data, 20)).isCloseTo(10.5, within(0.001));
        }

        @Test
        void periodLargerThanDataReturnsNaN() {
            assertThat(StockTool.sma(List.of(1.0, 2.0), 50)).isNaN();
        }
    }

    // =========================================================================
    //  computeHolding
    // =========================================================================

    @Nested
    class ComputeHoldingTests {

        @Test
        void basicProfitableHolding() {
            // 10 shares, bought at $150, now at $200 → gain $500 (+33.33%)
            double[] m = StockTool.computeHolding(10, 150, 200);
            assertThat(m[0]).isCloseTo(1500.0, within(0.01));  // cost
            assertThat(m[1]).isCloseTo(2000.0, within(0.01));  // value
            assertThat(m[2]).isCloseTo(500.0,  within(0.01));  // gainLoss
            assertThat(m[3]).isCloseTo(1.0/3,  within(0.001)); // gainPct ≈ 0.333
        }

        @Test
        void losingHolding() {
            // 5 shares, bought at $300, now at $240 → loss $300 (-20%)
            double[] m = StockTool.computeHolding(5, 300, 240);
            assertThat(m[0]).isCloseTo(1500.0,  within(0.01));
            assertThat(m[1]).isCloseTo(1200.0,  within(0.01));
            assertThat(m[2]).isCloseTo(-300.0,  within(0.01));
            assertThat(m[3]).isCloseTo(-0.20,   within(0.001));
        }

        @Test
        void breakEvenHolding() {
            double[] m = StockTool.computeHolding(10, 100, 100);
            assertThat(m[2]).isCloseTo(0.0, within(0.01));
            assertThat(m[3]).isCloseTo(0.0, within(0.001));
        }

        @Test
        void unknownPriceNaN() {
            // price = NaN → value = 0, gainLoss = -cost (still computable), gainPct = -1
            double[] m = StockTool.computeHolding(10, 150, Double.NaN);
            assertThat(m[1]).isCloseTo(0.0, within(0.01));   // value = 0 when price unknown
            // gainLoss and gainPct are computed from value=0
            assertThat(m[2]).isCloseTo(-1500.0, within(0.01)); // 0 - 1500
            assertThat(m[3]).isCloseTo(-1.0,    within(0.001)); // -1500 / 1500
        }

        @Test
        void zeroAvgCostSkipsGainCalc() {
            // avgCost = 0 (e.g. gifted shares) → gainLoss and gainPct are NaN
            double[] m = StockTool.computeHolding(10, 0, 200);
            assertThat(m[0]).isCloseTo(0.0, within(0.01));
            assertThat(m[1]).isCloseTo(2000.0, within(0.01));
            assertThat(Double.isNaN(m[2])).isTrue();
            assertThat(Double.isNaN(m[3])).isTrue();
        }

        @Test
        void fractionalShares() {
            // 0.5 shares at $100, now $120 → cost=$50, value=$60, gain=$10, +20%
            double[] m = StockTool.computeHolding(0.5, 100, 120);
            assertThat(m[0]).isCloseTo(50.0,  within(0.01));
            assertThat(m[1]).isCloseTo(60.0,  within(0.01));
            assertThat(m[2]).isCloseTo(10.0,  within(0.01));
            assertThat(m[3]).isCloseTo(0.20,  within(0.001));
        }

        @Test
        void largePosition() {
            // 1000 shares at $500, now $550 → gain = $50,000 (+10%)
            double[] m = StockTool.computeHolding(1000, 500, 550);
            assertThat(m[0]).isCloseTo(500_000.0, within(0.01));
            assertThat(m[1]).isCloseTo(550_000.0, within(0.01));
            assertThat(m[2]).isCloseTo(50_000.0,  within(0.01));
            assertThat(m[3]).isCloseTo(0.10,       within(0.001));
        }
    }

    // =========================================================================
    //  getRaw — Yahoo Finance nested value parsing
    // =========================================================================

    @Nested
    class GetRawTests {

        @Test
        void plainNumberReturned() throws Exception {
            JsonNode node = mapper.readTree("{\"pe\": 28.5}");
            assertThat(StockTool.getRaw(node, "pe")).isCloseTo(28.5, within(0.001));
        }

        @Test
        void nestedRawValueReturned() throws Exception {
            JsonNode node = mapper.readTree("{\"pe\": {\"raw\": 28.5, \"fmt\": \"28.50\"}}");
            assertThat(StockTool.getRaw(node, "pe")).isCloseTo(28.5, within(0.001));
        }

        @Test
        void missingFieldReturnsNaN() throws Exception {
            JsonNode node = mapper.readTree("{}");
            assertThat(StockTool.getRaw(node, "pe")).isNaN();
        }

        @Test
        void nullFieldReturnsNaN() throws Exception {
            JsonNode node = mapper.readTree("{\"pe\": null}");
            assertThat(StockTool.getRaw(node, "pe")).isNaN();
        }

        @Test
        void nullRawSubfieldFallsBackToNaN() throws Exception {
            JsonNode node = mapper.readTree("{\"pe\": {\"raw\": null, \"fmt\": \"N/A\"}}");
            assertThat(StockTool.getRaw(node, "pe")).isNaN();
        }

        @Test
        void integerValueReturned() throws Exception {
            JsonNode node = mapper.readTree("{\"vol\": 1000000}");
            assertThat(StockTool.getRaw(node, "vol")).isCloseTo(1_000_000.0, within(0.001));
        }

        @Test
        void negativeValueReturned() throws Exception {
            JsonNode node = mapper.readTree("{\"eps\": {\"raw\": -3.45}}");
            assertThat(StockTool.getRaw(node, "eps")).isCloseTo(-3.45, within(0.001));
        }

        @Test
        void stringValueReturnsNaN() throws Exception {
            // Text node without a nested raw → NaN
            JsonNode node = mapper.readTree("{\"x\": \"hello\"}");
            assertThat(StockTool.getRaw(node, "x")).isNaN();
        }
    }

    // =========================================================================
    //  getFmt — Yahoo Finance formatted string parsing
    // =========================================================================

    @Nested
    class GetFmtTests {

        @Test
        void plainStringReturned() throws Exception {
            JsonNode node = mapper.readTree("{\"sector\": \"Technology\"}");
            assertThat(StockTool.getFmt(node, "sector")).isEqualTo("Technology");
        }

        @Test
        void fmtSubfieldPreferred() throws Exception {
            JsonNode node = mapper.readTree("{\"pe\": {\"raw\": 28.5, \"fmt\": \"28.50\"}}");
            assertThat(StockTool.getFmt(node, "pe")).isEqualTo("28.50");
        }

        @Test
        void missingFieldReturnsNA() throws Exception {
            JsonNode node = mapper.readTree("{}");
            assertThat(StockTool.getFmt(node, "sector")).isEqualTo("N/A");
        }

        @Test
        void nullFieldReturnsNA() throws Exception {
            JsonNode node = mapper.readTree("{\"sector\": null}");
            assertThat(StockTool.getFmt(node, "sector")).isEqualTo("N/A");
        }

        @Test
        void numericNodeReturnedAsString() throws Exception {
            // No fmt subfield, plain number → toString
            JsonNode node = mapper.readTree("{\"count\": 42}");
            assertThat(StockTool.getFmt(node, "count")).isEqualTo("42");
        }

        @Test
        void emptyFmtSubfield() throws Exception {
            JsonNode node = mapper.readTree("{\"x\": {\"raw\": 0.0, \"fmt\": \"\"}}");
            assertThat(StockTool.getFmt(node, "x")).isEqualTo("");
        }
    }

    // =========================================================================
    //  wrapText
    // =========================================================================

    @Nested
    class WrapTextTests {

        @Test
        void nullInputReturnsEmpty() {
            assertThat(StockTool.wrapText(null, 80, "  ")).isEmpty();
        }

        @Test
        void blankInputReturnsEmpty() {
            assertThat(StockTool.wrapText("   ", 80, "  ")).isEmpty();
        }

        @Test
        void shortTextFitsOnOneLine() {
            assertThat(StockTool.wrapText("Hello world", 80, "")).isEqualTo("Hello world");
        }

        @Test
        void longTextWrapsAtWidth() {
            String text = "Apple Inc. designs, manufactures, and markets smartphones, personal computers, "
                    + "tablets, wearables, and accessories worldwide.";
            String result = StockTool.wrapText(text, 50, "  ");
            String[] lines = result.split("\n");
            for (String line : lines) {
                assertThat(line.length()).isLessThanOrEqualTo(55); // width + indent tolerance
            }
        }

        @Test
        void indentAppliedToEachLine() {
            String text = "one two three four five six seven eight nine ten eleven twelve";
            String result = StockTool.wrapText(text, 20, ">> ");
            for (String line : result.split("\n")) {
                assertThat(line).startsWith(">> ");
            }
        }

        @Test
        void singleLongWordNotBroken() {
            // A single word longer than width is still placed on its own line
            String result = StockTool.wrapText("supercalifragilisticexpialidocious", 10, "");
            assertThat(result).isEqualTo("supercalifragilisticexpialidocious");
        }

        @Test
        void multipleSpacesCollapsed() {
            // split("\\s+") collapses runs of whitespace
            String result = StockTool.wrapText("Hello   world", 80, "");
            assertThat(result).isEqualTo("Hello world");
        }
    }

    // =========================================================================
    //  portfolio JSON edge cases (validation only — no live HTTP)
    // =========================================================================

    @Nested
    class PortfolioJsonEdgeCases {

        @Test
        void jsonArrayOfPrimitivesReturnsError() {
            assertThat(tool.portfolioAnalyze("[1, 2, 3]")).startsWith("Error:");
        }

        @Test
        void jsonWithExtraFieldsIgnored() {
            // Extra unknown fields should not cause an error
            String result = tool.portfolioAnalyze(
                    "[{\"symbol\":\"AAPL\",\"shares\":10,\"avgCost\":150,\"note\":\"long-term\"}]");
            assertThat(result).doesNotStartWith("Error: invalid JSON");
        }

        @Test
        void multipleHoldingsSomeInvalidAreCulled() {
            // Two valid, one invalid (zero shares) → still processes the two valid ones
            String result = tool.portfolioAnalyze(
                    "[{\"symbol\":\"AAPL\",\"shares\":5,\"avgCost\":150},"
                    + "{\"symbol\":\"\",\"shares\":3,\"avgCost\":100},"
                    + "{\"symbol\":\"MSFT\",\"shares\":2,\"avgCost\":300}]");
            assertThat(result).doesNotStartWith("Error: no valid holdings");
        }

        @Test
        void suggestWithValidJsonAndInvalidRiskReturnsError() {
            assertThat(tool.portfolioSuggest(
                    "[{\"symbol\":\"AAPL\",\"shares\":5,\"avgCost\":150}]",
                    null, "reckless"))
                    .startsWith("Error: riskProfile");
        }

        @Test
        void suggestWithNullAmountAndNullRiskUsesDefaults() {
            // Should not hit any validation error
            String result = tool.portfolioSuggest(
                    "[{\"symbol\":\"AAPL\",\"shares\":5,\"avgCost\":150}]",
                    null, null);
            assertThat(result).doesNotStartWith("Error: riskProfile");
            assertThat(result).doesNotStartWith("Error: investmentAmount");
        }
    }

    // =========================================================================
    //  stock_review — input validation
    // =========================================================================

    @Nested
    class StockReviewValidation {

        @Test
        void nullSymbolReturnsError() {
            assertThat(tool.stockReview(null)).startsWith("Error:");
        }

        @Test
        void blankSymbolReturnsError() {
            assertThat(tool.stockReview("  ")).startsWith("Error:");
        }

        @Test
        void emptySymbolReturnsError() {
            assertThat(tool.stockReview("")).startsWith("Error:");
        }

        @Test
        void validStockSymbolPassesValidation() {
            String result = tool.stockReview("AAPL");
            assertThat(result).doesNotStartWith("Error: symbol is required");
        }

        @Test
        void validEtfSymbolPassesValidation() {
            String result = tool.stockReview("SPY");
            assertThat(result).doesNotStartWith("Error: symbol is required");
        }

        @Test
        void lowercaseSymbolNormalized() {
            String result = tool.stockReview("nvda");
            assertThat(result).doesNotStartWith("Error: symbol is required");
        }
    }

    // =========================================================================
    //  twoSentences
    // =========================================================================

    @Nested
    class TwoSentencesTests {

        @Test
        void nullReturnsEmpty() {
            assertThat(StockTool.twoSentences(null)).isEmpty();
        }

        @Test
        void blankReturnsEmpty() {
            assertThat(StockTool.twoSentences("   ")).isEmpty();
        }

        @Test
        void oneSentenceReturnedAsIs() {
            String s = "Apple designs smartphones.";
            assertThat(StockTool.twoSentences(s)).isEqualTo(s);
        }

        @Test
        void exactlyTwoSentencesReturnedInFull() {
            String s = "Apple makes iPhones. Microsoft makes Windows.";
            assertThat(StockTool.twoSentences(s)).isEqualTo(s);
        }

        @Test
        void threeSentencesTruncatesAfterSecond() {
            String s = "Apple makes iPhones. Microsoft makes Windows. Google makes Android.";
            assertThat(StockTool.twoSentences(s))
                    .isEqualTo("Apple makes iPhones. Microsoft makes Windows.");
        }

        @Test
        void manySentencesTruncatesAfterSecond() {
            String s = "Sentence one. Sentence two. Sentence three. Sentence four. Sentence five.";
            assertThat(StockTool.twoSentences(s)).isEqualTo("Sentence one. Sentence two.");
        }

        @Test
        void longSingleSentenceCappedAt260() {
            String long260 = "A".repeat(300);
            String result = StockTool.twoSentences(long260);
            assertThat(result).hasSize(260);
            assertThat(result).endsWith("...");
        }

        @Test
        void textUnder260WithNoSentenceBoundaryReturnedAsIs() {
            String s = "No period in this text at all so it has no sentence boundary";
            assertThat(StockTool.twoSentences(s)).isEqualTo(s);
        }

        @Test
        void periodAtVeryEndWithNoPeriodSpaceReturnsFullText() {
            // "sentence." has no ". " so the whole thing is treated as one sentence
            String s = "This is a sentence.";
            assertThat(StockTool.twoSentences(s)).isEqualTo(s);
        }

        @Test
        void preservesInternalPunctuation() {
            String s = "The company was founded in 1976. It employs 150,000 people worldwide.";
            assertThat(StockTool.twoSentences(s)).isEqualTo(s);
        }

        @Test
        void abbreviationsWithPeriodNotCountedAsSentence() {
            // "Inc. " looks like a sentence boundary — that's acceptable behaviour;
            // this test just documents the current behaviour without asserting correctness.
            String s = "Apple Inc. designs electronics. It sells them globally. Extra sentence.";
            String result = StockTool.twoSentences(s);
            // First ". " is after "Inc" so result ends at "Apple Inc."
            assertThat(result).startsWith("Apple Inc.");
            assertThat(result).doesNotContain("Extra sentence");
        }
    }
}
