package com.example.corebanking.account.repository;

import com.example.corebanking.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    // Find account by account number
    Optional<Account> findByAccountNumber(String accountNumber);

    // Find account list by user ID (Add if needed)
    // List<Account> findByUserId(Long userId);
}
