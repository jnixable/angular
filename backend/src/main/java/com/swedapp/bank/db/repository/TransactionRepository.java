package com.swedapp.bank.db.repository;

import com.swedapp.bank.db.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    @Query("""
            SELECT t FROM TransactionEntity t
            WHERE t.accountId = :accountId OR t.accountTo = :accountId
            ORDER BY t.createdAt DESC
            """)
    Page<TransactionEntity> findHistoryForAccount(@Param("accountId") Long accountId, Pageable pageable);
}
