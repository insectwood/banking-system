package com.example.corebanking.transfer.repository;

import com.example.corebanking.transfer.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    // "SELECT count(*) > 0 FROM transfers WHERE transaction_id = ?"
    boolean existsByTransactionId(String transactionId);
}
