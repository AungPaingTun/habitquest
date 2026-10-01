package com.habitquest.points;

public enum TransactionType {
    /** Points for a check-in. */
    EARN,
    /** Extra points for a streak. */
    STREAK_BONUS,
    /** Same-day undo of a check-in (negative). Also removes the XP, as if the check-in never happened. */
    UNDO,
    /** Points spent on a prize (negative). Lowers the balance but never the XP. */
    REDEEM
}
