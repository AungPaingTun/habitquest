package com.habitquest.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Loads the user with a row lock (SELECT ... FOR UPDATE) held until the transaction ends.
     * Every action that changes points takes this lock first, so two at the same moment
     * (a double-click on Redeem) run one after the other instead of both spending the same points.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
