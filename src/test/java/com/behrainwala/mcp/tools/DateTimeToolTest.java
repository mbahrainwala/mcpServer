package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeToolTest {

    private DateTimeTool tool;

    @BeforeEach
    void setUp() {
        tool = new DateTimeTool();
    }

    // ── getCurrentDateTime ───────────────────────────────────────────────────

    @Test
    void getCurrentDateTime_returnsAllFields() {
        String result = tool.getCurrentDateTime(null);
        assertThat(result)
                .contains("Date:")
                .contains("Time:")
                .contains("Day:")
                .contains("Timezone:")
                .contains("Formatted:")
                .contains("ISO 8601:")
                .contains("Unix Timestamp:");
    }

    @Test
    void getCurrentDateTime_systemDefault_returnsDate() {
        String result = tool.getCurrentDateTime(null);
        // Should contain today's date
        String today = LocalDate.now().toString();
        assertThat(result).contains(today);
    }

    @Test
    void getCurrentDateTime_withValidTimezone() {
        String result = tool.getCurrentDateTime("America/New_York");
        assertThat(result).contains("America/New_York");
    }

    @Test
    void getCurrentDateTime_utc() {
        String result = tool.getCurrentDateTime("UTC");
        assertThat(result).contains("UTC");
    }

    @Test
    void getCurrentDateTime_blankTimezone_usesDefault() {
        String result = tool.getCurrentDateTime("   ");
        assertThat(result).contains("Date:").contains("Time:");
    }

    @Test
    void getCurrentDateTime_invalidTimezone_returnsError() {
        String result = tool.getCurrentDateTime("InvalidZone/Nowhere");
        assertThat(result).contains("Error: Invalid timezone");
    }

    // ── convertTimezone ──────────────────────────────────────────────────────

    @Test
    void convertTimezone_nycToTokyo() {
        String result = tool.convertTimezone("2024-01-01T12:00:00", "America/New_York", "Asia/Tokyo");
        assertThat(result)
                .contains("Timezone Conversion")
                .contains("America/New_York")
                .contains("Asia/Tokyo");
    }

    @Test
    void convertTimezone_utcToLondon() {
        String result = tool.convertTimezone("2024-06-15T10:00:00", "UTC", "Europe/London");
        assertThat(result).contains("Europe/London");
    }

    @Test
    void convertTimezone_invalidDateTime_returnsError() {
        String result = tool.convertTimezone("not-a-date", "UTC", "UTC");
        assertThat(result).contains("Error:");
    }

    @Test
    void convertTimezone_invalidFromTimezone_returnsError() {
        String result = tool.convertTimezone("2024-01-01T12:00:00", "Fake/Zone", "UTC");
        assertThat(result).contains("Error:");
    }

    @Test
    void convertTimezone_invalidToTimezone_returnsError() {
        String result = tool.convertTimezone("2024-01-01T12:00:00", "UTC", "Fake/Zone");
        assertThat(result).contains("Error:");
    }

    // ── dateDifference ───────────────────────────────────────────────────────

    @Test
    void dateDifference_exactDays() {
        String result = tool.dateDifference("2024-01-01", "2024-01-11");
        assertThat(result).contains("Total days: 10");
    }

    @Test
    void dateDifference_sameDay() {
        String result = tool.dateDifference("2024-06-15", "2024-06-15");
        assertThat(result).contains("Total days: 0").contains("0 day(s)");
    }

    @Test
    void dateDifference_acrossYears() {
        String result = tool.dateDifference("2023-01-01", "2024-01-01");
        assertThat(result).contains("365").contains("1 year(s)");
    }

    @Test
    void dateDifference_acrossMonths() {
        String result = tool.dateDifference("2024-01-15", "2024-03-20");
        assertThat(result).contains("month(s)").contains("day(s)");
    }

    @Test
    void dateDifference_weeksCalculation() {
        String result = tool.dateDifference("2024-01-01", "2024-01-15");
        assertThat(result).contains("2 weeks and 0 days");
    }

    @Test
    void dateDifference_includesFormattedDates() {
        String result = tool.dateDifference("2024-01-01", "2024-01-02");
        assertThat(result).contains("Date Difference").contains("From:").contains("To:");
    }

    @Test
    void dateDifference_invalidStartDate_returnsError() {
        String result = tool.dateDifference("not-a-date", "2024-01-01");
        assertThat(result).contains("Error:").contains("YYYY-MM-DD");
    }

    @Test
    void dateDifference_invalidEndDate_returnsError() {
        String result = tool.dateDifference("2024-01-01", "not-a-date");
        assertThat(result).contains("Error:");
    }

    @Test
    void dateDifference_noYearsNoMonths_onlyDays() {
        String result = tool.dateDifference("2024-01-01", "2024-01-05");
        // 4 days, no years or months in output
        assertThat(result).contains("4 day(s)").doesNotContain("year(s)").doesNotContain("month(s)");
    }
}
