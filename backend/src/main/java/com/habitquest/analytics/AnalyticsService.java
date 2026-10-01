package com.habitquest.analytics;

import com.habitquest.analytics.dto.AnalyticsResponse;
import com.habitquest.analytics.dto.AnalyticsResponse.Bucket;
import com.habitquest.analytics.dto.AnalyticsResponse.HabitStats;
import com.habitquest.analytics.dto.AnalyticsResponse.PrizeStat;
import com.habitquest.analytics.dto.AnalyticsResponse.Summary;
import com.habitquest.analytics.dto.HeatmapResponse;
import com.habitquest.checkin.HabitLog;
import com.habitquest.checkin.HabitLogRepository;
import com.habitquest.checkin.StreakCalculator;
import com.habitquest.checkin.StreakCalculator.Streak;
import com.habitquest.common.ApiException;
import com.habitquest.common.UserClock;
import com.habitquest.habit.Frequency;
import com.habitquest.habit.Habit;
import com.habitquest.habit.HabitRepository;
import com.habitquest.points.PointTransaction;
import com.habitquest.points.PointTransactionRepository;
import com.habitquest.points.TransactionType;
import com.habitquest.prize.Prize;
import com.habitquest.prize.PrizeRepository;
import com.habitquest.prize.Redemption;
import com.habitquest.prize.RedemptionRepository;
import com.habitquest.user.User;
import com.habitquest.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Weekly / monthly / yearly stats for the Analytics page. Read-only. */
@Service
public class AnalyticsService {

    private final UserRepository users;
    private final HabitRepository habits;
    private final HabitLogRepository logs;
    private final PointTransactionRepository transactions;
    private final RedemptionRepository redemptions;
    private final PrizeRepository prizes;
    private final UserClock userClock;

    public AnalyticsService(UserRepository users, HabitRepository habits, HabitLogRepository logs,
                            PointTransactionRepository transactions, RedemptionRepository redemptions,
                            PrizeRepository prizes, UserClock userClock) {
        this.users = users;
        this.habits = habits;
        this.logs = logs;
        this.transactions = transactions;
        this.redemptions = redemptions;
        this.prizes = prizes;
        this.userClock = userClock;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(Long userId, Period period, LocalDate anchor) {
        User user = findUser(userId);
        ZoneId zone = ZoneId.of(user.getTimezone());
        LocalDate today = userClock.today(user.getTimezone());
        LocalDate start = period.startOf(anchor != null ? anchor : today);
        LocalDate end = period.endOf(start);
        LocalDate elapsedEnd = end.isAfter(today) ? today : end; // nothing is "expected" in the future

        // All of the user's logs: streaks need full history, everything else filters to the period.
        Map<Long, List<LocalDate>> datesByHabit = logs.findByUserId(userId).stream()
                .collect(Collectors.groupingBy(HabitLog::getHabitId,
                        Collectors.mapping(HabitLog::getLogDate, Collectors.toList())));

        List<HabitStats> habitStats = new ArrayList<>();
        int totalCheckIns = 0;
        double expectedSum = 0;
        double cappedDoneSum = 0;
        Set<LocalDate> activeDays = new HashSet<>();
        for (Habit habit : habits.findByUserIdOrderByCreatedAtAsc(userId)) {
            List<LocalDate> allDates = datesByHabit.getOrDefault(habit.getId(), List.of());
            List<LocalDate> inPeriod = allDates.stream().filter(d -> !d.isBefore(start) && !d.isAfter(end)).toList();
            double expected = expectedCheckIns(habit, start, elapsedEnd);
            boolean activeInPeriod = !habit.isArchived() && expected > 0;
            if (!activeInPeriod && inPeriod.isEmpty()) {
                continue;
            }

            LocalDate streakAsOf = habit.getEndDate() != null && habit.getEndDate().isBefore(today)
                    ? habit.getEndDate() : today;
            Streak streak = StreakCalculator.calculate(habit.getFrequency(), habit.getTargetCount(), allDates, streakAsOf);
            habitStats.add(new HabitStats(habit.getId(), habit.getName(), habit.getIcon(), habit.getFrequency(),
                    habit.getTargetCount(), habit.isArchived(), inPeriod.size(), round1(expected),
                    completionRate(inPeriod.size(), expected), streak.current(), streak.best()));

            totalCheckIns += inPeriod.size();
            expectedSum += expected;
            cappedDoneSum += Math.min(inPeriod.size(), expected);
            activeDays.addAll(inPeriod);
        }

        // Ledger and redemptions: bucket by the user's local date.
        Instant from = start.atStartOfDay(zone).toInstant();
        Instant to = end.plusDays(1).atStartOfDay(zone).toInstant();
        List<PointTransaction> ledger = transactions
                .findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, from, to);
        List<Redemption> redeemed = redemptions
                .findByUserIdAndRedeemedAtGreaterThanEqualAndRedeemedAtLessThan(userId, from, to);

        long earned = ledger.stream().filter(t -> t.getType() != TransactionType.REDEEM)
                .mapToLong(PointTransaction::getAmount).sum();
        long spent = -ledger.stream().filter(t -> t.getType() == TransactionType.REDEEM)
                .mapToLong(PointTransaction::getAmount).sum();

        Summary summary = new Summary(totalCheckIns,
                expectedSum > 0 ? (int) Math.round(cappedDoneSum / expectedSum * 100) : null,
                earned, spent, redeemed.size(), activeDays.size());

        return new AnalyticsResponse(period, start, end, today, summary, habitStats,
                buckets(period, start, end, today, zone, ledger, datesByHabit.values()),
                topPrizes(redeemed));
    }

