package com.example.corebanking.account.service;

import com.example.corebanking.account.domain.Account;
import com.example.corebanking.account.dto.AccountCreateRequest;
import com.example.corebanking.account.dto.AccountResponse;
import com.example.corebanking.account.repository.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    /**
     * Create account
     */
    @Transactional
    public AccountResponse createAccount(String userUuid, AccountCreateRequest request) {
        String newAccountNumber = generateAccountNumber();

        Account account = Account.builder()
                .userUuid(userUuid)
                .accountNumber(newAccountNumber)
                .balance(request.initialBalance())
                .build();

        Account savedAccount = accountRepository.save(account);

        return AccountResponse.from(savedAccount);
    }

    /**
     * Generate account number (Temporary logic)
     */
    private String generateAccountNumber() {
        return ThreadLocalRandom.current().nextInt(1000, 9999) + "-" +
                ThreadLocalRandom.current().nextInt(1000, 9999) + "-" +
                ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    /**
     * Account inquiry
     */
    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));

        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByUserUuid(String userUuid) {
        Account account = accountRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("Account not found."));

        return AccountResponse.from(account);
    }
}
