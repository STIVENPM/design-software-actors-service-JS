package co.sena.actors.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.UUID;

public class AvailabilityExceptionCreateRequest {

    @NotBlank
    @Pattern(
            regexp = "SICK_LEAVE|VACATION|COMMISSION|PERSONAL_LEAVE|TRAINING|OTHER",
            message = "exception_type must be one of: SICK_LEAVE, VACATION, COMMISSION, PERSONAL_LEAVE, TRAINING, OTHER"
    )
    @JsonProperty("exception_type")
    private String exceptionType;

    @NotNull
    @JsonProperty("start_datetime")
    private LocalDateTime startDatetime;

    @NotNull
    @JsonProperty("end_datetime")
    private LocalDateTime endDatetime;

    @JsonProperty("description")
    private String description;

    @JsonProperty("approved_by")
    private UUID approvedBy;

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public LocalDateTime getStartDatetime() {
        return startDatetime;
    }

    public void setStartDatetime(LocalDateTime startDatetime) {
        this.startDatetime = startDatetime;
    }

    public LocalDateTime getEndDatetime() {
        return endDatetime;
    }

    public void setEndDatetime(LocalDateTime endDatetime) {
        this.endDatetime = endDatetime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }
}
