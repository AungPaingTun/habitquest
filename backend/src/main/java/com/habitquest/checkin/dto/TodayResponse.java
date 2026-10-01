package com.habitquest.checkin.dto;

import com.habitquest.points.dto.PointsSummary;

import java.time.LocalDate;
import java.util.List;

/** Everything the Today page needs in one request. {@code date} is today in the user's time zone. */
public record TodayResponse(LocalDate date, PointsSummary points, List<TodayHabit> habits) {
}
