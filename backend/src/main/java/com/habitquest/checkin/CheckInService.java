package com.habitquest.checkin;

import com.habitquest.checkin.StreakCalculator.Streak;
import com.habitquest.checkin.dto.CheckInResult;
import com.habitquest.checkin.dto.TodayHabit;
import com.habitquest.checkin.dto.TodayResponse;
import com.habitquest.common.ApiException;
import com.habitquest.common.UserClock;
import com.habitquest.habit.Frequency;
import com.habitquest.habit.Habit;
import com.habitquest.habit.HabitRepository;
import com.habitquest.habit.HabitStatus;
import com.habitquest.points.PointTransaction;
import com.habitquest.points.PointsService;
import com.habitquest.points.TransactionType;
import com.habitquest.user.User;
import com.habitquest.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** The core game loop: check in a habit to earn points (plus streak bonus), or undo it the same day. PRD §5.2–5.4. */
@Service
public class CheckInService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final UserRepository users;
    private final HabitRepository habits;
    private final HabitLogRepository logs;
    private final PointsService points;
    private final UserClock userClock;

    public CheckInService(UserRepository users, HabitRepository habits, HabitLogRepository logs,
                          PointsService points, UserClock userClock) {
        this.users = users;
        this.habits = habits;
        this.logs = logs;
        this.points = points;
        this.userClock = userClock;
    }

    @Transactional
    public CheckInResult checkIn(Long userId, Long habitId) {
        User user = lockUser(userId);
        Habit habit = findOwned(userId, habitId);
        LocalDate today = userClock.today(user.getTimezone());

        if (habit.isArchived()) {
            throw conflict("Archived habits can't be checked in. Restore it first.");
        }
        switch (habit.statusOn(today)) {
            case UPCOMING -> throw conflict("This habit starts on " + DAY.format(habit.getStartDate()) + ".");
            case COMPLETED -> throw conflict("This habit ended on " + DAY.format(habit.getEndDate()) + ".");
            case ACTIVE -> { }
        }

        List<LocalDate> dates = new ArrayList<>(logs.findLogDates(habitId));
        if (dates.contains(today)) {
            throw conflict("Already checked in today.");
        }
        Frequency frequency = habit.getFrequency();
        LocalDate periodStart = StreakCalculator.periodStart(frequency, today);
        int target = StreakCalculator.effectiveTarget(frequency, habit.getTargetCount(), periodStart);
        int doneThisPeriod = StreakCalculator.countInCurrentPeriod(frequency, dates, today);
        if (doneThisPeriod >= target) {
            throw conflict("You've already reached this " + periodWord(frequency) + "'s target ("
                    + doneThisPeriod + "/" + target + ").");
        }

        dates.add(today);
        Streak streak = StreakCalculator.calculate(frequency, habit.getTargetCount(), dates, today);
        // The bonus is paid on the check-in that completes the period: every check-in for daily habits,
        // the one that reaches the target for weekly/monthly habits. At most once per period, checked
        // against stored logs, so raising the target mid-week can't pay the bonus again.
        boolean bonusAlreadyPaid = logs.existsByHabitIdAndLogDateBetweenAndBonusAwardedGreaterThan(
                habitId, periodStart, today, 0);
        boolean completesPeriod = !bonusAlreadyPaid && doneThisPeriod + 1 >= target;
        int bonus = completesPeriod ? StreakRules.bonusFor(frequency, streak.current()) : 0;

        logs.save(new HabitLog(habitId, userId, today, habit.getPoints(), bonus));
        points.record(PointTransaction.forHabit(userId, habit.getPoints(), TransactionType.EARN,
                habitId, today, habit.getName()));
        if (bonus > 0) {
            points.record(PointTransaction.forHabit(userId, bonus, TransactionType.STREAK_BONUS, habitId, today,
                    "Streak bonus: " + habit.getName() + " (" + streak.current() + "-" + periodWord(frequency) + " streak)"));
        }

        return new CheckInResult(habitId, habit.getPoints() + bonus, bonus, streak.current(), points.summary(userId));
    }

    /** Same-day undo. Blocked if the points were already spent, so the balance never goes negative. */
    @Transactional
    public CheckInResult undo(Long userId, Long habitId) {
        User user = lockUser(userId);
        Habit habit = findOwned(userId, habitId);
        LocalDate today = userClock.today(user.getTimezone());

        HabitLog log = logs.findByHabitIdAndLogDate(habitId, today)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "There's no check-in today to undo."));
        int total = log.totalAwarded();
        if (points.balance(userId) < total) {
            throw conflict("You've already spent these points, so this check-in can't be undone.");
        }

        logs.delete(log);
        points.record(PointTransaction.forHabit(userId, -total, TransactionType.UNDO, habitId, today,
                "Undo: " + habit.getName()));

        Streak streak = StreakCalculator.calculate(habit.getFrequency(), habit.getTargetCount(),
                logs.findLogDates(habitId), today);
        return new CheckInResult(habitId, -total, 0, streak.current(), points.summary(userId));
    }

    /** All non-archived habits with today's progress, plus the points summary. */
    @Transactional(readOnly = true)
    public TodayResponse today(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        LocalDate today = userClock.today(user.getTimezone());

        Map<Long, List<LocalDate>> datesByHabit = logs.findByUserId(userId).stream()
                .collect(Collectors.groupingBy(HabitLog::getHabitId,
                        Collectors.mapping(HabitLog::getLogDate, Collectors.toList())));

        List<TodayHabit> items = habits.findByUserIdAndArchivedOrderByCreatedAtAsc(userId, false).stream()
                .map(habit -> toTodayHabit(habit, datesByHabit.getOrDefault(habit.getId(), List.of()), today))
                .toList();

        return new TodayResponse(today, points.summary(userId), items);
    }

    private static TodayHabit toTodayHabit(Habit habit, List<LocalDate> dates, LocalDate today) {
        HabitStatus status = habit.statusOn(today);
        // A completed habit's streak and progress are frozen at its last day.
        LocalDate asOf = status == HabitStatus.COMPLETED ? habit.getEndDate() : today;
        Streak streak = StreakCalculator.calculate(habit.getFrequency(), habit.getTargetCount(), dates, asOf);
        boolean doneToday = dates.contains(today);
        int periodCount = StreakCalculator.countInCurrentPeriod(habit.getFrequency(), dates, asOf);
        // e.g. "30× per month" shows as 28 in February
        int target = StreakCalculator.effectiveTarget(habit.getFrequency(), habit.getTargetCount(),
                StreakCalculator.periodStart(habit.getFrequency(), asOf));
        boolean canCheckIn = status == HabitStatus.ACTIVE && !doneToday && periodCount < target;

        return new TodayHabit(habit.getId(), habit.getName(), habit.getIcon(), habit.getPoints(),
                habit.getFrequency(), target, habit.getStartDate(), habit.getEndDate(),
                status, doneToday, periodCount, streak.current(), streak.best(), canCheckIn);
    }

    private User lockUser(Long userId) {
        return users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }

    private Habit findOwned(Long userId, Long habitId) {
        return habits.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Habit not found"));
    }

    private static String periodWord(Frequency frequency) {
        return switch (frequency) {
            case DAILY -> "day";
            case WEEKLY -> "week";
            case MONTHLY -> "month";
        };
    }

    private static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}
