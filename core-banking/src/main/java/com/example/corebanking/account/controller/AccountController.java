package com.example.corebanking.account.controller;

import com.example.corebanking.account.dto.AccountCreateRequest;
import com.example.corebanking.account.dto.AccountResponse;
import com.example.corebanking.account.service.AccountService;
import com.example.corebanking.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    /**
     * Account create API
     * Request : POST /api/v1/accounts
     */
    @PostMapping
    public ApiResponse<AccountResponse> createAccount(@RequestBody @Valid AccountCreateRequest request) {
        AccountResponse response = accountService.createAccount(request);

        return ApiResponse.success("Account opened successfully.", response);
    }

    /**
     * Account inquiry API
     * GET /api/v1/accounts/{accountNumber}
     */
    @GetMapping("/{accountNumber}")
    public ApiResponse<AccountResponse> getAccount(@PathVariable String accountNumber) {
        AccountResponse response = accountService.getAccount(accountNumber);
        return ApiResponse.success(response);
    }
}
