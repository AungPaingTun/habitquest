package com.habitquest.points;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    /** Points available to spend: every earn, bonus, undo and redeem added together. */
    @Query("select coalesce(sum(t.amount), 0) from PointTransaction t where t.userId = :userId")
    long balance(Long userId);

    /** Lifetime XP: the same sum, but spending (REDEEM) doesn't count against it. */
    @Query("""
            select coalesce(sum(t.amount), 0) from PointTransaction t
            where t.userId = :userId and t.type <> com.habitquest.points.TransactionType.REDEEM
            """)
    long lifetimeXp(Long userId);

    List<PointTransaction> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    /** Ledger rows in [from, to). */
    List<PointTransaction> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long userId, Instant from,
                                                                                       Instant to);
}
