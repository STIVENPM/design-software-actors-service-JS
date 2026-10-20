package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.application.port.in.CompanyUseCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CompanyListResponse(List<CompanyResponse> data, Pagination pagination) {
    public record Pagination(int page, @JsonProperty("page_size") int pageSize,
            @JsonProperty("total_items") long totalItems, @JsonProperty("total_pages") int totalPages) {}

    public static CompanyListResponse from(CompanyUseCase.Page page) {
        return new CompanyListResponse(page.content().stream().map(CompanyResponse::from).toList(),
                new Pagination(page.page(), page.pageSize(), page.totalItems(), page.totalPages()));
    }
}
