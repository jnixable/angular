package com.swedapp.txchecker;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

  private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000000");

  @PostMapping("/check")
  public TransactionResponse check(@RequestBody TransactionRequest request) {
    validate(request);
    simulateProcessingDelay();
    String rejectionReason = rejectionReason(request);
    if (rejectionReason != null) {
      return new TransactionResponse(TransactionResponse.Status.REJECTED, rejectionReason);
    }
    return new TransactionResponse(TransactionResponse.Status.APPROVED, null);
  }

  private static void validate(TransactionRequest request) {
    if (request.operation() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "operation is required");
    }
    if (request.currency() == null || request.currency().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currency is required");
    }
    if (request.amount() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount is required");
    }
    if (request.amount().signum() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be positive");
    }
  }

  private static String rejectionReason(TransactionRequest request) {
    if (request.operation() == Operation.DEPOSIT && !"EUR".equalsIgnoreCase(request.currency())) {
      return "Deposits are allowed only in EUR";
    }
    if (request.amount().compareTo(MAX_AMOUNT) > 0) {
      return "Amount must not exceed " + MAX_AMOUNT.toPlainString();
    }
    if (!isIntegerPartEven(request.amount())) {
      return "Magic is not allowed here";
    }
    return null;
  }

  private static boolean isIntegerPartEven(BigDecimal amount) {
    return amount.toBigInteger().mod(BigInteger.TWO).signum() == 0;
  }

  private static void simulateProcessingDelay() {
    long millis = ThreadLocalRandom.current().nextLong(1000, 2001);
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
