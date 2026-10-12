package com.echo.api.engagement;

import com.echo.common.ApiResponse;
import com.echo.engagement.application.LikeService;
import com.echo.engagement.domain.AlreadyLikedException;
import com.echo.engagement.domain.Like;
import com.echo.post.application.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 좋아요 HTTP API 컨트롤러.
 * X-User-Id 헤더로 요청자를 식별한다 (인증은 15장에서 도입).
 *
 * <p>post와 engagement 두 모듈을 잇는 조립 지점(composition root)이다.
 * post 모듈로 게시물 존재를 확인한 뒤 engagement 모듈에 유스케이스를 위임한다. 두 모듈은 서로 직접 의존하지 않는다.
 */
@RestController
@RequestMapping("/posts/{postId}/likes")
@Tag(name = "Like", description = "좋아요 API")
public class LikeController {

    private final PostService postService;
    private final LikeService likeService;

    public LikeController(PostService postService, LikeService likeService) {
        this.postService = postService;
        this.likeService = likeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "좋아요", description = "게시물에 좋아요를 누릅니다. 없는 게시물이면 404, 이미 눌렀으면 409.")
    public ApiResponse<LikeResponse> like(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable Long postId) {

        requirePost(postId);

        // 도메인/application은 Spring Web에 의존하지 않으므로, 도메인 예외의 HTTP 상태 변환은 컨트롤러에서 처리한다.
        try {
            Like like = likeService.like(userId, postId);
            return ApiResponse.ok(LikeResponse.from(like), "좋아요를 눌렀습니다.");
        } catch (AlreadyLikedException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @DeleteMapping
    @Operation(summary = "좋아요 취소", description = "게시물의 좋아요를 취소합니다. 없는 게시물이면 404.")
    public ApiResponse<Void> unlike(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable Long postId) {

        requirePost(postId);

        likeService.unlike(userId, postId);
        return ApiResponse.ok(null, "좋아요를 취소했습니다.");
    }

    @GetMapping("/count")
    @Operation(summary = "좋아요 수 조회", description = "게시물이 받은 좋아요 수를 조회합니다. 없는 게시물이면 404.")
    public ApiResponse<LikeCountResponse> count(@PathVariable Long postId) {
        requirePost(postId);

        return ApiResponse.ok(new LikeCountResponse(postId, likeService.countByPost(postId)));
    }

    private void requirePost(Long id) {
        if (postService.findById(id).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "글을 찾을 수 없습니다. id=" + id);
        }
    }
}
