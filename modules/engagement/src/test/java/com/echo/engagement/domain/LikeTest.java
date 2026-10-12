package com.echo.engagement.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Like 도메인 불변식 단위 테스트 — Spring 컨텍스트 없이 순수 POJO로 검증.
 */
class LikeTest {

    @Test
    @DisplayName("userId 또는 postId가 null이면 IllegalArgumentException")
    void nullId_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Like(null, 10L));
        assertThrows(IllegalArgumentException.class, () -> new Like(1L, null));
    }

    @Test
    @DisplayName("유효한 id 쌍은 정상 생성되고 createdAt이 채워진다")
    void validIds_constructsSuccessfully() {
        Like like = new Like(1L, 10L);

        assertEquals(1L, like.getUserId());
        assertEquals(10L, like.getPostId());
        assertNotNull(like.getCreatedAt());
    }
}
