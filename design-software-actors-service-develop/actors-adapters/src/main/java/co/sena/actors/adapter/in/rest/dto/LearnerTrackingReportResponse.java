package co.sena.actors.adapter.in.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LearnerTrackingReportResponse(
        List<Entry> data,
        Pagination pagination) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Entry(
            UUID learnerId,
            String documentNumber,
            String fullName,
            String enrollmentStatus,
            UUID fichaId,
            String stage,
            LocalDate fichaEnrollmentDate,
            String productiveStageStatus,
            UUID productiveStageCompanyId) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Pagination(int page, int pageSize, long totalItems, int totalPages) {
    }
}