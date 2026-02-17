package com.example.corebanking.account.controller;

import com.example.corebanking.account.dto.AccountCreateRequest;
import com.example.corebanking.account.dto.AccountResponse;
import com.example.corebanking.account.service.AccountService;
import com.example.corebanking.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/banking/accounts")
public class AccountController {

    private final AccountService accountService;

    /**
     * Account create API
     * Request : POST /api/v1/banking/accounts
     */
    @PostMapping
    public ApiResponse<AccountResponse> createAccount(
            @AuthenticationPrincipal String userUuid,
            @RequestBody @Valid AccountCreateRequest request) {
        AccountResponse response = accountService.createAccount(userUuid, request);

        return ApiResponse.success("Account opened successfully.", response);
    }

    /**
     * Account inquiry API
     * GET /api/v1/accounts/banking/{accountNumber}
     */
    @GetMapping("/{accountNumber}")
    public ApiResponse<AccountResponse> getAccount(@PathVariable String accountNumber) {
        AccountResponse response = accountService.getAccount(accountNumber);
        return ApiResponse.success(response);
    }

    /**
     * Account inquiry API currently user
     * GET /api/v1/banking/accounts/me
     */
    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyAccount(
            @AuthenticationPrincipal String userUuid
    ) {
        return ResponseEntity.ok(accountService.getAccountByUserUuid(userUuid));
    }
    // test
}
