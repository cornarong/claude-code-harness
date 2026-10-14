package com.echo.moderation.application;

import com.echo.moderation.domain.Report;
import com.echo.moderation.domain.ReportNotFoundException;
import com.echo.moderation.domain.ReportStatus;
import com.echo.moderation.infrastructure.ReportRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Report 도메인 애플리케이션 서비스. 신고 접수·조회·상태 전환 유스케이스.
 *
 * <p>게시물 존재 여부는 이 모듈이 알 수 없다(post 모듈에 의존하지 않음). 호출자(앱 계층)가 먼저 확인한다.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /** 신고 접수. PENDING 상태로 저장된다. */
    @Transactional
    public Report report(Long reporterId, Long postId, String reason) {
        return reportRepository.save(new Report(reporterId, postId, reason));
    }

    /** 단건 조회. 존재 여부 판단(404 등 HTTP 처리)은 호출자(컨트롤러) 몫이라 Optional로 반환한다. */
    public Optional<Report> findById(Long reportId) {
        return reportRepository.findById(reportId);
    }

    /** 상태 전환. 없는 신고면 ReportNotFoundException, 허용되지 않는 전환이면 InvalidReportStatusTransitionException. */
    @Transactional
    public Report changeStatus(Long reportId, ReportStatus next) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));
        report.changeStatus(next);
        return report;
    }
}
