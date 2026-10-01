package com.habitquest.checkin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {

    Optional<HabitLog> findByHabitIdAndLogDate(Long habitId, LocalDate logDate);

    /** True if a streak bonus was already paid for this habit in the given date range (one period). */
    boolean existsByHabitIdAndLogDateBetweenAndBonusAwardedGreaterThan(Long habitId, LocalDate from, LocalDate to,
                                                                         int bonus);

    @Query("select l.logDate from HabitLog l where l.habitId = :habitId order by l.logDate")
    List<LocalDate> findLogDates(Long habitId);

    /** All of a user's logs in one query, for building the Today view without one query per habit. */
    List<HabitLog> findByUserId(Long userId);

    List<HabitLog> findByUserIdAndLogDateBetween(Long userId, LocalDate from, LocalDate to);
}
