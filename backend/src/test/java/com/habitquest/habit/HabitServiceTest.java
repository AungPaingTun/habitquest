package com.habitquest.habit;

import com.habitquest.common.ApiException;
import com.habitquest.common.UserClock;
import com.habitquest.habit.dto.HabitRequest;
import com.habitquest.habit.dto.HabitResponse;
import com.habitquest.user.User;
import com.habitquest.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long HABIT_ID = 10L;
    private static final String ZONE = "Pacific/Kiritimati";

    @Mock
    private HabitRepository habits;

    @Mock
    private UserRepository users;

    // Fixed instant: 2026-10-01 12:00 UTC is already 2026-10-02 in Kiritimati (UTC+14).
    @Spy
    private UserClock userClock = new UserClock(Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC));

    @InjectMocks
    private HabitService service;

    private static HabitRequest request(Frequency frequency, Integer targetCount, LocalDate start, LocalDate end) {
        return new HabitRequest("Read", "book", 10, frequency, targetCount, start, end);
    }

    private void stubUserAndSave() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(new User("a@b.c", "hash", "A", ZONE)));
        when(habits.save(any(Habit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubUser() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(new User("a@b.c", "hash", "A", ZONE)));
    }

    private static void assertBadRequest(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private Habit existingHabit(LocalDate start) {
        Habit habit = new Habit(USER_ID);
        habit.update("Old", null, 5, Frequency.DAILY, 1, start, null);
        return habit;
    }

    @Test
    void create_withOnlyNameAndPoints_defaultsToDailyTargetOneAndTodayInUserZone() {
        stubUserAndSave();

        HabitResponse response = service.create(USER_ID,
                new HabitRequest("Read", null, 10, null, null, null, null));

        assertThat(response.frequency()).isEqualTo(Frequency.DAILY);
        assertThat(response.targetCount()).isEqualTo(1);
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 10, 2)); // user's date, not the server's 10-01
        assertThat(response.endDate()).isNull();
        assertThat(response.name()).isEqualTo("Read");
        assertThat(response.points()).isEqualTo(10);
        assertThat(response.archived()).isFalse();
    }

    @Test
    void create_weeklyWithExplicitDates_keepsTargetAndDates() {
        stubUserAndSave();
        LocalDate start = LocalDate.of(2030, 1, 1);
        LocalDate end = LocalDate.of(2030, 6, 30);

        HabitResponse response = service.create(USER_ID, request(Frequency.WEEKLY, 2, start, end));

        assertThat(response.frequency()).isEqualTo(Frequency.WEEKLY);
        assertThat(response.targetCount()).isEqualTo(2);
        assertThat(response.startDate()).isEqualTo(start);
        assertThat(response.endDate()).isEqualTo(end);
        ArgumentCaptor<Habit> captor = ArgumentCaptor.forClass(Habit.class);
        verify(habits).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void create_dailyWithTargetCountAboveOne_throwsBadRequest() {
        stubUser();

        assertBadRequest(() -> service.create(USER_ID, request(Frequency.DAILY, 2, null, null)));
        verify(habits, never()).save(any());
    }

    @Test
    void create_weeklyWithTargetSeven_isAccepted() {
        stubUserAndSave();

        HabitResponse response = service.create(USER_ID, request(Frequency.WEEKLY, 7, null, null));

        assertThat(response.targetCount()).isEqualTo(7);
    }

    @Test
    void create_weeklyWithTargetEight_throwsBadRequest() {
        stubUser();

        assertBadRequest(() -> service.create(USER_ID, request(Frequency.WEEKLY, 8, null, null)));
        verify(habits, never()).save(any());
    }

    @Test
    void create_monthlyWithTargetThirtyOne_isAccepted() {
        stubUserAndSave();

        HabitResponse response = service.create(USER_ID, request(Frequency.MONTHLY, 31, null, null));

        assertThat(response.targetCount()).isEqualTo(31);
        assertThat(response.frequency()).isEqualTo(Frequency.MONTHLY);
    }

    @Test
    void create_monthlyWithTargetThirtyTwo_throwsBadRequest() {
        stubUser();

        assertBadRequest(() -> service.create(USER_ID, request(Frequency.MONTHLY, 32, null, null)));
        verify(habits, never()).save(any());
    }

    @Test
    void create_endDateBeforeStartDate_throwsBadRequest() {
        stubUser();
        LocalDate start = LocalDate.of(2030, 5, 10);

        assertBadRequest(() -> service.create(USER_ID, request(null, null, start, start.minusDays(1))));
        verify(habits, never()).save(any());
    }

    @Test
    void create_endDateEqualToStartDate_isAccepted() {
        stubUserAndSave();
        LocalDate day = LocalDate.of(2030, 5, 10);

        HabitResponse response = service.create(USER_ID, request(null, null, day, day));

        assertThat(response.startDate()).isEqualTo(day);
        assertThat(response.endDate()).isEqualTo(day);
    }

    @Test
    void create_whenUserNoLongerExists_throwsUnauthorized() {
        when(users.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER_ID, request(null, null, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(habits, never()).save(any());
    }

    @Test
    void update_withNullStartDate_keepsExistingStartDate() {
        LocalDate originalStart = LocalDate.of(2029, 3, 4);
        when(habits.findByIdAndUserId(HABIT_ID, USER_ID)).thenReturn(Optional.of(existingHabit(originalStart)));

        HabitResponse response = service.update(USER_ID, HABIT_ID,
                new HabitRequest("New", null, 20, Frequency.WEEKLY, 3, null, null));

        assertThat(response.startDate()).isEqualTo(originalStart);
        assertThat(response.name()).isEqualTo("New");
        assertThat(response.points()).isEqualTo(20);
        assertThat(response.frequency()).isEqualTo(Frequency.WEEKLY);
        assertThat(response.targetCount()).isEqualTo(3);
    }

    @Test
    void update_afterFirstCheckInChangingFrequency_throwsConflictAndKeepsRules() {
        Habit habit = existingHabit(LocalDate.of(2026, 9, 1)); // DAILY, target 1
        when(habits.findByIdAndUserId(HABIT_ID, USER_ID)).thenReturn(Optional.of(habit));
        when(habits.hasCheckIns(HABIT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(USER_ID, HABIT_ID,
                new HabitRequest("Old", null, 5, Frequency.WEEKLY, 1, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(habit.getFrequency()).isEqualTo(Frequency.DAILY);
    }

    @Test
    void update_afterFirstCheckInChangingTarget_throwsConflict() {
        Habit habit = new Habit(USER_ID);
        habit.update("Run", null, 5, Frequency.WEEKLY, 2, LocalDate.of(2026, 9, 1), null);
        when(habits.findByIdAndUserId(HABIT_ID, USER_ID)).thenReturn(Optional.of(habit));
        when(habits.hasCheckIns(HABIT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(USER_ID, HABIT_ID,
                new HabitRequest("Run", null, 5, Frequency.WEEKLY, 3, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(habit.getTargetCount()).isEqualTo(2);
    }

    @Test
    void update_afterFirstCheckInKeepingRules_canStillChangeNameIconPointsAndDates() {
        Habit habit = existingHabit(LocalDate.of(2026, 9, 1)); // DAILY, target 1
        when(habits.findByIdAndUserId(HABIT_ID, USER_ID)).thenReturn(Optional.of(habit));
        when(habits.hasCheckIns(HABIT_ID)).thenReturn(true);

        HabitResponse response = service.update(USER_ID, HABIT_ID, new HabitRequest(
                "New name", "🥗", 8, Frequency.DAILY, 1, null, LocalDate.of(2026, 12, 31)));

        assertThat(response.name()).isEqualTo("New name");
        assertThat(response.points()).isEqualTo(8);
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(response.rulesLocked()).isTrue();
    }

    @Test
    void update_whenHabitNotOwned_throwsNotFound() {
        when(habits.findByIdAndUserId(HABIT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(USER_ID, HABIT_ID, request(null, null, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void get_whenHabitNotOwned_throwsNotFound() {
        when(habits.findByIdAndUserId(HABIT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(USER_ID, HABIT_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void setArchived_whenHabitNotOwned_throwsNotFound() {
        when(habits.findByIdAndUserId(HABIT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setArchived(USER_ID, HABIT_ID, true))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void setArchived_trueThenFalse_flipsTheFlag() {
        Habit habit = existingHabit(LocalDate.of(2029, 3, 4));
        when(habits.findByIdAndUserId(HABIT_ID, USER_ID)).thenReturn(Optional.of(habit));

        HabitResponse archived = service.setArchived(USER_ID, HABIT_ID, true);
        assertThat(archived.archived()).isTrue();
        assertThat(habit.isArchived()).isTrue();

        HabitResponse restored = service.setArchived(USER_ID, HABIT_ID, false);
        assertThat(restored.archived()).isFalse();
        assertThat(habit.isArchived()).isFalse();
    }
}
