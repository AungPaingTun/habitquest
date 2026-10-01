package com.habitquest.prize.dto;

import com.habitquest.prize.Prize;

import java.time.Instant;

/** A prize plus how many times it's been redeemed ("Hotpot ×2"). */
public record PrizeResponse(
        Long id,
        String name,
        String icon,
        int cost,
        boolean archived,
        long redeemCount,
        Instant lastRedeemedAt,
        Instant createdAt
) {
    public static PrizeResponse from(Prize prize, long redeemCount, Instant lastRedeemedAt) {
        return new PrizeResponse(prize.getId(), prize.getName(), prize.getIcon(), prize.getCost(),
                prize.isArchived(), redeemCount, lastRedeemedAt, prize.getCreatedAt());
    }
}
