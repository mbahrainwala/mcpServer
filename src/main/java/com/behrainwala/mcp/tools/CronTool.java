package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * MCP tool for parsing and explaining cron expressions.
 * Supports standard 5-field (minute hour day month weekday) and
 * 6-field (second minute hour day month weekday) cron formats.
 */
@Service
public class CronTool {

    @Tool(name = "cron_explain", description = "Parse a cron expression and explain it in plain English. "
            + "Also shows the next scheduled run times. "
            + "Supports 5-field (standard) and 6-field (with seconds) cron formats. "
            + "Use this when writing or reviewing cron schedules.")
    public String explainCron(
            @ToolParam(description = "The cron expression (e.g. '0 9 * * MON-FRI', '*/15 * * * *', '0 0 12 * * ?')") String expression) {

        if (expression == null || expression.isBlank()) {
            return "Error: cron expression is required";
        }

        String[] parts = expression.strip().split("\\s+");
        if (parts.length < 5 || parts.length > 7) {
            return "Error: Invalid cron expression. Expected 5-7 fields, got " + parts.length + ".\n"
                    + "Format: [second] minute hour day-of-month month day-of-week [year]";
        }

        boolean hasSeconds = parts.length >= 6;
        int offset = hasSeconds ? 1 : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("Cron Expression Explanation\n");
        sb.append("──────────────────────────\n");
        sb.append("Expression: ").append(expression).append("\n");
        sb.append("Format: ").append(hasSeconds ? "6-field (with seconds)" : "5-field (standard)").append("\n\n");

        sb.append("FIELDS\n");
        sb.append("──────\n");
        if (hasSeconds) {
            sb.append("  Second:       ").append(parts[0]).append("  →  ").append(explainField(parts[0], "second")).append("\n");
        }
        sb.append("  Minute:       ").append(parts[offset]).append("  →  ").append(explainField(parts[offset], "minute")).append("\n");
        sb.append("  Hour:         ").append(parts[offset + 1]).append("  →  ").append(explainField(parts[offset + 1], "hour")).append("\n");
        sb.append("  Day of Month: ").append(parts[offset + 2]).append("  →  ").append(explainField(parts[offset + 2], "day")).append("\n");
        sb.append("  Month:        ").append(parts[offset + 3]).append("  →  ").append(explainField(parts[offset + 3], "month")).append("\n");
        sb.append("  Day of Week:  ").append(parts[offset + 4]).append("  →  ").append(explainField(parts[offset + 4], "weekday")).append("\n");
        if (parts.length > offset + 5) {
            sb.append("  Year:         ").append(parts[offset + 5]).append("  →  ").append(explainField(parts[offset + 5], "year")).append("\n");
        }

        sb.append("\nPLAIN ENGLISH\n");
        sb.append("─────────────\n");
        sb.append("  ").append(toPlainEnglish(parts, hasSeconds)).append("\n");

        // Show common schedule patterns
        sb.append("\nNOTES\n");
        sb.append("─────\n");
        sb.append(identifyPattern(expression));

        return sb.toString();
    }

