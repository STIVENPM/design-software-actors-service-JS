package co.sena.actors.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ActivityLogEntry {

    private UUID id;
    private UUID instructorId;
    private UUID learnerId;
    private String eventType;
    private String description;
    private String previousValue;
    private String newValue;
    private UUID recordedBy;
    private OffsetDateTime recordedAt;

    public ActivityLogEntry() {
    }

    public ActivityLogEntry(UUID id, UUID instructorId, UUID learnerId, String eventType, String description,
                            String previousValue, String newValue, UUID recordedBy, OffsetDateTime recordedAt) {
        this.id = id;
        this.instructorId = instructorId;
        this.learnerId = learnerId;
        this.eventType = eventType;
        this.description = description;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(UUID instructorId) {
        this.instructorId = instructorId;
    }

    public UUID getLearnerId() {
        return learnerId;
    }

    public void setLearnerId(UUID learnerId) {
        this.learnerId = learnerId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(String previousValue) {
        this.previousValue = previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public UUID getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(UUID recordedBy) {
        this.recordedBy = recordedBy;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(OffsetDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
