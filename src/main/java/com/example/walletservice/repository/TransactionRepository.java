package com.example.walletservice.repository;

import com.example.walletservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByWalletIdOrderByTimestampDesc(Long walletId);
    
    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId AND t.timestamp <= :timestamp ORDER BY t.timestamp DESC")
    List<Transaction> findTransactionsBeforeTime(@Param("walletId") Long walletId, @Param("timestamp") LocalDateTime timestamp);
    
    @Query("SELECT COALESCE(t.balanceAfterTransaction, 0) FROM Transaction t WHERE t.wallet.id = :walletId AND t.timestamp <= :timestamp ORDER BY t.timestamp DESC LIMIT 1")
    BigDecimal calculateBalanceAtTime(@Param("walletId") Long walletId, @Param("timestamp") LocalDateTime timestamp);
}