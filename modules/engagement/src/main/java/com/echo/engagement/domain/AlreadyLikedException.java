package com.echo.engagement.domain;

/**
 * 이미 좋아요를 누른 게시물에 다시 좋아요를 누르려 할 때 발생한다.
 * HTTP 상태(409) 변환은 호출자(컨트롤러) 몫이다.
 */
public class AlreadyLikedException extends RuntimeException {

    public AlreadyLikedException(Long userId, Long postId) {
        super("이미 좋아요를 눌렀습니다. userId=%d, postId=%d".formatted(userId, postId));
    }
}
