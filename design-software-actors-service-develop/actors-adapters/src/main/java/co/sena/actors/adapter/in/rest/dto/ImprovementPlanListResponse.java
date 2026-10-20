package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.application.port.in.ManageImprovementPlanUseCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ImprovementPlanListResponse(List<ImprovementPlanResponse> data, Pagination pagination) {
    public record Pagination(int page, @JsonProperty("page_size") int pageSize,
            @JsonProperty("total_items") long totalItems, @JsonProperty("total_pages") int totalPages) {}

    public static ImprovementPlanListResponse from(ManageImprovementPlanUseCase.Page page) {
        return new ImprovementPlanListResponse(page.content().stream().map(ImprovementPlanResponse::from).toList(),
                new Pagination(page.page(), page.pageSize(), page.totalItems(), page.totalPages()));
    }
}