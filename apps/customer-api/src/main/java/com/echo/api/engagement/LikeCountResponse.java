package com.echo.api.engagement;

/**
 * GET /posts/{postId}/likes/count 응답 DTO.
 */
public record LikeCountResponse(Long postId, long count) {
}
