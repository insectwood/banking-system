package com.example.corebanking.transfer.service;

import com.example.corebanking.account.service.AccountService;
import com.example.corebanking.transfer.domain.Transfer;
import com.example.corebanking.transfer.dto.TransferRequest;
import com.example.corebanking.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountService accountService;
    private final TransferRepository transferRepository;

    /**
     * Transfer logic
     */
    @Transactional
    public void transfer(TransferRequest request) {
        // 1. Validation: Sender and recipient must be different
        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            throw new IllegalArgumentException("The sender's and recipient's accounts cannot be the same.");
        }

        // 2. Order : Withdrawal -> Deposit
        // Check insufficient balance, within AccountService.
        accountService.withdraw(request.fromAccountNumber(), request.amount());
        accountService.deposit(request.toAccountNumber(), request.amount());

        // 3. Transfer history
        transferRepository.save(Transfer.builder()
                .fromAccountNumber(request.fromAccountNumber())
                .toAccountNumber(request.toAccountNumber())
                .amount(request.amount())
                .build());
    }
}
