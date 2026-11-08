package uz.cs2arena.payment;

import jakarta.persistence.*;

import java.time.Instant;

/** Balansni to'ldirish uchun buyurtma. Payme/Click tranzaksiyasi shunga bog'lanadi. */
@Entity
@Table(name = "payments")
public class Payment {

    public enum Provider { PAYME, CLICK }

    public enum Status { PENDING, PROCESSING, PAID, CANCELLED, REFUNDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    /** So'mda */
    private long amount;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private String providerTransId;
    private Integer providerState;
    private Long createTime;
    private Long performTime;
    private Long cancelTime;
    private Integer cancelReason;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected Payment() {}

    public Payment(Long userId, Provider provider, long amount) {
        this.userId = userId;
        this.provider = provider;
        this.amount = amount;
    }

    public void markProcessing(String transId, int state, long createTime) {
        this.providerTransId = transId;
        this.providerState = state;
        this.createTime = createTime;
        this.status = Status.PROCESSING;
        touch();
    }

    public void markPaid(int state, long performTime) {
        this.providerState = state;
        this.performTime = performTime;
        this.status = Status.PAID;
        touch();
    }

    public void markCancelled(int state, long cancelTime, Integer reason, boolean refunded) {
        this.providerState = state;
        this.cancelTime = cancelTime;
        this.cancelReason = reason;
        this.status = refunded ? Status.REFUNDED : Status.CANCELLED;
        touch();
    }

    private void touch() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Provider getProvider() { return provider; }
    public long getAmount() { return amount; }
    public Status getStatus() { return status; }
    public String getProviderTransId() { return providerTransId; }
    public Integer getProviderState() { return providerState; }
    public Long getCreateTime() { return createTime; }
    public Long getPerformTime() { return performTime; }
    public Long getCancelTime() { return cancelTime; }
    public Integer getCancelReason() { return cancelReason; }
    public Instant getCreatedAt() { return createdAt; }
}
