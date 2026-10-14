package com.echo.api.moderation;

import com.echo.common.ApiResponse;
import com.echo.moderation.application.ReportService;
import com.echo.moderation.domain.InvalidReportStatusTransitionException;
import com.echo.moderation.domain.Report;
import com.echo.moderation.domain.ReportNotFoundException;
import com.echo.post.application.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 신고 HTTP API 컨트롤러.
 * X-User-Id 헤더로 신고자를 식별한다 (인증은 15장에서 도입).
 *
 * <p>post와 moderation 두 모듈을 잇는 조립 지점(composition root)이다.
 * post 모듈로 게시물 존재를 확인한 뒤 moderation 모듈에 유스케이스를 위임한다. 두 모듈은 서로 직접 의존하지 않는다.
 */
@RestController
@RequestMapping("/reports")
@Tag(name = "Report", description = "신고 API")
public class ReportController {

    private final PostService postService;
    private final ReportService reportService;

    public ReportController(PostService postService, ReportService reportService) {
        this.postService = postService;
        this.reportService = reportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "신고 접수", description = "게시물을 신고합니다. PENDING 상태로 접수됩니다. 없는 게시물이면 404.")
    public ApiResponse<ReportResponse> report(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody CreateReportRequest request) {

        requirePost(request.postId());

        Report report = reportService.report(userId, request.postId(), request.reason());
        return ApiResponse.ok(ReportResponse.from(report), "신고가 접수되었습니다.");
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "신고 조회", description = "id로 신고 하나를 조회합니다. 없으면 404.")
    public ApiResponse<ReportResponse> getReport(@PathVariable Long reportId) {
        Report report = reportService.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다. id=" + reportId));
        return ApiResponse.ok(ReportResponse.from(report));
    }

    @PatchMapping("/{reportId}/status")
    @Operation(summary = "신고 처리",
            description = "신고 상태를 PENDING → REVIEWED → RESOLVED 순서로 한 단계씩 전환합니다. 없는 신고면 404, 허용되지 않는 전환이면 409.")
    public ApiResponse<ReportResponse> changeStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody UpdateReportStatusRequest request) {

        // 도메인/application은 Spring Web에 의존하지 않으므로, 도메인 예외의 HTTP 상태 변환은 컨트롤러에서 처리한다.
        try {
            Report report = reportService.changeStatus(reportId, request.status());
            return ApiResponse.ok(ReportResponse.from(report), "신고 상태를 변경했습니다.");
        } catch (ReportNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (InvalidReportStatusTransitionException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    private void requirePost(Long id) {
        if (postService.findById(id).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "글을 찾을 수 없습니다. id=" + id);
        }
    }
}
