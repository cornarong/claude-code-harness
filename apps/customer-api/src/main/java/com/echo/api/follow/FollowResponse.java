package com.echo.api.follow;

import com.echo.follow.domain.Follow;
import java.time.LocalDateTime;

/**
 * POST /follows/{targetUserId} 응답 DTO.
 */
public record FollowResponse(Long followerId, Long followeeId, LocalDateTime createdAt) {

    public static FollowResponse from(Follow follow) {
        return new FollowResponse(
            follow.getFollowerId(),
            follow.getFolloweeId(),
            follow.getCreatedAt()
        );
    }
}
