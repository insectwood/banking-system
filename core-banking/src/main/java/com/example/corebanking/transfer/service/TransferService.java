package com.example.corebanking.transfer.service;

import com.example.corebanking.account.domain.Account;
import com.example.corebanking.account.repository.AccountRepository;
import com.example.corebanking.account.service.AccountService;
import com.example.corebanking.transfer.domain.Transfer;
import com.example.corebanking.transfer.dto.TransferRequest;
import com.example.corebanking.transfer.repository.TransferRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountService accountService;
    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;

    /**
     * Execute transfer between accounts (With concurrency control and deadlock prevention)
     *  @param userUuid Identifier of the transfer requester
     *  @param request  Transfer request details (Recipient account, amount, unique transaction ID)
     *  @return Returns the transactionId upon success
     */
    @Transactional
    public String transfer(String userUuid, TransferRequest request) {
        log.info("transfer start - userUuid: {}, transactionId: {}",
                userUuid, request.transactionId());

        // Idempotency Check
        if (transferRepository.existsByTransactionId(request.transactionId())) {
            return "ALREADY_PROCESSED:" + request.transactionId();
        }

        // Pre-fetch IDs to determine locking order
        Long fromAccountId = accountRepository.findIdByUserUuid(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("Withdrawal account not found.."));
        Long toAccountId = accountRepository.findIdByAccountNumber(request.toAccountNumber())
                .orElseThrow(() -> new EntityNotFoundException("Deposit account not found."));

        // Validation: Sender and recipient must be different
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("The sender's and recipient's accounts cannot be the same.");
        }

        // [Core logic for deadlock prevention]
        // Deadlock Prevention: Create a list after sorting IDs
        List<Long> accountIds = Arrays.asList(fromAccountId, toAccountId);
        accountIds.sort(Long::compareTo);

        log.info("Account PK ID Lock Order - first: {}, second: {}", accountIds.get(0), accountIds.get(1));

        // Acquire Pessimistic Locks (FOR UPDATE)
        List<Account> lockedAccounts = accountRepository.findByIdsWithLock(accountIds);

        if (lockedAccounts.size() != 2) {
            throw new EntityNotFoundException("Some accounts not be found.");
        }

        // Re-map to locked objects
        Account sender = lockedAccounts.stream()
                .filter(a -> a.getId().equals(fromAccountId))
                .findFirst().orElseThrow();
        Account recipient = lockedAccounts.stream()
                .filter(a -> a.getId().equals(toAccountId))
                .findFirst().orElseThrow();

        log.info("Transfer processing - from: {} (balance: {}), to: {}, amount: {}",
                sender.getAccountNumber(),
                sender.getBalance(),
                recipient.getAccountNumber(),
                request.amount());

        // Execute business logic
        sender.withdraw(request.amount());
        recipient.deposit(request.amount());

        accountRepository.save(sender);
        accountRepository.save(recipient);

        // Save transaction history
        Transfer transfer = transferRepository.save(Transfer.builder()
                .fromAccountNumber(sender.getAccountNumber())
                .toAccountNumber(recipient.getAccountNumber())
                .amount(request.amount())
                .transactionId(request.transactionId())
                .userUuid(userUuid)
                .build());

        return transfer.getTransactionId();
    }
}
