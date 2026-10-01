package com.habitquest.points;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LevelsTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "1, 0",
            "49, 0",
            "50, 1",
            "199, 1",
            "200, 2",
            "449, 2",
            "450, 3",
            "1249, 4",
            "1250, 5",
            "4999, 9",
            "5000, 10"
    })
    void levelFor_xp_returnsLevel(long xp, int expectedLevel) {
        assertThat(Levels.levelFor(xp)).isEqualTo(expectedLevel);
    }

    @ParameterizedTest
    @CsvSource({"-1, 0", "-1000, 0"})
    void levelFor_negativeXp_isLevelZero(long xp, int expectedLevel) {
        assertThat(Levels.levelFor(xp)).isEqualTo(expectedLevel);
    }

    @Test
    void levelFor_veryLargeXp_doesNotOverflowAndStaysConsistent() {
        int level = Levels.levelFor(50L * 100_000 * 100_000);

        assertThat(level).isEqualTo(100_000);
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "1, 50", "2, 200", "3, 450", "5, 1250", "10, 5000"})
    void xpForLevel_level_is50TimesLevelSquared(int level, long expectedXp) {
        assertThat(Levels.xpForLevel(level)).isEqualTo(expectedXp);
    }
}
