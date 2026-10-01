package com.habitquest.analytics;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodTest {

    @Test
    void startOf_weekOnFriday_returnsMonday() {
        assertThat(Period.WEEK.startOf(LocalDate.of(2026, 10, 2))).isEqualTo(LocalDate.of(2026, 9, 28));
    }

    @Test
    void startOf_weekOnMonday_returnsSameDay() {
        assertThat(Period.WEEK.startOf(LocalDate.of(2026, 9, 28))).isEqualTo(LocalDate.of(2026, 9, 28));
    }

    @Test
    void startOf_weekOnSunday_returnsMondaySixDaysEarlier() {
        assertThat(Period.WEEK.startOf(LocalDate.of(2026, 10, 4))).isEqualTo(LocalDate.of(2026, 9, 28));
    }

    @Test
    void endOf_weekOnFriday_returnsSunday() {
        assertThat(Period.WEEK.endOf(LocalDate.of(2026, 10, 2))).isEqualTo(LocalDate.of(2026, 10, 4));
    }

    @Test
    void endOf_weekOnSunday_returnsSameDay() {
        assertThat(Period.WEEK.endOf(LocalDate.of(2026, 10, 4))).isEqualTo(LocalDate.of(2026, 10, 4));
    }

    @Test
    void endOf_weekOnMonday_returnsFollowingSunday() {
        assertThat(Period.WEEK.endOf(LocalDate.of(2026, 9, 28))).isEqualTo(LocalDate.of(2026, 10, 4));
    }

    @Test
    void startOf_month_returnsFirstOfMonth() {
        assertThat(Period.MONTH.startOf(LocalDate.of(2026, 10, 17))).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void endOf_monthInLeapFebruary_returns29th() {
        assertThat(Period.MONTH.endOf(LocalDate.of(2028, 2, 10))).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void endOf_monthInNonLeapFebruary_returns28th() {
        assertThat(Period.MONTH.endOf(LocalDate.of(2026, 2, 10))).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void startOf_year_returnsJanuaryFirst() {
        assertThat(Period.YEAR.startOf(LocalDate.of(2026, 10, 2))).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void endOf_year_returnsDecember31st() {
        assertThat(Period.YEAR.endOf(LocalDate.of(2026, 1, 1))).isEqualTo(LocalDate.of(2026, 12, 31));
    }
}
