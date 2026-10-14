package com.echo.api.moderation;

import com.echo.moderation.domain.Report;
import com.echo.moderation.domain.ReportStatus;
import java.time.LocalDateTime;

/**
 * 신고 응답 DTO.
 */
public record ReportResponse(
    Long id,
    Long reporterId,
    Long postId,
    String reason,
    ReportStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(
            report.getId(),
            report.getReporterId(),
            report.getPostId(),
            report.getReason(),
            report.getStatus(),
            report.getCreatedAt(),
            report.getUpdatedAt()
        );
    }
}
