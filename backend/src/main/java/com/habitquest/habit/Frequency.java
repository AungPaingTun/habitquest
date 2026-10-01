package com.habitquest.habit;

/** How often a habit should be done. Each value knows the allowed range for its target count. */
public enum Frequency {
    DAILY(1),
    WEEKLY(7),
    MONTHLY(31);

    private final int maxTarget;

    Frequency(int maxTarget) {
        this.maxTarget = maxTarget;
    }

    public int maxTarget() {
        return maxTarget;
    }
}
