package com.echo.follow.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Follow 도메인 불변식 단위 테스트 — Spring 컨텍스트 없이 순수 POJO로 검증.
 */
class FollowTest {

    @Test
    @DisplayName("자기 자신을 팔로우하면 IllegalArgumentException")
    void selfFollow_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Follow(1L, 1L));
    }

    @Test
    @DisplayName("followerId 또는 followeeId가 null이면 IllegalArgumentException")
    void nullId_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Follow(null, 2L));
        assertThrows(IllegalArgumentException.class, () -> new Follow(1L, null));
    }

    @Test
    @DisplayName("유효한 id 쌍은 정상 생성되고 createdAt이 채워진다")
    void validIds_constructsSuccessfully() {
        Follow follow = new Follow(1L, 2L);

        assertEquals(1L, follow.getFollowerId());
        assertEquals(2L, follow.getFolloweeId());
        assertNotNull(follow.getCreatedAt());
    }
}
