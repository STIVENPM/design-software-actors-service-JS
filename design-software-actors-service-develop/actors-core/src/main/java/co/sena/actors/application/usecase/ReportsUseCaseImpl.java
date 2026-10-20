package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.InstructorLoadReportEntry;
import co.sena.actors.application.port.in.InstructorLoadReportQuery;
import co.sena.actors.application.port.in.LearnerTrackingReportEntry;
import co.sena.actors.application.port.in.LearnerTrackingReportQuery;
import co.sena.actors.application.port.in.ReportPage;
import co.sena.actors.application.port.in.ReportsUseCase;
import co.sena.actors.application.port.out.ReportsRepositoryPort;
import java.util.Objects;

public class ReportsUseCaseImpl implements ReportsUseCase {

    private final ReportsRepositoryPort repository;

    public ReportsUseCaseImpl(ReportsRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public ReportPage<LearnerTrackingReportEntry> learnerTracking(LearnerTrackingReportQuery query) {
        validatePage(query.page(), query.pageSize());
        validateLearnerFilters(query);
        long total = repository.countLearnerTracking(query);
        return page(repository.findLearnerTracking(query), query.page(), query.pageSize(), total);
    }

    @Override
    public ReportPage<InstructorLoadReportEntry> instructorLoad(InstructorLoadReportQuery query) {
        validatePage(query.page(), query.pageSize());
        validateInstructorFilters(query);
        long total = repository.countInstructorLoad(query);
        return page(repository.findInstructorLoad(query), query.page(), query.pageSize(), total);
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("page must be >= 1 and page_size must be between 1 and 100");
        }
    }

    private void validateLearnerFilters(LearnerTrackingReportQuery query) {
        if (query.enrollmentStatus() != null && !java.util.Set.of(
                "ACTIVE", "DROPOUT", "TRANSFERRED", "GRADUATED", "CANCELLED")
                .contains(query.enrollmentStatus())) {
            throw new IllegalArgumentException("Unsupported enrollment_status");
        }
        if (query.from() != null && query.to() != null && !query.from().isBefore(query.to())) {
            throw new IllegalArgumentException("from must be before to");
        }
    }

    private void validateInstructorFilters(InstructorLoadReportQuery query) {
        if (query.from() != null && query.to() != null && !query.from().isBefore(query.to())) {
            throw new IllegalArgumentException("from must be before to");
        }
    }

    private <T> ReportPage<T> page(java.util.List<T> data, int page, int pageSize, long total) {
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return new ReportPage<>(Objects.requireNonNull(data), page, pageSize, total, totalPages);
    }
}