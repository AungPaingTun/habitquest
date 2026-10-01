package com.habitquest.checkin.dto;

import com.habitquest.points.dto.PointsSummary;

/**
 * Result of a check-in or undo.
 *
 * @param pointsChange points added (check-in) or removed (undo, negative), including any streak bonus
 * @param bonus        streak bonus part of a check-in (0 for undo)
 */
public record CheckInResult(Long habitId, int pointsChange, int bonus, int currentStreak, PointsSummary points) {
}