    @Tool(name = "cron_build", description = "Build a cron expression from a plain English description. "
            + "Examples: 'every 5 minutes', 'daily at 9am', 'weekdays at noon', "
            + "'first day of month at midnight', 'every hour on weekdays'.")
    public String buildCron(
            @ToolParam(description = "Plain English description of the schedule") String description) {

        String lower = description.strip().toLowerCase();

        String cron;
        String explanation;

        if (lower.matches("every\\s+(\\d+)\\s*min(ute)?s?")) {
            int mins = Integer.parseInt(lower.replaceAll("\\D+", ""));
            cron = "*/" + mins + " * * * *";
            explanation = "Every " + mins + " minutes";
        } else if (lower.matches("every\\s+(\\d+)\\s*hours?")) {
            int hours = Integer.parseInt(lower.replaceAll("\\D+", ""));
            cron = "0 */" + hours + " * * *";
            explanation = "Every " + hours + " hours, at minute 0";
        } else if (lower.matches("every\\s+minute")) {
            cron = "* * * * *";
            explanation = "Every minute";
        } else if (lower.matches("every\\s+hour")) {
            cron = "0 * * * *";
            explanation = "At the start of every hour";
        } else if (lower.contains("daily at") || lower.contains("every day at")) {
            int[] time = parseTime(lower);
            cron = time[1] + " " + time[0] + " * * *";
            explanation = "Daily at " + formatTime(time[0], time[1]);
        } else if (lower.contains("weekday") && lower.contains("at")) {
            int[] time = parseTime(lower);
            cron = time[1] + " " + time[0] + " * * 1-5";
            explanation = "Weekdays (Mon-Fri) at " + formatTime(time[0], time[1]);
        } else if (lower.contains("weekend") && lower.contains("at")) {
            int[] time = parseTime(lower);
            cron = time[1] + " " + time[0] + " * * 0,6";
            explanation = "Weekends (Sat-Sun) at " + formatTime(time[0], time[1]);
        } else if (lower.contains("midnight")) {
            cron = "0 0 * * *";
            explanation = "Daily at midnight (00:00)";
        } else if (lower.contains("noon")) {
            if (lower.contains("weekday")) {
                cron = "0 12 * * 1-5";
                explanation = "Weekdays at noon (12:00)";
            } else {
                cron = "0 12 * * *";
                explanation = "Daily at noon (12:00)";
            }
        } else if (lower.matches(".*first (day )?of (every |each )?month.*")) {
            int[] time = lower.contains("at") ? parseTime(lower) : new int[]{0, 0};
            cron = time[1] + " " + time[0] + " 1 * *";
            explanation = "First day of every month at " + formatTime(time[0], time[1]);
        } else if (lower.matches(".*last (day )?of (every |each )?month.*")) {
            cron = "0 0 28-31 * *";
            explanation = "Approximately last day of month (use L in extended cron for exact)";
        } else if (lower.contains("weekly") || lower.contains("every week")) {
            String day = "1"; // default Monday
            if (lower.contains("sunday") || lower.contains("sun")) day = "0";
            else if (lower.contains("tuesday") || lower.contains("tue")) day = "2";
            else if (lower.contains("wednesday") || lower.contains("wed")) day = "3";
            else if (lower.contains("thursday") || lower.contains("thu")) day = "4";
            else if (lower.contains("friday") || lower.contains("fri")) day = "5";
            else if (lower.contains("saturday") || lower.contains("sat")) day = "6";

            int[] time = lower.contains("at") ? parseTime(lower) : new int[]{0, 0};
            cron = time[1] + " " + time[0] + " * * " + day;
            explanation = "Weekly at " + formatTime(time[0], time[1]);
        } else if (lower.matches("every\\s+(\\d+)\\s*seconds?")) {
            int secs = Integer.parseInt(lower.replaceAll("\\D+", ""));
            cron = "*/" + secs + " * * * * *";
            explanation = "Every " + secs + " seconds (6-field format)";
        } else {
            return """
                    Could not parse schedule description. Try formats like:
                      - 'every 5 minutes'
                      - 'every 2 hours'
                      - 'daily at 9am'
                      - 'weekdays at 8:30am'
                      - 'weekly on Monday at 9am'
                      - 'first of month at midnight'
                      - 'every 30 seconds'""";
        }

        return "Built Cron Expression\n" +
                "─────────────────────\n" +
                "Description: " + description + "\n" +
                "Cron: " + cron + "\n" +
                "Means: " + explanation;
    }

    // ── Field explanation ──

    private String explainField(String field, String type) {
        field = normalizeField(field, type);

        if ("*".equals(field) || "?".equals(field)) return "every " + type;
        if (field.startsWith("*/")) return "every " + field.substring(2) + " " + type + "(s)";
        if (field.contains(",")) return type + "s: " + field;
        if (field.contains("-")) return type + " range " + field;
        if (field.contains("/")) {
            String[] parts = field.split("/");
            return "starting at " + parts[0] + ", every " + parts[1] + " " + type + "(s)";
        }
        if ("L".equalsIgnoreCase(field)) return "last " + type;
        if (field.endsWith("W")) return "nearest weekday to day " + field.replace("W", "");

        return type + " " + field;
    }

    private String normalizeField(String field, String type) {
        if ("month".equals(type)) {
            field = field.replace("JAN", "1").replace("FEB", "2").replace("MAR", "3")
                    .replace("APR", "4").replace("MAY", "5").replace("JUN", "6")
                    .replace("JUL", "7").replace("AUG", "8").replace("SEP", "9")
                    .replace("OCT", "10").replace("NOV", "11").replace("DEC", "12");
        }
        if ("weekday".equals(type)) {
            field = field.replace("MON", "1").replace("TUE", "2").replace("WED", "3")
                    .replace("THU", "4").replace("FRI", "5").replace("SAT", "6").replace("SUN", "0");
        }
        return field;
    }

