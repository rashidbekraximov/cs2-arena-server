package uz.cs2arena.wallet;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {

    public enum Type { TOPUP, TOPUP_REFUND, PREMIUM, ADMIN_ADJUST }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private long amount;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String description;
    private String reference;
    private long balanceAfter;
    private Instant createdAt = Instant.now();

    protected Transaction() {}

    public Transaction(Long userId, long amount, Type type, String description, String reference, long balanceAfter) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.reference = reference;
        this.balanceAfter = balanceAfter;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public long getAmount() { return amount; }
    public Type getType() { return type; }
    public String getDescription() { return description; }
    public String getReference() { return reference; }
    public long getBalanceAfter() { return balanceAfter; }
    public Instant getCreatedAt() { return createdAt; }
}
