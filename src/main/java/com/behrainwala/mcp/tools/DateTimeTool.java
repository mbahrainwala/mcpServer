package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * MCP tool for date and time operations.
 * LLMs have no concept of "now" — this tool provides real-time date/time info.
 */
@Service
public class DateTimeTool {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm:ss a z");

    @Tool(name = "get_current_datetime", description = "Get the current date and time. "
            + "Use this tool whenever the user asks about today's date, the current time, "
            + "what day it is, or any question that requires knowing 'now'. "
            + "You can optionally specify a timezone.")
    public String getCurrentDateTime(
            @ToolParam(description = "IANA timezone ID (e.g. 'America/New_York', 'Europe/London', 'Asia/Tokyo'). "
                    + "Defaults to the server's local timezone if not specified.", required = false) String timezone) {

        ZoneId zone;
        try {
            zone = (timezone != null && !timezone.isBlank())
                    ? ZoneId.of(timezone)
                    : ZoneId.systemDefault();
        } catch (Exception e) {
            return "Error: Invalid timezone '" + timezone + "'. Use IANA format like 'America/New_York'.";
        }

        ZonedDateTime now = ZonedDateTime.now(zone);

        return "Current Date & Time\n" +
                "-------------------\n" +
                "Date: " + now.toLocalDate() + "\n" +
                "Time: " + now.toLocalTime().truncatedTo(ChronoUnit.SECONDS) + "\n" +
                "Day: " + now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + "\n" +
                "Timezone: " + zone.getId() + " (" + now.getOffset() + ")\n" +
                "Formatted: " + now.format(DISPLAY_FORMAT) + "\n" +
                "ISO 8601: " + now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "\n" +
                "Unix Timestamp: " + now.toEpochSecond();
    }

    @Tool(name = "convert_timezone", description = "Convert a date/time from one timezone to another.")
    public String convertTimezone(
            @ToolParam(description = "The date and time to convert in ISO format (e.g. '2024-03-15T14:30:00')") String dateTime,
            @ToolParam(description = "Source timezone (e.g. 'America/New_York')") String fromTimezone,
            @ToolParam(description = "Target timezone (e.g. 'Asia/Tokyo')") String toTimezone) {

        try {
            LocalDateTime local = LocalDateTime.parse(dateTime);
            ZonedDateTime source = local.atZone(ZoneId.of(fromTimezone));
            ZonedDateTime target = source.withZoneSameInstant(ZoneId.of(toTimezone));

            return String.format("""
                            Timezone Conversion
                            -------------------
                            From: %s (%s)
                            To:   %s (%s)""",
                    source.format(DISPLAY_FORMAT), fromTimezone,
                    target.format(DISPLAY_FORMAT), toTimezone);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "date_difference", description = "Calculate the difference between two dates. "
            + "Returns the number of days, weeks, months, and years between them.")
    public String dateDifference(
            @ToolParam(description = "Start date in ISO format (YYYY-MM-DD)") String startDate,
            @ToolParam(description = "End date in ISO format (YYYY-MM-DD)") String endDate) {

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            Period period = Period.between(start, end);
            long totalDays = ChronoUnit.DAYS.between(start, end);

            StringBuilder sb = new StringBuilder();
            sb.append("Date Difference\n");
            sb.append("---------------\n");
            sb.append("From: ").append(start.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))).append("\n");
            sb.append("To:   ").append(end.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))).append("\n\n");
            sb.append("Difference: ");
            if (period.getYears() != 0) sb.append(Math.abs(period.getYears())).append(" year(s), ");
            if (period.getMonths() != 0) sb.append(Math.abs(period.getMonths())).append(" month(s), ");
            sb.append(Math.abs(period.getDays())).append(" day(s)\n");
            sb.append("Total days: ").append(totalDays).append("\n");
            sb.append("Total weeks: ").append(totalDays / 7).append(" weeks and ").append(totalDays % 7).append(" days");

            return sb.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage() + ". Use ISO format: YYYY-MM-DD";
        }
    }
}
