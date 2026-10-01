package com.habitquest.prize;

import com.habitquest.common.ApiException;
import com.habitquest.points.PointTransaction;
import com.habitquest.points.PointsService;
import com.habitquest.points.TransactionType;
import com.habitquest.points.dto.PointsSummary;
import com.habitquest.prize.RedemptionRepository.PrizeStats;
import com.habitquest.prize.dto.PrizeCreateRequest;
import com.habitquest.prize.dto.PrizeResponse;
import com.habitquest.prize.dto.PrizeUpdateRequest;
import com.habitquest.prize.dto.RedeemResult;
import com.habitquest.user.User;
import com.habitquest.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrizeServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PRIZE_ID = 20L;
    private static final Long REDEMPTION_ID = 77L;
    private static final PointsSummary SUMMARY = new PointsSummary(60, 300, 2, 200, 450);

    @Mock
    private PrizeRepository prizes;

    @Mock
    private RedemptionRepository redemptions;

    @Mock
    private UserRepository users;

    @Mock
    private PointsService points;

    @InjectMocks
    private PrizeService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("a@b.c", "hash", "A", "Asia/Yangon");
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    // ---------- fixtures ----------

    private static Prize prize(Long id, String name, String icon, int cost) {
        Prize prize = new Prize(USER_ID, name, icon, cost);
        ReflectionTestUtils.setField(prize, "id", id);
        return prize;
    }

    private static PrizeStats stats(Long prizeId, long count, Instant last) {
        return new PrizeStats() {
            @Override
            public Long getPrizeId() {
                return prizeId;
            }

            @Override
            public long getCount() {
                return count;
            }

            @Override
            public Instant getLastRedeemedAt() {
                return last;
            }
        };
    }

    private void stubOwnedPrize(Prize prize) {
        when(prizes.findByIdAndUserId(prize.getId(), USER_ID)).thenReturn(Optional.of(prize));
    }

    private void stubSavedRedemption() {
        when(redemptions.save(any(Redemption.class))).thenAnswer(inv -> {
            Redemption r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", REDEMPTION_ID);
            return r;
        });
    }

    // ---------- create ----------

    @Test
    void create_validRequest_returnsPrizeWithZeroRedeemCount() {
        when(prizes.save(any(Prize.class))).thenAnswer(inv -> {
            Prize p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", PRIZE_ID);
            return p;
        });

        PrizeResponse result = service.create(USER_ID, new PrizeCreateRequest("Hotpot", "🍲", 150));

        assertThat(result.id()).isEqualTo(PRIZE_ID);
        assertThat(result.name()).isEqualTo("Hotpot");
        assertThat(result.icon()).isEqualTo("🍲");
        assertThat(result.cost()).isEqualTo(150);
        assertThat(result.archived()).isFalse();
        assertThat(result.redeemCount()).isZero();
        assertThat(result.lastRedeemedAt()).isNull();
        ArgumentCaptor<Prize> saved = ArgumentCaptor.forClass(Prize.class);
        verify(prizes).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
    }

    // ---------- list ----------

    @Test
    void list_withRedemptions_attachesCountAndLastRedeemedAtPerPrize() {
        Instant last = Instant.parse("2026-10-01T10:00:00Z");
        Prize hotpot = prize(20L, "Hotpot", "🍲", 100);
        Prize movie = prize(21L, "Movie", null, 200);
        when(prizes.findByUserIdAndArchivedOrderByCostAscIdAsc(USER_ID, false)).thenReturn(List.of(hotpot, movie));
        when(redemptions.statsByUser(USER_ID)).thenReturn(List.of(stats(20L, 2, last)));

        List<PrizeResponse> result = service.list(USER_ID, false);

        assertThat(result).extracting(PrizeResponse::id).containsExactly(20L, 21L);
        assertThat(result.get(0).redeemCount()).isEqualTo(2);
        assertThat(result.get(0).lastRedeemedAt()).isEqualTo(last);
        assertThat(result.get(1).redeemCount()).isZero();
        assertThat(result.get(1).lastRedeemedAt()).isNull();
    }

    @Test
    void list_noPrizes_returnsEmptyList() {
        when(prizes.findByUserIdAndArchivedOrderByCostAscIdAsc(USER_ID, true)).thenReturn(List.of());

        assertThat(service.list(USER_ID, true)).isEmpty();
    }

    // ---------- update ----------

    @Test
    void update_newNameAndIcon_renamesButKeepsCostLocked() {
        Prize prize = prize(PRIZE_ID, "Hotpot", "🍲", 150);
        stubOwnedPrize(prize);

        PrizeResponse result = service.update(USER_ID, PRIZE_ID, new PrizeUpdateRequest("Korean BBQ", "🥩"));

        assertThat(result.name()).isEqualTo("Korean BBQ");
        assertThat(result.icon()).isEqualTo("🥩");
        assertThat(result.cost()).isEqualTo(150);
        assertThat(prize.getName()).isEqualTo("Korean BBQ");
        assertThat(prize.getIcon()).isEqualTo("🥩");
        assertThat(prize.getCost()).isEqualTo(150);
    }

    @Test
    void update_prizeNotOwned_throws404() {
        when(prizes.findByIdAndUserId(PRIZE_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(USER_ID, PRIZE_ID, new PrizeUpdateRequest("X", null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---------- setArchived ----------

    @Test
    void setArchived_true_archivesPrizeAndReturnsArchivedResponse() {
        Prize prize = prize(PRIZE_ID, "Hotpot", null, 150);
        stubOwnedPrize(prize);

        PrizeResponse result = service.setArchived(USER_ID, PRIZE_ID, true);

        assertThat(prize.isArchived()).isTrue();
        assertThat(result.archived()).isTrue();
    }

    @Test
    void setArchived_false_restoresArchivedPrize() {
        Prize prize = prize(PRIZE_ID, "Hotpot", null, 150);
        prize.setArchived(true);
        stubOwnedPrize(prize);

        PrizeResponse result = service.setArchived(USER_ID, PRIZE_ID, false);

        assertThat(prize.isArchived()).isFalse();
        assertThat(result.archived()).isFalse();
    }

    @Test
    void setArchived_prizeNotOwned_throws404() {
        when(prizes.findByIdAndUserId(PRIZE_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setArchived(USER_ID, PRIZE_ID, true))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---------- redeem ----------

    @Test
    void redeem_balanceAboveCost_savesRedemptionRecordsNegativeLedgerRowAndReturnsSummary() {
        Prize prize = prize(PRIZE_ID, "Hotpot", "🍲", 100);
        Instant last = Instant.parse("2026-10-02T08:00:00Z");
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        stubOwnedPrize(prize);
        when(points.balance(USER_ID)).thenReturn(160L);
        stubSavedRedemption();
        when(redemptions.statsByUser(USER_ID)).thenReturn(List.of(stats(PRIZE_ID, 1, last)));
        when(points.summary(USER_ID)).thenReturn(SUMMARY);

        RedeemResult result = service.redeem(USER_ID, PRIZE_ID);

        ArgumentCaptor<Redemption> redemption = ArgumentCaptor.forClass(Redemption.class);
        verify(redemptions).save(redemption.capture());
        assertThat(redemption.getValue().getPrizeId()).isEqualTo(PRIZE_ID);
        assertThat(redemption.getValue().getCostAtTime()).isEqualTo(100);

        ArgumentCaptor<PointTransaction> tx = ArgumentCaptor.forClass(PointTransaction.class);
        verify(points).record(tx.capture());
        assertThat(tx.getValue().getType()).isEqualTo(TransactionType.REDEEM);
        assertThat(tx.getValue().getAmount()).isEqualTo(-100);
        assertThat(tx.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(tx.getValue().getRedemptionId()).isEqualTo(REDEMPTION_ID);
        assertThat(tx.getValue().getDescription()).contains("Hotpot");

        assertThat(result.points()).isEqualTo(SUMMARY);
        assertThat(result.prize().id()).isEqualTo(PRIZE_ID);
        assertThat(result.prize().redeemCount()).isEqualTo(1);
        assertThat(result.prize().lastRedeemedAt()).isEqualTo(last);
    }

    @Test
    void redeem_balanceExactlyEqualsCost_succeeds() {
        Prize prize = prize(PRIZE_ID, "Hotpot", null, 100);
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        stubOwnedPrize(prize);
        when(points.balance(USER_ID)).thenReturn(100L);
        stubSavedRedemption();
        when(points.summary(USER_ID)).thenReturn(new PointsSummary(0, 300, 2, 200, 450));

        RedeemResult result = service.redeem(USER_ID, PRIZE_ID);

        ArgumentCaptor<PointTransaction> tx = ArgumentCaptor.forClass(PointTransaction.class);
        verify(points).record(tx.capture());
        assertThat(tx.getValue().getAmount()).isEqualTo(-100);
        assertThat(result.points().balance()).isZero();
    }

    @Test
    void redeem_balanceOnePointShort_throws409AndSavesNothing() {
        Prize prize = prize(PRIZE_ID, "Hotpot", null, 100);
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        stubOwnedPrize(prize);
        when(points.balance(USER_ID)).thenReturn(99L);

        assertThatThrownBy(() -> service.redeem(USER_ID, PRIZE_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("need 1 more");

        verify(redemptions, never()).save(any());
        verify(points, never()).record(any());
    }

    @Test
    void redeem_balanceBelowCost_throws409WithMissingAmountAndSavesNothing() {
        Prize prize = prize(PRIZE_ID, "Hotpot", null, 100);
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        stubOwnedPrize(prize);
        when(points.balance(USER_ID)).thenReturn(63L);

        assertThatThrownBy(() -> service.redeem(USER_ID, PRIZE_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("need 37 more");

        verify(redemptions, never()).save(any());
        verify(points, never()).record(any());
    }

    @Test
    void redeem_archivedPrize_throws409AndSavesNothing() {
        Prize prize = prize(PRIZE_ID, "Hotpot", null, 100);
        prize.setArchived(true);
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        stubOwnedPrize(prize);

        assertThatThrownBy(() -> service.redeem(USER_ID, PRIZE_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("Archived");

        verify(redemptions, never()).save(any());
        verify(points, never()).record(any());
    }

    @Test
    void redeem_prizeNotOwned_throws404AndSavesNothing() {
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(prizes.findByIdAndUserId(PRIZE_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeem(USER_ID, PRIZE_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(redemptions, never()).save(any());
        verify(points, never()).record(any());
    }

    @Test
    void redeem_locksUserRowBeforeReadingBalance() {
        Prize prize = prize(PRIZE_ID, "Hotpot", null, 100);
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        stubOwnedPrize(prize);
        when(points.balance(USER_ID)).thenReturn(100L);
        stubSavedRedemption();
        when(points.summary(USER_ID)).thenReturn(SUMMARY);

        service.redeem(USER_ID, PRIZE_ID);

        InOrder order = inOrder(users, points);
        order.verify(users).findByIdForUpdate(USER_ID);
        order.verify(points).balance(USER_ID);
        order.verify(points).record(any(PointTransaction.class));
    }

    @Test
    void redeem_unknownUser_throws401AndTouchesNothingElse() {
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeem(USER_ID, PRIZE_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verifyNoInteractions(prizes, redemptions, points);
    }
}
