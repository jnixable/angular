package com.swedapp.bank.db.entity;

import com.swedapp.bank.domain.Currency;
import com.swedapp.bank.domain.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

  @Id
  @UuidGenerator
  private UUID id;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Enumerated(EnumType.ORDINAL)
  @Column(nullable = false)
  private TransactionType type;

  @Column(name = "amount_in", nullable = true, precision = 19, scale = 2)
  private BigDecimal amountIn;

  @Enumerated(EnumType.STRING)
  @Column(name = "currency_in", nullable = true)
  private Currency currencyIn;

  @Column(name = "amount_out", nullable = true, precision = 19, scale = 2)
  private BigDecimal amountOut;

  @Enumerated(EnumType.STRING)
  @Column(name = "currency_out", nullable = true)
  private Currency currencyOut;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected TransactionEntity() {
    // Required by JPA.
  }

  public TransactionEntity(Long accountId, TransactionType type, BigDecimal amountIn, Currency currencyIn,
      BigDecimal amountOut, Currency currencyOut, Instant createdAt) {
    this.accountId = accountId;
    this.type = type;
    this.amountIn = amountIn;
    this.currencyIn = currencyIn;
    this.amountOut = amountOut;
    this.currencyOut = currencyOut;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public Long getAccountId() {
    return accountId;
  }

  public TransactionType getType() {
    return type;
  }

  public BigDecimal getAmountIn() {
    return amountIn;
  }

  public Currency getCurrencyIn() {
    return currencyIn;
  }

  public BigDecimal getAmountOut() {
    return amountOut;
  }

  public Currency getCurrencyOut() {
    return currencyOut;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
