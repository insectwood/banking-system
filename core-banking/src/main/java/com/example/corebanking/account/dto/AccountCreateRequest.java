package com.example.corebanking.account.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountCreateRequest(
        @NotNull(message = "User Id is required")
        Long userId,

        @Min(value = 1000, message = "A minimum deposit of 1,000 yen is required to create an account.")
        BigDecimal initialBalance
) {}
