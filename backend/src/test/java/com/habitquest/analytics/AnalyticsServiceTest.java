package com.habitquest.analytics;

import com.habitquest.analytics.dto.AnalyticsResponse;
import com.habitquest.analytics.dto.AnalyticsResponse.Bucket;
import com.habitquest.analytics.dto.AnalyticsResponse.HabitStats;
import com.habitquest.analytics.dto.AnalyticsResponse.PrizeStat;
import com.habitquest.analytics.dto.HeatmapResponse;
import com.habitquest.checkin.HabitLog;
import com.habitquest.checkin.HabitLogRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final Long USER_ID = 1L;
    private static final String ZONE = "Asia/Kuala_Lumpur"; // UTC+8
    // Friday; the week runs Mon 2026-09-28 .. Sun 2026-10-04.
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);
    private static final LocalDate LONG_AGO = LocalDate.of(2026, 1, 1);

    @Mock
    private UserRepository users;

    @Mock
    private HabitRepository habits;

    @Mock
    private HabitLogRepository logs;

    @Mock
    private PointTransactionRepository transactions;

    @Mock
    private RedemptionRepository redemptions;

    @Mock
    private PrizeRepository prizes;

    @Mock
    private UserClock userClock;

    @InjectMocks
    private AnalyticsService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("a@b.c", "hash", "A", ZONE);
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    // ---------- fixtures ----------

    private static Habit habit(long id, Frequency frequency, int target, LocalDate start, LocalDate end) {
        Habit habit = new Habit(USER_ID);
        habit.update("Habit " + id, "icon", 10, frequency, target, start, end);
        ReflectionTestUtils.setField(habit, "id", id);
        return habit;
    }

    private static HabitLog log(long habitId, LocalDate date) {
        return new HabitLog(habitId, USER_ID, date, 10, 0);
    }

    private static PointTransaction tx(TransactionType type, int amount, String utcInstant) {
        PointTransaction t = type == TransactionType.REDEEM
                ? PointTransaction.forRedemption(USER_ID, amount, 99L, "Redeemed")
                : PointTransaction.forHabit(USER_ID, amount, type, 1L, null, "x");
        ReflectionTestUtils.setField(t, "createdAt", Instant.parse(utcInstant));
        return t;
    }

    private static Redemption redemption(long prizeId, int cost, String utcInstant) {
        Redemption r = new Redemption(prizeId, USER_ID, cost);
        ReflectionTestUtils.setField(r, "redeemedAt", Instant.parse(utcInstant));
        return r;
    }

    private static Prize prize(long id, String name, int cost) {
        Prize p = new Prize(USER_ID, name, "gift", cost);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private void stubUserAndClock() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userClock.today(anyString())).thenReturn(TODAY);
    }

    /** Stubs everything analytics() reads; ledger/redemptions are returned whatever the time window. */
    private void stubData(List<Habit> habitList, List<HabitLog> logList, List<PointTransaction> ledger,
                          List<Redemption> redeemed) {
        stubUserAndClock();
        when(habits.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(habitList);
        when(logs.findByUserId(USER_ID)).thenReturn(logList);
        when(transactions.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(anyLong(), any(), any()))
                .thenReturn(ledger);
        when(redemptions.findByUserIdAndRedeemedAtGreaterThanEqualAndRedeemedAtLessThan(anyLong(), any(), any()))
                .thenReturn(redeemed);
    }

    private void stubData(List<Habit> habitList, List<HabitLog> logList) {
        stubData(habitList, logList, List.of(), List.of());
    }

    // ---------- expectedCheckIns ----------

    @Test
    void expectedCheckIns_dailyOverFiveDays_returnsFive() {
        Habit h = habit(1, Frequency.DAILY, 1, LONG_AGO, null);

        double expected = AnalyticsService.expectedCheckIns(h, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 2));

        assertThat(expected).isCloseTo(5.0, within(1e-9));
    }

    @Test
    void expectedCheckIns_weeklyTargetTwoOverFullWeek_returnsTwo() {
        Habit h = habit(1, Frequency.WEEKLY, 2, LONG_AGO, null);

        double expected = AnalyticsService.expectedCheckIns(h, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4));

        assertThat(expected).isCloseTo(2.0, within(1e-9));
    }

    @Test
    void expectedCheckIns_weeklyTargetTwoOverThreeDays_returnsSixSevenths() {
        Habit h = habit(1, Frequency.WEEKLY, 2, LONG_AGO, null);

        double expected = AnalyticsService.expectedCheckIns(h, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 30));

        assertThat(expected).isCloseTo(6.0 / 7.0, within(1e-9));
    }

    @Test
    void expectedCheckIns_monthlyTargetThirtyOverAllFebruary_isCappedAtMonthLength() {
        Habit h = habit(1, Frequency.MONTHLY, 30, LONG_AGO, null);

        double expected = AnalyticsService.expectedCheckIns(h, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

        assertThat(expected).isCloseTo(28.0, within(1e-9));
    }

    @Test
    void expectedCheckIns_monthlyTargetThirtyOverAllApril_returnsThirty() {
        Habit h = habit(1, Frequency.MONTHLY, 30, LONG_AGO, null);

        double expected = AnalyticsService.expectedCheckIns(h, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));

        assertThat(expected).isCloseTo(30.0, within(1e-9));
    }

    @Test
    void expectedCheckIns_habitStartsMidRange_countsOnlyFromStartDate() {
        Habit h = habit(1, Frequency.DAILY, 1, LocalDate.of(2026, 9, 30), null);

        double expected = AnalyticsService.expectedCheckIns(h, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4));

        assertThat(expected).isCloseTo(5.0, within(1e-9)); // 09-30 .. 10-04
    }

    @Test
    void expectedCheckIns_habitEndsMidRange_countsOnlyUpToEndDate() {
        Habit h = habit(1, Frequency.DAILY, 1, LONG_AGO, LocalDate.of(2026, 10, 1));

        double expected = AnalyticsService.expectedCheckIns(h, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4));

        assertThat(expected).isCloseTo(4.0, within(1e-9)); // 09-28 .. 10-01
    }

    @Test
    void expectedCheckIns_habitStartsAfterRangeEnd_returnsZero() {
        Habit h = habit(1, Frequency.DAILY, 1, LocalDate.of(2026, 10, 10), null);

        double expected = AnalyticsService.expectedCheckIns(h, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4));

        assertThat(expected).isZero();
    }

    @Test
    void expectedCheckIns_habitEndedBeforeRangeStart_returnsZero() {
        Habit h = habit(1, Frequency.DAILY, 1, LONG_AGO, LocalDate.of(2026, 9, 1));

        double expected = AnalyticsService.expectedCheckIns(h, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4));

        assertThat(expected).isZero();
    }

    // ---------- completionRate ----------

    @Test
    void completionRate_nothingExpected_returnsNull() {
        assertThat(AnalyticsService.completionRate(3, 0)).isNull();
    }

    @Test
    void completionRate_moreDoneThanExpected_isCappedAt100() {
        assertThat(AnalyticsService.completionRate(9, 3.0)).isEqualTo(100);
    }

    @Test
    void completionRate_twoOfThree_roundsTo67() {
        assertThat(AnalyticsService.completionRate(2, 3.0)).isEqualTo(67);
    }

    @Test
    void completionRate_noneDone_returnsZero() {
        assertThat(AnalyticsService.completionRate(0, 4.0)).isEqualTo(0);
    }

    // ---------- analytics: windows ----------

    @Test
    void analytics_currentWeek_returnsMondayToSundayAndToday() {
        stubData(List.of(), List.of());

        AnalyticsResponse r = service.analytics(USER_ID, Period.WEEK, null);

        assertThat(r.period()).isEqualTo(Period.WEEK);
        assertThat(r.start()).isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(r.end()).isEqualTo(LocalDate.of(2026, 10, 4));
        assertThat(r.today()).isEqualTo(TODAY);
    }

    @Test
    void analytics_anchorInPastMonth_returnsThatMonthsBounds() {
        stubData(List.of(), List.of());

        AnalyticsResponse r = service.analytics(USER_ID, Period.MONTH, LocalDate.of(2026, 8, 15));

        assertThat(r.start()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(r.end()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(r.series()).hasSize(31);
        assertThat(r.series()).noneMatch(Bucket::future);
    }

    @Test
    void analytics_week_queriesLedgerWithUserZoneBoundaries() {
        stubData(List.of(), List.of());

        service.analytics(USER_ID, Period.WEEK, null);

        // Mon 09-28 00:00 KL = 09-27T16:00Z ; next Mon 10-05 00:00 KL = 10-04T16:00Z
        verify(transactions).findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                USER_ID, Instant.parse("2026-09-27T16:00:00Z"), Instant.parse("2026-10-04T16:00:00Z"));
        verify(redemptions).findByUserIdAndRedeemedAtGreaterThanEqualAndRedeemedAtLessThan(
                USER_ID, Instant.parse("2026-09-27T16:00:00Z"), Instant.parse("2026-10-04T16:00:00Z"));
    }

    @Test
    void analytics_unknownUser_throwsUnauthorized() {
        when(users.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analytics(USER_ID, Period.WEEK, null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // ---------- analytics: summary & habits ----------

    @Test
    void analytics_currentWeekDailyHabit_expectsOnlyDaysUpToToday() {
        Habit daily = habit(1, Frequency.DAILY, 1, LONG_AGO, null);
        stubData(List.of(daily), List.of(log(1, LocalDate.of(2026, 9, 28)), log(1, LocalDate.of(2026, 9, 29))));

        AnalyticsResponse r = service.analytics(USER_ID, Period.WEEK, null);

        HabitStats stats = r.habits().get(0);
        assertThat(stats.expected()).isEqualTo(5.0);
        assertThat(stats.checkIns()).isEqualTo(2);
        assertThat(stats.completionRate()).isEqualTo(40);
        assertThat(r.summary().completionRate()).isEqualTo(40);
    }

    @Test
    void analytics_overDeliveringHabit_cannotHideAnotherHabitsShortfall() {
        Habit daily = habit(1, Frequency.DAILY, 1, LONG_AGO, null);          // expects 5, does 2
        Habit weekly = habit(2, Frequency.WEEKLY, 2, LONG_AGO, null);        // expects 10/7, does 3
        stubData(List.of(daily, weekly), List.of(
                log(1, LocalDate.of(2026, 9, 28)), log(1, LocalDate.of(2026, 9, 29)),
                log(2, LocalDate.of(2026, 9, 29)), log(2, LocalDate.of(2026, 9, 30)), log(2, LocalDate.of(2026, 10, 1))));

        AnalyticsResponse r = service.analytics(USER_ID, Period.WEEK, null);

        // capped: (2 + 10/7) / (5 + 10/7) = 53%  (uncapped would be 5 / 6.43 = 78%)
        assertThat(r.summary().checkIns()).isEqualTo(5);
        assertThat(r.summary().completionRate()).isEqualTo(53);
        assertThat(r.habits()).extracting(HabitStats::completionRate).containsExactly(40, 100);
    }

    @Test
    void analytics_activeDays_countsDistinctDaysAcrossHabits() {
        Habit a = habit(1, Frequency.DAILY, 1, LONG_AGO, null);
        Habit b = habit(2, Frequency.DAILY, 1, LONG_AGO, null);
        stubData(List.of(a, b), List.of(
                log(1, LocalDate.of(2026, 9, 28)), log(1, LocalDate.of(2026, 9, 29)),
                log(2, LocalDate.of(2026, 9, 29)), log(2, LocalDate.of(2026, 10, 1))));

        AnalyticsResponse r = service.analytics(USER_ID, Period.WEEK, null);

        assertThat(r.summary().checkIns()).isEqualTo(4);
        assertThat(r.summary().activeDays()).isEqualTo(3);
    }

    @Test
    void analytics_logsOutsidePeriod_areNotCounted() {
        Habit daily = habit(1, Frequency.DAILY, 1, LONG_AGO, null);
        stubData(List.of(daily), List.of(
                log(1, LocalDate.of(2026, 9, 27)), log(1, LocalDate.of(2026, 10, 5)), log(1, LocalDate.of(2026, 9, 30))));

        AnalyticsResponse r = service.analytics(USER_ID, Period.WEEK, null);

        assertThat(r.summary().checkIns()).isEqualTo(1);
        assertThat(r.summary().activeDays()).isEqualTo(1);
    }

    @Test
    void analytics_noHabits_completionRateIsNull() {
        stubData(List.of(), List.of());

        AnalyticsResponse r = service.analytics(USER_ID, Period.WEEK, null);

        assertThat(r.summary().completionRate()).isNull();
        assertThat(r.summary().checkIns()).isZero();
        assertThat(r.habits()).isEmpty();
        assertThat(r.topPrizes()).isEmpty();
    }

    @Test
    void analytics_archivedHabitWithoutCheckInsInPeriod_isExcluded() {
        Habit archived = habit(1, Frequency.DAILY, 1, LONG_AGO, null);
        archived.setArchived(true);
        stubData(List.of(archived), List.of(log(1, LocalDate.of(2026, 9, 10))));

        AnalyticsResponse r = service.analytics(USER_ID, Period.WEEK, null);

        assertThat(r.habits()).isEmpty();
        assertThat(r.summary().completionRate()).isNull();
    }

    @Test
    void analytics_archivedHabitWithCheckInsInPeriod_isIncludedAndFlagged() {
        Habit archived = habit(1, Frequency.DAILY, 1, LONG_AGO, null);
        archived.setArchived(true);
        stubData(List.of(archived), List.of(log(1, LocalDate.of(2026, 9, 29))));

        AnalyticsResponse r = service.analytics(USER_ID, Period.WEEK, null);

        assertThat(r.habits()).hasSize(1);
        assertThat(r.habits().get(0).archived()).isTrue();
        assertThat(r.habits().get(0).checkIns()).isEqualTo(1);
        assertThat(r.summary().checkIns()).isEqualTo(1);
    }

    @Test
    void analytics_habitNotYetStarted_isExcludedWhenNoCheckIns() {
        Habit future = habit(1, Frequency.DAILY, 1, LocalDate.of(2026, 10, 3), null);
        stubData(List.of(future), List.of());

        AnalyticsResponse r = service.analytics(USER_ID, Period.WEEK, null);

        assertThat(r.habits()).isEmpty();
    }

    // ---------- analytics: points ----------

    @Test
    void analytics_points_earnedIncludesBonusAndNegativeUndo_spentIsPositive() {
        stubData(List.of(), List.of(), List.of(
                tx(TransactionType.EARN, 10, "2026-09-29T02:00:00Z"),
                tx(TransactionType.STREAK_BONUS, 5, "2026-09-29T02:00:00Z"),
                tx(TransactionType.UNDO, -10, "2026-09-30T02:00:00Z"),
                tx(TransactionType.REDEEM, 20, "2026-10-01T02:00:00Z")),
                List.of(redemption(7, 20, "2026-10-01T02:00:00Z")));

        AnalyticsResponse r = service.analytics(USER_ID, Period.WEEK, null);

        assertThat(r.summary().pointsEarned()).isEqualTo(5);
        assertThat(r.summary().pointsSpent()).isEqualTo(20);
        assertThat(r.summary().redemptions()).isEqualTo(1);
    }

    // ---------- analytics: series ----------

    @Test
    void analytics_weekSeries_hasSevenDayBucketsWithWeekendMarkedFuture() {
        stubData(List.of(), List.of());

        List<Bucket> series = service.analytics(USER_ID, Period.WEEK, null).series();

        assertThat(series).extracting(Bucket::key).containsExactly(
                "2026-09-28", "2026-09-29", "2026-09-30", "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04");
        assertThat(series).extracting(Bucket::future)
                .containsExactly(false, false, false, false, false, true, true);
    }

    @Test
    void analytics_weekSeries_placesPointsAndCheckInsOnTheRightDayInUserZone() {
        Habit daily = habit(1, Frequency.DAILY, 1, LONG_AGO, null);
        stubData(List.of(daily),
                List.of(log(1, LocalDate.of(2026, 9, 28)), log(1, LocalDate.of(2026, 10, 1)), log(1, LocalDate.of(2026, 10, 2))),
                List.of(
                        // 17:00Z on 09-30 is 01:00 on 10-01 in KL
                        tx(TransactionType.EARN, 10, "2026-09-30T17:00:00Z"),
                        tx(TransactionType.STREAK_BONUS, 5, "2026-09-30T17:00:00Z"),
                        // 15:59Z on 09-30 is still 23:59 on 09-30 in KL
                        tx(TransactionType.EARN, 7, "2026-09-30T15:59:00Z"),
                        tx(TransactionType.UNDO, -10, "2026-09-28T02:00:00Z"),
                        // 20:00Z on 10-01 is 04:00 on 10-02 in KL
                        tx(TransactionType.REDEEM, 20, "2026-10-01T20:00:00Z")),
                List.of());

        List<Bucket> series = service.analytics(USER_ID, Period.WEEK, null).series();

        assertThat(series.get(0)).isEqualTo(new Bucket("2026-09-28", -10, 0, 1, false));
        assertThat(series.get(2)).isEqualTo(new Bucket("2026-09-30", 7, 0, 0, false));
        assertThat(series.get(3)).isEqualTo(new Bucket("2026-10-01", 15, 0, 1, false));
        assertThat(series.get(4)).isEqualTo(new Bucket("2026-10-02", 0, 20, 1, false));
        assertThat(series.get(5)).isEqualTo(new Bucket("2026-10-03", 0, 0, 0, true));
    }

    @Test
    void analytics_year_hasTwelveMonthBucketsWithMonthsAfterOctoberFuture() {
        stubData(List.of(), List.of());

        List<Bucket> series = service.analytics(USER_ID, Period.YEAR, null).series();

        assertThat(series).extracting(Bucket::key).containsExactly(
                "2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06",
                "2026-07", "2026-08", "2026-09", "2026-10", "2026-11", "2026-12");
        assertThat(series).extracting(Bucket::future).containsExactly(
                false, false, false, false, false, false, false, false, false, false, true, true);
    }

    @Test
    void analytics_yearSeries_aggregatesCheckInsAndPointsPerMonth() {
        Habit daily = habit(1, Frequency.DAILY, 1, LONG_AGO, null);
        stubData(List.of(daily),
                List.of(log(1, LocalDate.of(2026, 3, 1)), log(1, LocalDate.of(2026, 3, 31)), log(1, LocalDate.of(2026, 10, 1))),
                List.of(
                        tx(TransactionType.EARN, 10, "2026-03-01T02:00:00Z"),
                        tx(TransactionType.EARN, 10, "2026-03-31T02:00:00Z"),
                        // 16:00Z on 03-31 is 00:00 on 04-01 in KL
                        tx(TransactionType.EARN, 3, "2026-03-31T16:00:00Z"),
                        tx(TransactionType.REDEEM, 15, "2026-10-01T02:00:00Z")),
                List.of());

        List<Bucket> series = service.analytics(USER_ID, Period.YEAR, null).series();

        assertThat(series.get(2)).isEqualTo(new Bucket("2026-03", 20, 0, 2, false));
        assertThat(series.get(3)).isEqualTo(new Bucket("2026-04", 3, 0, 0, false));
        assertThat(series.get(9)).isEqualTo(new Bucket("2026-10", 0, 15, 1, false));
    }

    // ---------- analytics: top prizes ----------

    @Test
    void topPrizes_groupsByPrizeSortsByCountDescAndSumsCostAtTime() {
        List<Redemption> redeemed = List.of(
                redemption(1, 50, "2026-09-29T02:00:00Z"),
                redemption(2, 30, "2026-09-29T02:00:00Z"),
                redemption(2, 40, "2026-09-30T02:00:00Z"),   // cost changed? sum uses cost_at_time
                redemption(2, 30, "2026-10-01T02:00:00Z"),
                redemption(1, 50, "2026-10-01T02:00:00Z"));
        stubData(List.of(), List.of(), List.of(), redeemed);
        when(prizes.findAllById(any())).thenReturn(List.of(prize(1, "Movie", 50), prize(2, "Coffee", 30)));

        List<PrizeStat> top = service.analytics(USER_ID, Period.WEEK, null).topPrizes();

        assertThat(top).containsExactly(
                new PrizeStat(2L, "Coffee", "gift", 3, 100),
                new PrizeStat(1L, "Movie", "gift", 2, 100));
    }

    @Test
    void topPrizes_moreThanFivePrizes_returnsOnlyTopFive() {
        stubData(List.of(), List.of(), List.of(), List.of(
                redemption(1, 10, "2026-09-29T02:00:00Z"), redemption(1, 10, "2026-09-29T02:00:00Z"),
                redemption(1, 10, "2026-09-29T02:00:00Z"), redemption(1, 10, "2026-09-29T02:00:00Z"),
                redemption(1, 10, "2026-09-29T02:00:00Z"), redemption(1, 10, "2026-09-29T02:00:00Z"),
                redemption(2, 10, "2026-09-29T02:00:00Z"), redemption(2, 10, "2026-09-29T02:00:00Z"),
                redemption(2, 10, "2026-09-29T02:00:00Z"), redemption(2, 10, "2026-09-29T02:00:00Z"),
                redemption(2, 10, "2026-09-29T02:00:00Z"),
                redemption(3, 10, "2026-09-29T02:00:00Z"), redemption(3, 10, "2026-09-29T02:00:00Z"),
                redemption(3, 10, "2026-09-29T02:00:00Z"), redemption(3, 10, "2026-09-29T02:00:00Z"),
                redemption(4, 10, "2026-09-29T02:00:00Z"), redemption(4, 10, "2026-09-29T02:00:00Z"),
                redemption(4, 10, "2026-09-29T02:00:00Z"),
                redemption(5, 10, "2026-09-29T02:00:00Z"), redemption(5, 10, "2026-09-29T02:00:00Z"),
                redemption(6, 10, "2026-09-29T02:00:00Z")));
        when(prizes.findAllById(any())).thenReturn(List.of(
                prize(1, "P1", 10), prize(2, "P2", 10), prize(3, "P3", 10),
                prize(4, "P4", 10), prize(5, "P5", 10), prize(6, "P6", 10)));

        List<PrizeStat> top = service.analytics(USER_ID, Period.WEEK, null).topPrizes();

        assertThat(top).extracting(PrizeStat::name).containsExactly("P1", "P2", "P3", "P4", "P5");
        assertThat(top).extracting(PrizeStat::count).containsExactly(6, 5, 4, 3, 2);
    }

    @Test
    void topPrizes_prizeRowMissing_isSkipped() {
        stubData(List.of(), List.of(), List.of(), List.of(
                redemption(1, 10, "2026-09-29T02:00:00Z"), redemption(2, 10, "2026-09-29T02:00:00Z")));
        when(prizes.findAllById(any())).thenReturn(List.of(prize(2, "Coffee", 10)));

        List<PrizeStat> top = service.analytics(USER_ID, Period.WEEK, null).topPrizes();

        assertThat(top).extracting(PrizeStat::id).containsExactly(2L);
    }

    // ---------- heatmap ----------

    @Test
    void heatmap_countsPerDayOnlyDaysWithCheckInsInDateOrderAndMaxCount() {
        stubUserAndClock();
        when(logs.findByUserIdAndLogDateBetween(USER_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(
                        log(1, LocalDate.of(2026, 5, 3)),
                        log(1, LocalDate.of(2026, 2, 1)),
                        log(2, LocalDate.of(2026, 5, 3)),
                        log(3, LocalDate.of(2026, 5, 3))));

        HeatmapResponse r = service.heatmap(USER_ID, null);

        assertThat(r.year()).isEqualTo(2026);
        assertThat(r.today()).isEqualTo(TODAY);
        assertThat(r.maxCount()).isEqualTo(3);
        assertThat(r.days()).containsExactly(
                new HeatmapResponse.Day(LocalDate.of(2026, 2, 1), 1),
                new HeatmapResponse.Day(LocalDate.of(2026, 5, 3), 3));
    }

    @Test
    void heatmap_explicitYear_queriesThatYear() {
        stubUserAndClock();
        when(logs.findByUserIdAndLogDateBetween(eq(USER_ID), eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2025, 12, 31))))
                .thenReturn(List.of(log(1, LocalDate.of(2025, 12, 31))));

        HeatmapResponse r = service.heatmap(USER_ID, 2025);

        assertThat(r.year()).isEqualTo(2025);
        assertThat(r.days()).containsExactly(new HeatmapResponse.Day(LocalDate.of(2025, 12, 31), 1));
    }

    @Test
    void heatmap_noLogs_returnsEmptyDaysAndZeroMax() {
        stubUserAndClock();
        when(logs.findByUserIdAndLogDateBetween(anyLong(), any(), any())).thenReturn(List.of());

        HeatmapResponse r = service.heatmap(USER_ID, null);

        assertThat(r.days()).isEmpty();
        assertThat(r.maxCount()).isZero();
    }

    @Test
    void heatmap_unknownUser_throwsUnauthorized() {
        when(users.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.heatmap(USER_ID, null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
