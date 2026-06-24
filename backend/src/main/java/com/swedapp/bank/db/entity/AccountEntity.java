package com.swedapp.bank.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String number;

    @Column(name = "owner_code", nullable = false)
    private String ownerCode;

    protected AccountEntity() {
        // Required by JPA.
    }

    public AccountEntity(String name, String number, String ownerCode) {
        this.name = name;
        this.number = number;
        this.ownerCode = ownerCode;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    public String getOwnerCode() {
        return ownerCode;
    }
}
