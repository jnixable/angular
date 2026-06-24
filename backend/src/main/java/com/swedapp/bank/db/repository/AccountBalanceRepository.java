package com.swedapp.bank.db.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swedapp.bank.db.entity.AccountBalanceEntity;
import com.swedapp.bank.domain.Currency;

public interface AccountBalanceRepository extends JpaRepository<AccountBalanceEntity, Long> {

    Optional<AccountBalanceEntity> findByAccountIdAndCurrency(Long accountId, Currency currency);

    List<AccountBalanceEntity> findByAccountId(Long accountId);
}
