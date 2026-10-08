package com.echo.api.follow;

import com.echo.common.ApiResponse;
import com.echo.follow.application.FollowService;
import com.echo.follow.domain.AlreadyFollowingException;
import com.echo.follow.domain.Follow;
import com.echo.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 팔로우 HTTP API 컨트롤러.
 * X-User-Id 헤더로 요청자를 식별한다 (인증은 15장에서 도입).
 *
 * <p>user와 follow 두 모듈을 잇는 조립 지점(composition root)이다.
 * user 모듈로 유저 존재를 확인한 뒤 follow 모듈에 유스케이스를 위임한다. 두 모듈은 서로 직접 의존하지 않는다.
 */
@RestController
@RequestMapping("/follows")
@Tag(name = "Follow", description = "팔로우 API")
public class FollowController {

    private final UserService userService;
    private final FollowService followService;

    public FollowController(UserService userService, FollowService followService) {
        this.userService = userService;
        this.followService = followService;
    }

    @PostMapping("/{targetUserId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "팔로우", description = "대상 유저를 팔로우합니다. 없는 유저면 404, 이미 팔로우 중이면 409.")
    public ApiResponse<FollowResponse> follow(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable Long targetUserId) {

        requireUser(userId);
        requireUser(targetUserId);

        // 도메인/application은 Spring Web에 의존하지 않으므로, 도메인 예외의 HTTP 상태 변환은 컨트롤러에서 처리한다.
        try {
            Follow follow = followService.follow(userId, targetUserId);
            return ApiResponse.ok(FollowResponse.from(follow), "팔로우했습니다.");
        } catch (AlreadyFollowingException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{targetUserId}")
    @Operation(summary = "언팔로우", description = "대상 유저를 언팔로우합니다. 없는 유저면 404.")
    public ApiResponse<Void> unfollow(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable Long targetUserId) {

        requireUser(userId);
        requireUser(targetUserId);

        followService.unfollow(userId, targetUserId);
        return ApiResponse.ok(null, "언팔로우했습니다.");
    }

    private void requireUser(Long id) {
        if (!userService.exists(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다. id=" + id);
        }
    }
}
