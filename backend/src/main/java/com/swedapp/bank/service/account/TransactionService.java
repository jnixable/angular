package com.swedapp.bank.service.account;

import com.swedapp.bank.db.entity.AccountEntity;
import com.swedapp.bank.db.entity.TransactionEntity;
import com.swedapp.bank.db.repository.AccountRepository;
import com.swedapp.bank.db.repository.TransactionRepository;
import com.swedapp.bank.domain.Transaction;
import com.swedapp.bank.domain.TransactionType;
import com.swedapp.bank.service.account.errors.AccountAccessDeniedException;
import com.swedapp.bank.service.account.errors.AccountNotFoundException;
import com.swedapp.bank.service.account.errors.TransactionNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getHistory(String ownerCode, String accountNumber, Instant from, Instant to, Pageable pageable) {
        var account = ownedAccount(ownerCode, accountNumber);
        var fromBound = from != null ? from : now().minus(7, DAYS);
        var toBound = to != null ? to : now();
        var page = transactionRepository.findHistoryForAccount(account.getId(), fromBound, toBound, pageable);
        var numbersById = resolveAccountNumbers(page.getContent());
        return page.map(tx -> toDomain(tx, numbersById));
    }

    @Transactional(readOnly = true)
    public Transaction getTransaction(String ownerCode, String accountNumber, UUID transactionId) {
        var account = ownedAccount(ownerCode, accountNumber);
        var transaction = transactionRepository.findById(transactionId)
                .filter(tx -> belongsToAccount(tx, account.getId()))
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return toDomain(transaction, resolveAccountNumbers(List.of(transaction)));
    }

    private boolean belongsToAccount(TransactionEntity transaction, Long accountId) {
        return accountId.equals(transaction.getAccountId()) || accountId.equals(transaction.getAccountTo());
    }

    private Map<Long, String> resolveAccountNumbers(List<TransactionEntity> transactions) {
        Set<Long> ids = transactions.stream()
                .filter(tx -> tx.getType() == TransactionType.TRANSFER)
                .flatMap(tx -> Stream.of(tx.getAccountId(), tx.getAccountTo()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return accountRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(AccountEntity::getId, AccountEntity::getNumber));
    }

    private Transaction toDomain(TransactionEntity tx, Map<Long, String> numbersById) {
        String accountFrom = null;
        String accountTo = null;
        if (tx.getType() == TransactionType.TRANSFER) {
            accountFrom = numbersById.get(tx.getAccountId());
            accountTo = numbersById.get(tx.getAccountTo());
        }
        return new Transaction(
                tx.getId(), tx.getType(), tx.getAmountIn(), tx.getCurrencyIn(),
                tx.getAmountOut(), tx.getCurrencyOut(), accountFrom, accountTo, tx.getCreatedAt());
    }

    private AccountEntity ownedAccount(String ownerCode, String accountNumber) {
        var account = accountRepository.findByNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        if (!account.getOwnerCode().equals(ownerCode)) {
            throw new AccountAccessDeniedException(accountNumber);
        }
        return account;
    }
}
