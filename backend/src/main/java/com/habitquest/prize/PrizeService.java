package com.habitquest.prize;

import com.habitquest.common.ApiException;
import com.habitquest.points.PointTransaction;
import com.habitquest.points.PointsService;
import com.habitquest.prize.RedemptionRepository.PrizeStats;
import com.habitquest.prize.dto.PrizeCreateRequest;
import com.habitquest.prize.dto.PrizeResponse;
import com.habitquest.prize.dto.PrizeUpdateRequest;
import com.habitquest.prize.dto.RedeemResult;
import com.habitquest.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Prizes the user sets for themselves, and spending points on them (PRD §5.5). */
@Service
public class PrizeService {

    private final PrizeRepository prizes;
    private final RedemptionRepository redemptions;
    private final UserRepository users;
    private final PointsService points;

    public PrizeService(PrizeRepository prizes, RedemptionRepository redemptions, UserRepository users,
                        PointsService points) {
        this.prizes = prizes;
        this.redemptions = redemptions;
        this.users = users;
        this.points = points;
    }

    @Transactional(readOnly = true)
    public List<PrizeResponse> list(Long userId, boolean archived) {
        Map<Long, PrizeStats> stats = statsByPrize(userId);
        return prizes.findByUserIdAndArchivedOrderByCostAscIdAsc(userId, archived).stream()
                .map(prize -> toResponse(prize, stats))
                .toList();
    }

    @Transactional
    public PrizeResponse create(Long userId, PrizeCreateRequest request) {
        Prize prize = prizes.save(new Prize(userId, request.name(), request.icon(), request.cost()));
        return PrizeResponse.from(prize, 0, null);
    }

    @Transactional
    public PrizeResponse update(Long userId, Long prizeId, PrizeUpdateRequest request) {
        Prize prize = findOwned(userId, prizeId);
        prize.rename(request.name(), request.icon());
        return toResponse(prize, statsByPrize(userId));
    }

    @Transactional
    public PrizeResponse setArchived(Long userId, Long prizeId, boolean archived) {
        Prize prize = findOwned(userId, prizeId);
        prize.setArchived(archived);
        return toResponse(prize, statsByPrize(userId));
    }

    /**
     * Spends the prize's cost. Locks the user's row first, so two redeems at the same moment
     * (a double-click) run one after the other and the second one sees the lower balance.
     */
    @Transactional
    public RedeemResult redeem(Long userId, Long prizeId) {
        users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        Prize prize = findOwned(userId, prizeId);
        if (prize.isArchived()) {
            throw new ApiException(HttpStatus.CONFLICT, "Archived prizes can't be redeemed. Restore it first.");
        }

        long balance = points.balance(userId);
        if (balance < prize.getCost()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "You need " + (prize.getCost() - balance) + " more points to redeem " + prize.getName() + ".");
        }

        Redemption redemption = redemptions.save(new Redemption(prizeId, userId, prize.getCost()));
        points.record(PointTransaction.forRedemption(userId, prize.getCost(), redemption.getId(),
                "Redeemed: " + prize.getName()));

        return new RedeemResult(toResponse(prize, statsByPrize(userId)), points.summary(userId));
    }

    private Map<Long, PrizeStats> statsByPrize(Long userId) {
        return redemptions.statsByUser(userId).stream()
                .collect(Collectors.toMap(PrizeStats::getPrizeId, Function.identity()));
    }

    private static PrizeResponse toResponse(Prize prize, Map<Long, PrizeStats> stats) {
        PrizeStats s = stats.get(prize.getId());
        return s == null
                ? PrizeResponse.from(prize, 0, null)
                : PrizeResponse.from(prize, s.getCount(), s.getLastRedeemedAt());
    }

    private Prize findOwned(Long userId, Long prizeId) {
        return prizes.findByIdAndUserId(prizeId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prize not found"));
    }
}
