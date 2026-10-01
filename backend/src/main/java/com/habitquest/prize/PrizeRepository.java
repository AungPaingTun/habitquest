package com.habitquest.prize;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrizeRepository extends JpaRepository<Prize, Long> {

    List<Prize> findByUserIdAndArchivedOrderByCostAscIdAsc(Long userId, boolean archived);

    Optional<Prize> findByIdAndUserId(Long id, Long userId);
}