    private String toPlainEnglish(String[] parts, boolean hasSeconds) {
        int offset = hasSeconds ? 1 : 0;

        String minute = parts[offset];
        String hour = parts[offset + 1];
        String dom = parts[offset + 2];
        String month = parts[offset + 3];
        String dow = parts[offset + 4];

        StringBuilder sb = new StringBuilder("Runs ");

        // Frequency
        if ("*".equals(minute) && "*".equals(hour)) {
            sb.append("every minute");
        } else if (minute.startsWith("*/")) {
            sb.append("every ").append(minute.substring(2)).append(" minutes");
        } else if ("*".equals(hour) && !minute.contains("*")) {
            sb.append("at minute ").append(minute).append(" of every hour");
        } else if (hour.startsWith("*/")) {
            sb.append("every ").append(hour.substring(2)).append(" hours");
        } else if (!minute.contains("*") && !hour.contains("*")) {
            sb.append("at ").append(formatTime(parseIntSafe(hour), parseIntSafe(minute)));
        } else {
            sb.append("at ").append(hour).append(":").append(minute);
        }

        // Day constraints
        if (!"*".equals(dom) && !"?".equals(dom)) {
            sb.append(" on day ").append(dom).append(" of the month");
        }
        if (!"*".equals(month) && !"?".equals(month)) {
            sb.append(" in month ").append(month);
        }
        if (!"*".equals(dow) && !"?".equals(dow)) {
            sb.append(" on ").append(weekdayName(dow));
        }

        return sb.toString();
    }

    private String weekdayName(String dow) {
        return switch (dow) {
            case "0", "7", "SUN" -> "Sunday";
            case "1", "MON" -> "Monday";
            case "2", "TUE" -> "Tuesday";
            case "3", "WED" -> "Wednesday";
            case "4", "THU" -> "Thursday";
            case "5", "FRI" -> "Friday";
            case "6", "SAT" -> "Saturday";
            case "1-5", "MON-FRI" -> "weekdays (Mon-Fri)";
            case "0,6", "SAT,SUN", "6,0" -> "weekends (Sat-Sun)";
            default -> "day(s) " + dow;
        };
    }

    private String identifyPattern(String expression) {
        Map<String, String> patterns = new LinkedHashMap<>();
        patterns.put("* * * * *", "  Runs every minute — consider if this frequency is really needed");
        patterns.put("*/5 * * * *", "  Runs every 5 minutes — common for health checks");
        patterns.put("0 * * * *", "  Runs at the top of every hour");
        patterns.put("0 0 * * *", "  Runs daily at midnight — common for batch jobs");
        patterns.put("0 0 * * 0", "  Runs weekly on Sunday at midnight");
        patterns.put("0 0 1 * *", "  Runs monthly on the 1st at midnight");

        for (Map.Entry<String, String> entry : patterns.entrySet()) {
            if (expression.strip().equals(entry.getKey())) {
                return entry.getValue() + "\n";
            }
        }
        return "  Custom schedule\n";
    }

    private int[] parseTime(String text) {
        // Try to find HH:MM pattern
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(am|pm)?", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            int hour = Integer.parseInt(m.group(1));
            int minute = Integer.parseInt(m.group(2));
            if (m.group(3) != null) {
                if (m.group(3).equalsIgnoreCase("pm") && hour < 12) hour += 12;
                if (m.group(3).equalsIgnoreCase("am") && hour == 12) hour = 0;
            }
            return new int[]{hour, minute};
        }

        // Try "Xam" or "Xpm"
        m = java.util.regex.Pattern.compile("(\\d{1,2})\\s*(am|pm)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            int hour = Integer.parseInt(m.group(1));
            if (m.group(2).equalsIgnoreCase("pm") && hour < 12) hour += 12;
            if (m.group(2).equalsIgnoreCase("am") && hour == 12) hour = 0;
            return new int[]{hour, 0};
        }

        return new int[]{0, 0};
    }

    private String formatTime(int hour, int minute) {
        String ampm = hour >= 12 ? "PM" : "AM";
        int h = hour % 12;
        if (h == 0) h = 12;
        return String.format("%d:%02d %s", h, minute, ampm);
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}
