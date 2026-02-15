package com.example.corebanking.transfer.controller;

import com.example.corebanking.common.ApiResponse;
import com.example.corebanking.transfer.dto.TransferRequest;
import com.example.corebanking.transfer.dto.TransferResponse;
import com.example.corebanking.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/banking/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfer", description = "API for Bank Transfers")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<String> transfer(
            @AuthenticationPrincipal String userUuid,
            @RequestBody @Valid TransferRequest request) {
        String txId = transferService.transfer(userUuid, request);
        return ResponseEntity.ok("Transfer completed successfully. Transaction ID : " + txId);
    }

    @GetMapping("/me")
    public ApiResponse<List<TransferResponse>> getMyTransfers(
            @AuthenticationPrincipal String userUuid) {
        List<TransferResponse> transfers = transferService.getTransfersByUser(userUuid);
        return ApiResponse.success(transfers);
    }
}
