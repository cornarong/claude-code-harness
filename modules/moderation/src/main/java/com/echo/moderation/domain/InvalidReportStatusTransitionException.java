package com.echo.moderation.domain;

/**
 * 허용되지 않는 신고 상태 전환(건너뛰기·되돌리기·같은 상태)을 시도할 때 발생한다.
 * HTTP 상태(409) 변환은 호출자(컨트롤러) 몫이다.
 */
public class InvalidReportStatusTransitionException extends RuntimeException {

    public InvalidReportStatusTransitionException(ReportStatus from, ReportStatus to) {
        super("신고 상태를 %s에서 %s(으)로 바꿀 수 없습니다.".formatted(from, to));
    }
}
