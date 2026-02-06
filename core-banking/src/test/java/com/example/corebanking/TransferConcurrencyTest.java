package com.example.corebanking;

import com.example.corebanking.account.domain.Account;
import com.example.corebanking.account.repository.AccountRepository;
import com.example.corebanking.transfer.dto.TransferRequest;
import com.example.corebanking.transfer.repository.TransferRepository;
import com.example.corebanking.transfer.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TransferConcurrencyTest {

    @Autowired private TransferService transferService;
    @Autowired private AccountRepository accountRepository;
    @Autowired
    private TransferRepository transferRepository;

    @BeforeEach
    void setUp() {
        transferRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrency issue test: 100 users transfer 10 yen simultaneously")
    void transfer_concurrency_fail_test() throws InterruptedException {
        // given: Account A (1000 yen), Account B (0 yen)
        Account sender = accountRepository.save(
                Account.builder().userId(1L).accountNumber("1111").balance(1000L).build()
        );
        Account receiver = accountRepository.save(
                Account.builder().userId(2L).accountNumber("2222").balance(0L).build()
        );

        int threadCount = 100;
        // Set up multi-threaded environment (Thread pool size: 32)
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        // Tool to wait for 100 requests to complete (CountDownLatch)
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: Execute 100 transfer requests 'concurrently'
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    transferService.transfer(
                            new TransferRequest("1111", "2222", 10L)
                    );
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();// Wait until all 100 transfers are finished

        // then: Verify
        Account updatedSender = accountRepository.findById(sender.getId()).orElseThrow();
        Account updatedReceiver = accountRepository.findById(receiver.getId()).orElseThrow();

        // [Expected Result]
        // Sender: 1000 yen - (10 yen * 100 times) = 0 yen
        // Recipient: 0 yen + (10 yen * 100 times) = 1,000 yen

        System.out.println("Sender Balance: " + updatedSender.getBalance());
        System.out.println("Recipient Balance: " + updatedReceiver.getBalance());

        assertThat(updatedSender.getBalance()).isEqualTo(0L);
        assertThat(updatedReceiver.getBalance()).isEqualTo(1000L);
    }
}
