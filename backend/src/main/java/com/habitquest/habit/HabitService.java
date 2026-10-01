package com.habitquest.habit;

import com.habitquest.common.ApiException;
import com.habitquest.common.UserClock;
import com.habitquest.habit.dto.HabitRequest;
import com.habitquest.habit.dto.HabitResponse;
import com.habitquest.user.User;
import com.habitquest.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class HabitService {

    private final HabitRepository habits;
    private final UserRepository users;
    private final UserClock userClock;

    public HabitService(HabitRepository habits, UserRepository users, UserClock userClock) {
        this.habits = habits;
        this.users = users;
        this.userClock = userClock;
    }

    @Transactional(readOnly = true)
    public List<HabitResponse> list(Long userId, boolean archived) {
        Set<Long> locked = new HashSet<>(habits.findHabitIdsWithCheckIns(userId));
        return habits.findByUserIdAndArchivedOrderByCreatedAtAsc(userId, archived).stream()
                .map(habit -> HabitResponse.from(habit, locked.contains(habit.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public HabitResponse get(Long userId, Long habitId) {
        Habit habit = findOwned(userId, habitId);
        return HabitResponse.from(habit, habits.hasCheckIns(habitId));
    }

    @Transactional
    public HabitResponse create(Long userId, HabitRequest request) {
        Habit habit = new Habit(userId);
        apply(habit, request, today(userId));
        return HabitResponse.from(habits.save(habit), false);
    }

    /**
     * Replaces all editable fields. Changing points only affects future check-ins:
     * each check-in stores the points it earned.
     * Frequency and target are locked after the first check-in (PRD §5.1), so streaks are always
     * measured against the rules the user started with.
     */
    @Transactional
    public HabitResponse update(Long userId, Long habitId, HabitRequest request) {
        Habit habit = findOwned(userId, habitId);
        boolean locked = habits.hasCheckIns(habitId);
        if (locked && changesRules(habit, request)) {
            throw new ApiException(HttpStatus.CONFLICT, "Frequency and target are locked after the first check-in. "
                    + "To change them, archive this habit and create a new one.");
        }
        apply(habit, request, habit.getStartDate());
        return HabitResponse.from(habit, locked);
    }

    /** Archiving hides a habit without deleting its history. */
    @Transactional
    public HabitResponse setArchived(Long userId, Long habitId, boolean archived) {
        Habit habit = findOwned(userId, habitId);
        habit.setArchived(archived);
        return HabitResponse.from(habit, habits.hasCheckIns(habitId));
    }

    private static boolean changesRules(Habit habit, HabitRequest request) {
        Frequency frequency = request.frequency() != null ? request.frequency() : Frequency.DAILY;
        int targetCount = request.targetCount() != null ? request.targetCount() : 1;
        return frequency != habit.getFrequency() || targetCount != habit.getTargetCount();
    }

    private void apply(Habit habit, HabitRequest request, LocalDate defaultStartDate) {
        Frequency frequency = request.frequency() != null ? request.frequency() : Frequency.DAILY;
        int targetCount = request.targetCount() != null ? request.targetCount() : 1;
        if (targetCount > frequency.maxTarget()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, switch (frequency) {
                case DAILY -> "Daily habits are done once a day";
                case WEEKLY -> "A weekly habit can be done at most 7 times per week";
                case MONTHLY -> "A monthly habit can be done at most 31 times per month";
            });
        }

        LocalDate startDate = request.startDate() != null ? request.startDate() : defaultStartDate;
        if (request.endDate() != null && request.endDate().isBefore(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End date can't be before the start date");
        }

        habit.update(request.name(), request.icon(), request.points(), frequency, targetCount,
                startDate, request.endDate());
    }

    /** Returns 404 both when the habit doesn't exist and when it belongs to someone else. */
    private Habit findOwned(Long userId, Long habitId) {
        return habits.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Habit not found"));
    }

    /** "Today" for this user, in their own time zone. */
    private LocalDate today(Long userId) {
        String timezone = users.findById(userId)
                .map(User::getTimezone)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        return userClock.today(timezone);
    }
}
