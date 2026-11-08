package uz.cs2arena.admin;

import jakarta.persistence.*;

import java.time.Instant;

/** Adminlar amallari jurnali (kim, nima, kimga). */
@Entity
@Table(name = "admin_actions")
public class AdminAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long adminId;
    private String action;
    private String target;
    private String details;
    private Instant createdAt = Instant.now();

    protected AdminAction() {}

    public AdminAction(Long adminId, String action, String target, String details) {
        this.adminId = adminId;
        this.action = action;
        this.target = target;
        this.details = details == null ? null : details.length() > 500 ? details.substring(0, 500) : details;
    }

    public Long getId() { return id; }
    public Long getAdminId() { return adminId; }
    public String getAction() { return action; }
    public String getTarget() { return target; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
