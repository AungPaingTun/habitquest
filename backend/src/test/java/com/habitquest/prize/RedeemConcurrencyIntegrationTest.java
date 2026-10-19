package com.habitquest.prize;

import com.habitquest.common.ApiException;
import com.habitquest.points.PointTransaction;
import com.habitquest.points.PointsService;
import com.habitquest.points.TransactionType;
import com.habitquest.user.User;
import com.habitquest.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the pessimistic row lock in {@link PrizeService#redeem} stops a double-spend.
 *
 * <p>A user is given exactly enough points for one redeem, then two redeems are fired at the same instant.
 * Without the lock, both could read the old balance and both succeed, leaving the balance negative. With the
 * lock ({@code SELECT ... FOR UPDATE} on the user row), the second redeem waits, then sees the lower balance and
 * is rejected. This runs against the real PostgreSQL, because row locking is exactly the behaviour an in-memory
 * database would not reproduce.
 */
@SpringBootTest
class RedeemConcurrencyIntegrationTest {

    @Autowired private PrizeService prizeService;
    @Autowired private PointsService pointsService;
    @Autowired private UserRepository users;
    @Autowired private PrizeRepository prizes;

    @Test
    void twoRedeemsAtOnce_onlyOneSucceeds() throws Exception {
        int cost = 100;
        Long userId = users.save(new User("race-" + System.nanoTime() + "@test.local", "x", "Racer", "UTC")).getId();
        // Give the user exactly one redeem's worth of points.
        pointsService.record(PointTransaction.forHabit(userId, cost, TransactionType.EARN, null, null, "seed"));
        Long prizeId = prizes.save(new Prize(userId, "Coffee", "coffee", cost)).getId();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        Callable<Void> attempt = () -> {
            startLine.await();           // both threads wait here, then are released together
            try {
                prizeService.redeem(userId, prizeId);
                successes.incrementAndGet();
            } catch (ApiException expectedWhenOutbid) {
                rejected.incrementAndGet();
            }
            return null;
        };
        Future<Void> a = pool.submit(attempt);
        Future<Void> b = pool.submit(attempt);
        startLine.countDown();           // fire both redeems at the same moment
        a.get(10, TimeUnit.SECONDS);
        b.get(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(successes.get()).as("exactly one redeem goes through").isEqualTo(1);
        assertThat(rejected.get()).as("the other is rejected for lack of points").isEqualTo(1);
        assertThat(pointsService.balance(userId)).as("balance never goes negative").isZero();
    }
}
