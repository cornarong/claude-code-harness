package com.echo.api.engagement;

import com.echo.engagement.domain.Like;
import java.time.LocalDateTime;

/**
 * POST /posts/{postId}/likes 응답 DTO.
 */
public record LikeResponse(Long userId, Long postId, LocalDateTime createdAt) {

    public static LikeResponse from(Like like) {
        return new LikeResponse(
            like.getUserId(),
            like.getPostId(),
            like.getCreatedAt()
        );
    }
}
