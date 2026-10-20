package co.sena.actors.adapter.out.persistence.company;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Company only: city_id is a logical UUID reference, never a JPA association. */
@Entity
@Table(name = "company", schema = "actors_companies")
public class CompanyJpaEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String nit;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "trade_name", length = 200)
    private String tradeName;

    @Column(name = "economic_activity", length = 100)
    private String economicActivity;

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "contact_name", length = 200)
    private String contactName;

    @Column(name = "contact_email", columnDefinition = "text")
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Version
    @Column(name = "row_version", nullable = false)
    private int rowVersion;

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public String getNit() { return nit; }
    public void setNit(String value) { this.nit = value; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String value) { this.businessName = value; }

    public String getTradeName() { return tradeName; }
    public void setTradeName(String value) { this.tradeName = value; }

    public String getEconomicActivity() { return economicActivity; }
    public void setEconomicActivity(String value) { this.economicActivity = value; }

    public String getAddress() { return address; }
    public void setAddress(String value) { this.address = value; }

    public UUID getCityId() { return cityId; }
    public void setCityId(UUID value) { this.cityId = value; }

    public String getContactName() { return contactName; }
    public void setContactName(String value) { this.contactName = value; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String value) { this.contactEmail = value; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String value) { this.contactPhone = value; }

    public boolean getActive() { return active; }
    public void setActive(boolean value) { this.active = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt = value; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID value) { this.createdBy = value; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt = value; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID value) { this.updatedBy = value; }

    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime value) { this.deletedAt = value; }

    public UUID getDeletedBy() { return deletedBy; }
    public void setDeletedBy(UUID value) { this.deletedBy = value; }

    public int getRowVersion() { return rowVersion; }
    public void setRowVersion(int value) { this.rowVersion = value; }
}
