package com.example.corebanking.transfer.dto;

import com.example.corebanking.transfer.domain.Transfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        String transactionId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        LocalDateTime transferredAt
) {
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getTransactionId(),
                transfer.getFromAccountNumber(),
                transfer.getToAccountNumber(),
                transfer.getAmount(),
                transfer.getTransferredAt()
        );
    }
}
