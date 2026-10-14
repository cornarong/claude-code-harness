package com.echo.moderation.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Report 도메인 불변식·상태 전환 단위 테스트 — Spring 컨텍스트 없이 순수 POJO로 검증.
 */
class ReportTest {

    @Test
    @DisplayName("reporterId 또는 postId가 null이면 IllegalArgumentException")
    void nullId_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Report(null, 10L, "스팸"));
        assertThrows(IllegalArgumentException.class, () -> new Report(1L, null, "스팸"));
    }

    @Test
    @DisplayName("신고 사유가 비었거나 500자를 넘으면 IllegalArgumentException")
    void invalidReason_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Report(1L, 10L, null));
        assertThrows(IllegalArgumentException.class, () -> new Report(1L, 10L, "  "));
        assertThrows(IllegalArgumentException.class, () -> new Report(1L, 10L, "a".repeat(501)));
    }

    @Test
    @DisplayName("접수 시 PENDING 상태로 생성되고 시각이 채워진다")
    void newReport_isPending() {
        Report report = new Report(1L, 10L, "스팸");

        assertEquals(1L, report.getReporterId());
        assertEquals(10L, report.getPostId());
        assertEquals("스팸", report.getReason());
        assertEquals(ReportStatus.PENDING, report.getStatus());
        assertNotNull(report.getCreatedAt());
        assertNotNull(report.getUpdatedAt());
    }

    @Test
    @DisplayName("PENDING → REVIEWED → RESOLVED 순서로 전환된다")
    void changeStatus_inOrder() {
        Report report = new Report(1L, 10L, "스팸");

        report.changeStatus(ReportStatus.REVIEWED);
        assertEquals(ReportStatus.REVIEWED, report.getStatus());

        report.changeStatus(ReportStatus.RESOLVED);
        assertEquals(ReportStatus.RESOLVED, report.getStatus());
    }

    @Test
    @DisplayName("건너뛰기·되돌리기·같은 상태·null 전환은 InvalidReportStatusTransitionException")
    void changeStatus_invalid_throws() {
        Report pending = new Report(1L, 10L, "스팸");
        assertThrows(InvalidReportStatusTransitionException.class,
                () -> pending.changeStatus(ReportStatus.RESOLVED));
        assertThrows(InvalidReportStatusTransitionException.class,
                () -> pending.changeStatus(ReportStatus.PENDING));
        assertThrows(InvalidReportStatusTransitionException.class,
                () -> pending.changeStatus(null));

        Report reviewed = new Report(1L, 10L, "스팸");
        reviewed.changeStatus(ReportStatus.REVIEWED);
        assertThrows(InvalidReportStatusTransitionException.class,
                () -> reviewed.changeStatus(ReportStatus.PENDING));

        reviewed.changeStatus(ReportStatus.RESOLVED);
        assertThrows(InvalidReportStatusTransitionException.class,
                () -> reviewed.changeStatus(ReportStatus.RESOLVED));
    }
}
