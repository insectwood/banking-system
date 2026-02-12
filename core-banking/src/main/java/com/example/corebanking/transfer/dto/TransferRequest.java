package com.example.corebanking.transfer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TransferRequest(
        @NotNull String toAccountNumber,
        @Min(1) Long amount,
        @NotNull String transactionId
) {}
