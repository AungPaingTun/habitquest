package com.habitquest.demo;

import com.habitquest.auth.AuthService;
import com.habitquest.auth.dto.RegisterRequest;
import com.habitquest.checkin.CheckInService;
import com.habitquest.common.ApiException;
import com.habitquest.habit.Frequency;
import com.habitquest.habit.HabitService;
import com.habitquest.habit.dto.HabitRequest;
import com.habitquest.prize.PrizeService;
import com.habitquest.prize.dto.PrizeCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Creates (or recreates) the public demo account with about two months of history.
 * Run with the demo-seed profile; it seeds, then shuts the app down:
 * <pre>java -Dspring.profiles.active=demo-seed -jar target/backend-*.jar</pre>
 * Check-ins and redemptions go through the real services on a moving clock, so streaks,
 * bonuses and levels follow the same rules as real use.
 */
@Component
@Profile("demo-seed")
class DemoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);
    private static final String TIMEZONE = "Asia/Kuala_Lumpur";
    private static final int DAYS_OF_HISTORY = 60;

    private final MutableClock clock;
    private final AuthService auth;
    private final HabitService habits;
    private final PrizeService prizes;
    private final CheckInService checkIns;
    private final JdbcTemplate jdbc;
    private final ConfigurableApplicationContext context;
    private final String email;
    private final String password;

    DemoSeeder(MutableClock clock, AuthService auth, HabitService habits, PrizeService prizes,
               CheckInService checkIns, JdbcTemplate jdbc, ConfigurableApplicationContext context,
               @Value("${app.demo.email}") String email, @Value("${app.demo.password}") String password) {
        this.clock = clock;
        this.auth = auth;
        this.habits = habits;
        this.prizes = prizes;
        this.checkIns = checkIns;
        this.jdbc = jdbc;
        this.context = context;
        this.email = email;
        this.password = password;
    }

    /** One habit plus how likely the demo user is to do it on a given day. */
    private record Plan(Long id, double chance, int startOffset, boolean perfectLastMonth) {
    }

    @Override
    public void run(ApplicationArguments args) {
        ZoneId zone = ZoneId.of(TIMEZONE);
        LocalDate today = LocalDate.now(zone);
        LocalDate first = today.minusDays(DAYS_OF_HISTORY);
        Random random = new Random(42); // same history every run

        // Foreign keys cascade, so this removes the old demo user's habits, logs, points and prizes too.
        jdbc.update("DELETE FROM users WHERE email = ?", email);

        moveTo(first, zone);
        Long userId = auth.register(new RegisterRequest(email, password, "Demo", TIMEZONE)).user().id();

        List<Plan> plans = new ArrayList<>();
        plans.add(new Plan(habit(userId, "Drink 2L water", "💧", 2, Frequency.DAILY, 1), 0.85, 0, true));
        plans.add(new Plan(habit(userId, "Read 20 pages", "📚", 3, Frequency.DAILY, 1), 0.7, 0, false));
        plans.add(new Plan(habit(userId, "Workout", "🏋️", 5, Frequency.WEEKLY, 3), 0.55, 0, false));
        plans.add(new Plan(habit(userId, "Clean my room", "🧹", 4, Frequency.MONTHLY, 4), 0.2, 0, false));
        Long bubbleTea = prize(userId, "Bubble tea", "🧋", 30);
        Long movieNight = prize(userId, "Movie night", "🎬", 80);
        prize(userId, "Gaming hour", "🎮", 25);
        prize(userId, "New book", "📖", 150);

        // Meditation starts halfway through, so the charts show a habit being added later.
        int meditateFrom = DAYS_OF_HISTORY / 2;
        moveTo(first.plusDays(meditateFrom), zone);
        plans.add(new Plan(habit(userId, "Meditate 10 min", "🧘", 2, Frequency.DAILY, 1), 0.6, meditateFrom, false));

        // Every day up to yesterday.
        for (int day = 0; day < DAYS_OF_HISTORY; day++) {
            LocalDate date = first.plusDays(day);
            moveTo(date, zone);
            for (Plan plan : plans) {
                boolean lastMonth = day >= DAYS_OF_HISTORY - 30;
                double chance = plan.perfectLastMonth() && lastMonth ? 1.0 : plan.chance();
                if (day >= plan.startOffset() && random.nextDouble() < chance) {
                    tryCheckIn(userId, plan.id());
                }
            }
            // Treat yourself every couple of weeks; skipped quietly if the balance is too low.
            if (day % 12 == 11) tryRedeem(userId, bubbleTea, date, zone);
            if (day == 40) tryRedeem(userId, movieNight, date, zone);
        }

        // Today: water and reading are already done, so this week's stats are never empty;
        // the rest stay open for visitors to try checking in.
        moveTo(today, zone);
        tryCheckIn(userId, plans.get(0).id());
        tryCheckIn(userId, plans.get(1).id());

        backdateTimestamps(userId, first, today, zone);
        log.info("Demo account {} seeded with {} days of history", email, DAYS_OF_HISTORY);
        context.close();
    }

    private Long habit(Long userId, String name, String icon, int points, Frequency frequency, int target) {
        return habits.create(userId, new HabitRequest(name, icon, points, frequency, target, null, null)).id();
    }

    private Long prize(Long userId, String name, String icon, int cost) {
        return prizes.create(userId, new PrizeCreateRequest(name, icon, cost)).id();
    }

    private void tryCheckIn(Long userId, Long habitId) {
        try {
            checkIns.checkIn(userId, habitId);
        } catch (ApiException e) {
            // e.g. weekly target already reached: the real app would refuse too
        }
    }

    private void tryRedeem(Long userId, Long prizeId, LocalDate date, ZoneId zone) {
        try {
            prizes.redeem(userId, prizeId);
        } catch (ApiException e) {
            return; // not enough points yet
        }
        // Stamp it on the seeding day (after that evening's check-ins) instead of the real time.
        var at = Timestamp.from(date.atTime(21, 0).atZone(zone).toInstant());
        jdbc.update("""
                UPDATE redemptions SET redeemed_at = ?
                WHERE id = (SELECT max(id) FROM redemptions WHERE user_id = ?)""", at, userId);
        jdbc.update("""
                UPDATE point_transactions SET created_at = ?
                WHERE id = (SELECT max(id) FROM point_transactions WHERE user_id = ? AND type = 'REDEEM')""", at, userId);
    }

    /** Midday in the user's zone, so "today" is unambiguous. */
    private void moveTo(LocalDate date, ZoneId zone) {
        clock.set(date.atTime(LocalTime.NOON).atZone(zone).toInstant());
    }

    /**
     * The entities stamp created_at with the real time, so every row would say "today".
     * Move them to the day they belong to, so recent activity reads naturally.
     */
    private void backdateTimestamps(Long userId, LocalDate first, LocalDate today, ZoneId zone) {
        var start = Timestamp.from(first.atTime(9, 0).atZone(zone).toInstant());
        jdbc.update("UPDATE users SET created_at = ? WHERE id = ?", start, userId);
        jdbc.update("UPDATE prizes SET created_at = ? WHERE user_id = ?", start, userId);
        jdbc.update("""
                UPDATE habits h SET created_at = (h.start_date + time '09:00') AT TIME ZONE ?
                WHERE h.user_id = ?""", zone.getId(), userId);
        // Past check-ins: evening of the logged day; the id keeps earn → bonus order. Today's keep the real time.
        jdbc.update("""
                UPDATE habit_logs SET created_at = (log_date + time '20:00') AT TIME ZONE ? + id * interval '1 second'
                WHERE user_id = ? AND log_date < ?""", zone.getId(), userId, today);
        jdbc.update("""
                UPDATE point_transactions SET created_at = (log_date + time '20:00') AT TIME ZONE ? + id * interval '1 second'
                WHERE user_id = ? AND log_date < ?""", zone.getId(), userId, today);
    }
}
