package com.echo.api.moderation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /reports 요청 DTO.
 * 도메인에서도 같은 제약을 강제하지만, Bean Validation으로 HTTP 400을 먼저 반환한다.
 */
public record CreateReportRequest(
    @NotNull(message = "postId는 비어 있을 수 없습니다.")
    Long postId,

    @NotBlank(message = "신고 사유는 비어 있을 수 없습니다.")
    @Size(max = 500, message = "신고 사유는 500자를 초과할 수 없습니다.")
    String reason
) {
}
