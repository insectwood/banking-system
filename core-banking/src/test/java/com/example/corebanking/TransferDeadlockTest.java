package com.example.corebanking;

import com.example.corebanking.account.domain.Account;
import com.example.corebanking.account.repository.AccountRepository;
import com.example.corebanking.transfer.dto.TransferRequest;
import com.example.corebanking.transfer.service.TransferService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TransferDeadlockTest {

    // This test makes two threads attempt transfers in opposite directions.
    // Thread 1: Account A → Account B (3000 yen)
    // Thread 2: Account B → Account A (2000 yen)

    @Autowired
    private TransferService transferService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountRepository transferRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        accountRepository.save(Account.builder().userId(1L).accountNumber("1111").balance(10000L).build());
        accountRepository.save(Account.builder().userId(2L).accountNumber("2222").balance(10000L).build());
    }

    @Test
    @DisplayName("Reproducing Deadlock: Both threads fall into an infinite wait while waiting for each other's account locks.")
    void transfer_deadlock_test() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // Thread 1: 1111 -> 2222
        executorService.submit(() -> {
            try {
                transferService.transfer(new TransferRequest("1111", "2222", 1000L, UUID.randomUUID().toString()));
            } finally {
                latch.countDown();
            }
        });

        // Thread 2: 2222 -> 1111 (opposite directions)
        executorService.submit(() -> {
            try {
                transferService.transfer(new TransferRequest("2222", "1111", 1000L, UUID.randomUUID().toString()));
            } finally {
                latch.countDown();
            }
        });

        // the system either hangs indefinitely or the database detects a deadlock and throws an error.
        latch.await();
    }

    @AfterEach
    void tearDown() {
        // Cleanup after testing.
        transferRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }
}
