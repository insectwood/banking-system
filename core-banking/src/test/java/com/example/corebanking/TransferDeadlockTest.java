package com.example.corebanking;

import com.example.corebanking.account.domain.Account;
import com.example.corebanking.account.repository.AccountRepository;
import com.example.corebanking.transfer.dto.TransferRequest;
import com.example.corebanking.transfer.repository.TransferRepository;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TransferDeadlockTest {

    // This test makes two threads attempt transfers in opposite directions.
    // Thread 1: Account A → Account B (3000 yen)
    // Thread 2: Account B → Account A (2000 yen)

    @Autowired private TransferService transferService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransferRepository transferRepository;

    private final String USER_A_UUID = "user-a-uuid";
    private final String USER_B_UUID = "user-b-uuid";

    @BeforeEach
    void setUp() {
        transferRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();

        // When generate Account Object, it use userUuid instead of userId
        accountRepository.saveAndFlush(Account.builder()
                .userUuid(USER_A_UUID)
                .accountNumber("1111")
                .balance(10000L)
                .build());

        accountRepository.saveAndFlush(Account.builder()
                .userUuid(USER_B_UUID)
                .accountNumber("2222")
                .balance(10000L)
                .build());
    }

    @Test
    @DisplayName("Reproducing Deadlock: Both threads fall into an infinite wait while waiting for each other's account locks.")
    void transfer_deadlock_prevention_test() throws InterruptedException {
        // given
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Thread 1: 1111 -> 2222
        executorService.submit(() -> {
            try {
                transferService.transfer(USER_A_UUID,
                        new TransferRequest("2222", 3000L, UUID.randomUUID().toString()));
            } catch (Exception e) {
                System.err.println("Thread 1 Error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        // Thread 2: 2222 -> 1111 (opposite directions)
        executorService.submit(() -> {
            try {
                transferService.transfer(USER_B_UUID,
                        new TransferRequest("1111", 2000L, UUID.randomUUID().toString()));
            } catch (Exception e) {
                System.err.println("Thread 2 Error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        // when
        // the system either hangs indefinitely or the database detects a deadlock and throws an error.
        boolean completed = latch.await(10, TimeUnit.SECONDS);

        // then
        assertThat(completed).isTrue();

        Account accountA = accountRepository.findByAccountNumber("1111").orElseThrow();
        Account accountB = accountRepository.findByAccountNumber("2222").orElseThrow();

        assertThat(accountA.getBalance()).isEqualTo(9000L);
        assertThat(accountB.getBalance()).isEqualTo(11000L);
    }

    @AfterEach
    void tearDown() {
        // Cleanup after testing.
        transferRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }
}
