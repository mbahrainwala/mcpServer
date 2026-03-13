package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for financial calculations including compound interest, loan amortization,
 * investment analysis, break-even analysis, depreciation, retirement planning, and currency conversion.
 */
@Service
public class FinanceTool {

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Compound Interest
    // ──────────────────────────────────────────────────────────────────────────

    @Tool(name = "finance_compound_interest",
            description = "Calculate compound interest. Returns final amount, total interest earned, "
                    + "effective annual rate, and a year-by-year breakdown. "
                    + "Example: principal=10000, annual_rate=5, compounding_per_year=12, years=10")
    public String compoundInterest(
            @ToolParam(description = "Initial principal amount") double principal,
            @ToolParam(description = "Annual interest rate in percent (e.g. 5 for 5%)") double annual_rate,
            @ToolParam(description = "Number of times interest is compounded per year (default 12)", required = false) String compounding_per_year,
            @ToolParam(description = "Number of years") double totalYears) {

        if (principal < 0) return "Error: principal must be non-negative";
        if (totalYears <= 0) return "Error: years must be positive";

        int n = 12;
        if (compounding_per_year != null && !compounding_per_year.isBlank()) {
            try {
                n = Integer.parseInt(compounding_per_year.trim());
                if (n <= 0) return "Error: compounding_per_year must be positive";
            } catch (NumberFormatException e) {
                return "Error: compounding_per_year must be a valid integer";
            }
        }

        double r = annual_rate / 100.0;
        int wholeYears = (int) Math.ceil(totalYears);

        // Final amount: A = P * (1 + r/n)^(n*t)
        double finalAmount = principal * Math.pow(1 + r / n, n * totalYears);
        double totalInterest = finalAmount - principal;

        // Effective annual rate: EAR = (1 + r/n)^n - 1
        double ear = Math.pow(1 + r / n, n) - 1;

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("       COMPOUND INTEREST CALCULATOR\n");
        sb.append("═══════════════════════════════════════\n\n");
        sb.append(String.format("  Principal:              $%,.2f%n", principal));
        sb.append(String.format("  Annual Rate:            %.4f%%%n", annual_rate));
        sb.append(String.format("  Compounding:            %d times/year%n", n));
        sb.append(String.format("  Duration:               %.1f years%n%n", totalYears));
        sb.append("───────────────────────────────────────\n");
        sb.append("  RESULTS\n");
        sb.append("───────────────────────────────────────\n");
        sb.append(String.format("  Final Amount:           $%,.2f%n", finalAmount));
        sb.append(String.format("  Total Interest Earned:  $%,.2f%n", totalInterest));
        sb.append(String.format("  Effective Annual Rate:  %.4f%%%n%n", ear * 100));

        // Year-by-year breakdown
        sb.append("───────────────────────────────────────\n");
        sb.append("  YEAR-BY-YEAR BREAKDOWN\n");
        sb.append("───────────────────────────────────────\n");
        sb.append(String.format("  %-6s  %-16s  %-16s%n", "Year", "Balance", "Interest"));
        sb.append("  ------  ----------------  ----------------\n");

        double prevBalance = principal;
        for (int y = 1; y <= wholeYears; y++) {
            double t = Math.min(y, totalYears);
            double balance = principal * Math.pow(1 + r / n, n * t);
            double yearInterest = balance - prevBalance;
            sb.append(String.format("  %-6d  $%,14.2f  $%,14.2f%n", y, balance, yearInterest));
            prevBalance = balance;
        }

        sb.append("\n═══════════════════════════════════════\n");
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Loan / Mortgage Calculator
    // ──────────────────────────────────────────────────────────────────────────

    @Tool(name = "finance_loan_calculator",
            description = "Calculate loan or mortgage amortization. Returns monthly payment, total paid, "
                    + "total interest, payoff summary, and first 12 months amortization schedule. "
                    + "Example: principal=250000, annual_rate=6.5, term_years=30, extra_payment=200")
    public String loanCalculator(
            @ToolParam(description = "Loan principal amount") double principal,
            @ToolParam(description = "Annual interest rate in percent (e.g. 6.5 for 6.5%)") double annual_rate,
            @ToolParam(description = "Loan term in years") int term_years,
            @ToolParam(description = "Optional extra monthly payment (default 0)", required = false) String extra_payment) {

        if (principal <= 0) return "Error: principal must be positive";
        if (term_years <= 0) return "Error: term_years must be positive";
        if (annual_rate < 0) return "Error: annual_rate must be non-negative";

        double extra = 0;
        if (extra_payment != null && !extra_payment.isBlank()) {
            try {
                extra = Double.parseDouble(extra_payment.trim());
                if (extra < 0) return "Error: extra_payment must be non-negative";
            } catch (NumberFormatException e) {
                return "Error: extra_payment must be a valid number";
            }
        }

        double monthlyRate = annual_rate / 100.0 / 12.0;
        int totalPayments = term_years * 12;

        double monthlyPayment;
        if (monthlyRate == 0) {
            monthlyPayment = principal / totalPayments;
        } else {
            // M = P * [r(1+r)^n] / [(1+r)^n - 1]
            monthlyPayment = principal * (monthlyRate * Math.pow(1 + monthlyRate, totalPayments))
                    / (Math.pow(1 + monthlyRate, totalPayments) - 1);
        }

        // Simulate full amortization with extra payments
        double balance = principal;
        double totalPaid = 0;
        double totalInterestPaid = 0;
        int actualPayments = 0;

        // Store first 12 months for display
        double[] schedPrincipal = new double[12];
        double[] schedInterest = new double[12];
        double[] schedBalance = new double[12];

        while (balance > 0.005 && actualPayments < totalPayments * 2) {
            actualPayments++;
            double interestPortion = balance * monthlyRate;
            double principalPortion = monthlyPayment - interestPortion + extra;

            if (principalPortion > balance) {
                principalPortion = balance;
                double finalPayment = interestPortion + principalPortion;
                totalPaid += finalPayment;
                totalInterestPaid += interestPortion;

                if (actualPayments <= 12) {
                    schedPrincipal[actualPayments - 1] = principalPortion;
                    schedInterest[actualPayments - 1] = interestPortion;
                    schedBalance[actualPayments - 1] = 0;
                }
                balance = 0;
            } else {
                balance -= principalPortion;
                totalPaid += monthlyPayment + extra;
                totalInterestPaid += interestPortion;

                if (actualPayments <= 12) {
                    schedPrincipal[actualPayments - 1] = principalPortion;
                    schedInterest[actualPayments - 1] = interestPortion;
                    schedBalance[actualPayments - 1] = balance;
                }
            }
        }

        int displayMonths = Math.min(12, actualPayments);

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("          LOAN / MORTGAGE CALCULATOR\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        sb.append(String.format("  Loan Amount:         $%,.2f%n", principal));
        sb.append(String.format("  Annual Rate:         %.4f%%%n", annual_rate));
        sb.append(String.format("  Term:                %d years (%d payments)%n", term_years, totalPayments));
        if (extra > 0) {
            sb.append(String.format("  Extra Payment:       $%,.2f/month%n", extra));
        }

        sb.append("\n───────────────────────────────────────────────────\n");
        sb.append("  PAYMENT SUMMARY\n");
        sb.append("───────────────────────────────────────────────────\n");
        sb.append(String.format("  Monthly Payment:     $%,.2f%n", monthlyPayment));
        if (extra > 0) {
            sb.append(String.format("  Total Monthly:       $%,.2f (incl. extra)%n", monthlyPayment + extra));
        }
        sb.append(String.format("  Total Paid:          $%,.2f%n", totalPaid));
        sb.append(String.format("  Total Interest:      $%,.2f%n", totalInterestPaid));
        sb.append(String.format("  Actual Payoff:       %d months (%.1f years)%n", actualPayments, actualPayments / 12.0));
        if (extra > 0 && actualPayments < totalPayments) {
            sb.append(String.format("  Time Saved:          %d months (%.1f years)%n",
                    totalPayments - actualPayments, (totalPayments - actualPayments) / 12.0));
        }

        sb.append("\n───────────────────────────────────────────────────\n");
        sb.append("  FIRST 12 MONTHS AMORTIZATION\n");
        sb.append("───────────────────────────────────────────────────\n");
        sb.append(String.format("  %-6s  %-14s  %-14s  %-14s%n", "Month", "Principal", "Interest", "Balance"));
        sb.append("  ------  --------------  --------------  --------------\n");

        for (int i = 0; i < displayMonths; i++) {
            sb.append(String.format("  %-6d  $%,12.2f  $%,12.2f  $%,12.2f%n",
                    i + 1, schedPrincipal[i], schedInterest[i], schedBalance[i]));
        }

        sb.append("\n═══════════════════════════════════════════════════\n");
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Investment Analysis (NPV, IRR, Payback)
    // ──────────────────────────────────────────────────────────────────────────

    @Tool(name = "finance_investment_analysis",
            description = "Analyze an investment using NPV, IRR, and payback period. "
                    + "Provide the initial investment and comma-separated yearly cash flows. "
                    + "Example: initial_investment=50000, cash_flows='15000,18000,20000,22000', discount_rate=10")
    public String investmentAnalysis(
            @ToolParam(description = "Initial investment amount (positive number)") double initial_investment,
            @ToolParam(description = "Comma-separated yearly cash flows (e.g. '15000,18000,20000')") String cash_flows,
            @ToolParam(description = "Discount rate in percent (e.g. 10 for 10%)") double discount_rate) {

        if (initial_investment <= 0) return "Error: initial_investment must be positive";
        if (cash_flows == null || cash_flows.isBlank()) return "Error: cash_flows is required";

        String[] parts = cash_flows.split(",");
        double[] flows = new double[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                flows[i] = Double.parseDouble(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            return "Error: cash_flows must be comma-separated numbers";
        }

        double r = discount_rate / 100.0;

        // NPV = -initial + sum(cf_t / (1+r)^t)
        double npv = -initial_investment;
        for (int t = 1; t <= flows.length; t++) {
            npv += flows[t - 1] / Math.pow(1 + r, t);
        }

        // Payback period
        double cumulative = 0;
        double paybackPeriod = -1;
        for (int t = 0; t < flows.length; t++) {
            double prev = cumulative;
            cumulative += flows[t];
            if (cumulative >= initial_investment && paybackPeriod < 0) {
                // Linear interpolation within the year
                double remaining = initial_investment - prev;
                paybackPeriod = t + remaining / flows[t];
            }
        }

        // IRR via bisection method
        double irr = calculateIRR(initial_investment, flows);

        // Profitability index = PV of future cash flows / initial investment
        double pvCashFlows = 0;
        for (int t = 1; t <= flows.length; t++) {
            pvCashFlows += flows[t - 1] / Math.pow(1 + r, t);
        }
        double profitabilityIndex = pvCashFlows / initial_investment;

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("        INVESTMENT ANALYSIS\n");
        sb.append("═══════════════════════════════════════\n\n");
        sb.append(String.format("  Initial Investment:  $%,.2f%n", initial_investment));
        sb.append(String.format("  Discount Rate:       %.4f%%%n", discount_rate));
        sb.append("  Cash Flows:\n");
        for (int i = 0; i < flows.length; i++) {
            sb.append(String.format("    Year %d:  $%,.2f%n", i + 1, flows[i]));
        }

        sb.append("\n───────────────────────────────────────\n");
        sb.append("  RESULTS\n");
        sb.append("───────────────────────────────────────\n");
        sb.append(String.format("  Net Present Value (NPV):   $%,.2f%n", npv));
        sb.append(String.format("  NPV Decision:              %s%n", npv >= 0 ? "ACCEPT (NPV >= 0)" : "REJECT (NPV < 0)"));

        if (!Double.isNaN(irr)) {
            sb.append(String.format("  Internal Rate of Return:   %.4f%%%n", irr * 100));
        } else {
            sb.append("  Internal Rate of Return:   Could not converge\n");
        }

        if (paybackPeriod >= 0) {
            sb.append(String.format("  Payback Period:            %.2f years%n", paybackPeriod));
        } else {
            sb.append("  Payback Period:            Investment not recovered\n");
        }

        sb.append(String.format("  Profitability Index:       %.4f%n", profitabilityIndex));
        sb.append(String.format("  PI Decision:               %s%n", profitabilityIndex >= 1.0 ? "ACCEPT (PI >= 1)" : "REJECT (PI < 1)"));

        sb.append("\n═══════════════════════════════════════\n");
        return sb.toString();
    }

    /**
     * Calculate IRR using the bisection method.
     */
    private double calculateIRR(double initialInvestment, double[] cashFlows) {
        double low = -0.99;
        double high = 10.0;
        double tolerance = 1e-9;
        int maxIterations = 1000;

        for (int i = 0; i < maxIterations; i++) {
            double mid = (low + high) / 2.0;
            double npvMid = -initialInvestment;
            for (int t = 1; t <= cashFlows.length; t++) {
                npvMid += cashFlows[t - 1] / Math.pow(1 + mid, t);
            }

            if (Math.abs(npvMid) < tolerance) {
                return mid;
            }

            double npvLow = -initialInvestment;
            for (int t = 1; t <= cashFlows.length; t++) {
                npvLow += cashFlows[t - 1] / Math.pow(1 + low, t);
            }

            if (npvLow * npvMid < 0) {
                high = mid;
            } else {
                low = mid;
            }
        }

        // Check if we got close enough
        double finalNpv = -initialInvestment;
        double finalRate = (low + high) / 2.0;
        for (int t = 1; t <= cashFlows.length; t++) {
            finalNpv += cashFlows[t - 1] / Math.pow(1 + finalRate, t);
        }
        return Math.abs(finalNpv) < 0.01 ? finalRate : Double.NaN;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Break-Even Analysis
    // ──────────────────────────────────────────────────────────────────────────

    @Tool(name = "finance_break_even",
            description = "Perform break-even analysis. Returns break-even units, break-even revenue, "
                    + "contribution margin, and contribution margin ratio. "
                    + "Example: fixed_costs=50000, variable_cost_per_unit=25, selling_price_per_unit=75")
    public String breakEvenAnalysis(
            @ToolParam(description = "Total fixed costs") double fixed_costs,
            @ToolParam(description = "Variable cost per unit") double variable_cost_per_unit,
            @ToolParam(description = "Selling price per unit") double selling_price_per_unit) {

        if (fixed_costs < 0) return "Error: fixed_costs must be non-negative";
        if (variable_cost_per_unit < 0) return "Error: variable_cost_per_unit must be non-negative";
        if (selling_price_per_unit <= 0) return "Error: selling_price_per_unit must be positive";

        double contributionMargin = selling_price_per_unit - variable_cost_per_unit;
        if (contributionMargin <= 0) {
            return "Error: selling price must be greater than variable cost per unit "
                    + "(contribution margin is $" + String.format("%.2f", contributionMargin) + ")";
        }

        double contributionMarginRatio = contributionMargin / selling_price_per_unit;
        double breakEvenUnits = fixed_costs / contributionMargin;
        double breakEvenRevenue = fixed_costs / contributionMarginRatio;

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("         BREAK-EVEN ANALYSIS\n");
        sb.append("═══════════════════════════════════════\n\n");
        sb.append(String.format("  Fixed Costs:              $%,.2f%n", fixed_costs));
        sb.append(String.format("  Variable Cost/Unit:       $%,.2f%n", variable_cost_per_unit));
        sb.append(String.format("  Selling Price/Unit:       $%,.2f%n", selling_price_per_unit));

        sb.append("\n───────────────────────────────────────\n");
        sb.append("  RESULTS\n");
        sb.append("───────────────────────────────────────\n");
        sb.append(String.format("  Contribution Margin:      $%,.2f per unit%n", contributionMargin));
        sb.append(String.format("  Contribution Margin Ratio: %.4f (%.2f%%)%n", contributionMarginRatio, contributionMarginRatio * 100));
        sb.append(String.format("  Break-Even Units:         %,.2f units%n", breakEvenUnits));
        sb.append(String.format("  Break-Even Revenue:       $%,.2f%n", breakEvenRevenue));

        sb.append("\n───────────────────────────────────────\n");
        sb.append("  PROFIT AT VARIOUS VOLUMES\n");
        sb.append("───────────────────────────────────────\n");
        sb.append(String.format("  %-12s  %-14s  %-14s  %-14s%n", "Units", "Revenue", "Total Cost", "Profit/Loss"));
        sb.append("  ------------  --------------  --------------  --------------\n");

        int beUnitsInt = (int) Math.ceil(breakEvenUnits);
        int[] sampleVolumes = {
                beUnitsInt / 4,
                beUnitsInt / 2,
                (int) (beUnitsInt * 0.75),
                beUnitsInt,
                (int) (beUnitsInt * 1.25),
                beUnitsInt * 2
        };

        for (int units : sampleVolumes) {
            if (units <= 0) continue;
            double revenue = units * selling_price_per_unit;
            double totalCost = fixed_costs + (units * variable_cost_per_unit);
            double profit = revenue - totalCost;
            sb.append(String.format("  %-12s  $%,12.2f  $%,12.2f  $%,12.2f%n",
                    String.format("%,d", units), revenue, totalCost, profit));
        }

        sb.append("\n═══════════════════════════════════════\n");
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. Depreciation
    // ──────────────────────────────────────────────────────────────────────────

    @Tool(name = "finance_depreciation",
            description = "Calculate asset depreciation using straight-line, declining balance, or sum-of-years-digits method. "
                    + "Returns an annual depreciation schedule with book value each year. "
                    + "Example: asset_cost=100000, salvage_value=10000, useful_life_years=5, method=straight_line")
    public String depreciation(
            @ToolParam(description = "Original cost of the asset") double asset_cost,
            @ToolParam(description = "Salvage (residual) value at end of useful life") double salvage_value,
            @ToolParam(description = "Useful life of the asset in years") int useful_life_years,
            @ToolParam(description = "Depreciation method: straight_line, declining_balance, or sum_of_years") String method) {

        if (asset_cost <= 0) return "Error: asset_cost must be positive";
        if (salvage_value < 0) return "Error: salvage_value must be non-negative";
        if (salvage_value >= asset_cost) return "Error: salvage_value must be less than asset_cost";
        if (useful_life_years <= 0) return "Error: useful_life_years must be positive";
        if (method == null || method.isBlank()) return "Error: method is required";

        String m = method.trim().toLowerCase();
        double depreciableBase = asset_cost - salvage_value;

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("           DEPRECIATION SCHEDULE\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        sb.append(String.format("  Asset Cost:        $%,.2f%n", asset_cost));
        sb.append(String.format("  Salvage Value:     $%,.2f%n", salvage_value));
        sb.append(String.format("  Depreciable Base:  $%,.2f%n", depreciableBase));
        sb.append(String.format("  Useful Life:       %d years%n", useful_life_years));

        switch (m) {
            case "straight_line":
                sb.append("  Method:            Straight-Line\n");
                sb.append(buildStraightLineSchedule(asset_cost, salvage_value, useful_life_years, depreciableBase));
                break;

            case "declining_balance":
                sb.append("  Method:            Double Declining Balance\n");
                sb.append(buildDecliningBalanceSchedule(asset_cost, salvage_value, useful_life_years));
                break;

            case "sum_of_years":
                sb.append("  Method:            Sum-of-Years-Digits\n");
                sb.append(buildSumOfYearsSchedule(asset_cost, salvage_value, useful_life_years, depreciableBase));
                break;

            default:
                return "Error: unknown method '" + method + "'. Use: straight_line, declining_balance, or sum_of_years";
        }

        sb.append("\n═══════════════════════════════════════════════════\n");
        return sb.toString();
    }

    private String buildStraightLineSchedule(double cost, double salvage, int life, double base) {
        double annualDep = base / life;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  Annual Depreciation: $%,.2f%n%n", annualDep));
        sb.append("───────────────────────────────────────────────────\n");
        sb.append(String.format("  %-6s  %-16s  %-16s  %-16s%n", "Year", "Depreciation", "Accum. Depr.", "Book Value"));
        sb.append("  ------  ----------------  ----------------  ----------------\n");

        double accumDep = 0;
        double bookValue;
        for (int y = 1; y <= life; y++) {
            accumDep += annualDep;
            bookValue = cost - accumDep;
            sb.append(String.format("  %-6d  $%,14.2f  $%,14.2f  $%,14.2f%n", y, annualDep, accumDep, bookValue));
        }
        return sb.toString();
    }

    private String buildDecliningBalanceSchedule(double cost, double salvage, int life) {
        double rate = 2.0 / life; // Double declining balance rate
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  DDB Rate: %.4f (%.2f%%)%n%n", rate, rate * 100));
        sb.append("───────────────────────────────────────────────────\n");
        sb.append(String.format("  %-6s  %-16s  %-16s  %-16s%n", "Year", "Depreciation", "Accum. Depr.", "Book Value"));
        sb.append("  ------  ----------------  ----------------  ----------------\n");

        double bookValue = cost;
        double accumDep = 0;
        for (int y = 1; y <= life; y++) {
            double dep = bookValue * rate;
            // Cannot depreciate below salvage value
            if (bookValue - dep < salvage) {
                dep = bookValue - salvage;
            }
            if (dep < 0) dep = 0;
            accumDep += dep;
            bookValue -= dep;
            sb.append(String.format("  %-6d  $%,14.2f  $%,14.2f  $%,14.2f%n", y, dep, accumDep, bookValue));
        }
        return sb.toString();
    }

    private String buildSumOfYearsSchedule(double cost, double salvage, int life, double base) {
        int sumOfYears = life * (life + 1) / 2;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  Sum of Years Digits: %d%n%n", sumOfYears));
        sb.append("───────────────────────────────────────────────────\n");
        sb.append(String.format("  %-6s  %-8s  %-16s  %-16s  %-16s%n", "Year", "Fraction", "Depreciation", "Accum. Depr.", "Book Value"));
        sb.append("  ------  --------  ----------------  ----------------  ----------------\n");

        double accumDep = 0;
        double bookValue;
        for (int y = 1; y <= life; y++) {
            int remainingLife = life - y + 1;
            double fraction = (double) remainingLife / sumOfYears;
            double dep = base * fraction;
            accumDep += dep;
            bookValue = cost - accumDep;
            sb.append(String.format("  %-6d  %d/%-5d  $%,14.2f  $%,14.2f  $%,14.2f%n",
                    y, remainingLife, sumOfYears, dep, accumDep, bookValue));
        }
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. Retirement Calculator
    // ──────────────────────────────────────────────────────────────────────────

    @Tool(name = "finance_retirement_calculator",
            description = "Calculate retirement savings projections. Returns projected savings at retirement, "
                    + "inflation-adjusted value, and estimated monthly retirement income using the 4% rule. "
                    + "Example: current_age=30, retirement_age=65, current_savings=50000, "
                    + "monthly_contribution=500, annual_return_rate=7, inflation_rate=3")
    public String retirementCalculator(
            @ToolParam(description = "Current age") int current_age,
            @ToolParam(description = "Target retirement age") int retirement_age,
            @ToolParam(description = "Current total savings/investments") double current_savings,
            @ToolParam(description = "Monthly contribution amount") double monthly_contribution,
            @ToolParam(description = "Expected annual return rate in percent (e.g. 7 for 7%)") double annual_return_rate,
            @ToolParam(description = "Expected annual inflation rate in percent (e.g. 3 for 3%)") double inflation_rate) {

        if (current_age < 0) return "Error: current_age must be non-negative";
        if (retirement_age <= current_age) return "Error: retirement_age must be greater than current_age";
        if (current_savings < 0) return "Error: current_savings must be non-negative";
        if (monthly_contribution < 0) return "Error: monthly_contribution must be non-negative";

        int yearsToRetire = retirement_age - current_age;
        double monthlyRate = annual_return_rate / 100.0 / 12.0;
        int totalMonths = yearsToRetire * 12;

        // Future value of current savings
        double fvCurrentSavings = current_savings * Math.pow(1 + monthlyRate, totalMonths);

        // Future value of monthly contributions (annuity)
        double fvContributions;
        if (monthlyRate == 0) {
            fvContributions = monthly_contribution * totalMonths;
        } else {
            fvContributions = monthly_contribution * (Math.pow(1 + monthlyRate, totalMonths) - 1) / monthlyRate;
        }

        double totalAtRetirement = fvCurrentSavings + fvContributions;
        double totalContributed = current_savings + (monthly_contribution * totalMonths);

        // Inflation-adjusted value
        double inflationFactor = Math.pow(1 + inflation_rate / 100.0, yearsToRetire);
        double inflationAdjusted = totalAtRetirement / inflationFactor;

        // 4% rule: annual withdrawal = 4% of total, monthly = that / 12
        double annualIncome = totalAtRetirement * 0.04;
        double monthlyIncome = annualIncome / 12.0;
        double monthlyIncomeAdjusted = inflationAdjusted * 0.04 / 12.0;

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("          RETIREMENT PLANNING CALCULATOR\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        sb.append(String.format("  Current Age:            %d%n", current_age));
        sb.append(String.format("  Retirement Age:         %d%n", retirement_age));
        sb.append(String.format("  Years to Retirement:    %d%n", yearsToRetire));
        sb.append(String.format("  Current Savings:        $%,.2f%n", current_savings));
        sb.append(String.format("  Monthly Contribution:   $%,.2f%n", monthly_contribution));
        sb.append(String.format("  Annual Return Rate:     %.4f%%%n", annual_return_rate));
        sb.append(String.format("  Inflation Rate:         %.4f%%%n", inflation_rate));

        sb.append("\n───────────────────────────────────────────────────\n");
        sb.append("  PROJECTION AT RETIREMENT\n");
        sb.append("───────────────────────────────────────────────────\n");
        sb.append(String.format("  Total Contributed:               $%,.2f%n", totalContributed));
        sb.append(String.format("  Growth from Current Savings:     $%,.2f%n", fvCurrentSavings - current_savings));
        sb.append(String.format("  Growth from Contributions:       $%,.2f%n", fvContributions - (monthly_contribution * totalMonths)));
        sb.append(String.format("  Projected Savings (Nominal):     $%,.2f%n", totalAtRetirement));
        sb.append(String.format("  Projected Savings (Real/%d$):    $%,.2f%n", 2026, inflationAdjusted));

        sb.append("\n───────────────────────────────────────────────────\n");
        sb.append("  RETIREMENT INCOME (4%% RULE)\n");
        sb.append("───────────────────────────────────────────────────\n");
        sb.append(String.format("  Annual Income (Nominal):         $%,.2f%n", annualIncome));
        sb.append(String.format("  Monthly Income (Nominal):        $%,.2f%n", monthlyIncome));
        sb.append(String.format("  Monthly Income (Today's $):      $%,.2f%n", monthlyIncomeAdjusted));

        // Milestone table
        sb.append("\n───────────────────────────────────────────────────\n");
        sb.append("  SAVINGS MILESTONES\n");
        sb.append("───────────────────────────────────────────────────\n");
        sb.append(String.format("  %-6s  %-6s  %-18s  %-18s%n", "Age", "Year", "Nominal Value", "Real Value"));
        sb.append("  ------  ------  ------------------  ------------------\n");

        for (int y = 5; y <= yearsToRetire; y += 5) {
            int months = y * 12;
            double fvSav = current_savings * Math.pow(1 + monthlyRate, months);
            double fvCon;
            if (monthlyRate == 0) {
                fvCon = monthly_contribution * months;
            } else {
                fvCon = monthly_contribution * (Math.pow(1 + monthlyRate, months) - 1) / monthlyRate;
            }
            double total = fvSav + fvCon;
            double real = total / Math.pow(1 + inflation_rate / 100.0, y);
            sb.append(String.format("  %-6d  %-6d  $%,16.2f  $%,16.2f%n",
                    current_age + y, y, total, real));
        }
        // Always show retirement year
        if (yearsToRetire % 5 != 0) {
            sb.append(String.format("  %-6d  %-6d  $%,16.2f  $%,16.2f%n",
                    retirement_age, yearsToRetire, totalAtRetirement, inflationAdjusted));
        }

        sb.append("\n═══════════════════════════════════════════════════\n");
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 7. Currency Conversion
    // ──────────────────────────────────────────────────────────────────────────

    @Tool(name = "finance_currency_convert",
            description = "Convert an amount between currencies using a user-provided exchange rate. "
                    + "No live API is needed — the user supplies the rate. "
                    + "Example: amount=1000, from_currency=USD, to_currency=EUR, exchange_rate=0.92")
    public String currencyConvert(
            @ToolParam(description = "Amount to convert") double amount,
            @ToolParam(description = "Source currency code (e.g. USD)") String from_currency,
            @ToolParam(description = "Target currency code (e.g. EUR)") String to_currency,
            @ToolParam(description = "Exchange rate (1 unit of from_currency = X units of to_currency)") double exchange_rate) {

        if (from_currency == null || from_currency.isBlank()) return "Error: from_currency is required";
        if (to_currency == null || to_currency.isBlank()) return "Error: to_currency is required";
        if (exchange_rate <= 0) return "Error: exchange_rate must be positive";

        String from = from_currency.trim().toUpperCase();
        String to = to_currency.trim().toUpperCase();

        double converted = amount * exchange_rate;
        double inverseRate = 1.0 / exchange_rate;

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("         CURRENCY CONVERSION\n");
        sb.append("═══════════════════════════════════════\n\n");
        sb.append(String.format("  %,.4f %s  →  %,.4f %s%n%n", amount, from, converted, to));
        sb.append("───────────────────────────────────────\n");
        sb.append("  DETAILS\n");
        sb.append("───────────────────────────────────────\n");
        sb.append(String.format("  Amount:          %,.4f %s%n", amount, from));
        sb.append(String.format("  Exchange Rate:   1 %s = %,.6f %s%n", from, exchange_rate, to));
        sb.append(String.format("  Inverse Rate:    1 %s = %,.6f %s%n", to, inverseRate, from));
        sb.append(String.format("  Converted:       %,.4f %s%n", converted, to));

        // Handy reference amounts
        sb.append("\n───────────────────────────────────────\n");
        sb.append("  QUICK REFERENCE\n");
        sb.append("───────────────────────────────────────\n");
        double[] refs = {1, 10, 100, 1000, 10000};
        sb.append(String.format("  %-16s  %-16s%n", from, to));
        sb.append("  ----------------  ----------------\n");
        for (double ref : refs) {
            sb.append(String.format("  %,14.2f    %,14.2f%n", ref, ref * exchange_rate));
        }

        sb.append("\n═══════════════════════════════════════\n");
        return sb.toString();
    }
}
