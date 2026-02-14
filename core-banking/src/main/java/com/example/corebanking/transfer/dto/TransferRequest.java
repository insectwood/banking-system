package com.example.corebanking.transfer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull String toAccountNumber,
        @Min(1) BigDecimal amount,
        @NotNull String transactionId
) {}
