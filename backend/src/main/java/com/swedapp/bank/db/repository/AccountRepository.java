package com.swedapp.bank.db.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swedapp.bank.db.entity.AccountEntity;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

  Optional<AccountEntity> findByNumber(String number);

  List<AccountEntity> findByOwnerCode(String ownerCode);
}
