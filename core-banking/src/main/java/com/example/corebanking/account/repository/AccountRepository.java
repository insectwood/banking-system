package com.example.corebanking.account.repository;

import com.example.corebanking.account.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    // Find account by account number
    Optional<Account> findByAccountNumber(String accountNumber);

    // Pessimistic Lock
    // PESSIMISTIC_WRITE: Strongest lock level. Prevents both reading and writing from other transactions
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    // Find account list by user ID (Add if needed)
    // List<Account> findByUserId(Long userId);
}
