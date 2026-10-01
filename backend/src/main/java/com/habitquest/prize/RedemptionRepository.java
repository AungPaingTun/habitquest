package com.habitquest.prize;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {

    /** Redeem count and last redeem time per prize, for all of a user's prizes in one query. */
    interface PrizeStats {
        Long getPrizeId();

        long getCount();

        Instant getLastRedeemedAt();
    }

    @Query("""
            select r.prizeId as prizeId, count(r) as count, max(r.redeemedAt) as lastRedeemedAt
            from Redemption r where r.userId = :userId group by r.prizeId
            """)
    List<PrizeStats> statsByUser(Long userId);

    /** Redemptions in [from, to). */
    List<Redemption> findByUserIdAndRedeemedAtGreaterThanEqualAndRedeemedAtLessThan(Long userId, Instant from,
                                                                                   Instant to);
}
