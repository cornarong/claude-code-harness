package com.echo.follow.application;

import com.echo.follow.domain.AlreadyFollowingException;
import com.echo.follow.domain.Follow;
import com.echo.follow.infrastructure.FollowRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Follow 도메인 애플리케이션 서비스. 팔로우·언팔로우 유스케이스.
 *
 * <p>유저 존재 여부는 이 모듈이 알 수 없다(user 모듈에 의존하지 않음). 호출자(앱 계층)가 먼저 확인한다.
 */
@Service
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;

    public FollowService(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    @Transactional
    public Follow follow(Long followerId, Long followeeId) {
        Follow follow = new Follow(followerId, followeeId);
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new AlreadyFollowingException(followerId, followeeId);
        }
        try {
            return followRepository.saveAndFlush(follow);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청이 존재 확인을 함께 통과한 경우 — 유니크 제약이 최종 방어선
            throw new AlreadyFollowingException(followerId, followeeId);
        }
    }

    /** 언팔로우. 팔로우 중이 아니어도 예외 없이 끝난다(멱등). */
    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
    }
}
