package uz.cs2arena.wallet;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    private Long userId;

    @Column(nullable = false)
    private long balance;

    private Instant updatedAt = Instant.now();

    protected Wallet() {}

    public Wallet(Long userId) {
        this.userId = userId;
    }

    void change(long delta) {
        if (balance + delta < 0) throw new IllegalStateException("Balans manfiy bo'la olmaydi");
        balance += delta;
        updatedAt = Instant.now();
    }

    public Long getUserId() { return userId; }
    public long getBalance() { return balance; }
}
