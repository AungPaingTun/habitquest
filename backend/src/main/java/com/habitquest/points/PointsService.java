package com.habitquest.points;

import com.habitquest.points.dto.PointsSummary;
import com.habitquest.points.dto.TransactionResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PointsService {

    private static final int MAX_HISTORY = 100;

    private final PointTransactionRepository transactions;

    public PointsService(PointTransactionRepository transactions) {
        this.transactions = transactions;
    }

    public long balance(Long userId) {
        return transactions.balance(userId);
    }

    /** Adds a ledger row. Callers must hold the user's row lock (UserRepository.findByIdForUpdate). */
    public void record(PointTransaction transaction) {
        transactions.save(transaction);
    }

    @Transactional(readOnly = true)
    public PointsSummary summary(Long userId) {
        long xp = transactions.lifetimeXp(userId);
        int level = Levels.levelFor(xp);
        return new PointsSummary(transactions.balance(userId), xp, level,
                Levels.xpForLevel(level), Levels.xpForLevel(level + 1));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> history(Long userId, int limit) {
        int size = Math.clamp(limit, 1, MAX_HISTORY);
        return transactions.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(0, size)).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
