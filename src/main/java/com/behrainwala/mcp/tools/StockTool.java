package com.behrainwala.mcp.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * MCP tools for real-time stock analysis and portfolio management.
 * Uses Yahoo Finance (no API key required).
 * Tools: stock_quote, stock_analyze, stock_history, portfolio_analyze, portfolio_suggest
 */
@Service
public class StockTool {

    private static final Logger log = LoggerFactory.getLogger(StockTool.class);

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final HttpClient http;
    private final ObjectMapper mapper;

    // Yahoo Finance session state — refreshed automatically when stale
    private volatile String crumb;
    private volatile String cookies;
    private volatile long crumbExpiryMs = 0L;

    public StockTool() {
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.mapper = new ObjectMapper();
    }

    // ── Yahoo Finance session management ─────────────────────────────────────

    private synchronized void refreshSession() throws Exception {
        HttpResponse<String> home = http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://finance.yahoo.com/"))
                        .header("User-Agent", UA)
                        .header("Accept", "text/html")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        StringBuilder jar = new StringBuilder();
        for (String sc : home.headers().allValues("set-cookie")) {
            if (!jar.isEmpty()) jar.append("; ");
            jar.append(sc.split(";")[0]);
        }
        String cookieStr = jar.toString();

        HttpResponse<String> crumbResp = http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://query2.finance.yahoo.com/v1/test/getcrumb"))
                        .header("User-Agent", UA)
                        .header("Cookie", cookieStr)
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        String c = crumbResp.body().trim();
        if (c.isEmpty() || c.startsWith("<")) {
            throw new RuntimeException("Could not obtain Yahoo Finance crumb — API may require login");
        }
        this.crumb = c;
        this.cookies = cookieStr;
        this.crumbExpiryMs = System.currentTimeMillis() + 3_600_000L;
    }

