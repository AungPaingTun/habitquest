package com.habitquest.analytics.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Check-ins per day for one year, GitHub-style.
 *
 * @param days     only days with at least one check-in, in date order
 * @param maxCount the busiest day's count, for scaling the colors
 */
public record HeatmapResponse(int year, LocalDate today, int maxCount, List<Day> days) {

    public record Day(LocalDate date, int count) {
    }
}
