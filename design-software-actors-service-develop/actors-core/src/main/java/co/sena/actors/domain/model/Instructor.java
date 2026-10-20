package co.sena.actors.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base instructor profile (table actors_instructors.instructor).
 * full_name is derived (first_name + last_name), never persisted.
 * contract_type lives in instructor_contract (HU-ACT-002), not here.
 */
public class Instructor {

    private UUID id;
    private UUID userId;
    private DocumentType documentType;
    private String documentNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private BigDecimal defaultMaxHoursPerWeek;
    private boolean active = true;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
    private OffsetDateTime deletedAt;
    private UUID deletedBy;
    private int rowVersion;

    public String getFullName() {
        String first = firstName == null ? "" : firstName;
        String last = lastName == null ? "" : lastName;
        return (first + " " + last).trim().replaceAll("\\s+", " ");
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BigDecimal getDefaultMaxHoursPerWeek() {
        return defaultMaxHoursPerWeek;
    }

    public void setDefaultMaxHoursPerWeek(BigDecimal defaultMaxHoursPerWeek) {
        this.defaultMaxHoursPerWeek = defaultMaxHoursPerWeek;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public UUID getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(UUID deletedBy) {
        this.deletedBy = deletedBy;
    }

    public int getRowVersion() {
        return rowVersion;
    }

    public void setRowVersion(int rowVersion) {
        this.rowVersion = rowVersion;
    }
}
