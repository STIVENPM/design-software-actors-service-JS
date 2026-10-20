package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.config.ReportRateLimiter;
import co.sena.actors.adapter.in.rest.dto.InstructorLoadReportResponse;
import co.sena.actors.adapter.in.rest.dto.LearnerTrackingReportResponse;
import co.sena.actors.application.port.in.InstructorLoadReportEntry;
import co.sena.actors.application.port.in.InstructorLoadReportQuery;
import co.sena.actors.application.port.in.LearnerTrackingReportEntry;
import co.sena.actors.application.port.in.LearnerTrackingReportQuery;
import co.sena.actors.application.port.in.ReportPage;
import co.sena.actors.application.port.in.ReportsUseCase;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Observed(name = "actors.reports.controller")
public class ReportsController {

    private final ReportsUseCase reports;
    private final ReportRateLimiter rateLimiter;

    public ReportsController(ReportsUseCase reports, ReportRateLimiter rateLimiter) {
        this.reports = reports;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/learner-tracking")
    @PreAuthorize("hasAuthority('ACT_REPORT_VIEW')")
    public ResponseEntity<LearnerTrackingReportResponse> learnerTracking(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "ficha_id", required = false) UUID fichaId,
            @RequestParam(name = "enrollment_status", required = false) String enrollmentStatus,
            @RequestParam(name = "from", required = false) LocalDate from,
            @RequestParam(name = "to", required = false) LocalDate to,
            Authentication authentication) {
        rateLimiter.check(authentication.getName());
        return ResponseEntity.ok(toLearnerResponse(reports.learnerTracking(
                new LearnerTrackingReportQuery(page, pageSize, sort, fichaId, enrollmentStatus, from, to))));
    }

    @GetMapping("/instructor-load")
    @PreAuthorize("hasAuthority('ACT_REPORT_VIEW')")
    public ResponseEntity<InstructorLoadReportResponse> instructorLoad(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "training_center_id", required = false) UUID trainingCenterId,
            @RequestParam(name = "area", required = false) String area,
            @RequestParam(name = "from", required = false) OffsetDateTime from,
            @RequestParam(name = "to", required = false) OffsetDateTime to,
            Authentication authentication) {
        rateLimiter.check(authentication.getName());
        return ResponseEntity.ok(toInstructorResponse(reports.instructorLoad(
                new InstructorLoadReportQuery(page, pageSize, sort, trainingCenterId, area, from, to))));
    }

    private LearnerTrackingReportResponse toLearnerResponse(ReportPage<LearnerTrackingReportEntry> page) {
        return new LearnerTrackingReportResponse(
                page.data().stream().map(entry -> new LearnerTrackingReportResponse.Entry(
                        entry.learnerId(), entry.documentNumber(), entry.fullName(), entry.enrollmentStatus(),
                        entry.fichaId(), entry.stage(), entry.fichaEnrollmentDate(),
                        entry.productiveStageStatus(), entry.productiveStageCompanyId())).toList(),
                new LearnerTrackingReportResponse.Pagination(
                        page.page(), page.pageSize(), page.totalItems(), page.totalPages()));
    }

    private InstructorLoadReportResponse toInstructorResponse(ReportPage<InstructorLoadReportEntry> page) {
        return new InstructorLoadReportResponse(
                page.data().stream().map(entry -> new InstructorLoadReportResponse.Entry(
                        entry.instructorId(), entry.documentNumber(), entry.fullName(),
                        entry.defaultMaxHoursPerWeek(), entry.currentContractWeeklyHourLimit(),
                        entry.trainingCenterId(), entry.activeAreasCount(), entry.activeCompetenciesCount(),
                        entry.activeExceptionsCount())).toList(),
                new InstructorLoadReportResponse.Pagination(
                        page.page(), page.pageSize(), page.totalItems(), page.totalPages()));
    }
}