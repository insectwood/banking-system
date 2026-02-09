package com.example.corebanking.transfer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "transfers")
public class Transfer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fromAccountNumber;

    @Column(nullable = false)
    private String toAccountNumber;

    @Column(nullable = false)
    private Long amount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime transferredAt;

    @Column(nullable = false, unique = true)
    private String transactionId;

    @Builder
    public Transfer(String fromAccountNumber, String toAccountNumber, Long amount, String transactionId) {
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
        this.transactionId = transactionId;
    }
}
