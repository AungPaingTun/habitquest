package com.habitquest.checkin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/** A habit done on one day. Stores the points it earned, so later edits to the habit don't rewrite history. */
@Entity
@Table(name = "habit_logs")
public class HabitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "habit_id", nullable = false, updatable = false)
    private Long habitId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "log_date", nullable = false, updatable = false)
    private LocalDate logDate;

    @Column(name = "points_awarded", nullable = false, updatable = false)
    private int pointsAwarded;

    @Column(name = "bonus_awarded", nullable = false, updatable = false)
    private int bonusAwarded;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected HabitLog() {
        // required by JPA
    }

    public HabitLog(Long habitId, Long userId, LocalDate logDate, int pointsAwarded, int bonusAwarded) {
        this.habitId = habitId;
        this.userId = userId;
        this.logDate = logDate;
        this.pointsAwarded = pointsAwarded;
        this.bonusAwarded = bonusAwarded;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getHabitId() {
        return habitId;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getLogDate() {
        return logDate;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }

    public int getBonusAwarded() {
        return bonusAwarded;
    }

    public int totalAwarded() {
        return pointsAwarded + bonusAwarded;
    }
}
