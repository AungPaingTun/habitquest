package com.habitquest.habit;

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

@Entity
@Table(name = "habits")
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stored as a plain id rather than a @ManyToOne User: we only ever need it for "is this my habit?" checks.
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    private String icon;

    @Column(nullable = false)
    private int points;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Habit() {
        // required by JPA
    }

    public Habit(Long userId) {
        this.userId = userId;
        this.createdAt = Instant.now();
    }

    /** Applies all editable fields at once; HabitService validates the values before calling this. */
    public void update(String name, String icon, int points, Frequency frequency, int targetCount,
                       LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.icon = icon;
        this.points = points;
        this.frequency = frequency;
        this.targetCount = targetCount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public int getPoints() {
        return points;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isArchived() {
        return archived;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
