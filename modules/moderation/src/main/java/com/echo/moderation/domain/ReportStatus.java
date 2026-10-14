package com.echo.moderation.domain;

/**
 * 신고 처리 상태. PENDING → REVIEWED → RESOLVED 순서로 한 단계씩만 진행한다.
 */
public enum ReportStatus {
    PENDING,
    REVIEWED,
    RESOLVED;

    /** 바로 다음 단계로의 전환만 허용한다(건너뛰기·되돌리기·같은 상태 유지는 불가). */
    public boolean canTransitionTo(ReportStatus next) {
        return next != null && next.ordinal() == this.ordinal() + 1;
    }
}
