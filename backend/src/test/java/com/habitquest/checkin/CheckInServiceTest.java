package com.habitquest.checkin;

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
import com.habitquest.points.dto.PointsSummary;
import com.habitquest.user.User;
import com.habitquest.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long HABIT_ID = 10L;
    private static final String ZONE = "Asia/Yangon";
    // Friday; the week runs Mon 2026-09-28 .. Sun 2026-10-04.
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);
    private static final PointsSummary SUMMARY = new PointsSummary(100, 100, 1, 50, 200);

    @Mock
    private UserRepository users;

    @Mock
    private HabitRepository habits;

    @Mock
    private HabitLogRepository logs;

    @Mock
    private PointsService points;

    @Mock
    private UserClock userClock;

    @InjectMocks
    private CheckInService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("a@b.c", "hash", "A", ZONE);
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    // ---------- fixtures ----------

    private static Habit habit(Frequency frequency, int target, int pointsValue, LocalDate start, LocalDate end) {
        Habit habit = new Habit(USER_ID);
        habit.update("Read", "book", pointsValue, frequency, target, start, end);
        ReflectionTestUtils.setField(habit, "id", HABIT_ID);
        return habit;
    }

    private static Habit activeHabit(Frequency frequency, int target, int pointsValue) {
        return habit(frequency, target, pointsValue, LocalDate.of(2026, 1, 1), null);
    }

    private static List<LocalDate> days(LocalDate... dates) {
        return List.of(dates);
    }

    /** Stubs user lock, habit lookup and clock: everything checkIn/undo need before any rule is checked. */
    private void stubUserHabitClock(Habit habit) {
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(habits.findByIdAndUserId(HABIT_ID, USER_ID)).thenReturn(Optional.of(habit));
        when(userClock.today(anyString())).thenReturn(TODAY);
    }

    private void stubSummary() {
        when(points.summary(USER_ID)).thenReturn(SUMMARY);
    }

    private void assertConflictAndNothingWritten(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(logs, never()).save(any());
        verify(points, never()).record(any());
    }

    // ---------- checkIn: happy paths ----------

    @Test
    void checkIn_dailyHabitShortStreak_savesLogAndRecordsEarnWithoutBonus() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        stubUserHabitClock(habit);
        when(logs.findLogDates(HABIT_ID)).thenReturn(days(LocalDate.of(2026, 10, 1)));
        stubSummary();

        CheckInResult result = service.checkIn(USER_ID, HABIT_ID);

        ArgumentCaptor<HabitLog> logCaptor = ArgumentCaptor.forClass(HabitLog.class);
        verify(logs).save(logCaptor.capture());
        HabitLog saved = logCaptor.getValue();
        assertThat(saved.getHabitId()).isEqualTo(HABIT_ID);
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getLogDate()).isEqualTo(TODAY);
        assertThat(saved.getPointsAwarded()).isEqualTo(10);
        assertThat(saved.getBonusAwarded()).isZero();

        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(points).record(txCaptor.capture());
        PointTransaction earn = txCaptor.getValue();
        assertThat(earn.getType()).isEqualTo(TransactionType.EARN);
        assertThat(earn.getAmount()).isEqualTo(10);
        assertThat(earn.getUserId()).isEqualTo(USER_ID);
        assertThat(earn.getHabitId()).isEqualTo(HABIT_ID);
        assertThat(earn.getLogDate()).isEqualTo(TODAY);

        assertThat(result.habitId()).isEqualTo(HABIT_ID);
        assertThat(result.pointsChange()).isEqualTo(10);
        assertThat(result.bonus()).isZero();
        assertThat(result.currentStreak()).isEqualTo(2);
        assertThat(result.points()).isEqualTo(SUMMARY);
    }

    @Test
    void checkIn_dailySixthConsecutiveDay_noBonusYet() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        stubUserHabitClock(habit);
        when(logs.findLogDates(HABIT_ID)).thenReturn(days(
                LocalDate.of(2026, 9, 27), LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 29),
                LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1)));
        stubSummary();

        CheckInResult result = service.checkIn(USER_ID, HABIT_ID);

        assertThat(result.currentStreak()).isEqualTo(6);
        assertThat(result.bonus()).isZero();
        verify(points, times(1)).record(any());
    }

    @Test
    void checkIn_dailySeventhConsecutiveDay_paysBonusTwoAndRecordsEarnAndStreakBonus() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        stubUserHabitClock(habit);
        when(logs.findLogDates(HABIT_ID)).thenReturn(days(
                LocalDate.of(2026, 9, 26), LocalDate.of(2026, 9, 27), LocalDate.of(2026, 9, 28),
                LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1)));
        stubSummary();

        CheckInResult result = service.checkIn(USER_ID, HABIT_ID);

        ArgumentCaptor<HabitLog> logCaptor = ArgumentCaptor.forClass(HabitLog.class);
        verify(logs).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getPointsAwarded()).isEqualTo(10);
        assertThat(logCaptor.getValue().getBonusAwarded()).isEqualTo(2);

        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(points, times(2)).record(txCaptor.capture());
        List<PointTransaction> recorded = txCaptor.getAllValues();
        assertThat(recorded.get(0).getType()).isEqualTo(TransactionType.EARN);
        assertThat(recorded.get(0).getAmount()).isEqualTo(10);
        assertThat(recorded.get(1).getType()).isEqualTo(TransactionType.STREAK_BONUS);
        assertThat(recorded.get(1).getAmount()).isEqualTo(2);
        assertThat(recorded.get(1).getHabitId()).isEqualTo(HABIT_ID);
        assertThat(recorded.get(1).getLogDate()).isEqualTo(TODAY);

        assertThat(result.bonus()).isEqualTo(2);
        assertThat(result.pointsChange()).isEqualTo(12);
        assertThat(result.currentStreak()).isEqualTo(7);
    }

    @Test
    void checkIn_dailyThirtiethConsecutiveDay_paysBonusFour() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        stubUserHabitClock(habit);
        // 29 previous days: 2026-09-03 .. 2026-10-01
        List<LocalDate> previous = LocalDate.of(2026, 9, 3).datesUntil(LocalDate.of(2026, 10, 2)).toList();
        when(logs.findLogDates(HABIT_ID)).thenReturn(previous);
        stubSummary();

        CheckInResult result = service.checkIn(USER_ID, HABIT_ID);

        assertThat(result.currentStreak()).isEqualTo(30);
        assertThat(result.bonus()).isEqualTo(4);
        assertThat(result.pointsChange()).isEqualTo(14);
    }

    @Test
    void checkIn_weeklyTarget2FirstCheckInOfWeekWithLongStreak_paysNoBonus() {
        Habit habit = activeHabit(Frequency.WEEKLY, 2, 20);
        stubUserHabitClock(habit);
        // Three met weeks (09-07, 09-14, 09-21), nothing yet in the current week.
        when(logs.findLogDates(HABIT_ID)).thenReturn(days(
                LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 22)));
        stubSummary();

        CheckInResult result = service.checkIn(USER_ID, HABIT_ID);

        assertThat(result.bonus()).isZero();
        assertThat(result.pointsChange()).isEqualTo(20);
        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(points, times(1)).record(txCaptor.capture());
        assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.EARN);
    }

    @Test
    void checkIn_weeklyTarget2ReachingTargetWithTwoPriorMetWeeks_paysBonusTwo() {
        Habit habit = activeHabit(Frequency.WEEKLY, 2, 20);
        stubUserHabitClock(habit);
        // Two met weeks (09-14, 09-21) plus one check-in already this week (Mon 09-28).
        when(logs.findLogDates(HABIT_ID)).thenReturn(days(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 22),
                LocalDate.of(2026, 9, 28)));
        stubSummary();

        CheckInResult result = service.checkIn(USER_ID, HABIT_ID);

        assertThat(result.currentStreak()).isEqualTo(3);
        assertThat(result.bonus()).isEqualTo(2);
        assertThat(result.pointsChange()).isEqualTo(22);
        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(points, times(2)).record(txCaptor.capture());
        assertThat(txCaptor.getAllValues()).extracting(PointTransaction::getType)
                .containsExactly(TransactionType.EARN, TransactionType.STREAK_BONUS);
        assertThat(txCaptor.getAllValues()).extracting(PointTransaction::getAmount)
                .containsExactly(20, 2);
    }

    @Test
    void checkIn_targetRaisedAfterBonusAlreadyPaidThisWeek_paysNoSecondBonus() {
        // Target was 2 and the bonus was paid on Tue 09-29; the user then raised it to 3 and checks in again.
        Habit habit = activeHabit(Frequency.WEEKLY, 3, 20);
        stubUserHabitClock(habit);
        when(logs.findLogDates(HABIT_ID)).thenReturn(days(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 16),
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 23),
                LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 29)));
        when(logs.existsByHabitIdAndLogDateBetweenAndBonusAwardedGreaterThan(
                HABIT_ID, LocalDate.of(2026, 9, 28), TODAY, 0)).thenReturn(true);
        stubSummary();

        CheckInResult result = service.checkIn(USER_ID, HABIT_ID);

        assertThat(result.bonus()).isZero();
        assertThat(result.pointsChange()).isEqualTo(20);
        verify(points, times(1)).record(any(PointTransaction.class));
    }

    @Test
    void checkIn_weeklyTarget2ReachingTargetWithOnlyOnePriorMetWeek_paysNoBonus() {
        Habit habit = activeHabit(Frequency.WEEKLY, 2, 20);
        stubUserHabitClock(habit);
        when(logs.findLogDates(HABIT_ID)).thenReturn(days(
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 22),
                LocalDate.of(2026, 9, 28)));
        stubSummary();

        CheckInResult result = service.checkIn(USER_ID, HABIT_ID);

        assertThat(result.currentStreak()).isEqualTo(2);
        assertThat(result.bonus()).isZero();
    }

    @Test
    void checkIn_firstEverCheckIn_isAllowedAndStreakIsOne() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 5);
        stubUserHabitClock(habit);
        when(logs.findLogDates(HABIT_ID)).thenReturn(List.of());
        stubSummary();

        CheckInResult result = service.checkIn(USER_ID, HABIT_ID);

        assertThat(result.currentStreak()).isEqualTo(1);
        assertThat(result.pointsChange()).isEqualTo(5);
    }

    @Test
    void checkIn_onStartDateAndOnEndDate_isAllowed() {
        Habit habit = habit(Frequency.DAILY, 1, 10, TODAY, TODAY);
        stubUserHabitClock(habit);
        when(logs.findLogDates(HABIT_ID)).thenReturn(List.of());
        stubSummary();

        CheckInResult result = service.checkIn(USER_ID, HABIT_ID);

        assertThat(result.pointsChange()).isEqualTo(10);
        verify(logs).save(any(HabitLog.class));
    }

    @Test
    void checkIn_locksUserWithFindByIdForUpdateNotFindById() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        stubUserHabitClock(habit);
        when(logs.findLogDates(HABIT_ID)).thenReturn(List.of());
        stubSummary();

        service.checkIn(USER_ID, HABIT_ID);

        verify(users).findByIdForUpdate(USER_ID);
        verify(users, never()).findById(anyLong());
    }

    @Test
    void checkIn_usesTheUsersTimezoneToFindToday() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        stubUserHabitClock(habit);
        when(logs.findLogDates(HABIT_ID)).thenReturn(List.of());
        stubSummary();

        service.checkIn(USER_ID, HABIT_ID);

        verify(userClock).today(ZONE);
    }

    // ---------- checkIn: rejections ----------

    @Test
    void checkIn_alreadyCheckedInToday_throwsConflictAndWritesNothing() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        stubUserHabitClock(habit);
        when(logs.findLogDates(HABIT_ID)).thenReturn(days(TODAY));

        assertConflictAndNothingWritten(() -> service.checkIn(USER_ID, HABIT_ID));
    }

    @Test
    void checkIn_alreadyCheckedInTodayOnWeeklyHabit_throwsConflictEvenIfTargetNotReached() {
        Habit habit = activeHabit(Frequency.WEEKLY, 3, 10);
        stubUserHabitClock(habit);
        when(logs.findLogDates(HABIT_ID)).thenReturn(days(TODAY));

        assertThatThrownBy(() -> service.checkIn(USER_ID, HABIT_ID))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).contains("Already checked in today");
                });
        verify(logs, never()).save(any());
        verify(points, never()).record(any());
    }

    @Test
    void checkIn_weeklyTargetAlreadyReached_throwsConflictAndWritesNothing() {
        Habit habit = activeHabit(Frequency.WEEKLY, 2, 10);
        stubUserHabitClock(habit);
        when(logs.findLogDates(HABIT_ID)).thenReturn(days(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 29)));

        assertThatThrownBy(() -> service.checkIn(USER_ID, HABIT_ID))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).contains("2/2");
                });
        verify(logs, never()).save(any());
        verify(points, never()).record(any());
    }

    @Test
    void checkIn_archivedHabit_throwsConflictAndWritesNothing() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        habit.setArchived(true);
        stubUserHabitClock(habit);

        assertConflictAndNothingWritten(() -> service.checkIn(USER_ID, HABIT_ID));
    }

    @Test
    void checkIn_beforeStartDate_throwsConflictAndWritesNothing() {
        Habit habit = habit(Frequency.DAILY, 1, 10, TODAY.plusDays(1), null);
        stubUserHabitClock(habit);

        assertConflictAndNothingWritten(() -> service.checkIn(USER_ID, HABIT_ID));
    }

    @Test
    void checkIn_afterEndDate_throwsConflictAndWritesNothing() {
        Habit habit = habit(Frequency.DAILY, 1, 10, LocalDate.of(2026, 1, 1), TODAY.minusDays(1));
        stubUserHabitClock(habit);

        assertConflictAndNothingWritten(() -> service.checkIn(USER_ID, HABIT_ID));
    }

    @Test
    void checkIn_habitNotOwnedByUser_throwsNotFoundAndWritesNothing() {
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(habits.findByIdAndUserId(HABIT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkIn(USER_ID, HABIT_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(logs, never()).save(any());
        verify(points, never()).record(any());
    }

    @Test
    void checkIn_unknownUser_throwsUnauthorizedAndWritesNothing() {
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkIn(USER_ID, HABIT_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(logs, never()).save(any());
        verify(points, never()).record(any());
    }

    // ---------- undo ----------

    @Test
    void undo_todaysCheckInWithoutBonus_deletesLogAndRecordsNegativeUndo() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        stubUserHabitClock(habit);
        HabitLog log = new HabitLog(HABIT_ID, USER_ID, TODAY, 10, 0);
        when(logs.findByHabitIdAndLogDate(HABIT_ID, TODAY)).thenReturn(Optional.of(log));
        when(points.balance(USER_ID)).thenReturn(10L);
        when(logs.findLogDates(HABIT_ID)).thenReturn(days(LocalDate.of(2026, 10, 1)));
        stubSummary();

        CheckInResult result = service.undo(USER_ID, HABIT_ID);

        verify(logs).delete(log);
        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(points).record(txCaptor.capture());
        PointTransaction undo = txCaptor.getValue();
        assertThat(undo.getType()).isEqualTo(TransactionType.UNDO);
        assertThat(undo.getAmount()).isEqualTo(-10);
        assertThat(undo.getHabitId()).isEqualTo(HABIT_ID);
        assertThat(undo.getLogDate()).isEqualTo(TODAY);
        assertThat(result.pointsChange()).isEqualTo(-10);
        assertThat(result.bonus()).isZero();
        assertThat(result.currentStreak()).isEqualTo(1);
        assertThat(result.points()).isEqualTo(SUMMARY);
    }

    @Test
    void undo_checkInThatPaidBonus_reversesPointsPlusBonus() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        stubUserHabitClock(habit);
        HabitLog log = new HabitLog(HABIT_ID, USER_ID, TODAY, 10, 2);
        when(logs.findByHabitIdAndLogDate(HABIT_ID, TODAY)).thenReturn(Optional.of(log));
        when(points.balance(USER_ID)).thenReturn(12L);
        when(logs.findLogDates(HABIT_ID)).thenReturn(List.of());
        stubSummary();

        CheckInResult result = service.undo(USER_ID, HABIT_ID);

        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(points).record(txCaptor.capture());
        assertThat(txCaptor.getValue().getAmount()).isEqualTo(-12);
        assertThat(result.pointsChange()).isEqualTo(-12);
        assertThat(result.currentStreak()).isZero();
    }

    @Test
    void undo_balanceExactlyEqualsTotal_isAllowed() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        stubUserHabitClock(habit);
        HabitLog log = new HabitLog(HABIT_ID, USER_ID, TODAY, 10, 0);
        when(logs.findByHabitIdAndLogDate(HABIT_ID, TODAY)).thenReturn(Optional.of(log));
        when(points.balance(USER_ID)).thenReturn(10L);
        when(logs.findLogDates(HABIT_ID)).thenReturn(List.of());
        stubSummary();

        service.undo(USER_ID, HABIT_ID);

        verify(logs).delete(log);
    }

    @Test
    void undo_noCheckInToday_throwsNotFoundAndDeletesNothing() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        stubUserHabitClock(habit);
        when(logs.findByHabitIdAndLogDate(HABIT_ID, TODAY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.undo(USER_ID, HABIT_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(logs, never()).delete(any());
        verify(points, never()).record(any());
    }

    @Test
    void undo_pointsAlreadySpent_throwsConflictAndDeletesNothing() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        stubUserHabitClock(habit);
        HabitLog log = new HabitLog(HABIT_ID, USER_ID, TODAY, 10, 2);
        when(logs.findByHabitIdAndLogDate(HABIT_ID, TODAY)).thenReturn(Optional.of(log));
        when(points.balance(USER_ID)).thenReturn(11L);

        assertThatThrownBy(() -> service.undo(USER_ID, HABIT_ID))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).contains("already spent");
                });
        verify(logs, never()).delete(any());
        verify(points, never()).record(any());
    }

    @Test
    void undo_habitNotOwned_throwsNotFoundAndDeletesNothing() {
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(habits.findByIdAndUserId(HABIT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.undo(USER_ID, HABIT_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(logs, never()).delete(any());
        verify(points, never()).record(any());
    }

    @Test
    void undo_locksUserWithFindByIdForUpdateNotFindById() {
        Habit habit = activeHabit(Frequency.DAILY, 1, 10);
        stubUserHabitClock(habit);
        when(logs.findByHabitIdAndLogDate(HABIT_ID, TODAY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.undo(USER_ID, HABIT_ID)).isInstanceOf(ApiException.class);

        verify(users).findByIdForUpdate(USER_ID);
        verify(users, never()).findById(anyLong());
    }

    // ---------- today() ----------

    private void stubTodayBasics(List<Habit> habitList, List<HabitLog> logList) {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userClock.today(anyString())).thenReturn(TODAY);
        when(logs.findByUserId(USER_ID)).thenReturn(logList);
        when(habits.findByUserIdAndArchivedOrderByCreatedAtAsc(USER_ID, false)).thenReturn(habitList);
        stubSummary();
    }

    private static HabitLog logOn(LocalDate date) {
        return new HabitLog(HABIT_ID, USER_ID, date, 10, 0);
    }

    @Test
    void today_activeHabitNotDone_canCheckIn() {
        stubTodayBasics(List.of(activeHabit(Frequency.DAILY, 1, 10)), List.of());

        TodayResponse response = service.today(USER_ID);

        assertThat(response.date()).isEqualTo(TODAY);
        assertThat(response.points()).isEqualTo(SUMMARY);
        assertThat(response.habits()).hasSize(1);
        TodayHabit item = response.habits().get(0);
        assertThat(item.id()).isEqualTo(HABIT_ID);
        assertThat(item.status()).isEqualTo(HabitStatus.ACTIVE);
        assertThat(item.doneToday()).isFalse();
        assertThat(item.periodCount()).isZero();
        assertThat(item.canCheckIn()).isTrue();
    }

    @Test
    void today_habitDoneToday_doneTodayTrueAndCannotCheckIn() {
        stubTodayBasics(List.of(activeHabit(Frequency.DAILY, 1, 10)),
                List.of(logOn(LocalDate.of(2026, 10, 1)), logOn(TODAY)));

        TodayHabit item = service.today(USER_ID).habits().get(0);

        assertThat(item.doneToday()).isTrue();
        assertThat(item.canCheckIn()).isFalse();
        assertThat(item.currentStreak()).isEqualTo(2);
        assertThat(item.bestStreak()).isEqualTo(2);
    }

    @Test
    void today_weeklyTargetMetEarlierInWeek_notDoneTodayButCannotCheckIn() {
        stubTodayBasics(List.of(activeHabit(Frequency.WEEKLY, 2, 10)),
                List.of(logOn(LocalDate.of(2026, 9, 28)), logOn(LocalDate.of(2026, 9, 29))));

        TodayHabit item = service.today(USER_ID).habits().get(0);

        assertThat(item.doneToday()).isFalse();
        assertThat(item.periodCount()).isEqualTo(2);
        assertThat(item.canCheckIn()).isFalse();
    }

    @Test
    void today_weeklyTargetNotYetMet_canCheckInAndReportsPeriodCount() {
        stubTodayBasics(List.of(activeHabit(Frequency.WEEKLY, 2, 10)),
                List.of(logOn(LocalDate.of(2026, 9, 28))));

        TodayHabit item = service.today(USER_ID).habits().get(0);

        assertThat(item.periodCount()).isEqualTo(1);
        assertThat(item.canCheckIn()).isTrue();
    }

    @Test
    void today_habitStartingTomorrow_isUpcomingAndCannotCheckIn() {
        stubTodayBasics(List.of(habit(Frequency.DAILY, 1, 10, TODAY.plusDays(1), null)), List.of());

        TodayHabit item = service.today(USER_ID).habits().get(0);

        assertThat(item.status()).isEqualTo(HabitStatus.UPCOMING);
        assertThat(item.canCheckIn()).isFalse();
    }

    @Test
    void today_habitEndedYesterday_isCompletedAndCannotCheckIn() {
        stubTodayBasics(List.of(habit(Frequency.DAILY, 1, 10, LocalDate.of(2026, 9, 1), TODAY.minusDays(1))),
                List.of());

        TodayHabit item = service.today(USER_ID).habits().get(0);

        assertThat(item.status()).isEqualTo(HabitStatus.COMPLETED);
        assertThat(item.canCheckIn()).isFalse();
    }

    @Test
    void today_completedHabit_streakIsFrozenAtItsEndDate() {
        LocalDate end = LocalDate.of(2026, 9, 20);
        stubTodayBasics(List.of(habit(Frequency.DAILY, 1, 10, LocalDate.of(2026, 9, 1), end)),
                List.of(logOn(LocalDate.of(2026, 9, 19)), logOn(end)));

        TodayHabit item = service.today(USER_ID).habits().get(0);

        assertThat(item.status()).isEqualTo(HabitStatus.COMPLETED);
        assertThat(item.currentStreak()).isEqualTo(2);
    }

    @Test
    void today_habitEndingToday_isStillActive() {
        stubTodayBasics(List.of(habit(Frequency.DAILY, 1, 10, LocalDate.of(2026, 9, 1), TODAY)), List.of());

        TodayHabit item = service.today(USER_ID).habits().get(0);

        assertThat(item.status()).isEqualTo(HabitStatus.ACTIVE);
        assertThat(item.canCheckIn()).isTrue();
    }

    @Test
    void today_noHabits_returnsEmptyListWithSummaryAndDate() {
        stubTodayBasics(List.of(), List.of());

        TodayResponse response = service.today(USER_ID);

        assertThat(response.habits()).isEmpty();
        assertThat(response.points()).isEqualTo(SUMMARY);
        assertThat(response.date()).isEqualTo(TODAY);
    }

    @Test
    void today_unknownUser_throwsUnauthorized() {
        when(users.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.today(USER_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
