package com.habitquest.habit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findByUserIdAndArchivedOrderByCreatedAtAsc(Long userId, boolean archived);

    /** All habits including archived ones (analytics still counts their past check-ins). */
    List<Habit> findByUserIdOrderByCreatedAtAsc(Long userId);

    /** Looks up a habit only if it belongs to the user, so nobody can read or edit someone else's habit. */
    Optional<Habit> findByIdAndUserId(Long id, Long userId);

    // These read habit_logs through JPQL instead of importing the checkin package, so the habit package
    // doesn't depend on checkin (checkin already depends on habit).

    @Query("select count(l) > 0 from HabitLog l where l.habitId = :habitId")
    boolean hasCheckIns(Long habitId);

    @Query("select distinct l.habitId from HabitLog l where l.userId = :userId")
    List<Long> findHabitIdsWithCheckIns(Long userId);
}
