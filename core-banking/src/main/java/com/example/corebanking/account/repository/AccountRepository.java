package com.example.corebanking.account.repository;

import com.example.corebanking.account.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    // Find account by account number
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByUserUuid(String userUuid);


    // Pessimistic Lock
    // PESSIMISTIC_WRITE: Strongest lock level. Prevents both reading and writing from other transactions
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id in :ids ORDER BY a.id")
    List<Account> findByIdsWithLock(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.userUuid = :userUuid")
    Optional<Account> findByUserUuidWithLock(@Param("userUuid") String userUuid);

    @Query("SELECT a.id FROM Account a WHERE a.userUuid = :userUuid")
    Optional<Long> findIdByUserUuid(@Param("userUuid") String userUuid);

    @Query("SELECT a.id FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Long> findIdByAccountNumber(@Param("accountNumber") String accountNumber);
    //Find account list by user ID (Add if needed)
     //List<Account> findByUserId(Long userId);
}
