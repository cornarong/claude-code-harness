package com.echo.api;

import com.echo.api.moderation.CreateReportRequest;
import com.echo.api.moderation.UpdateReportStatusRequest;
import com.echo.moderation.domain.ReportStatus;
import com.echo.post.application.PostService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /reports, GET /reports/{reportId}, PATCH /reports/{reportId}/status 통합 테스트.
 * post → moderation → HTTP 조립 경로가 모듈 간 직접 의존 없이 앱 계층에서 연결되는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class ReportIntegrationTest {

    private static final long UNKNOWN_ID = 999999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostService postService;

    @Test
    @DisplayName("POST /reports — 정상 접수 시 201, PENDING 상태로 저장")
    void report_success() throws Exception {
        Long postId = createPost();

        submitReport(postId, "스팸입니다")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reporterId").value(7))
                .andExpect(jsonPath("$.data.postId").value(postId))
                .andExpect(jsonPath("$.data.reason").value("스팸입니다"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /reports — 없는 게시물이면 404 반환")
    void report_postNotFound_404() throws Exception {
        submitReport(UNKNOWN_ID, "스팸입니다")
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /reports — 신고 사유가 비었거나 500자를 넘으면 400 반환")
    void report_invalidReason_400() throws Exception {
        Long postId = createPost();

        submitReport(postId, " ").andExpect(status().isBadRequest());
        submitReport(postId, "a".repeat(501)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /reports/{reportId} — 접수한 신고를 조회")
    void getReport_success() throws Exception {
        Long reportId = createReport();

        mockMvc.perform(get("/reports/{reportId}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(reportId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /reports/{reportId} — 없는 신고면 404 반환")
    void getReport_notFound_404() throws Exception {
        mockMvc.perform(get("/reports/{reportId}", UNKNOWN_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /reports/{reportId}/status — PENDING → REVIEWED → RESOLVED 순서로 전환되고 조회에 반영된다")
    void changeStatus_inOrder() throws Exception {
        Long reportId = createReport();

        changeStatus(reportId, ReportStatus.REVIEWED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVIEWED"));

        changeStatus(reportId, ReportStatus.RESOLVED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        mockMvc.perform(get("/reports/{reportId}", reportId))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("PATCH /reports/{reportId}/status — 단계를 건너뛰거나 되돌리면 409 반환")
    void changeStatus_invalidTransition_409() throws Exception {
        Long reportId = createReport();

        // PENDING → RESOLVED 건너뛰기
        changeStatus(reportId, ReportStatus.RESOLVED).andExpect(status().isConflict());

        // REVIEWED → PENDING 되돌리기
        changeStatus(reportId, ReportStatus.REVIEWED).andExpect(status().isOk());
        changeStatus(reportId, ReportStatus.PENDING).andExpect(status().isConflict());

        // 실패한 전환은 상태를 바꾸지 않는다
        mockMvc.perform(get("/reports/{reportId}", reportId))
                .andExpect(jsonPath("$.data.status").value("REVIEWED"));
    }

    @Test
    @DisplayName("PATCH /reports/{reportId}/status — 없는 신고면 404 반환")
    void changeStatus_notFound_404() throws Exception {
        changeStatus(UNKNOWN_ID, ReportStatus.REVIEWED).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /reports/{reportId}/status — status가 없거나 알 수 없는 값이면 400 반환")
    void changeStatus_invalidBody_400() throws Exception {
        Long reportId = createReport();

        mockMvc.perform(patch("/reports/{reportId}/status", reportId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/reports/{reportId}/status", reportId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DELETED\"}"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions submitReport(Long postId, String reason) throws Exception {
        return mockMvc.perform(post("/reports")
                .header("X-User-Id", 7)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReportRequest(postId, reason))));
    }

    private ResultActions changeStatus(Long reportId, ReportStatus status) throws Exception {
        return mockMvc.perform(patch("/reports/{reportId}/status", reportId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateReportStatusRequest(status))));
    }

    /** 신고를 접수하고 생성된 id를 반환한다. */
    private Long createReport() throws Exception {
        String response = submitReport(createPost(), "스팸입니다")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("id").asLong();
    }

    /** 글을 저장하고 생성된 id를 반환한다. */
    private Long createPost() {
        return postService.create(1L, "신고 테스트용 글").getId();
    }
}
