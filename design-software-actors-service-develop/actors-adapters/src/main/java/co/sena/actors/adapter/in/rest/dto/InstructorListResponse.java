package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.application.port.in.InstructorPage;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Paginated list envelope (actors.yaml list responses).
 * Built from the application-layer page so the controller
 * never assembles pagination itself.
 */
public record InstructorListResponse(
        List<InstructorResponse> data,
        Pagination pagination) {

    public record Pagination(
            int page,
            @JsonProperty("page_size") int pageSize,
            @JsonProperty("total_items") long totalItems,
            @JsonProperty("total_pages") int totalPages) {
    }

    public static InstructorListResponse from(InstructorPage page) {
        List<InstructorResponse> data = page.content().stream()
                .map(InstructorResponse::from)
                .toList();
        return new InstructorListResponse(
                data,
                new Pagination(page.page(), page.pageSize(), page.totalItems(), page.totalPages()));
    }
}
