package com.echo.follow.domain;

/**
 * 이미 팔로우 중인 대상을 다시 팔로우하려 할 때 발생한다.
 * HTTP 상태(409) 변환은 호출자(컨트롤러) 몫이다.
 */
public class AlreadyFollowingException extends RuntimeException {

    public AlreadyFollowingException(Long followerId, Long followeeId) {
        super("이미 팔로우 중입니다. followerId=%d, followeeId=%d".formatted(followerId, followeeId));
    }
}
