package com.example.corebanking.account.dto;

import com.example.corebanking.account.domain.Account;

public record AccountResponse(
        String accountNumber,
        Long balance,
        Long userId
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getBalance(),
                account.getUserId()
        );
    }
}
