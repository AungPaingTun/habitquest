package com.habitquest.prize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "prizes")
public class Prize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    private String icon;

    /** Locked once created (PRD §5.5): no setter, and updatable = false so JPA never writes it again. */
    @Column(nullable = false, updatable = false)
    private int cost;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Prize() {
        // required by JPA
    }

    public Prize(Long userId, String name, String icon, int cost) {
        this.userId = userId;
        this.name = name;
        this.icon = icon;
        this.cost = cost;
        this.createdAt = Instant.now();
    }

    /** Name and icon can change; the cost can't. */
    public void rename(String name, String icon) {
        this.name = name;
        this.icon = icon;
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

    public int getCost() {
        return cost;
    }

    public boolean isArchived() {
        return archived;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
