package co.sena.actors.adapter.in.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InstructorLoadReportResponse(
        List<Entry> data,
        Pagination pagination) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Entry(
            UUID instructorId,
            String documentNumber,
            String fullName,
            BigDecimal defaultMaxHoursPerWeek,
            BigDecimal currentContractWeeklyHourLimit,
            UUID trainingCenterId,
            int activeAreasCount,
            int activeCompetenciesCount,
            int activeExceptionsCount) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Pagination(int page, int pageSize, long totalItems, int totalPages) {
    }
}