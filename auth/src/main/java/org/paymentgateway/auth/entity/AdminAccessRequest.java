package org.paymentgateway.auth.entity;

import jakarta.persistence.*;
import org.paymentgateway.auth.constants.AdminAccessRequestStatus;

import java.time.Instant;

@Entity
@Table(name = "admin_access_requests", indexes = {
    @Index(name = "idx_admin_access_requests_status", columnList = "status"),
    @Index(name = "idx_admin_access_requests_user", columnList = "user_id")
})
public class AdminAccessRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private JwtUser requester;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminAccessRequestStatus status = AdminAccessRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private JwtUser reviewer;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    public Long getId() {
        return id;
    }

    public JwtUser getRequester() {
        return requester;
    }

    public void setRequester(JwtUser requester) {
        this.requester = requester;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public AdminAccessRequestStatus getStatus() {
        return status;
    }

    public void setStatus(AdminAccessRequestStatus status) {
        this.status = status;
    }

    public JwtUser getReviewer() {
        return reviewer;
    }

    public void setReviewer(JwtUser reviewer) {
        this.reviewer = reviewer;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }
}
