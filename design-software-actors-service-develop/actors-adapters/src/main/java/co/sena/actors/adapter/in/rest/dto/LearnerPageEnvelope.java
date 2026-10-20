package co.sena.actors.adapter.in.rest.dto;

import java.util.List;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public record LearnerPageEnvelope(
        List<LearnerDto> data,
        PaginationInfo pagination) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PaginationInfo(
            int totalItems,
            int currentPage,
            int totalPages) {}
}
