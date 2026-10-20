package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.application.port.in.ActivityLogCursorPage;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ActivityLogCursorPageDto(
        List<ActivityLogEntryDto> data,
        Pagination pagination) {

    public record Pagination(
            @JsonProperty("next_cursor") String nextCursor,
            int limit,
            @JsonProperty("has_more") boolean hasMore) {
    }

    public static ActivityLogCursorPageDto from(ActivityLogCursorPage domain) {
        List<ActivityLogEntryDto> dtos = domain.content().stream()
                .map(ActivityLogEntryDto::from)
                .toList();
        return new ActivityLogCursorPageDto(dtos, new Pagination(domain.nextCursor(), domain.limit(), domain.hasNext()));
    }
}
