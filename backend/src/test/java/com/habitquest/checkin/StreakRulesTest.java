package com.habitquest.checkin;

import com.habitquest.habit.Frequency;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StreakRulesTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "6, 0",
            "7, 2",
            "29, 2",
            "30, 4",
            "100, 4"
    })
    void bonusFor_daily_usesSevenAndThirtyDayTiers(int streak, int expectedBonus) {
        assertThat(StreakRules.bonusFor(Frequency.DAILY, streak)).isEqualTo(expectedBonus);
    }

    @ParameterizedTest
    @CsvSource({
            "WEEKLY, 0, 0",
            "WEEKLY, 2, 0",
            "WEEKLY, 3, 2",
            "WEEKLY, 7, 2",
            "WEEKLY, 8, 4",
            "WEEKLY, 50, 4",
            "MONTHLY, 2, 0",
            "MONTHLY, 3, 2",
            "MONTHLY, 7, 2",
            "MONTHLY, 8, 4"
    })
    void bonusFor_weeklyAndMonthly_useThreeAndEightPeriodTiers(Frequency frequency, int streak, int expectedBonus) {
        assertThat(StreakRules.bonusFor(frequency, streak)).isEqualTo(expectedBonus);
    }
}
