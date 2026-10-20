package co.sena.actors.adapter.in.rest.dto;

import co.sena.actors.domain.model.ActivityLogEntry;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ActivityLogEntryDto(
        UUID id,
        @JsonProperty("instructor_id") UUID instructorId,
        @JsonProperty("learner_id") UUID learnerId,
        @JsonProperty("event_type") String eventType,
        String description,
        @JsonProperty("previous_value") String previousValue,
        @JsonProperty("new_value") String newValue,
        @JsonProperty("recorded_by") UUID recordedBy,
        @JsonProperty("recorded_at") OffsetDateTime recordedAt) {

    public static ActivityLogEntryDto from(ActivityLogEntry domain) {
        return new ActivityLogEntryDto(
                domain.getId(),
                domain.getInstructorId(),
                domain.getLearnerId(),
                domain.getEventType(),
                domain.getDescription(),
                domain.getPreviousValue(),
                domain.getNewValue(),
                domain.getRecordedBy(),
                domain.getRecordedAt()
        );
    }
}