    private String yahooGet(String url) throws Exception {
        if (crumb == null || System.currentTimeMillis() > crumbExpiryMs) {
            refreshSession();
        }
        String fullUrl = url + (url.contains("?") ? "&" : "?")
                + "crumb=" + URLEncoder.encode(crumb, StandardCharsets.UTF_8);

        HttpResponse<String> resp = doGet(fullUrl);

        // On auth failure, refresh once and retry
        if (resp.statusCode() == 401 || resp.statusCode() == 403) {
            synchronized (this) { crumbExpiryMs = 0L; }
            refreshSession();
            fullUrl = url + (url.contains("?") ? "&" : "?")
                    + "crumb=" + URLEncoder.encode(crumb, StandardCharsets.UTF_8);
            resp = doGet(fullUrl);
        }

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Yahoo Finance returned HTTP " + resp.statusCode() + " for " + url);
        }
        return resp.body();
    }

    private HttpResponse<String> doGet(String url) throws Exception {
        return http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", UA)
                        .header("Cookie", cookies != null ? cookies : "")
                        .header("Accept", "application/json")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    // ── Formatting helpers (package-private for testing) ─────────────────────

    static String fmtPrice(double v) {
        return Double.isNaN(v) ? "N/A" : String.format("$%,.2f", v);
    }

    // v is a decimal fraction (0.05 → "5.00%")
    static String fmtPct(double v) {
        return Double.isNaN(v) ? "N/A" : String.format("%.2f%%", v * 100);
    }

    static String fmtLarge(double v) {
        if (Double.isNaN(v) || v == 0) return "N/A";
        double a = Math.abs(v);
        if (a >= 1e12) return String.format("$%.2fT", v / 1e12);
        if (a >= 1e9)  return String.format("$%.2fB", v / 1e9);
        if (a >= 1e6)  return String.format("$%.2fM", v / 1e6);
        return String.format("$%,.0f", v);
    }

    static String fmtRatio(double v) {
        return (Double.isNaN(v) || v <= 0) ? "N/A" : String.format("%.2f", v);
    }

    // Yahoo Finance wraps numeric fields as {"raw": 28.5, "fmt": "28.50"}
    static double getRaw(JsonNode parent, String field) {
        JsonNode n = parent.path(field);
        if (n.isMissingNode() || n.isNull()) return Double.NaN;
        JsonNode raw = n.path("raw");
        if (!raw.isMissingNode() && !raw.isNull()) return raw.asDouble();
        if (n.isNumber()) return n.asDouble();
        return Double.NaN;
    }

    static String getFmt(JsonNode parent, String field) {
        JsonNode n = parent.path(field);
        if (n.isMissingNode() || n.isNull()) return "N/A";
        if (n.isTextual()) return n.asText();
        JsonNode fmt = n.path("fmt");
        if (!fmt.isMissingNode() && !fmt.isNull()) return fmt.asText();
        return n.asText("N/A");
    }

    static double sma(List<Double> data, int period) {
        if (data.size() < period) return Double.NaN;
        return data.subList(data.size() - period, data.size())
                .stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    /**
     * Pure portfolio row calculation — no HTTP. Returns {cost, value, gainLoss, gainPct}.
     * gainPct is a decimal fraction (e.g. 0.20 = 20%).
     */
    static double[] computeHolding(double shares, double avgCost, double price) {
        double cost     = shares * avgCost;
        double value    = Double.isNaN(price) ? 0.0 : shares * price;
        double gainLoss = avgCost > 0 ? value - cost : Double.NaN;
        double gainPct  = (avgCost > 0 && cost > 0) ? gainLoss / cost : Double.NaN;
        return new double[]{cost, value, gainLoss, gainPct};
    }

    static String wrapText(String text, int width, String indent) {
        if (text == null || text.isBlank()) return "";
        StringBuilder result = new StringBuilder();
        int lineLen = 0;
        for (String word : text.split("\\s+")) {
            if (lineLen == 0) {
                result.append(indent).append(word);
                lineLen = indent.length() + word.length();
            } else if (lineLen + 1 + word.length() > width) {
                result.append("\n").append(indent).append(word);
                lineLen = indent.length() + word.length();
            } else {
                result.append(' ').append(word);
                lineLen += 1 + word.length();
            }
        }
        return result.toString();
    }

    // ── Tool: stock_quote ─────────────────────────────────────────────────────

    @Tool(name = "stock_quote",
            description = "Get real-time stock quotes for 1–5 ticker symbols. Returns current price, "
                    + "daily change, volume, market cap, P/E ratio, 52-week range, and dividend yield. "
                    + "Data sourced from Yahoo Finance. Example: symbols='AAPL,MSFT,GOOGL'")
    public String stockQuote(
            @ToolParam(description = "Comma-separated ticker symbols, e.g. 'AAPL,MSFT,GOOGL' (max 5)") String symbols) {

        if (symbols == null || symbols.isBlank()) return "Error: symbols is required";

        String[] tickers = Arrays.stream(symbols.split(","))
                .map(String::trim).map(String::toUpperCase)
                .filter(s -> !s.isEmpty()).limit(5).toArray(String[]::new);

        if (tickers.length == 0) return "Error: no valid symbols provided";

        try {
            String url = "https://query1.finance.yahoo.com/v7/finance/quote?symbols="
                    + URLEncoder.encode(String.join(",", tickers), StandardCharsets.UTF_8)
                    + "&fields=symbol,shortName,regularMarketPrice,regularMarketChange,"
                    + "regularMarketChangePercent,regularMarketVolume,marketCap,trailingPE,"
                    + "fiftyTwoWeekHigh,fiftyTwoWeekLow,regularMarketDayHigh,regularMarketDayLow,"
                    + "dividendYield,currency,regularMarketPreviousClose";

            JsonNode results = mapper.readTree(yahooGet(url))
                    .path("quoteResponse").path("result");

            if (results.isEmpty()) return "No data found for: " + String.join(",", tickers);

            StringBuilder sb = new StringBuilder();
            for (JsonNode q : results) {
                String sym = q.path("symbol").asText();
                String name = q.path("shortName").asText("N/A");
                double price = q.path("regularMarketPrice").asDouble(Double.NaN);
                double change = q.path("regularMarketChange").asDouble(Double.NaN);
                double changePct = q.path("regularMarketChangePercent").asDouble(Double.NaN);
                double volume = q.path("regularMarketVolume").asDouble(Double.NaN);
                double mktCap = q.path("marketCap").asDouble(Double.NaN);
                double pe = q.path("trailingPE").asDouble(Double.NaN);
                double high52 = q.path("fiftyTwoWeekHigh").asDouble(Double.NaN);
                double low52 = q.path("fiftyTwoWeekLow").asDouble(Double.NaN);
                double dayHigh = q.path("regularMarketDayHigh").asDouble(Double.NaN);
                double dayLow = q.path("regularMarketDayLow").asDouble(Double.NaN);
                double divYield = q.path("dividendYield").asDouble(Double.NaN);

                String arrow = !Double.isNaN(change) && change >= 0 ? "▲" : "▼";
                String changeStr = Double.isNaN(change) ? "N/A"
                        : String.format("%+.2f (%+.2f%%)", change, changePct);

                sb.append("──────────────────────────────────────────\n");
                sb.append(String.format("%s  %s  %s\n", sym, arrow, name));
                sb.append("──────────────────────────────────────────\n");
                sb.append(String.format("Price:       %s   %s\n", fmtPrice(price), changeStr));
                sb.append(String.format("Day Range:   %s – %s\n", fmtPrice(dayLow), fmtPrice(dayHigh)));
                sb.append(String.format("52W Range:   %s – %s\n", fmtPrice(low52), fmtPrice(high52)));
                sb.append(String.format("Volume:      %s\n",
                        Double.isNaN(volume) ? "N/A" : String.format("%,.0f", volume)));
                sb.append(String.format("Market Cap:  %s\n", fmtLarge(mktCap)));
                sb.append(String.format("P/E Ratio:   %s\n", fmtRatio(pe)));
                sb.append(String.format("Div Yield:   %s\n\n",
                        Double.isNaN(divYield) || divYield == 0 ? "None"
                                : String.format("%.2f%%", divYield * 100)));
            }
            return sb.toString().trim();

        } catch (Exception e) {
            log.error("stock_quote error: {}", e.getMessage(), e);
            return "Error fetching quotes: " + e.getMessage();
        }
    }

    // ── Tool: stock_analyze ───────────────────────────────────────────────────

    @Tool(name = "stock_analyze",
            description = "Deep fundamental analysis of a single stock. Returns sector/industry, "
                    + "valuation ratios (P/E, P/B, P/S, PEG, EV/EBITDA), financials (revenue, margins, ROE, "
                    + "debt/equity), per-share metrics (EPS, book value), analyst targets, and business summary. "
                    + "Data sourced from Yahoo Finance. Example: symbol='AAPL'")
    public String stockAnalyze(
            @ToolParam(description = "Ticker symbol, e.g. 'AAPL'") String symbol) {

        if (symbol == null || symbol.isBlank()) return "Error: symbol is required";
        symbol = symbol.trim().toUpperCase();

        try {
            String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" + symbol
                    + "?modules=price,summaryDetail,defaultKeyStatistics,financialData,summaryProfile";

            JsonNode result = mapper.readTree(yahooGet(url))
                    .path("quoteSummary").path("result");

            if (result.isEmpty()) return "No data found for: " + symbol;

            JsonNode data    = result.get(0);
            JsonNode price   = data.path("price");
            JsonNode summary = data.path("summaryDetail");
            JsonNode kstats  = data.path("defaultKeyStatistics");
            JsonNode fin     = data.path("financialData");
            JsonNode profile = data.path("summaryProfile");

            String name     = getFmt(price, "longName");
            String sector   = getFmt(profile, "sector");
            String industry = getFmt(profile, "industry");
            String currency = getFmt(price, "currency");

            double curPrice  = getRaw(price, "regularMarketPrice");
            double change    = getRaw(price, "regularMarketChange");
            String chgPctFmt = getFmt(price, "regularMarketChangePercent");
            double mktCap    = getRaw(price, "marketCap");
            double entValue  = getRaw(kstats, "enterpriseValue");
            double beta      = getRaw(kstats, "beta");

            StringBuilder sb = new StringBuilder();
            sb.append("══════════════════════════════════════════════════\n");
            sb.append(String.format("  %s — %s\n", symbol, name));
            sb.append("══════════════════════════════════════════════════\n");
            sb.append(String.format("  Sector: %s  |  Industry: %s\n\n", sector, industry));

            sb.append("  PRICE\n  ──────────────────────────────────────────\n");
            sb.append(String.format("  Current:     %s %s   %+.2f (%s)\n",
                    currency, String.format("%.2f", curPrice), change, chgPctFmt));
            sb.append(String.format("  Market Cap:  %s\n", fmtLarge(mktCap)));
            sb.append(String.format("  Enterprise:  %s\n", fmtLarge(entValue)));
            sb.append(String.format("  Beta:        %s\n\n", fmtRatio(beta)));

            double pe       = getRaw(summary, "trailingPE");
            double fwdPE    = getRaw(summary, "forwardPE");
            double pb       = getRaw(kstats, "priceToBook");
            double ps       = getRaw(kstats, "priceToSalesTrailing12Months");
            double peg      = getRaw(kstats, "pegRatio");
            double evEbitda = getRaw(kstats, "enterpriseToEbitda");
            double evRev    = getRaw(kstats, "enterpriseToRevenue");

            sb.append("  VALUATION\n  ──────────────────────────────────────────\n");
            sb.append(String.format("  Trailing P/E:    %s\n", fmtRatio(pe)));
            sb.append(String.format("  Forward P/E:     %s\n", fmtRatio(fwdPE)));
            sb.append(String.format("  P/B:             %s\n", fmtRatio(pb)));
            sb.append(String.format("  P/S (ttm):       %s\n", fmtRatio(ps)));
            sb.append(String.format("  PEG Ratio:       %s\n", fmtRatio(peg)));
            sb.append(String.format("  EV/EBITDA:       %s\n", fmtRatio(evEbitda)));
            sb.append(String.format("  EV/Revenue:      %s\n\n", fmtRatio(evRev)));

            double revenue     = getRaw(fin, "totalRevenue");
            double revGrowth   = getRaw(fin, "revenueGrowth");
            double grossMargin = getRaw(fin, "grossMargins");
            double opMargin    = getRaw(fin, "operatingMargins");
            double profMargin  = getRaw(fin, "profitMargins");
            double roe         = getRaw(fin, "returnOnEquity");
            double roa         = getRaw(fin, "returnOnAssets");
            double debtEq      = getRaw(fin, "debtToEquity");
            double fcf         = getRaw(fin, "freeCashflow");

            sb.append("  FINANCIALS\n  ──────────────────────────────────────────\n");
            sb.append(String.format("  Revenue (ttm):   %s\n", fmtLarge(revenue)));
            sb.append(String.format("  Revenue Growth:  %s\n", fmtPct(revGrowth)));
            sb.append(String.format("  Gross Margin:    %s\n", fmtPct(grossMargin)));
            sb.append(String.format("  Op. Margin:      %s\n", fmtPct(opMargin)));
            sb.append(String.format("  Profit Margin:   %s\n", fmtPct(profMargin)));
            sb.append(String.format("  ROE:             %s\n", fmtPct(roe)));
            sb.append(String.format("  ROA:             %s\n", fmtPct(roa)));
            sb.append(String.format("  Debt/Equity:     %s\n", fmtRatio(debtEq)));
            sb.append(String.format("  Free Cash Flow:  %s\n\n", fmtLarge(fcf)));

            double eps     = getRaw(kstats, "trailingEps");
            double fwdEps  = getRaw(kstats, "forwardEps");
            double bv      = getRaw(kstats, "bookValue");
            double divRate = getRaw(summary, "dividendRate");
            double divYld  = getRaw(summary, "dividendYield");
            double payout  = getRaw(summary, "payoutRatio");

            sb.append("  PER SHARE\n  ──────────────────────────────────────────\n");
            sb.append(String.format("  EPS (ttm):       %s\n",
                    Double.isNaN(eps) ? "N/A" : String.format("$%.2f", eps)));
            sb.append(String.format("  Forward EPS:     %s\n",
                    Double.isNaN(fwdEps) ? "N/A" : String.format("$%.2f", fwdEps)));
            sb.append(String.format("  Book Value:      %s\n",
                    Double.isNaN(bv) ? "N/A" : String.format("$%.2f", bv)));
            sb.append(String.format("  Dividend:        %s  Yield: %s\n",
                    Double.isNaN(divRate) || divRate == 0 ? "None"
                            : String.format("$%.2f/yr", divRate),
                    Double.isNaN(divYld) || divYld == 0 ? "None" : fmtPct(divYld)));
            if (!Double.isNaN(payout) && payout > 0) {
                sb.append(String.format("  Payout Ratio:    %s\n", fmtPct(payout)));
            }
            sb.append("\n");

            double tgtLow  = getRaw(fin, "targetLowPrice");
            double tgtMean = getRaw(fin, "targetMeanPrice");
            double tgtHigh = getRaw(fin, "targetHighPrice");
            double recMean = getRaw(fin, "recommendationMean");
            String recKey  = fin.path("recommendationKey").asText("");
            int numAnalysts = (int) getRaw(fin, "numberOfAnalystOpinions");

            if (!Double.isNaN(tgtMean)) {
                double upside = (Double.isNaN(curPrice) || curPrice == 0) ? Double.NaN
                        : tgtMean / curPrice - 1;
                sb.append("  ANALYST CONSENSUS\n  ──────────────────────────────────────────\n");
                sb.append(String.format("  Rating:      %s (%.1f/5, %d analysts)\n",
                        recKey.isBlank() ? "N/A" : recKey.toUpperCase(), recMean, numAnalysts));
                sb.append(String.format("  Target:      %s – %s  mean %s  (%s upside)\n\n",
                        fmtPrice(tgtLow), fmtPrice(tgtHigh), fmtPrice(tgtMean),
                        Double.isNaN(upside) ? "N/A" : fmtPct(upside)));
            }

            String bizSummary = profile.path("longBusinessSummary").asText("");
            if (!bizSummary.isBlank()) {
                if (bizSummary.length() > 600) bizSummary = bizSummary.substring(0, 597) + "...";
                sb.append("  BUSINESS\n  ──────────────────────────────────────────\n");
                sb.append(wrapText(bizSummary, 75, "  ")).append("\n\n");
            }

            sb.append("══════════════════════════════════════════════════\n");
            return sb.toString();

        } catch (Exception e) {
            log.error("stock_analyze error for {}: {}", symbol, e.getMessage(), e);
            return "Error analyzing " + symbol + ": " + e.getMessage();
        }
    }

    // ── Tool: stock_history ───────────────────────────────────────────────────

    @Tool(name = "stock_history",
            description = "Historical price data for a stock with moving averages and period return. "
                    + "Valid periods: 5d, 1mo, 3mo, 6mo, 1y, 2y, 5y, ytd. Default: 3mo. "
                    + "Data sourced from Yahoo Finance. Example: symbol='AAPL', period='1y'")
    public String stockHistory(
            @ToolParam(description = "Ticker symbol, e.g. 'AAPL'") String symbol,
            @ToolParam(description = "Period: 5d, 1mo, 3mo, 6mo, 1y, 2y, 5y, ytd. Default: 3mo",
                    required = false) String period) {

        if (symbol == null || symbol.isBlank()) return "Error: symbol is required";
        symbol = symbol.trim().toUpperCase();

        String p = (period == null || period.isBlank()) ? "3mo" : period.trim().toLowerCase();
        if (!Set.of("5d", "1mo", "3mo", "6mo", "1y", "2y", "5y", "ytd").contains(p)) {
            return "Error: invalid period. Use: 5d, 1mo, 3mo, 6mo, 1y, 2y, 5y, ytd";
        }

        String interval = switch (p) {
            case "5d", "1mo", "3mo", "ytd" -> "1d";
            default -> "1wk";
        };

        try {
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol
                    + "?interval=" + interval + "&range=" + p;

            JsonNode result = mapper.readTree(yahooGet(url)).path("chart").path("result");
            if (result.isEmpty()) return "No historical data for: " + symbol;

            JsonNode data       = result.get(0);
            JsonNode meta       = data.path("meta");
            JsonNode timestamps = data.path("timestamp");
            JsonNode q          = data.path("indicators").path("quote").get(0);

            if (q == null || timestamps.isEmpty()) {
                return "No price data available for " + symbol + " period=" + p;
            }

            String currency   = meta.path("currency").asText("USD");
            double curPrice   = meta.path("regularMarketPrice").asDouble(Double.NaN);
            int n             = timestamps.size();

            List<Double> closes = new ArrayList<>();
            for (JsonNode c : q.path("close")) {
                if (!c.isNull()) closes.add(c.asDouble());
            }

            double periodHigh = closes.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
            double periodLow  = closes.stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN);
            double first      = closes.isEmpty() ? Double.NaN : closes.getFirst();
            double last       = closes.isEmpty() ? Double.NaN : closes.getLast();
            double ret        = (Double.isNaN(first) || first == 0) ? Double.NaN : last / first - 1;
            double sma20      = sma(closes, 20);
            double sma50      = sma(closes, 50);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("PRICE HISTORY: %s  |  Period: %s  |  Interval: %s\n",
                    symbol, p, interval));
            sb.append("────────────────────────────────────────────────────────────\n");
            sb.append(String.format("Current: %s %.2f   Period Return: %s\n",
                    currency, curPrice, Double.isNaN(ret) ? "N/A" : fmtPct(ret)));
            sb.append(String.format("High:    %s %.2f   Low: %s %.2f\n",
                    currency, periodHigh, currency, periodLow));
            if (!Double.isNaN(sma20)) {
                sb.append(String.format("SMA-20:  %s %.2f   SMA-50: %s\n",
                        currency, sma20, Double.isNaN(sma50) ? "N/A"
                                : currency + " " + String.format("%.2f", sma50)));
            }
            sb.append("\n");

            // Cap to ~50 rows
            int step = Math.max(1, n / 50);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            sb.append(String.format("%-12s  %8s  %8s  %8s  %8s  %12s\n",
                    "Date", "Open", "High", "Low", "Close", "Volume"));
            sb.append("────────────────────────────────────────────────────────────\n");

            JsonNode opens   = q.path("open");
            JsonNode highs   = q.path("high");
            JsonNode lows    = q.path("low");
            JsonNode closesN = q.path("close");
            JsonNode volumes = q.path("volume");

            for (int i = 0; i < n; i += step) {
                String date = Instant.ofEpochSecond(timestamps.get(i).asLong())
                        .atZone(ZoneOffset.UTC).toLocalDate().format(dtf);
                JsonNode o = opens.get(i), h = highs.get(i), l = lows.get(i),
                         c = closesN.get(i), v = volumes.get(i);
                sb.append(String.format("%-12s  %8s  %8s  %8s  %8s  %12s\n",
                        date,
                        o == null || o.isNull() ? "N/A" : String.format("%.2f", o.asDouble()),
                        h == null || h.isNull() ? "N/A" : String.format("%.2f", h.asDouble()),
                        l == null || l.isNull() ? "N/A" : String.format("%.2f", l.asDouble()),
                        c == null || c.isNull() ? "N/A" : String.format("%.2f", c.asDouble()),
                        v == null || v.isNull() ? "N/A" : String.format("%,d", v.asLong())));
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("stock_history error for {}: {}", symbol, e.getMessage(), e);
            return "Error fetching history for " + symbol + ": " + e.getMessage();
        }
    }

    // ── Tool: portfolio_analyze ───────────────────────────────────────────────

    @Tool(name = "portfolio_analyze",
            description = "Analyze a stock portfolio with live prices. Shows current values, "
                    + "unrealized P&L, per-holding allocation %, and concentration warnings. "
                    + "Holdings format: [{\"symbol\":\"AAPL\",\"shares\":10,\"avgCost\":150.00}, ...]. "
                    + "avgCost is the average purchase price per share.")
    public String portfolioAnalyze(
            @ToolParam(description = "JSON array: [{\"symbol\":\"AAPL\",\"shares\":10,\"avgCost\":150.00}, ...]")
            String holdings) {

        if (holdings == null || holdings.isBlank()) return "Error: holdings is required";

        try {
            return buildPortfolioAnalysis(mapper.readTree(holdings), false, 0, "moderate");
        } catch (JsonProcessingException e) {
            return "Error: invalid JSON — " + e.getOriginalMessage();
        } catch (Exception e) {
            log.error("portfolio_analyze error: {}", e.getMessage(), e);
            return "Error analyzing portfolio: " + e.getMessage();
        }
    }

    // ── Tool: portfolio_suggest ───────────────────────────────────────────────

    @Tool(name = "portfolio_suggest",
            description = "Full portfolio analysis with diversification gaps and investment framework. "
                    + "Fetches valuation metrics (P/E, margins, beta) for each holding, identifies missing "
                    + "sectors, and provides a risk-matched investment decision scaffold. "
                    + "Holdings format: [{\"symbol\":\"AAPL\",\"shares\":10,\"avgCost\":150.00}, ...]. "
                    + "Use stock_analyze on suggested tickers to do deeper research before investing.")
    public String portfolioSuggest(
            @ToolParam(description = "JSON array: [{\"symbol\":\"AAPL\",\"shares\":10,\"avgCost\":150.00}, ...]")
            String holdings,
            @ToolParam(description = "Amount available to invest in USD. Default 0.", required = false)
            String investmentAmount,
            @ToolParam(description = "Risk profile: conservative, moderate, or aggressive. Default: moderate.",
                    required = false) String riskProfile) {

        if (holdings == null || holdings.isBlank()) return "Error: holdings is required";

        double investAmt = 0;
        if (investmentAmount != null && !investmentAmount.isBlank()) {
            try { investAmt = Double.parseDouble(investmentAmount.trim()); }
            catch (NumberFormatException e) { return "Error: investmentAmount must be a number"; }
        }

        String risk = (riskProfile == null || riskProfile.isBlank()) ? "moderate"
                : riskProfile.trim().toLowerCase();
        if (!Set.of("conservative", "moderate", "aggressive").contains(risk)) {
            return "Error: riskProfile must be conservative, moderate, or aggressive";
        }

        try {
            return buildPortfolioAnalysis(mapper.readTree(holdings), true, investAmt, risk);
        } catch (JsonProcessingException e) {
            return "Error: invalid JSON — " + e.getOriginalMessage();
        } catch (Exception e) {
            log.error("portfolio_suggest error: {}", e.getMessage(), e);
            return "Error in portfolio_suggest: " + e.getMessage();
        }
    }

    // ── Shared portfolio logic ────────────────────────────────────────────────

    private String buildPortfolioAnalysis(JsonNode holdingsNode, boolean detailed,
                                           double investAmt, String risk) throws Exception {

        if (!holdingsNode.isArray() || holdingsNode.isEmpty()) {
            return "Error: holdings must be a non-empty JSON array";
        }

        List<String>  symbols   = new ArrayList<>();
        List<Double>  sharesList = new ArrayList<>();
        List<Double>  costsList  = new ArrayList<>();

        for (JsonNode h : holdingsNode) {
            String sym   = h.path("symbol").asText("").trim().toUpperCase();
            double shares = h.path("shares").asDouble(0);
            double avg    = h.path("avgCost").asDouble(0);
            if (!sym.isEmpty() && shares > 0) {
                symbols.add(sym);
                sharesList.add(shares);
                costsList.add(avg);
            }
        }
        if (symbols.isEmpty()) return "Error: no valid holdings found";

        // Fetch quotes in batches of 5
        Map<String, Double> prices  = new LinkedHashMap<>();
        Map<String, String> names   = new LinkedHashMap<>();
        Map<String, Double> changes = new LinkedHashMap<>();

        for (int i = 0; i < symbols.size(); i += 5) {
            List<String> batch = symbols.subList(i, Math.min(i + 5, symbols.size()));
            String url = "https://query1.finance.yahoo.com/v7/finance/quote?symbols="
                    + URLEncoder.encode(String.join(",", batch), StandardCharsets.UTF_8)
                    + "&fields=symbol,shortName,regularMarketPrice,regularMarketChangePercent";
            JsonNode results = mapper.readTree(yahooGet(url)).path("quoteResponse").path("result");
            for (JsonNode q : results) {
                String s = q.path("symbol").asText();
                prices.put(s,  q.path("regularMarketPrice").asDouble(Double.NaN));
                names.put(s,   q.path("shortName").asText("N/A"));
                changes.put(s, q.path("regularMarketChangePercent").asDouble(Double.NaN));
            }
        }

        // If detailed, also fetch fundamentals
        Map<String, String> sectors    = new LinkedHashMap<>();
        Map<String, String> industries = new LinkedHashMap<>();
        Map<String, Double> peRatios   = new LinkedHashMap<>();
        Map<String, Double> margins    = new LinkedHashMap<>();
        Map<String, Double> revGrowths = new LinkedHashMap<>();
        Map<String, Double> betas      = new LinkedHashMap<>();

        if (detailed) {
            for (String sym : symbols) {
                try {
                    String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" + sym
                            + "?modules=price,summaryDetail,defaultKeyStatistics,financialData,summaryProfile";
                    JsonNode res = mapper.readTree(yahooGet(url)).path("quoteSummary").path("result");
                    if (!res.isEmpty()) {
                        JsonNode d = res.get(0);
                        sectors.put(sym,    getFmt(d.path("summaryProfile"), "sector"));
                        industries.put(sym, getFmt(d.path("summaryProfile"), "industry"));
                        peRatios.put(sym,   getRaw(d.path("summaryDetail"), "trailingPE"));
                        margins.put(sym,    getRaw(d.path("financialData"), "profitMargins"));
                        revGrowths.put(sym, getRaw(d.path("financialData"), "revenueGrowth"));
                        betas.put(sym,      getRaw(d.path("defaultKeyStatistics"), "beta"));
                    }
                } catch (Exception ex) {
                    log.warn("Could not fetch fundamentals for {}: {}", sym, ex.getMessage());
                }
            }
        }

        // Calculate portfolio metrics
        double totalValue = 0, totalCost = 0;
        double[] values    = new double[symbols.size()];
        double[] costs     = new double[symbols.size()];
        double[] gainLoss  = new double[symbols.size()];
        double[] gainPct   = new double[symbols.size()];

        for (int i = 0; i < symbols.size(); i++) {
            double price = prices.getOrDefault(symbols.get(i), Double.NaN);
            double shares = sharesList.get(i);
            double avg    = costsList.get(i);
            costs[i]  = shares * avg;
            values[i] = Double.isNaN(price) ? 0.0 : shares * price;
            gainLoss[i] = avg > 0 ? values[i] - costs[i] : Double.NaN;
            gainPct[i]  = (avg > 0 && costs[i] > 0) ? gainLoss[i] / costs[i] : Double.NaN;
            totalValue += values[i];
            totalCost  += costs[i];
        }

        double totalGL    = totalValue - totalCost;
        double totalGLPct = totalCost > 0 ? totalGL / totalCost : 0;

        StringBuilder sb = new StringBuilder();

        if (detailed) {
            sb.append("══════════════════════════════════════════════════════════════════\n");
            sb.append("  PORTFOLIO INVESTMENT ANALYSIS\n");
            sb.append("══════════════════════════════════════════════════════════════════\n");
            sb.append(String.format("  Risk Profile: %s%s\n",
                    risk.substring(0, 1).toUpperCase() + risk.substring(1),
                    investAmt > 0 ? "   |  Available to invest: " + fmtPrice(investAmt) : ""));
        } else {
            sb.append("══════════════════════════════════════════════════════════════════\n");
            sb.append("  PORTFOLIO ANALYSIS\n");
            sb.append("══════════════════════════════════════════════════════════════════\n");
        }

        sb.append(String.format("  Total Value:    %s\n", fmtPrice(totalValue)));
        sb.append(String.format("  Total Cost:     %s\n", fmtPrice(totalCost)));
        sb.append(String.format("  Unrealized P&L: %+,.2f (%s)\n\n", totalGL, fmtPct(totalGLPct)));

        // Holdings table
        if (detailed) {
            sb.append("  HOLDINGS METRICS\n");
            sb.append("  ─────────────────────────────────────────────────────────────\n");
            sb.append(String.format("  %-6s  %-20s  %-18s  %6s  %6s  %7s  %9s  %5s\n",
                    "Sym", "Name", "Sector", "Alloc%", "P/E", "ProfMgn", "RevGrowth", "Beta"));
            sb.append("  ──────  ────────────────────  ──────────────────  ──────  ──────  ───────  ─────────  ─────\n");
            for (int i = 0; i < symbols.size(); i++) {
                String sym  = symbols.get(i);
                double alloc = totalValue > 0 ? values[i] / totalValue * 100 : 0;
                String name = names.getOrDefault(sym, "N/A");
                if (name.length() > 20) name = name.substring(0, 19) + "…";
                String sector = sectors.getOrDefault(sym, "N/A");
                if (sector.length() > 18) sector = sector.substring(0, 17) + "…";
                double pe = peRatios.getOrDefault(sym, Double.NaN);
                double pm = margins.getOrDefault(sym, Double.NaN);
                double rg = revGrowths.getOrDefault(sym, Double.NaN);
                double bt = betas.getOrDefault(sym, Double.NaN);
                sb.append(String.format("  %-6s  %-20s  %-18s  %5.1f%%  %6s  %7s  %9s  %5s\n",
                        sym, name, sector, alloc,
                        fmtRatio(pe),
                        Double.isNaN(pm) ? "N/A" : String.format("%.1f%%", pm * 100),
                        Double.isNaN(rg) ? "N/A" : String.format("%+.1f%%", rg * 100),
                        Double.isNaN(bt) ? "N/A" : String.format("%.2f", bt)));
            }
        } else {
            sb.append("  HOLDINGS\n");
            sb.append("  ─────────────────────────────────────────────────────────────\n");
            sb.append(String.format("  %-6s  %-22s  %7s  %8s  %9s  %9s  %7s  %6s\n",
                    "Symbol", "Name", "Shares", "AvgCost", "Price", "Value", "P&L%", "Alloc%"));
            sb.append("  ──────  ──────────────────────  ───────  ────────  "
                    + "─────────  ─────────  ───────  ──────\n");
            for (int i = 0; i < symbols.size(); i++) {
                String sym   = symbols.get(i);
                String name  = names.getOrDefault(sym, "N/A");
                if (name.length() > 22) name = name.substring(0, 21) + "…";
                double shares = sharesList.get(i);
                double avg    = costsList.get(i);
                double price  = prices.getOrDefault(sym, Double.NaN);
                double alloc  = totalValue > 0 ? values[i] / totalValue * 100 : 0;
                sb.append(String.format("  %-6s  %-22s  %7.2f  %8s  %9s  %9s  %7s  %5.1f%%\n",
                        sym, name, shares,
                        avg > 0 ? String.format("$%.2f", avg) : "N/A",
                        Double.isNaN(price) ? "N/A" : String.format("$%.2f", price),
                        values[i] > 0 ? String.format("$%,.0f", values[i]) : "N/A",
                        Double.isNaN(gainPct[i]) ? "N/A"
                                : String.format("%+.1f%%", gainPct[i] * 100),
                        alloc));
            }
        }

        // Concentration warnings (both modes)
        sb.append("\n");
        boolean warnHeader = false;
        for (int i = 0; i < symbols.size(); i++) {
            double alloc = totalValue > 0 ? values[i] / totalValue * 100 : 0;
            if (alloc > 25) {
                if (!warnHeader) {
                    sb.append("  CONCENTRATION WARNINGS\n");
                    sb.append("  ─────────────────────────────────────────────────────────────\n");
                    warnHeader = true;
                }
                sb.append(String.format("  ⚠ %s = %.1f%% of portfolio (>25%% single-position risk)\n",
                        symbols.get(i), alloc));
            }
        }

        if (detailed) {
            // Sector allocation
            Map<String, Double> sectorAlloc  = new LinkedHashMap<>();
            Map<String, List<String>> sectorMap = new LinkedHashMap<>();
            for (int i = 0; i < symbols.size(); i++) {
                String sym    = symbols.get(i);
                String sector = sectors.getOrDefault(sym, "Unknown");
                sectorAlloc.merge(sector, values[i], Double::sum);
                sectorMap.computeIfAbsent(sector, k -> new ArrayList<>()).add(sym);
            }

            sb.append("\n  SECTOR ALLOCATION\n");
            sb.append("  ─────────────────────────────────────────────────────────────\n");
            final double finalTotalValue = totalValue;
            sectorAlloc.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(e -> {
                        double pct = finalTotalValue > 0 ? e.getValue() / finalTotalValue * 100 : 0;
                        sb.append(String.format("  %-28s  %5.1f%%  (%s)\n",
                                e.getKey(), pct,
                                String.join(", ", sectorMap.getOrDefault(e.getKey(), List.of()))));
                    });

            // Diversification gaps
            List<String> allSectors = List.of(
                    "Technology", "Healthcare", "Financial Services", "Consumer Cyclical",
                    "Consumer Defensive", "Industrials", "Energy", "Utilities",
                    "Real Estate", "Basic Materials", "Communication Services");
            List<String> missing = allSectors.stream()
                    .filter(s -> sectorAlloc.keySet().stream().noneMatch(c -> c.equalsIgnoreCase(s)))
                    .toList();

            if (!missing.isEmpty()) {
                Map<String, String> examples = Map.ofEntries(
                        Map.entry("Technology",              "AAPL, MSFT, NVDA, AVGO, ORCL"),
                        Map.entry("Healthcare",              "JNJ, UNH, LLY, ABBV, PFE"),
                        Map.entry("Financial Services",      "JPM, BRK-B, V, MA, BAC"),
                        Map.entry("Consumer Cyclical",       "AMZN, TSLA, HD, MCD, NKE"),
                        Map.entry("Consumer Defensive",      "WMT, KO, PG, COST, PEP"),
                        Map.entry("Industrials",             "CAT, HON, UPS, BA, GE"),
                        Map.entry("Energy",                  "XOM, CVX, COP, SLB, EOG"),
                        Map.entry("Utilities",               "NEE, DUK, SO, D, AEP"),
                        Map.entry("Real Estate",             "PLD, AMT, EQIX, SPG, O"),
                        Map.entry("Basic Materials",         "LIN, APD, ECL, NEM, FCX"),
                        Map.entry("Communication Services",  "GOOGL, META, NFLX, DIS, T"));

                sb.append("\n  DIVERSIFICATION GAPS (0% allocation)\n");
                sb.append("  ─────────────────────────────────────────────────────────────\n");
                for (String s : missing) {
                    sb.append(String.format("  %-28s  Consider: %s\n",
                            s, examples.getOrDefault(s, "research sector ETF")));
                }
            }

            // Risk-based guidance
            sb.append("\n  INVESTMENT FRAMEWORK — ").append(risk.toUpperCase()).append(" PROFILE\n");
            sb.append("  ─────────────────────────────────────────────────────────────\n");
            switch (risk) {
                case "conservative" -> sb.append(
                        """
                                  Focus on dividend payers, low beta (<0.8), large-cap quality.
                                  Priority sectors: Consumer Defensive, Utilities, Healthcare, Financials.
                                  Consider VYM, SCHD, or individual dividend aristocrats.
                                """);
                case "moderate" -> sb.append(
                        """
                                  Blend growth + income. Beta 0.8–1.2. Mix large-cap and dividend stocks.
                                  Use index ETFs (SPY, VTI, QQQ) for broad diversification.
                                  Rebalance concentrated positions toward missing sectors.
                                """);
                case "aggressive" -> sb.append(
                        """
                                  Higher-growth stocks acceptable. Beta >1.2 tolerated.
                                  Focus: Technology, Healthcare (biotech), Consumer Cyclical.
                                  Themes: AI/semiconductors (NVDA, AVGO), GLP-1/pharma (LLY, NVO).
                                """);
            }

            sb.append("\n  SUGGESTED NEXT STEPS\n");
            sb.append("  ─────────────────────────────────────────────────────────────\n");
            sb.append("  1. Use stock_analyze on your holdings to review current valuation\n");
            sb.append("  2. Use stock_analyze on tickers from gap sectors to compare metrics\n");
            sb.append("  3. Compare P/E vs. sector average, revenue growth, and analyst targets\n");
            sb.append("  4. Look for low correlation with your existing positions\n");
            if (investAmt > 0) {
                sb.append(String.format("  5. Deploy %s across 1–3 new positions to reduce concentration\n",
                        fmtPrice(investAmt)));
            }
        }

        sb.append("\n══════════════════════════════════════════════════════════════════\n");
        return sb.toString();
    }

    // ── Tool: stock_review ────────────────────────────────────────────────────

    @Tool(name = "stock_review",
            description = "Analyst review for a stock, or full holdings breakdown for an ETF — all data "
                    + "fetched by the tool, no LLM inference required. "
                    + "For stocks: 4-month analyst recommendation trend (strong buy/buy/hold/sell counts), "
                    + "recent upgrades and downgrades with firm names and grade changes, and an overall "
                    + "sentiment summary (bullish/neutral/bearish). "
                    + "For ETFs (SPY, QQQ, VTI, etc.): expense ratio, allocation breakdown, holdings "
                    + "P/E and P/B, and the top 10 holdings each with ticker, weight, sector, and a "
                    + "2-sentence company description fetched directly from Yahoo Finance. "
                    + "Example: symbol='NVDA' or symbol='SPY'")
    public String stockReview(
            @ToolParam(description = "Ticker symbol, e.g. 'NVDA' or 'SPY'") String symbol) {

        if (symbol == null || symbol.isBlank()) return "Error: symbol is required";
        symbol = symbol.trim().toUpperCase();

        try {
            String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" + symbol
                    + "?modules=price,financialData,recommendationTrend,"
                    + "upgradeDowngradeHistory,summaryProfile,topHoldings,fundProfile";

            JsonNode result = mapper.readTree(yahooGet(url))
                    .path("quoteSummary").path("result");
            if (result.isEmpty()) return "No data found for: " + symbol;

            JsonNode data      = result.get(0);
            JsonNode price     = data.path("price");
            String  name       = getFmt(price, "longName");
            String  quoteType  = price.path("quoteType").asText("");
            boolean isEtf      = "ETF".equalsIgnoreCase(quoteType)
                    || "MUTUALFUND".equalsIgnoreCase(quoteType);

            StringBuilder sb = new StringBuilder();
            sb.append("══════════════════════════════════════════════════════════════\n");
            if (isEtf) {
                sb.append(String.format("  %s — %s  [ETF]\n", symbol, name));
                sb.append("  Holdings Review\n");
            } else {
                sb.append(String.format("  %s — %s\n", symbol, name));
                sb.append("  Analyst Review & Consensus\n");
            }
            sb.append("══════════════════════════════════════════════════════════════\n\n");

            if (isEtf) {
                buildEtfReview(sb, symbol, data);
            } else {
                buildStockReview(sb, symbol, data);
            }

            sb.append("══════════════════════════════════════════════════════════════\n");
            return sb.toString();

        } catch (Exception e) {
            log.error("stock_review error for {}: {}", symbol, e.getMessage(), e);
            return "Error reviewing " + symbol + ": " + e.getMessage();
        }
    }

    // ── stock_review: stock branch ────────────────────────────────────────────

    private void buildStockReview(StringBuilder sb, String symbol, JsonNode data) {
        JsonNode fin    = data.path("financialData");
        JsonNode trend  = data.path("recommendationTrend").path("trend");
        JsonNode hist   = data.path("upgradeDowngradeHistory").path("history");
        JsonNode price  = data.path("price");

        double curPrice    = getRaw(price, "regularMarketPrice");
        double tgtLow      = getRaw(fin, "targetLowPrice");
        double tgtMean     = getRaw(fin, "targetMeanPrice");
        double tgtHigh     = getRaw(fin, "targetHighPrice");
        double recMean     = getRaw(fin, "recommendationMean");
        String recKey      = fin.path("recommendationKey").asText("");
        int    numAnalysts = (int) getRaw(fin, "numberOfAnalystOpinions");
        double upside      = (Double.isNaN(curPrice) || curPrice == 0 || Double.isNaN(tgtMean))
                ? Double.NaN : tgtMean / curPrice - 1;

        // ── Current consensus ─────────────────────────────────────────────────
        sb.append("  CURRENT CONSENSUS\n");
        sb.append("  ──────────────────────────────────────────────────────────\n");
        sb.append(String.format("  Rating:   %s  (%.1f / 5.0,  %d analysts)\n",
                recKey.isBlank() ? "N/A"
                        : recKey.toUpperCase().replace("_", " "),
                recMean, numAnalysts));
        if (!Double.isNaN(tgtMean)) {
            sb.append(String.format("  Targets:  %s – %s   mean %s\n",
                    fmtPrice(tgtLow), fmtPrice(tgtHigh), fmtPrice(tgtMean)));
            sb.append(String.format("  Upside:   %s from current %s\n",
                    Double.isNaN(upside) ? "N/A"
                            : String.format("%+.1f%%", upside * 100),
                    fmtPrice(curPrice)));
        }
        sb.append("\n");

        // ── Recommendation trend (4 months) ──────────────────────────────────
        if (!trend.isEmpty()) {
            sb.append("  RECOMMENDATION TREND  (analyst counts by month)\n");
            sb.append("  ──────────────────────────────────────────────────────────\n");
            sb.append(String.format("  %-12s  %9s  %5s  %5s  %5s  %9s  %8s\n",
                    "Period", "StrongBuy", "Buy", "Hold", "Sell", "StrongSell", "Score"));
            sb.append("  ────────────  ─────────  ─────  ─────  ─────  ──────────  ────────\n");
            for (JsonNode t : trend) {
                String period   = t.path("period").asText("?");
                int strongBuy   = t.path("strongBuy").asInt(0);
                int buy         = t.path("buy").asInt(0);
                int hold        = t.path("hold").asInt(0);
                int sell        = t.path("sell").asInt(0);
                int strongSell  = t.path("strongSell").asInt(0);
                int total       = strongBuy + buy + hold + sell + strongSell;
                // Bullish score: % of analysts rating buy or strong buy
                String score = total == 0 ? "N/A"
                        : String.format("%.0f%% bull",
                            (strongBuy + buy) * 100.0 / total);
                String label = period.equals("0m") ? "Current     "
                        : period.replace("-", "") + " months ago ";
                sb.append(String.format("  %-12s  %9d  %5d  %5d  %5d  %10d  %8s\n",
                        label, strongBuy, buy, hold, sell, strongSell, score));
            }
            sb.append("\n");
        }

        // ── Recent upgrades / downgrades (last 12) ────────────────────────────
        if (!hist.isEmpty()) {
            sb.append("  RECENT UPGRADES / DOWNGRADES\n");
            sb.append("  ──────────────────────────────────────────────────────────\n");
            sb.append(String.format("  %-12s  %-24s  %-11s  %s\n",
                    "Date", "Firm", "Action", "Grade Change"));
            sb.append("  ────────────  ────────────────────────  ───────────  "
                    + "────────────────────────────\n");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            int shown = 0;
            for (JsonNode h : hist) {
                if (shown++ >= 12) break;
                long   epoch      = h.path("epochGradeDate").asLong(0);
                String date       = epoch > 0
                        ? Instant.ofEpochSecond(epoch).atZone(ZoneOffset.UTC)
                                .toLocalDate().format(dtf) : "N/A";
                String firm       = h.path("firm").asText("N/A");
                if (firm.length() > 24) firm = firm.substring(0, 23) + "…";
                String action     = h.path("action").asText("").toLowerCase();
                String toGrade    = h.path("toGrade").asText("N/A");
                String fromGrade  = h.path("fromGrade").asText("");
                String gradeStr   = fromGrade.isBlank() ? toGrade : fromGrade + " → " + toGrade;
                String actionLabel = switch (action) {
                    case "up"   -> "UPGRADE    ";
                    case "down" -> "DOWNGRADE  ";
                    default     -> "REITERATE  ";
                };
                sb.append(String.format("  %-12s  %-24s  %-11s  %s\n",
                        date, firm, actionLabel, gradeStr));
            }
            sb.append("\n");
        }

        // ── Sentiment summary ─────────────────────────────────────────────────
        sb.append("  SENTIMENT SUMMARY\n");
        sb.append("  ──────────────────────────────────────────────────────────\n");

        String overall = recKey.contains("buy") ? "BULLISH"
                : recKey.contains("sell") ? "BEARISH" : "NEUTRAL";
        sb.append(String.format("  Overall:  %s\n", overall));

        if (numAnalysts > 0 && !Double.isNaN(upside)) {
            sb.append(String.format("  %d analysts cover %s — mean target implies %+.1f%% upside\n",
                    numAnalysts, symbol, upside * 100));
        }

        // Count upgrades/downgrades in last 30 days
        long cutoff = System.currentTimeMillis() / 1000 - 30L * 24 * 3600;
        int ups = 0, downs = 0;
        for (JsonNode h : hist) {
            if (h.path("epochGradeDate").asLong(0) < cutoff) break;
            String act = h.path("action").asText("").toLowerCase();
            if ("up".equals(act))   ups++;
            if ("down".equals(act)) downs++;
        }
        if (ups + downs > 0) {
            sb.append(String.format("  Last 30 days: %d upgrade(s), %d downgrade(s)\n", ups, downs));
        }

        // Trend direction
        if (trend.size() >= 2) {
            int currBull = trend.get(0).path("strongBuy").asInt(0)
                    + trend.get(0).path("buy").asInt(0);
            int prevBull = trend.get(1).path("strongBuy").asInt(0)
                    + trend.get(1).path("buy").asInt(0);
            if (currBull > prevBull) {
                sb.append("  Trend: buy/strong-buy count rose vs. last month ↑\n");
            } else if (currBull < prevBull) {
                sb.append("  Trend: buy/strong-buy count fell vs. last month ↓\n");
            } else {
                sb.append("  Trend: buy/strong-buy count unchanged vs. last month →\n");
            }
        }
        sb.append("\n");
    }

    // ── stock_review: ETF branch ──────────────────────────────────────────────

    private void buildEtfReview(StringBuilder sb, String etfSymbol, JsonNode data)
            throws Exception {

        JsonNode fundProfile = data.path("fundProfile");
        JsonNode topHoldings = data.path("topHoldings");
        JsonNode price       = data.path("price");

        // ── ETF profile ───────────────────────────────────────────────────────
        double curPrice  = getRaw(price, "regularMarketPrice");
        String category  = getFmt(fundProfile, "categoryName");
        String family    = getFmt(fundProfile, "family");
        String legalType = getFmt(fundProfile, "legalType");

        JsonNode fees     = fundProfile.path("feesExpensesInvestment");
        double   expRatio = getRaw(fees, "annualReportExpenseRatio");
        if (Double.isNaN(expRatio)) expRatio = getRaw(fees, "netExpRatio");

        double stockPos = getRaw(topHoldings, "stockPosition");
        double bondPos  = getRaw(topHoldings, "bondPosition");
        double cashPos  = getRaw(topHoldings, "cashPosition");

        sb.append("  ETF PROFILE\n");
        sb.append("  ──────────────────────────────────────────────────────────\n");
        sb.append(String.format("  Fund Family:   %s\n", family));
        sb.append(String.format("  Category:      %s\n", category));
        sb.append(String.format("  Type:          %s\n", legalType));
        sb.append(String.format("  Price:         %s\n", fmtPrice(curPrice)));
        if (!Double.isNaN(expRatio) && expRatio > 0) {
            sb.append(String.format("  Expense Ratio: %.4f%%  "
                    + "(annual cost on $10,000 invested: $%.2f)\n",
                    expRatio * 100, expRatio * 10_000));
        }
        if (!Double.isNaN(stockPos)) {
            sb.append(String.format("  Allocation:    %.1f%% stocks",  stockPos * 100));
            if (!Double.isNaN(bondPos) && bondPos > 0)
                sb.append(String.format("  /  %.1f%% bonds", bondPos * 100));
            if (!Double.isNaN(cashPos) && cashPos > 0)
                sb.append(String.format("  /  %.1f%% cash",  cashPos * 100));
            sb.append("\n");
        }

        // Blended valuation of the ETF's equity holdings
        JsonNode eqh = topHoldings.path("equityHoldings");
        if (!eqh.isMissingNode() && !eqh.isNull()) {
            double pe  = getRaw(eqh, "priceToEarnings");
            double pb  = getRaw(eqh, "priceToBook");
            double ps  = getRaw(eqh, "priceToSales");
            double pcf = getRaw(eqh, "priceToCashflow");
            if (!Double.isNaN(pe)) {
                sb.append(String.format("  Holdings avg:  P/E %.1f  |  P/B %.1f"
                        + "  |  P/S %.1f  |  P/CF %.1f\n", pe, pb, ps, pcf));
            }
        }
        sb.append("\n");

        // ── Top holdings ──────────────────────────────────────────────────────
        JsonNode holdings = topHoldings.path("holdings");
        if (holdings.isEmpty()) {
            sb.append("  Holdings data not available from Yahoo Finance.\n");
            return;
        }

        int limit = Math.min(holdings.size(), 10);

        // Collect symbols, names, weights
        List<String> syms    = new ArrayList<>();
        List<String> hNames  = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            JsonNode h = holdings.get(i);
            syms.add(h.path("symbol").asText("").trim().toUpperCase());
            hNames.add(h.path("holdingName").asText("N/A"));
            weights.add(h.path("holdingPercent").asDouble(0));
        }

        // Warm up session before parallel calls so crumb is ready
        if (crumb == null || System.currentTimeMillis() > crumbExpiryMs) refreshSession();
        Map<String, String[]> details = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(limit, 4));

        List<Future<?>> futures = new ArrayList<>();
        for (String sym : syms) {
            if (sym.isBlank()) continue;
            futures.add(pool.submit(() -> {
                try {
                    String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/"
                            + sym + "?modules=summaryProfile";
                    JsonNode res = mapper.readTree(yahooGet(url))
                            .path("quoteSummary").path("result");
                    if (!res.isEmpty()) {
                        JsonNode prof    = res.get(0).path("summaryProfile");
                        String   sector  = getFmt(prof, "sector");
                        String   rawDesc = prof.path("longBusinessSummary").asText("");
                        details.put(sym, new String[]{sector, twoSentences(rawDesc)});
                    }
                } catch (Exception ex) {
                    log.debug("Could not fetch profile for holding {}: {}", sym, ex.getMessage());
                }
            }));
        }

        pool.shutdown();
        try {
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("ETF holding description fetch timed out for some holdings");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // ── Render holdings table ─────────────────────────────────────────────
        sb.append(String.format("  TOP %d HOLDINGS\n", limit));
        sb.append("  ──────────────────────────────────────────────────────────────\n");
        double weightSum = 0;
        for (int i = 0; i < limit; i++) {
            String sym    = syms.get(i);
            String hName  = hNames.get(i);
            double wt     = weights.get(i);
            weightSum    += wt;
            String[] det  = details.get(sym);
            String sector = (det != null && det[0] != null) ? det[0] : "N/A";
            String desc   = (det != null && det[1] != null) ? det[1] : "";

            // Header line: rank, symbol, name, weight, sector
            if (hName.length() > 30) hName = hName.substring(0, 29) + "…";
            sb.append(String.format("  %2d. %-6s  %-30s  %5.2f%%  %s\n",
                    i + 1, sym, hName, wt * 100, sector));

            // Description (word-wrapped, indented)
            if (!desc.isBlank()) {
                sb.append(wrapText(desc, 74, "      ")).append("\n");
            }
            sb.append("\n");
        }
        sb.append(String.format("  Top %d holdings account for %.1f%% of fund\n\n",
                limit, weightSum * 100));
    }

    // ── Helpers for stock_review ──────────────────────────────────────────────

    /** Returns the first two sentences of text, or the full text if fewer than two. */
    static String twoSentences(String text) {
        if (text == null || text.isBlank()) return "";
        int count = 0, pos = 0;
        while (pos < text.length()) {
            int dot = text.indexOf(". ", pos);
            if (dot < 0) break;
            if (++count == 2) return text.substring(0, dot + 1);
            pos = dot + 2;
        }
        return text.length() > 260 ? text.substring(0, 257) + "..." : text;
    }
}
