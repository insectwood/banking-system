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
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // Integration Test: Uses actual database and all Spring Beans
@Transactional
class TransferServiceTest {

    @Autowired private TransferService transferService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransferRepository transferRepository;

    private final String SENDER_UUID = "sender-uuid-001";
    private final String RECIPIENT_UUID = "recipient-uuid-002";

    // [Core] Ensure the database is cleared before each test execution.
    @BeforeEach
    void setUp() {
        transferRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();

        accountRepository.saveAndFlush(Account.builder()
                .userUuid(SENDER_UUID)
                .accountNumber("1111")
                .balance(10000L)
                .build());

        accountRepository.saveAndFlush(Account.builder()
                .userUuid(RECIPIENT_UUID)
                .accountNumber("2222")
                .balance(0L)
                .build());
    }

    @Test
    @DisplayName("Successful transfer: The amount should be transferred from Account A to Account B correctly.")
    void transfer_success() {
        // given: Create account in advance.
        TransferRequest request = new TransferRequest("2222", 3000L, UUID.randomUUID().toString());

        // when: Call the transfer service.
        transferService.transfer(SENDER_UUID, request);

        // then: Verify if the balances are correct.
        Account updatedSender = accountRepository.findByAccountNumber("1111").orElseThrow();
        Account updatedReceiver = accountRepository.findByAccountNumber("2222").orElseThrow();

        assertThat(updatedSender.getBalance()).isEqualTo(7000L); // 10000 - 3000
        assertThat(updatedReceiver.getBalance()).isEqualTo(3000L); // 0 + 3000
    }

    @Test
    @DisplayName("Idempotency Test: Even if the same Transaction ID is requested twice, the transfer must occur only 1 time.")
    void transfer_idempotency_test() {
        // given
        String txId = "UNIQUE-TX-ID-123";
        TransferRequest request = new TransferRequest("2222", 1000L, txId);

        // when
        transferService.transfer(SENDER_UUID, request);
        transferService.transfer(SENDER_UUID, request);

        // then
        Account sender = accountRepository.findByAccountNumber("1111").orElseThrow();
        assertThat(sender.getBalance()).isEqualTo(9000L); // 10000 - 1000

        assertThat(transferRepository.count()).isEqualTo(1L);
    }

    @AfterEach
    void tearDown() {
        // Cleanup after testing.
        transferRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }
}
