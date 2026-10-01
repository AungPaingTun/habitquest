package com.habitquest.habit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findByUserIdAndArchivedOrderByCreatedAtAsc(Long userId, boolean archived);

    /** Looks up a habit only if it belongs to the user, so nobody can read or edit someone else's habit. */
    Optional<Habit> findByIdAndUserId(Long id, Long userId);
}
