package com.habitquest.analytics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** The time window an analytics view covers. Weeks run Monday–Sunday (PRD §5.2). */
public enum Period {
    WEEK, MONTH, YEAR;

    public LocalDate startOf(LocalDate date) {
        return switch (this) {
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    public LocalDate endOf(LocalDate date) {
        return switch (this) {
            case WEEK -> date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            case MONTH -> date.with(TemporalAdjusters.lastDayOfMonth());
            case YEAR -> date.with(TemporalAdjusters.lastDayOfYear());
        };
    }
}
