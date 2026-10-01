package com.habitquest.points;

import com.habitquest.auth.CurrentUser;
import com.habitquest.points.dto.PointsSummary;
import com.habitquest.points.dto.TransactionResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/points")
public class PointsController {

    private final PointsService pointsService;

    public PointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    /** Balance, lifetime XP and level. */
    @GetMapping("/summary")
    public PointsSummary summary(@AuthenticationPrincipal Jwt jwt) {
        return pointsService.summary(CurrentUser.id(jwt));
    }

    /** Most recent ledger rows first (max 100). */
    @GetMapping("/history")
    public List<TransactionResponse> history(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(defaultValue = "50") int limit) {
        return pointsService.history(CurrentUser.id(jwt), limit);
    }
}
