package com.swedapp.bank.db.repository;

import com.swedapp.bank.db.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    Page<TransactionEntity> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
}
