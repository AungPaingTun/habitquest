package com.habitquest.checkin;

import com.habitquest.checkin.StreakCalculator.Streak;
import com.habitquest.habit.Frequency;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreakCalculatorTest {

    // 2026-09-28 is a Monday, so this Friday is in the week Mon 09-28 .. Sun 10-04.
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);

    private static LocalDate d(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    // ---------- daily ----------

    @Test
    void calculate_dailyConsecutiveDaysEndingToday_currentIsRunLength() {
        List<LocalDate> logs = List.of(d(2026, 9, 30), d(2026, 10, 1), d(2026, 10, 2));

        Streak streak = StreakCalculator.calculate(Frequency.DAILY, 1, logs, TODAY);

        assertThat(streak.current()).isEqualTo(3);
        assertThat(streak.best()).isEqualTo(3);
    }

    @Test
    void calculate_dailyTodayNotDoneButYesterdayWas_streakStillAlive() {
        List<LocalDate> logs = List.of(d(2026, 9, 30), d(2026, 10, 1));

        Streak streak = StreakCalculator.calculate(Frequency.DAILY, 1, logs, TODAY);

        assertThat(streak.current()).isEqualTo(2);
    }

    @Test
    void calculate_dailyYesterdayAndTodayBothMissed_currentResetsToZero() {
        List<LocalDate> logs = List.of(d(2026, 9, 29), d(2026, 9, 30));

        Streak streak = StreakCalculator.calculate(Frequency.DAILY, 1, logs, TODAY);

        assertThat(streak.current()).isZero();
        assertThat(streak.best()).isEqualTo(2);
    }

    @Test
    void calculate_dailyGapInHistory_currentOnlyCountsAfterGapAndBestKeepsLongestPastRun() {
        List<LocalDate> logs = List.of(d(2026, 9, 20), d(2026, 9, 21), d(2026, 9, 22), d(2026, 9, 23),
                d(2026, 10, 1), d(2026, 10, 2));

        Streak streak = StreakCalculator.calculate(Frequency.DAILY, 1, logs, TODAY);

        assertThat(streak.current()).isEqualTo(2);
        assertThat(streak.best()).isEqualTo(4);
    }

    @Test
    void calculate_unsortedInput_givesSameResultAsSorted() {
        List<LocalDate> logs = List.of(d(2026, 10, 2), d(2026, 9, 30), d(2026, 10, 1));

        Streak streak = StreakCalculator.calculate(Frequency.DAILY, 1, logs, TODAY);

        assertThat(streak.current()).isEqualTo(3);
    }

    @Test
    void calculate_emptyLogs_isZeroZero() {
        Streak streak = StreakCalculator.calculate(Frequency.DAILY, 1, List.of(), TODAY);

        assertThat(streak.current()).isZero();
        assertThat(streak.best()).isZero();
    }

    // ---------- weekly ----------

    @Test
    void calculate_weeklyOnlyOneOfTwoCheckIns_weekDoesNotCount() {
        List<LocalDate> logs = List.of(d(2026, 9, 21), d(2026, 9, 22), d(2026, 9, 29));

        Streak streak = StreakCalculator.calculate(Frequency.WEEKLY, 2, logs, TODAY);

        // Previous week (09-21) is met, the current week is not met yet -> streak of 1 stays alive.
        assertThat(streak.current()).isEqualTo(1);
        assertThat(streak.best()).isEqualTo(1);
    }

    @Test
    void calculate_weeklyOnlyUnmetWeek_isZero() {
        Streak streak = StreakCalculator.calculate(Frequency.WEEKLY, 2, List.of(d(2026, 9, 29)), TODAY);

        assertThat(streak.current()).isZero();
        assertThat(streak.best()).isZero();
    }

    @Test
    void calculate_weeklyThreeMetWeeksInARow_currentIsThree() {
        List<LocalDate> logs = List.of(
                d(2026, 9, 14), d(2026, 9, 16),
                d(2026, 9, 21), d(2026, 9, 25),
                d(2026, 9, 28), d(2026, 10, 2));

        Streak streak = StreakCalculator.calculate(Frequency.WEEKLY, 2, logs, TODAY);

        assertThat(streak.current()).isEqualTo(3);
        assertThat(streak.best()).isEqualTo(3);
    }

    @Test
    void calculate_weeklyCurrentWeekNotYetMet_doesNotBreakStreak() {
        List<LocalDate> logs = List.of(
                d(2026, 9, 14), d(2026, 9, 16),
                d(2026, 9, 21), d(2026, 9, 25),
                d(2026, 9, 29));

        Streak streak = StreakCalculator.calculate(Frequency.WEEKLY, 2, logs, TODAY);

        assertThat(streak.current()).isEqualTo(2);
    }

    @Test
    void calculate_weeklySundayAndMondayBelongToDifferentWeeks() {
        // Sun 09-27 is in the week of 09-21; Mon 09-28 starts a new week, so neither week reaches 2.
        List<LocalDate> logs = List.of(d(2026, 9, 27), d(2026, 9, 28));

        Streak streak = StreakCalculator.calculate(Frequency.WEEKLY, 2, logs, TODAY);

        assertThat(streak.current()).isZero();
        assertThat(streak.best()).isZero();
    }

    @Test
    void calculate_weeklyMondayAndSundayOfSameWeek_countAsOneMetWeek() {
        List<LocalDate> logs = List.of(d(2026, 9, 28), d(2026, 10, 4));

        Streak streak = StreakCalculator.calculate(Frequency.WEEKLY, 2, logs, d(2026, 10, 4));

        assertThat(streak.current()).isEqualTo(1);
    }

    @Test
    void calculate_weeklyMissedWeek_resetsCurrentAndBestKeepsOlderRun() {
        List<LocalDate> logs = List.of(
                d(2026, 8, 31), d(2026, 9, 1),   // week of 08-31
                d(2026, 9, 7), d(2026, 9, 8),    // week of 09-07
                // week of 09-14 missed
                d(2026, 9, 21), d(2026, 9, 22)); // week of 09-21

        Streak streak = StreakCalculator.calculate(Frequency.WEEKLY, 2, logs, TODAY);

        assertThat(streak.current()).isEqualTo(1);
        assertThat(streak.best()).isEqualTo(2);
    }

    // ---------- monthly ----------

    @Test
    void calculate_monthlyJan31AndFeb1_areDifferentPeriods() {
        List<LocalDate> logs = List.of(d(2026, 1, 31), d(2026, 2, 1));

        Streak streak = StreakCalculator.calculate(Frequency.MONTHLY, 2, logs, d(2026, 2, 15));

        assertThat(streak.current()).isZero();
        assertThat(streak.best()).isZero();
    }

    @Test
    void calculate_monthlyTargetMetInPreviousMonth_streakAliveWhileCurrentMonthUnmet() {
        List<LocalDate> logs = List.of(d(2026, 1, 30), d(2026, 1, 31), d(2026, 2, 1));

        Streak streak = StreakCalculator.calculate(Frequency.MONTHLY, 2, logs, d(2026, 2, 15));

        assertThat(streak.current()).isEqualTo(1);
    }

    @Test
    void calculate_monthlyConsecutiveMonthsAcrossYearBoundary_countsAsRun() {
        List<LocalDate> logs = List.of(
                d(2025, 11, 3), d(2025, 11, 20),
                d(2025, 12, 5), d(2025, 12, 6),
                d(2026, 1, 10), d(2026, 1, 11));

        Streak streak = StreakCalculator.calculate(Frequency.MONTHLY, 2, logs, d(2026, 1, 12));

        assertThat(streak.current()).isEqualTo(3);
        assertThat(streak.best()).isEqualTo(3);
    }

    @Test
    void calculate_monthlySkippedMonth_breaksStreak() {
        List<LocalDate> logs = List.of(d(2026, 7, 1), d(2026, 7, 2), d(2026, 9, 1), d(2026, 9, 2));

        Streak streak = StreakCalculator.calculate(Frequency.MONTHLY, 2, logs, TODAY);

        assertThat(streak.current()).isEqualTo(1);
        assertThat(streak.best()).isEqualTo(1);
    }

    // ---------- countInCurrentPeriod ----------

    @Test
    void countInCurrentPeriod_weekly_startsOnMonday() {
        // Sun 09-27 is the previous week; Mon 09-28 and Wed 09-30 are this week.
        List<LocalDate> logs = List.of(d(2026, 9, 27), d(2026, 9, 28), d(2026, 9, 30));

        int count = StreakCalculator.countInCurrentPeriod(Frequency.WEEKLY, logs, TODAY);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countInCurrentPeriod_monthly_onlyCountsCurrentCalendarMonth() {
        List<LocalDate> logs = List.of(d(2026, 9, 30), d(2026, 10, 1), d(2026, 10, 2));

        int count = StreakCalculator.countInCurrentPeriod(Frequency.MONTHLY, logs, TODAY);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countInCurrentPeriod_daily_onlyCountsToday() {
        List<LocalDate> logs = List.of(d(2026, 10, 1), d(2026, 10, 2));

        int count = StreakCalculator.countInCurrentPeriod(Frequency.DAILY, logs, TODAY);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void countInCurrentPeriod_ignoresDatesAfterToday() {
        List<LocalDate> logs = List.of(d(2026, 10, 2), d(2026, 10, 3));

        int count = StreakCalculator.countInCurrentPeriod(Frequency.WEEKLY, logs, TODAY);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void countInCurrentPeriod_emptyLogs_isZero() {
        assertThat(StreakCalculator.countInCurrentPeriod(Frequency.WEEKLY, List.of(), TODAY)).isZero();
    }

    // ---------- periodStart ----------

    @Test
    void periodStart_weeklyOnSunday_returnsPreviousMonday() {
        assertThat(StreakCalculator.periodStart(Frequency.WEEKLY, d(2026, 10, 4))).isEqualTo(d(2026, 9, 28));
    }

    @Test
    void periodStart_weeklyOnMonday_returnsSameDay() {
        assertThat(StreakCalculator.periodStart(Frequency.WEEKLY, d(2026, 9, 28))).isEqualTo(d(2026, 9, 28));
    }

    @Test
    void periodStart_monthly_returnsFirstOfMonth() {
        assertThat(StreakCalculator.periodStart(Frequency.MONTHLY, d(2026, 2, 28))).isEqualTo(d(2026, 2, 1));
    }
}
