package com.habitquest.analytics;

import com.habitquest.analytics.dto.AnalyticsResponse;
import com.habitquest.analytics.dto.HeatmapResponse;
import com.habitquest.auth.CurrentUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /** e.g. /api/analytics?period=MONTH&date=2026-09-15 for September 2026. Without a date: the current period. */
    @GetMapping
    public AnalyticsResponse analytics(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(defaultValue = "WEEK") Period period,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return analyticsService.analytics(CurrentUser.id(jwt), period, date);
    }

    /** Check-ins per day for a year (default: this year). */
    @GetMapping("/heatmap")
    public HeatmapResponse heatmap(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Integer year) {
        return analyticsService.heatmap(CurrentUser.id(jwt), year);
    }
}
