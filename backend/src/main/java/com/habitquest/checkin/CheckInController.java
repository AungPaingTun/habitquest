package com.habitquest.checkin;

import com.habitquest.auth.CurrentUser;
import com.habitquest.checkin.dto.CheckInResult;
import com.habitquest.checkin.dto.TodayResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @GetMapping("/today")
    public TodayResponse today(@AuthenticationPrincipal Jwt jwt) {
        return checkInService.today(CurrentUser.id(jwt));
    }

    /** Check in today. */
    @PostMapping("/habits/{id}/check-in")
    public CheckInResult checkIn(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return checkInService.checkIn(CurrentUser.id(jwt), id);
    }

    /** Undo today's check-in. */
    @DeleteMapping("/habits/{id}/check-in")
    public CheckInResult undo(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return checkInService.undo(CurrentUser.id(jwt), id);
    }
}
