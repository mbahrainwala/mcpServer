package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeToolTest {

    private DateTimeTool tool;

    @BeforeEach
    void setUp() {
        tool = new DateTimeTool();
    }

    // ── getCurrentDateTime ───────────────────────────────────────────────────

    @Test
    void getCurrentDateTime_returnsDateFields() {
        String result = tool.getCurrentDateTime(null);
        assertThat(result).containsIgnoringCase("Date:")
                .containsIgnoringCase("Time:")
                .containsIgnoringCase("Timezone:")
                .containsIgnoringCase("Unix Timestamp:");
    }

    @Test
    void getCurrentDateTime_withValidTimezone() {
        String result = tool.getCurrentDateTime("America/New_York");
        assertThat(result).contains("America/New_York");
    }

    @Test
    void getCurrentDateTime_invalidTimezone_returnsError() {
        String result = tool.getCurrentDateTime("InvalidZone/Nowhere");
        assertThat(result).containsIgnoringCase("error").containsIgnoringCase("invalid");
    }

    // ── convertTimezone ──────────────────────────────────────────────────────

    @Test
    void convertTimezone_nycToTokyo() {
        String result = tool.convertTimezone("2024-01-01T12:00:00", "America/New_York", "Asia/Tokyo");
        assertThat(result).contains("America/New_York").contains("Asia/Tokyo");
    }

    @Test
    void convertTimezone_invalidDateTime_returnsError() {
        String result = tool.convertTimezone("not-a-date", "UTC", "UTC");
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void convertTimezone_invalidTimezone_returnsError() {
        String result = tool.convertTimezone("2024-01-01T12:00:00", "Fake/Zone", "UTC");
        assertThat(result).containsIgnoringCase("error");
    }

    // ── dateDifference ───────────────────────────────────────────────────────

    @Test
    void dateDifference_exactDays() {
        String result = tool.dateDifference("2024-01-01", "2024-01-11");
        assertThat(result).contains("10");
    }

    @Test
    void dateDifference_sameDay() {
        String result = tool.dateDifference("2024-06-15", "2024-06-15");
        assertThat(result).contains("0");
    }

    @Test
    void dateDifference_acrossYears() {
        String result = tool.dateDifference("2023-01-01", "2024-01-01");
        // 365 days (2023 is not a leap year)
        assertThat(result).contains("365");
    }

    @Test
    void dateDifference_invalidDate_returnsError() {
        String result = tool.dateDifference("not-a-date", "2024-01-01");
        assertThat(result).containsIgnoringCase("error");
    }
}
