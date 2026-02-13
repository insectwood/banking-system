package com.example.corebanking.transfer.controller;

import com.example.corebanking.transfer.dto.TransferRequest;
import com.example.corebanking.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/banking/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfer", description = "API for Bank Transfers")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<String> transfer(
            @AuthenticationPrincipal String userUuid, // 토큰에서 안전하게 추출
            @RequestBody @Valid TransferRequest request) {
        String txId = transferService.transfer(userUuid, request);
        //transferService.transfer(request);
        return ResponseEntity.ok("Transfer completed successfully. Transaction ID : " + txId);
    }
}
