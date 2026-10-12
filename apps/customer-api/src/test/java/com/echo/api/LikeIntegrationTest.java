package com.echo.api;

import com.echo.post.application.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST/DELETE /posts/{postId}/likes, GET /posts/{postId}/likes/count 통합 테스트.
 * post → engagement → HTTP 조립 경로가 모듈 간 직접 의존 없이 앱 계층에서 연결되는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LikeIntegrationTest {

    private static final long UNKNOWN_POST_ID = 999999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostService postService;

    @Test
    @DisplayName("POST /posts/{postId}/likes — 정상 좋아요 시 201 반환")
    void like_success() throws Exception {
        Long postId = createPost();

        mockMvc.perform(post("/posts/{postId}/likes", postId).header("X-User-Id", 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.postId").value(postId));
    }

    @Test
    @DisplayName("POST /posts/{postId}/likes — 같은 사용자가 같은 게시물에 다시 누르면 409 반환")
    void like_duplicate_409() throws Exception {
        Long postId = createPost();
        mockMvc.perform(post("/posts/{postId}/likes", postId).header("X-User-Id", 1))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/posts/{postId}/likes", postId).header("X-User-Id", 1))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /posts/{postId}/likes — 없는 게시물이면 404 반환")
    void like_postNotFound_404() throws Exception {
        mockMvc.perform(post("/posts/{postId}/likes", UNKNOWN_POST_ID).header("X-User-Id", 1))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /posts/{postId}/likes — 좋아요 취소 후 다시 누를 수 있다")
    void unlike_success() throws Exception {
        Long postId = createPost();
        mockMvc.perform(post("/posts/{postId}/likes", postId).header("X-User-Id", 1))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/posts/{postId}/likes", postId).header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 좋아요가 실제로 지워졌다면 409가 아니라 201이 나온다
        mockMvc.perform(post("/posts/{postId}/likes", postId).header("X-User-Id", 1))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /posts/{postId}/likes — 없는 게시물이면 404 반환")
    void unlike_postNotFound_404() throws Exception {
        mockMvc.perform(delete("/posts/{postId}/likes", UNKNOWN_POST_ID).header("X-User-Id", 1))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /posts/{postId}/likes/count — 사용자별 좋아요를 세고, 취소하면 줄어든다")
    void count_reflectsLikesAndUnlikes() throws Exception {
        Long postId = createPost();

        mockMvc.perform(get("/posts/{postId}/likes/count", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));

        mockMvc.perform(post("/posts/{postId}/likes", postId).header("X-User-Id", 1))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/posts/{postId}/likes", postId).header("X-User-Id", 2))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/posts/{postId}/likes/count", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(postId))
                .andExpect(jsonPath("$.data.count").value(2));

        mockMvc.perform(delete("/posts/{postId}/likes", postId).header("X-User-Id", 1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/posts/{postId}/likes/count", postId))
                .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    @DisplayName("GET /posts/{postId}/likes/count — 없는 게시물이면 404 반환")
    void count_postNotFound_404() throws Exception {
        mockMvc.perform(get("/posts/{postId}/likes/count", UNKNOWN_POST_ID))
                .andExpect(status().isNotFound());
    }

    /** 글을 저장하고 생성된 id를 반환한다. */
    private Long createPost() {
        return postService.create(1L, "좋아요 테스트용 글").getId();
    }
}
