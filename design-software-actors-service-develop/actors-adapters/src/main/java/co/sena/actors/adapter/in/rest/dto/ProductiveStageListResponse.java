package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.application.port.in.ProductiveStageUseCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ProductiveStageListResponse(List<ProductiveStageResponse> data, Pagination pagination) {
    public record Pagination(int page, @JsonProperty("page_size") int pageSize,
            @JsonProperty("total_items") long totalItems,
            @JsonProperty("total_pages") int totalPages) {}

    public static ProductiveStageListResponse from(ProductiveStageUseCase.Page page) {
        return new ProductiveStageListResponse(
                page.content().stream().map(ProductiveStageResponse::from).toList(),
                new Pagination(page.page(), page.pageSize(), page.totalItems(), page.totalPages()));
    }
}
