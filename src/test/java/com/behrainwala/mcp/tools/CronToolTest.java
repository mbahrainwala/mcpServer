package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CronToolTest {

    private CronTool tool;

    @BeforeEach
    void setUp() {
        tool = new CronTool();
    }

    // ── explainCron ──────────────────────────────────────────────────────────

    @Test
    void explainCron_everyMinute() {
        String result = tool.explainCron("* * * * *");
        assertThat(result).containsIgnoringCase("every").containsIgnoringCase("minute");
    }

    @Test
    void explainCron_everyFifteenMinutes() {
        String result = tool.explainCron("*/15 * * * *");
        assertThat(result).contains("15");
    }

    @Test
    void explainCron_daily9am() {
        String result = tool.explainCron("0 9 * * *");
        assertThat(result).contains("9");
    }

    @Test
    void explainCron_weekdays() {
        String result = tool.explainCron("0 9 * * MON-FRI");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("MON"), s -> assertThat(s).containsIgnoringCase("weekday"));
    }

    @Test
    void explainCron_withSeconds_6fields() {
        String result = tool.explainCron("0 */15 * * * *");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("second"), s -> assertThat(s).contains("6-field"));
    }

    @Test
    void explainCron_blank_returnsError() {
        String result = tool.explainCron("");
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void explainCron_null_returnsError() {
        String result = tool.explainCron(null);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void explainCron_tooFewFields_returnsError() {
        String result = tool.explainCron("* * *");
        assertThat(result).containsIgnoringCase("error").containsIgnoringCase("field");
    }

    // ── buildCron ────────────────────────────────────────────────────────────

    @Test
    void buildCron_every5Minutes() {
        String result = tool.buildCron("every 5 minutes");
        assertThat(result).contains("*/5");
    }

    @Test
    void buildCron_dailyMidnight() {
        String result = tool.buildCron("daily at midnight");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("0 0"), s -> assertThat(s).contains("0"));
    }

    @Test
    void buildCron_weekdaysNoon() {
        String result = tool.buildCron("weekdays at noon");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("12"), s -> assertThat(s).containsIgnoringCase("weekday"));
    }
}
