package com.habitquest.habit;

import com.habitquest.auth.CurrentUser;
import com.habitquest.habit.dto.HabitRequest;
import com.habitquest.habit.dto.HabitResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    /** Active habits by default; ?archived=true lists archived ones instead. */
    @GetMapping
    public List<HabitResponse> list(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(defaultValue = "false") boolean archived) {
        return habitService.list(CurrentUser.id(jwt), archived);
    }

    @GetMapping("/{id}")
    public HabitResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return habitService.get(CurrentUser.id(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HabitResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody HabitRequest request) {
        return habitService.create(CurrentUser.id(jwt), request);
    }

    @PutMapping("/{id}")
    public HabitResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                @Valid @RequestBody HabitRequest request) {
        return habitService.update(CurrentUser.id(jwt), id, request);
    }

    @PostMapping("/{id}/archive")
    public HabitResponse archive(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return habitService.setArchived(CurrentUser.id(jwt), id, true);
    }

    @PostMapping("/{id}/restore")
    public HabitResponse restore(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return habitService.setArchived(CurrentUser.id(jwt), id, false);
    }
}
