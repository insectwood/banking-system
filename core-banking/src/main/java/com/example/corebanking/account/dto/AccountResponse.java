package com.example.corebanking.account.dto;

import com.example.corebanking.account.domain.Account;

import java.math.BigDecimal;

public record AccountResponse(
        String accountNumber,
        BigDecimal balance,
        String userUuid
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getBalance(),
                account.getUserUuid()
        );
    }
}
