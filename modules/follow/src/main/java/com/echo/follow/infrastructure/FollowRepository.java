package com.echo.follow.infrastructure;

import com.echo.follow.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Follow JPA 리포지토리. infrastructure 계층.
 */
public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    /** 팔로우 관계 삭제. 삭제된 행 수를 반환한다. */
    long deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
}
