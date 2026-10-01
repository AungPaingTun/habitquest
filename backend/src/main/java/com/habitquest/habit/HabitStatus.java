package com.habitquest.habit;

public enum HabitStatus {
    /** Start date is in the future. */
    UPCOMING,
    /** Can be checked in today. */
    ACTIVE,
    /** End date has passed; kept with its final stats. */
    COMPLETED
}
