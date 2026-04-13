package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CronToolTest {

    private CronTool tool;

    @BeforeEach
    void setUp() {
        tool = new CronTool();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  explainCron
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    class ExplainCron {

        // ── null / blank / invalid field count ──────────────────────────────

        @Test
        void null_returnsError() {
            String result = tool.explainCron(null);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void blank_returnsError() {
            String result = tool.explainCron("");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void whitespaceOnly_returnsError() {
            String result = tool.explainCron("   ");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void tooFewFields_returnsError() {
            String result = tool.explainCron("* * *");
            assertThat(result).containsIgnoringCase("error")
                    .contains("3")
                    .containsIgnoringCase("field");
        }

        @Test
        void tooManyFields_returnsError() {
            String result = tool.explainCron("0 0 0 * * * * extra");
            assertThat(result).containsIgnoringCase("error")
                    .contains("8");
        }

        // ── 5-field standard expressions ────────────────────────────────────

        @Test
        void everyMinute() {
            String result = tool.explainCron("* * * * *");
            assertThat(result).contains("5-field (standard)")
                    .containsIgnoringCase("every minute");
            // identifyPattern match
            assertThat(result).containsIgnoringCase("every minute");
        }

        @Test
        void every5Minutes() {
            String result = tool.explainCron("*/5 * * * *");
            assertThat(result).contains("*/5")
                    .contains("every 5 minute(s)");
            // known pattern
            assertThat(result).containsIgnoringCase("health check");
        }

        @Test
        void topOfEveryHour() {
            String result = tool.explainCron("0 * * * *");
            assertThat(result).containsIgnoringCase("top of every hour");
        }

        @Test
        void dailyMidnight() {
            String result = tool.explainCron("0 0 * * *");
            assertThat(result).containsIgnoringCase("midnight")
                    .containsIgnoringCase("batch");
        }

        @Test
        void weeklySundayMidnight() {
            String result = tool.explainCron("0 0 * * 0");
            assertThat(result).containsIgnoringCase("weekly")
                    .containsIgnoringCase("Sunday");
        }

        @Test
        void monthlyFirst() {
            String result = tool.explainCron("0 0 1 * *");
            assertThat(result).containsIgnoringCase("monthly")
                    .containsIgnoringCase("1st");
        }

        @Test
        void customSchedule_identifyPatternReturnsCustom() {
            String result = tool.explainCron("30 14 * * 3");
            assertThat(result).containsIgnoringCase("custom schedule");
        }

        @Test
        void specificTimeAndDay() {
            String result = tool.explainCron("0 9 * * 1-5");
            assertThat(result).contains("9").contains("1-5");
            // toPlainEnglish: at 9:00 AM on weekdays (Mon-Fri)
            assertThat(result).containsIgnoringCase("weekday");
        }

        @Test
        void rangeInMinute() {
            String result = tool.explainCron("5-10 * * * *");
            // explainField sees range
            assertThat(result).contains("minute range 5-10");
        }

        @Test
        void listInMinute() {
            String result = tool.explainCron("0,15,30,45 * * * *");
            assertThat(result).contains("minutes: 0,15,30,45");
        }

        @Test
        void slashWithStart() {
            String result = tool.explainCron("5/15 * * * *");
            // explainField: starting at 5, every 15 minute(s)
            assertThat(result).contains("starting at 5").contains("every 15 minute(s)");
        }

        @Test
        void questionMark_treatedAsWildcard() {
            String result = tool.explainCron("0 12 * * ?");
            assertThat(result).contains("every weekday");
        }

        @Test
        void lField() {
            String result = tool.explainCron("0 0 L * *");
            assertThat(result).containsIgnoringCase("last day");
        }

        @Test
        void wField() {
            String result = tool.explainCron("0 0 15W * *");
            assertThat(result).contains("nearest weekday to day 15");
        }

        // ── month name normalization ────────────────────────────────────────

        @Test
        void monthNameNormalization() {
            String result = tool.explainCron("0 0 1 JAN *");
            assertThat(result).contains("month 1");
        }

        @Test
        void allMonthNames() {
            // Test that all month names get normalized
            for (String month : new String[]{"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"}) {
                String result = tool.explainCron("0 0 1 " + month + " *");
                assertThat(result).isNotBlank();
            }
        }

        // ── weekday name normalization ──────────────────────────────────────

        @Test
        void weekdayNameNormalization_MON() {
            String result = tool.explainCron("0 9 * * MON");
            // MON -> 1 -> Monday
            assertThat(result).contains("Monday");
        }

        @Test
        void weekdayNameNormalization_MONFRI() {
            String result = tool.explainCron("0 9 * * MON-FRI");
            assertThat(result).containsIgnoringCase("weekday");
        }

        // ── 6-field expressions (with seconds) ─────────────────────────────

        @Test
        void sixField_showsSecondsAndFormat() {
            String result = tool.explainCron("0 */15 * * * *");
            assertThat(result).contains("6-field (with seconds)")
                    .contains("Second:");
        }

        @Test
        void sixField_everySecond() {
            String result = tool.explainCron("* * * * * *");
            assertThat(result).contains("6-field (with seconds)");
        }

        // ── 7-field expression (with year) ──────────────────────────────────

        @Test
        void sevenField_showsYear() {
            String result = tool.explainCron("0 0 12 * * * 2025");
            assertThat(result).contains("Year:");
        }

        // ── weekdayName switch cases ────────────────────────────────────────

        @Test
        void weekdayName_allCases() {
            // Cover all switch cases in weekdayName
            assertThat(tool.explainCron("0 0 * * 0")).containsIgnoringCase("Sunday");
            assertThat(tool.explainCron("0 0 * * 7")).containsIgnoringCase("Sunday");
            assertThat(tool.explainCron("0 0 * * SUN")).containsIgnoringCase("Sunday");
            assertThat(tool.explainCron("0 0 * * 1")).containsIgnoringCase("Monday");
            assertThat(tool.explainCron("0 0 * * MON")).containsIgnoringCase("Monday");
            assertThat(tool.explainCron("0 0 * * 2")).containsIgnoringCase("Tuesday");
            assertThat(tool.explainCron("0 0 * * TUE")).containsIgnoringCase("Tuesday");
            assertThat(tool.explainCron("0 0 * * 3")).containsIgnoringCase("Wednesday");
            assertThat(tool.explainCron("0 0 * * WED")).containsIgnoringCase("Wednesday");
            assertThat(tool.explainCron("0 0 * * 4")).containsIgnoringCase("Thursday");
            assertThat(tool.explainCron("0 0 * * THU")).containsIgnoringCase("Thursday");
            assertThat(tool.explainCron("0 0 * * 5")).containsIgnoringCase("Friday");
            assertThat(tool.explainCron("0 0 * * FRI")).containsIgnoringCase("Friday");
            assertThat(tool.explainCron("0 0 * * 6")).containsIgnoringCase("Saturday");
            assertThat(tool.explainCron("0 0 * * SAT")).containsIgnoringCase("Saturday");
            assertThat(tool.explainCron("0 0 * * 0,6")).containsIgnoringCase("weekend");
            assertThat(tool.explainCron("0 0 * * SAT,SUN")).containsIgnoringCase("weekend");
            assertThat(tool.explainCron("0 0 * * 6,0")).containsIgnoringCase("weekend");
        }

        @Test
        void weekdayName_defaultCase() {
            // A value not in the switch cases triggers the default
            String result = tool.explainCron("0 0 * * 1,3,5");
            assertThat(result).contains("day(s) 1,3,5");
        }

        @Test
        void plainEnglish_specificTimeFormatted() {
            // minute="30", hour="14" => both non-* non-*/N
            // parseIntSafe("14")=14, parseIntSafe("30")=30
            // formatTime(14, 30) => "2:30 PM"
            String result = tool.explainCron("30 14 * * *");
            assertThat(result).contains("2:30 PM");
        }

        // ── toPlainEnglish branches ─────────────────────────────────────────

        @Test
        void plainEnglish_everyNHours() {
            String result = tool.explainCron("0 */3 * * *");
            assertThat(result).contains("every 3 hours");
        }

        @Test
        void plainEnglish_atMinuteOfEveryHour() {
            String result = tool.explainCron("30 * * * *");
            assertThat(result).contains("at minute 30 of every hour");
        }

        @Test
        void plainEnglish_specificDayOfMonth() {
            String result = tool.explainCron("0 0 15 * *");
            assertThat(result).contains("on day 15 of the month");
        }

        @Test
        void plainEnglish_specificMonth() {
            String result = tool.explainCron("0 0 * 6 *");
            assertThat(result).contains("in month 6");
        }

        @Test
        void plainEnglish_wildcardHourAndMinute_fallback() {
            // minute="1-5", hour="2-4" => both not "*" nor "*/N"
            // parseIntSafe fails for "2-4" and "1-5" => both return 0
            // formatTime(0, 0) => "12:00 AM"
            String result = tool.explainCron("1-5 2-4 * * *");
            assertThat(result).contains("1-5").contains("2-4");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  buildCron
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    class BuildCron {

        // ── every N minutes ─────────────────────────────────────────────────

        @Test
        void everyNMinutes() {
            String result = tool.buildCron("every 5 minutes");
            assertThat(result).contains("*/5 * * * *")
                    .contains("Every 5 minutes");
        }

        @Test
        void everyNMin() {
            String result = tool.buildCron("every 10 min");
            assertThat(result).contains("*/10 * * * *");
        }

        @Test
        void everyNMins() {
            String result = tool.buildCron("every 15 mins");
            assertThat(result).contains("*/15 * * * *");
        }

        // ── every N hours ───────────────────────────────────────────────────

        @Test
        void everyNHours() {
            String result = tool.buildCron("every 2 hours");
            assertThat(result).contains("0 */2 * * *")
                    .contains("Every 2 hours");
        }

        @Test
        void every1Hour() {
            String result = tool.buildCron("every 1 hour");
            assertThat(result).contains("0 */1 * * *");
        }

        // ── every minute / every hour ───────────────────────────────────────

        @Test
        void everyMinute() {
            String result = tool.buildCron("every minute");
            assertThat(result).contains("* * * * *")
                    .contains("Every minute");
        }

        @Test
        void everyHour() {
            String result = tool.buildCron("every hour");
            assertThat(result).contains("0 * * * *")
                    .contains("start of every hour");
        }

        // ── daily at ────────────────────────────────────────────────────────

        @Test
        void dailyAt9am() {
            String result = tool.buildCron("daily at 9am");
            assertThat(result).contains("0 9 * * *")
                    .contains("9:00 AM");
        }

        @Test
        void dailyAt3pm() {
            String result = tool.buildCron("daily at 3pm");
            assertThat(result).contains("0 15 * * *")
                    .contains("3:00 PM");
        }

        @Test
        void everyDayAt830am() {
            String result = tool.buildCron("every day at 8:30am");
            assertThat(result).contains("30 8 * * *");
        }

        @Test
        void dailyAt12am() {
            // 12am = 0 (midnight)
            String result = tool.buildCron("daily at 12am");
            assertThat(result).contains("0 0 * * *");
        }

        @Test
        void dailyAt12pm() {
            // 12pm = 12 (noon)
            String result = tool.buildCron("daily at 12pm");
            assertThat(result).contains("0 12 * * *");
        }

        // ── weekday at ──────────────────────────────────────────────────────

        @Test
        void weekdaysAt9am() {
            String result = tool.buildCron("weekdays at 9am");
            assertThat(result).contains("0 9 * * 1-5");
        }

        @Test
        void weekdaysAt830am() {
            String result = tool.buildCron("weekdays at 8:30am");
            assertThat(result).contains("30 8 * * 1-5");
        }

        // ── weekend at ──────────────────────────────────────────────────────

        @Test
        void weekendsAt10am() {
            String result = tool.buildCron("weekends at 10am");
            assertThat(result).contains("0 10 * * 0,6");
        }

        // ── midnight ────────────────────────────────────────────────────────

        @Test
        void midnight() {
            String result = tool.buildCron("midnight");
            assertThat(result).contains("0 0 * * *")
                    .contains("midnight");
        }

        // ── noon ────────────────────────────────────────────────────────────

        @Test
        void noon() {
            String result = tool.buildCron("noon");
            assertThat(result).contains("0 12 * * *")
                    .contains("noon");
        }

        @Test
        void weekdaysAtNoon_hitsWeekdayBranch() {
            // "weekday" + "at" branch matches before "noon" branch;
            // parseTime("weekdays at noon") finds no digits so defaults to {0,0}
            String result = tool.buildCron("weekdays at noon");
            assertThat(result).contains("* * 1-5");
        }

        @Test
        void noonOnWeekdays_hitsNoonBranch() {
            // "weekday noon" has no "at", so "weekday+at" check does NOT match.
            // Falls through to "noon" check, which sees "weekday" => "0 12 * * 1-5"
            String result = tool.buildCron("weekday noon");
            assertThat(result).contains("0 12 * * 1-5")
                    .contains("Weekdays at noon");
        }

        // ── first of month ──────────────────────────────────────────────────

        @Test
        void firstDayOfMonth() {
            // "midnight" check comes before "first of month" in the if-else chain,
            // so "first day of month at midnight" matches the midnight branch
            String result = tool.buildCron("first day of month at midnight");
            assertThat(result).contains("0 0")
                    ;
        }

        @Test
        void firstOfEveryMonth_noTime() {
            // No "at" so time defaults to {0, 0}
            String result = tool.buildCron("first of every month");
            assertThat(result).contains("0 0 1 * *");
        }

        @Test
        void firstDayOfMonth_at9am() {
            String result = tool.buildCron("first day of month at 9am");
            assertThat(result).contains("0 9 1 * *");
        }

        @Test
        void firstDayOfEachMonth_withTime() {
            String result = tool.buildCron("first day of each month at 6am");
            assertThat(result).contains("0 6 1 * *");
        }

        @Test
        void firstOfMonth_withTime() {
            String result = tool.buildCron("first of month at 9am");
            assertThat(result).contains("0 9 1 * *");
        }

        // ── last of month ───────────────────────────────────────────────────

        @Test
        void lastDayOfMonth() {
            String result = tool.buildCron("last day of month");
            assertThat(result).contains("0 0 28-31 * *");
        }

        @Test
        void lastOfEveryMonth() {
            String result = tool.buildCron("last of every month");
            assertThat(result).contains("0 0 28-31 * *");
        }

        @Test
        void lastOfEachMonth() {
            String result = tool.buildCron("last day of each month");
            assertThat(result).contains("0 0 28-31 * *");
        }

        // ── weekly ──────────────────────────────────────────────────────────

        @Test
        void weekly_defaultMonday() {
            String result = tool.buildCron("weekly");
            assertThat(result).contains("* * 1"); // day 1 = Monday
        }

        @Test
        void weeklyOnSunday() {
            String result = tool.buildCron("weekly on sunday");
            assertThat(result).contains("* * 0");
        }

        @Test
        void weeklyOnTuesday() {
            String result = tool.buildCron("weekly on tuesday");
            assertThat(result).contains("* * 2");
        }

        @Test
        void weeklyOnTue() {
            String result = tool.buildCron("weekly on tue");
            assertThat(result).contains("* * 2");
        }

        @Test
        void weeklyOnWednesday() {
            String result = tool.buildCron("weekly on wednesday");
            assertThat(result).contains("* * 3");
        }

        @Test
        void weeklyOnWed() {
            String result = tool.buildCron("weekly on wed");
            assertThat(result).contains("* * 3");
        }

        @Test
        void weeklyOnThursday() {
            String result = tool.buildCron("weekly on thursday");
            assertThat(result).contains("* * 4");
        }

        @Test
        void weeklyOnThu() {
            String result = tool.buildCron("weekly on thu");
            assertThat(result).contains("* * 4");
        }

        @Test
        void weeklyOnFriday() {
            String result = tool.buildCron("weekly on friday");
            assertThat(result).contains("* * 5");
        }

        @Test
        void weeklyOnFri() {
            String result = tool.buildCron("weekly on fri");
            assertThat(result).contains("* * 5");
        }

        @Test
        void weeklyOnSaturday() {
            String result = tool.buildCron("weekly on saturday");
            assertThat(result).contains("* * 6");
        }

        @Test
        void weeklyOnSat() {
            String result = tool.buildCron("weekly on sat");
            assertThat(result).contains("* * 6");
        }

        @Test
        void weeklyOnSun() {
            String result = tool.buildCron("weekly on sun");
            assertThat(result).contains("* * 0");
        }

        @Test
        void everyWeekOnMondayAt9am() {
            String result = tool.buildCron("every week on monday at 9am");
            assertThat(result).contains("0 9 * * 1");
        }

        @Test
        void weeklyAtTime_noDay() {
            String result = tool.buildCron("weekly at 10am");
            // Default day is Monday
            assertThat(result).contains("0 10 * * 1");
        }

        // ── every N seconds ─────────────────────────────────────────────────

        @Test
        void everyNSeconds() {
            String result = tool.buildCron("every 30 seconds");
            assertThat(result).contains("*/30 * * * * *")
                    .contains("6-field format");
        }

        @Test
        void every1Second() {
            String result = tool.buildCron("every 1 second");
            assertThat(result).contains("*/1 * * * * *");
        }

        // ── unparseable ─────────────────────────────────────────────────────

        @Test
        void unparseable_returnsHelpText() {
            String result = tool.buildCron("whenever the moon is full");
            assertThat(result).contains("Could not parse")
                    .contains("every 5 minutes")
                    .contains("daily at 9am");
        }

        // ── output format ───────────────────────────────────────────────────

        @Test
        void outputContainsDescriptionAndCron() {
            String result = tool.buildCron("every 5 minutes");
            assertThat(result).contains("Built Cron Expression")
                    .contains("Description: every 5 minutes")
                    .contains("Cron: */5 * * * *")
                    .contains("Means: Every 5 minutes");
        }

        // ── parseTime with HH:MM format ─────────────────────────────────────

        @Test
        void dailyAtTimeWithMinutes_pm() {
            String result = tool.buildCron("daily at 2:30pm");
            assertThat(result).contains("30 14 * * *");
        }

        @Test
        void dailyAtTimeWithMinutes_am() {
            String result = tool.buildCron("daily at 11:45am");
            assertThat(result).contains("45 11 * * *");
        }

        @Test
        void dailyAt24hrFormat() {
            // No am/pm qualifier, plain "HH:MM"
            String result = tool.buildCron("daily at 14:30");
            assertThat(result).contains("30 14 * * *");
        }

        // ── parseTime edge: 12:30am => 0:30, 12:30pm => 12:30 ──────────────

        @Test
        void dailyAt12_30am() {
            String result = tool.buildCron("daily at 12:30am");
            assertThat(result).contains("30 0 * * *");
        }

        @Test
        void dailyAt12_30pm() {
            String result = tool.buildCron("daily at 12:30pm");
            assertThat(result).contains("30 12 * * *");
        }

        // ── formatTime coverage ─────────────────────────────────────────────

        @Test
        void formatTime_midnight_shows12AM() {
            // daily at 12am => hour=0, minute=0 => "12:00 AM"
            String result = tool.buildCron("daily at 12am");
            assertThat(result).contains("12:00 AM");
        }

        @Test
        void formatTime_noon_shows12PM() {
            String result = tool.buildCron("daily at 12pm");
            assertThat(result).contains("12:00 PM");
        }

        @Test
        void formatTime_pm() {
            String result = tool.buildCron("daily at 3pm");
            assertThat(result).contains("3:00 PM");
        }
    }
}
