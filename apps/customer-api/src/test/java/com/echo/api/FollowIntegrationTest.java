package com.echo.api;

import com.echo.user.domain.User;
import com.echo.user.infrastructure.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST/DELETE /follows/{targetUserId} 통합 테스트.
 * user → follow → HTTP 조립 경로가 모듈 간 직접 의존 없이 앱 계층에서 연결되는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class FollowIntegrationTest {

    private static final long UNKNOWN_USER_ID = 999999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("POST /follows/{targetUserId} — 정상 팔로우 시 201 반환")
    void follow_success() throws Exception {
        Long me = createUser();
        Long target = createUser();

        mockMvc.perform(post("/follows/{targetUserId}", target).header("X-User-Id", me))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.followerId").value(me))
                .andExpect(jsonPath("$.data.followeeId").value(target));
    }

    @Test
    @DisplayName("POST /follows/{targetUserId} — 이미 팔로우 중이면 409 반환")
    void follow_alreadyFollowing_409() throws Exception {
        Long me = createUser();
        Long target = createUser();
        mockMvc.perform(post("/follows/{targetUserId}", target).header("X-User-Id", me))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/follows/{targetUserId}", target).header("X-User-Id", me))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /follows/{targetUserId} — 대상 유저가 없으면 404 반환")
    void follow_targetNotFound_404() throws Exception {
        Long me = createUser();

        mockMvc.perform(post("/follows/{targetUserId}", UNKNOWN_USER_ID).header("X-User-Id", me))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /follows/{targetUserId} — 요청자(X-User-Id)가 없는 유저면 404 반환")
    void follow_requesterNotFound_404() throws Exception {
        Long target = createUser();

        mockMvc.perform(post("/follows/{targetUserId}", target).header("X-User-Id", UNKNOWN_USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /follows/{targetUserId} — 자기 자신을 팔로우하면 400 반환")
    void follow_self_400() throws Exception {
        Long me = createUser();

        mockMvc.perform(post("/follows/{targetUserId}", me).header("X-User-Id", me))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /follows/{targetUserId} — 언팔로우 후 다시 팔로우할 수 있다")
    void unfollow_thenFollowAgain() throws Exception {
        Long me = createUser();
        Long target = createUser();
        mockMvc.perform(post("/follows/{targetUserId}", target).header("X-User-Id", me))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/follows/{targetUserId}", target).header("X-User-Id", me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 관계가 실제로 지워졌다면 409가 아니라 201이 나온다
        mockMvc.perform(post("/follows/{targetUserId}", target).header("X-User-Id", me))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /follows/{targetUserId} — 대상 유저가 없으면 404 반환")
    void unfollow_targetNotFound_404() throws Exception {
        Long me = createUser();

        mockMvc.perform(delete("/follows/{targetUserId}", UNKNOWN_USER_ID).header("X-User-Id", me))
                .andExpect(status().isNotFound());
    }

    /** 유저를 저장하고 생성된 id를 반환한다. username은 유니크 제약이 있어 매번 새로 만든다. */
    private Long createUser() {
        String username = "user-" + UUID.randomUUID();
        return userRepository.save(new User(username, username)).getId();
    }
}
