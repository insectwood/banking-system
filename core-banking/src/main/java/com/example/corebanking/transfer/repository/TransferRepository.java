package com.example.corebanking.transfer.repository;

import com.example.corebanking.transfer.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}
