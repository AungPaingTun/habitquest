package com.habitquest.prize.dto;

import com.habitquest.points.dto.PointsSummary;

public record RedeemResult(PrizeResponse prize, PointsSummary points) {
}
