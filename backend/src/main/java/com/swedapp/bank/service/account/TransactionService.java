package com.swedapp.bank.service.account;

import com.swedapp.bank.db.entity.AccountEntity;
import com.swedapp.bank.db.repository.AccountRepository;
import com.swedapp.bank.db.repository.TransactionRepository;
import com.swedapp.bank.domain.Transaction;
import com.swedapp.bank.service.account.errors.AccountAccessDeniedException;
import com.swedapp.bank.service.account.errors.AccountNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getHistory(String ownerCode, String accountNumber, Pageable pageable) {
        var account = ownedAccount(ownerCode, accountNumber);
        return transactionRepository
                .findHistoryForAccount(account.getId(), pageable)
                .map(tx -> new Transaction(
                        tx.getId(), tx.getType(), tx.getAmountIn(), tx.getCurrencyIn(),
                        tx.getAmountOut(), tx.getCurrencyOut(), tx.getCreatedAt()));
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
