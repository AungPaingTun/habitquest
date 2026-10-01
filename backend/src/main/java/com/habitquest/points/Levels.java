package com.habitquest.points;

/**
 * Level n needs 50 × n² lifetime XP (PRD §5.3): L1 = 50, L2 = 200, L3 = 450, L5 = 1,250, L10 = 5,000.
 * Below 50 XP the user is level 0.
 */
public final class Levels {

    static final int XP_FACTOR = 50;

    private Levels() {
    }

    public static int levelFor(long xp) {
        if (xp <= 0) return 0;
        int level = (int) Math.sqrt((double) xp / XP_FACTOR);
        // Correct for floating-point rounding right at a level boundary.
        while (xpForLevel(level + 1) <= xp) level++;
        while (level > 0 && xpForLevel(level) > xp) level--;
        return level;
    }

    public static long xpForLevel(int level) {
        return (long) XP_FACTOR * level * level;
    }
}
