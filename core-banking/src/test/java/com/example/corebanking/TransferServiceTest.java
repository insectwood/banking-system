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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // Integration Test: Uses actual database and all Spring Beans
@Transactional
class TransferServiceTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    // [Core] Ensure the database is cleared before each test execution.
    @BeforeEach
    void setUp() {
        transferRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Successful transfer: The amount should be transferred from Account A to Account B correctly.")
    void transfer_success() {
        // given: Create two accounts in advance.
        Account sender = accountRepository.save(
                Account.builder().userId(1L).accountNumber("1111").balance(10000L).build()
        );
        Account receiver = accountRepository.save(
                Account.builder().userId(2L).accountNumber("2222").balance(0L).build()
        );

        TransferRequest request = new TransferRequest("1111", "2222", 3000L);

        // when: Call the transfer service.
        transferService.transfer(request);

        // then: Verify if the balances are correct.
        Account updatedSender = accountRepository.findById(sender.getId()).get();
        Account updatedReceiver = accountRepository.findById(receiver.getId()).get();

        assertThat(updatedSender.getBalance()).isEqualTo(7000L); // 10000 - 3000
        assertThat(updatedReceiver.getBalance()).isEqualTo(3000L); // 0 + 3000
    }
}
