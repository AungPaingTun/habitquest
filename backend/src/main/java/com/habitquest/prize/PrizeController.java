package com.habitquest.prize;

import com.habitquest.auth.CurrentUser;
import com.habitquest.prize.dto.PrizeCreateRequest;
import com.habitquest.prize.dto.PrizeResponse;
import com.habitquest.prize.dto.PrizeUpdateRequest;
import com.habitquest.prize.dto.RedeemResult;
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
@RequestMapping("/api/prizes")
public class PrizeController {

    private final PrizeService prizeService;

    public PrizeController(PrizeService prizeService) {
        this.prizeService = prizeService;
    }

    /** Cheapest first. ?archived=true lists archived prizes instead. */
    @GetMapping
    public List<PrizeResponse> list(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(defaultValue = "false") boolean archived) {
        return prizeService.list(CurrentUser.id(jwt), archived);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrizeResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PrizeCreateRequest request) {
        return prizeService.create(CurrentUser.id(jwt), request);
    }

    /** Renames a prize or changes its icon. A "cost" field in the body is ignored: the cost is locked. */
    @PutMapping("/{id}")
    public PrizeResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                @Valid @RequestBody PrizeUpdateRequest request) {
        return prizeService.update(CurrentUser.id(jwt), id, request);
    }

    @PostMapping("/{id}/archive")
    public PrizeResponse archive(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return prizeService.setArchived(CurrentUser.id(jwt), id, true);
    }

    @PostMapping("/{id}/restore")
    public PrizeResponse restore(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return prizeService.setArchived(CurrentUser.id(jwt), id, false);
    }

    @PostMapping("/{id}/redeem")
    public RedeemResult redeem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return prizeService.redeem(CurrentUser.id(jwt), id);
    }
}
