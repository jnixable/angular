package com.swedapp.bank.db.entity;

import com.swedapp.bank.domain.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Currency currency;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance;

  @Column(name = "owner_code", nullable = false)
  private String ownerCode;

  protected AccountEntity() {
    // Required by JPA.
  }

  public AccountEntity(String name, String number, Currency currency, BigDecimal balance, String ownerCode) {
    this.name = name;
    this.number = number;
    this.currency = currency;
    this.balance = balance;
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

  public Currency getCurrency() {
    return currency;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  public String getOwnerCode() {
    return ownerCode;
  }
}
