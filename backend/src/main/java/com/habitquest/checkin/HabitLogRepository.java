package com.habitquest.checkin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {

    Optional<HabitLog> findByHabitIdAndLogDate(Long habitId, LocalDate logDate);

    @Query("select l.logDate from HabitLog l where l.habitId = :habitId order by l.logDate")
    List<LocalDate> findLogDates(Long habitId);

    /** All of a user's logs in one query, for building the Today view without one query per habit. */
    List<HabitLog> findByUserId(Long userId);
}
