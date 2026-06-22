package com.swedapp.bank.db.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swedapp.bank.db.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
}