    @Transactional(readOnly = true)
    public HeatmapResponse heatmap(Long userId, Integer year) {
        User user = findUser(userId);
        LocalDate today = userClock.today(user.getTimezone());
        int y = year != null ? year : today.getYear();
        Map<LocalDate, Integer> counts = new TreeMap<>();
        for (HabitLog log : logs.findByUserIdAndLogDateBetween(userId, LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31))) {
            counts.merge(log.getLogDate(), 1, Integer::sum);
        }
        List<HeatmapResponse.Day> days = counts.entrySet().stream()
                .map(e -> new HeatmapResponse.Day(e.getKey(), e.getValue()))
                .toList();
        int max = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return new HeatmapResponse(y, today, max, days);
    }

    /**
     * How many check-ins the habit's target asks for between {@code from} and {@code to} (inclusive),
     * limited to the habit's own start/end dates. Weekly/monthly targets are spread evenly over the days,
     * so a 2×/week habit expects 1.0 check-ins by Wednesday night; monthly targets use the capped target
     * for short months (see StreakCalculator.effectiveTarget).
     */
    static double expectedCheckIns(Habit habit, LocalDate from, LocalDate to) {
        LocalDate first = habit.getStartDate().isAfter(from) ? habit.getStartDate() : from;
        LocalDate last = habit.getEndDate() != null && habit.getEndDate().isBefore(to) ? habit.getEndDate() : to;
        double expected = 0;
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            expected += switch (habit.getFrequency()) {
                case DAILY -> 1.0;
                case WEEKLY -> habit.getTargetCount() / 7.0;
                case MONTHLY -> StreakCalculator.effectiveTarget(Frequency.MONTHLY, habit.getTargetCount(),
                        day.withDayOfMonth(1)) / (double) day.lengthOfMonth();
            };
        }
        return expected;
    }

    static Integer completionRate(int done, double expected) {
        if (expected <= 0) return null;
        return (int) Math.round(Math.min(1.0, done / expected) * 100);
    }

    private static List<Bucket> buckets(Period period, LocalDate start, LocalDate end, LocalDate today, ZoneId zone,
                                        List<PointTransaction> ledger, Iterable<List<LocalDate>> allLogDates) {
        Function<LocalDate, String> keyOf = period == Period.YEAR
                ? d -> YearMonth.from(d).toString()
                : LocalDate::toString;

        Map<String, long[]> points = new LinkedHashMap<>(); // key -> {earned, spent}
        Map<String, Integer> checkIns = new LinkedHashMap<>();
        Map<String, Boolean> future = new LinkedHashMap<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String key = keyOf.apply(cursor);
            points.putIfAbsent(key, new long[2]);
            checkIns.putIfAbsent(key, 0);
            // a bucket is "future" only if it starts after today
            future.putIfAbsent(key, cursor.isAfter(today));
            cursor = period == Period.YEAR ? cursor.plusMonths(1).withDayOfMonth(1) : cursor.plusDays(1);
        }

        for (PointTransaction t : ledger) {
            String key = keyOf.apply(t.getCreatedAt().atZone(zone).toLocalDate());
            long[] p = points.get(key);
            if (p == null) continue;
            if (t.getType() == TransactionType.REDEEM) p[1] -= t.getAmount();
            else p[0] += t.getAmount();
        }
        for (List<LocalDate> dates : allLogDates) {
            for (LocalDate d : dates) {
                if (!d.isBefore(start) && !d.isAfter(end)) checkIns.merge(keyOf.apply(d), 1, Integer::sum);
            }
        }

        return points.entrySet().stream()
                .map(e -> new Bucket(e.getKey(), e.getValue()[0], e.getValue()[1], checkIns.get(e.getKey()),
                        future.get(e.getKey())))
                .toList();
    }

    private List<PrizeStat> topPrizes(List<Redemption> redeemed) {
        if (redeemed.isEmpty()) return List.of();
        Map<Long, List<Redemption>> byPrize = redeemed.stream().collect(Collectors.groupingBy(Redemption::getPrizeId));
        Map<Long, Prize> prizeById = prizes.findAllById(byPrize.keySet()).stream()
                .collect(Collectors.toMap(Prize::getId, Function.identity()));
        return byPrize.entrySet().stream()
                .filter(e -> prizeById.containsKey(e.getKey()))
                .map(e -> {
                    Prize prize = prizeById.get(e.getKey());
                    long spent = e.getValue().stream().mapToLong(Redemption::getCostAtTime).sum();
                    return new PrizeStat(prize.getId(), prize.getName(), prize.getIcon(), e.getValue().size(), spent);
                })
                .sorted(Comparator.comparingInt(PrizeStat::count).reversed()
                        .thenComparing(Comparator.comparingLong(PrizeStat::pointsSpent).reversed()))
                .limit(5)
                .toList();
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private User findUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }
}
