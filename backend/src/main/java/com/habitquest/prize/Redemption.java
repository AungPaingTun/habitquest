package com.habitquest.prize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "redemptions")
public class Redemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prize_id", nullable = false, updatable = false)
    private Long prizeId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "cost_at_time", nullable = false, updatable = false)
    private int costAtTime;

    @Column(name = "redeemed_at", nullable = false, updatable = false)
    private Instant redeemedAt;

    protected Redemption() {
        // required by JPA
    }

    public Redemption(Long prizeId, Long userId, int costAtTime) {
        this.prizeId = prizeId;
        this.userId = userId;
        this.costAtTime = costAtTime;
        this.redeemedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPrizeId() {
        return prizeId;
    }

    public int getCostAtTime() {
        return costAtTime;
    }

    public Instant getRedeemedAt() {
        return redeemedAt;
    }
}
