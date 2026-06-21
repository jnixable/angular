package com.swedapp.bank.db.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swedapp.bank.db.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByPcode(String pcode);
}
