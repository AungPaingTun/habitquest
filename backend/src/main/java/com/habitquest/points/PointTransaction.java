package com.habitquest.points;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/** One row in the points ledger. Rows are only ever added, never changed or deleted. */
@Entity
@Table(name = "point_transactions")
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TransactionType type;

    @Column(name = "habit_id", updatable = false)
    private Long habitId;

    @Column(name = "log_date", updatable = false)
    private LocalDate logDate;

    @Column(name = "redemption_id", updatable = false)
    private Long redemptionId;

    @Column(nullable = false, updatable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PointTransaction() {
        // required by JPA
    }

    private PointTransaction(Long userId, int amount, TransactionType type, Long habitId, LocalDate logDate,
                             Long redemptionId, String description) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.habitId = habitId;
        this.logDate = logDate;
        this.redemptionId = redemptionId;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public static PointTransaction forHabit(Long userId, int amount, TransactionType type, Long habitId,
                                            LocalDate logDate, String description) {
        return new PointTransaction(userId, amount, type, habitId, logDate, null, description);
    }

    public static PointTransaction forRedemption(Long userId, int cost, Long redemptionId, String description) {
        return new PointTransaction(userId, -cost, TransactionType.REDEEM, null, null, redemptionId, description);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public int getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public Long getHabitId() {
        return habitId;
    }

    public LocalDate getLogDate() {
        return logDate;
    }

    public Long getRedemptionId() {
        return redemptionId;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
