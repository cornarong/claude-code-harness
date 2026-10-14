package com.echo.moderation.domain;

/**
 * 검사한 콘텐츠에 금칙어가 들어 있을 때 발생한다.
 * HTTP 상태(400) 변환은 호출자(컨트롤러) 몫이다.
 */
public class BannedWordDetectedException extends RuntimeException {

    public BannedWordDetectedException() {
        super("금칙어가 포함되어 있어 등록할 수 없습니다.");
    }
}
