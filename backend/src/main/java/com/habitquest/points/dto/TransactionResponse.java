package com.habitquest.points.dto;

import com.habitquest.points.PointTransaction;
import com.habitquest.points.TransactionType;

import java.time.Instant;

public record TransactionResponse(Long id, int amount, TransactionType type, String description, Instant createdAt) {

    public static TransactionResponse from(PointTransaction t) {
        return new TransactionResponse(t.getId(), t.getAmount(), t.getType(), t.getDescription(), t.getCreatedAt());
    }
}
