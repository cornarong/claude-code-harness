package com.echo.moderation.domain;

/**
 * 없는 신고를 처리하려 할 때 발생한다.
 * HTTP 상태(404) 변환은 호출자(컨트롤러) 몫이다.
 */
public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException(Long reportId) {
        super("신고를 찾을 수 없습니다. id=" + reportId);
    }
}
