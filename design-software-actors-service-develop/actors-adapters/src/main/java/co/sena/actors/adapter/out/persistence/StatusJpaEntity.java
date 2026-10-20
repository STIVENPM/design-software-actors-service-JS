package co.sena.actors.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "status", schema = "actors_parameterization")
public class StatusJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "code")
    private String code;

    protected StatusJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }
}
