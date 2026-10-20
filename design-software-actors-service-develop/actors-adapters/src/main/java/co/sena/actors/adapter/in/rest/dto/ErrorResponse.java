package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.adapter.config.TraceIds;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Error shaped as _shared.yaml#/components/schemas/Error. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(ErrorBody error) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(
            String code,
            String message,
            List<ErrorDetail> details,
            @JsonProperty("trace_id") String traceId) {
    }

    public record ErrorDetail(String field, String issue) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message, null, TraceIds.current()));
    }

    public static ErrorResponse of(String code, String message, List<ErrorDetail> details) {
        return new ErrorResponse(new ErrorBody(code, message, details, TraceIds.current()));
    }
}
