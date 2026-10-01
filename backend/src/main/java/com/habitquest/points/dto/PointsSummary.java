package com.habitquest.points.dto;

/**
 * The numbers shown in the header and on the Today page.
 *
 * @param balance        points available to spend
 * @param lifetimeXp     all points ever earned (spending doesn't lower it)
 * @param level          current level
 * @param levelStartXp   XP where the current level began
 * @param nextLevelXp    XP needed for the next level
 */
public record PointsSummary(long balance, long lifetimeXp, int level, long levelStartXp, long nextLevelXp) {
}
