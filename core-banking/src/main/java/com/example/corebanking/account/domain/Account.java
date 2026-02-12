package com.example.corebanking.account.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "accounts")
public class Account {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userUuid;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private Long balance;

    // [Point] Versioning for Optimistic Lock
    //@Version
    //private Long version;

    @Builder
    public Account(String userUuid, String accountNumber, Long balance) {
        this.userUuid = userUuid;
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
