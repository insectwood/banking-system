package com.example.corebanking.account.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "accounts")
public class Account {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private Long balance;

    // [Point] Versioning for Optimistic Lock
    @Version
    private Long version;

    @Builder
    public Account(Long userId, String accountNumber, Long balance) {
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public void deposit(Long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be greater than 0.");
        this.balance += amount;
    }

    public void withdraw(Long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be greater than 0.");
        if (this.balance < amount) throw new IllegalStateException("Insufficient balance.");
        this.balance -= amount;
    }
}
