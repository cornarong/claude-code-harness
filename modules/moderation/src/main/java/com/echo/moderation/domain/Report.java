package com.echo.moderation.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Report 도메인 엔티티. 유저가 게시물에 대해 접수한 신고 한 건.
 *
 * <p>user·post 모듈을 import하지 않고 id만 보관한다. 접수 시 PENDING으로 시작한다.
 */
@Entity
@Table(name = "reports")
public class Report {

    public static final int MAX_REASON_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long reporterId;

    @Column(nullable = false, updatable = false)
    private Long postId;

    @Column(nullable = false, updatable = false, length = MAX_REASON_LENGTH)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Report() {
    }

    public Report(Long reporterId, Long postId, String reason) {
        if (reporterId == null || postId == null) {
            throw new IllegalArgumentException("reporterId와 postId는 비어 있을 수 없습니다.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("신고 사유는 비어 있을 수 없습니다.");
        }
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("신고 사유는 " + MAX_REASON_LENGTH + "자를 초과할 수 없습니다.");
        }
        this.reporterId = reporterId;
        this.postId = postId;
        this.reason = reason;
        this.status = ReportStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** 상태 전환. PENDING → REVIEWED → RESOLVED 순서로 한 단계씩만 진행한다. */
    public void changeStatus(ReportStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidReportStatusTransitionException(status, next);
        }
        this.status = next;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getReporterId() { return reporterId; }
    public Long getPostId() { return postId; }
    public String getReason() { return reason; }
    public ReportStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
