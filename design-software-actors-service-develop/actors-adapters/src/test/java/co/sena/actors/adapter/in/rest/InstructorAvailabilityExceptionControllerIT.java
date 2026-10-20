package co.sena.actors.adapter.in.rest;

import co.sena.actors.adapter.out.persistence.InstructorAvailabilityExceptionJpaEntity;
import co.sena.actors.adapter.out.persistence.InstructorAvailabilityExceptionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InstructorAvailabilityExceptionControllerIT.TestApplication.class)
@Testcontainers
class InstructorAvailabilityExceptionControllerIT {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "co.sena.actors")
    @EnableJpaRepositories(basePackages = "co.sena.actors.adapter.out.persistence")
    static class TestApplication {}

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("actors_test")
            .withUsername("actors")
            .withPassword("actors");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "public");
        registry.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "UTC");

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_instructors;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_learners;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_tracking;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_parameterization;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS parameterization;");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create schema", e);
        }
    }

    @Autowired
    private InstructorAvailabilityExceptionJpaRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldPersistAvailabilityExceptionInRealPostgres() {
        UUID instructorId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        InstructorAvailabilityExceptionJpaEntity entity = new InstructorAvailabilityExceptionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setInstructorId(instructorId);
        entity.setExceptionType("SICK_LEAVE");
        entity.setStartDatetime(LocalDateTime.of(2026, 10, 1, 8, 0));
        entity.setEndDatetime(LocalDateTime.of(2026, 10, 15, 18, 0));
        entity.setDescription("Incapacidad médica");
        entity.setApprovedBy(null);
        entity.setActive(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy(createdBy);
        entity.setUpdatedBy(createdBy);
        entity.setDeletedAt(null);
        entity.setDeletedBy(null);
        entity.setRowVersion(0);

        InstructorAvailabilityExceptionJpaEntity saved = repository.saveAndFlush(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDescription()).isEqualTo("Incapacidad médica");
        assertThat(saved.isActive()).isTrue();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void shouldSoftDeleteExceptionAndSetActiveFalse() {
        UUID instructorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        InstructorAvailabilityExceptionJpaEntity entity = new InstructorAvailabilityExceptionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setInstructorId(instructorId);
        entity.setExceptionType("TRAINING");
        entity.setStartDatetime(LocalDateTime.of(2026, 12, 1, 0, 0));
        entity.setEndDatetime(LocalDateTime.of(2026, 12, 5, 23, 59));
        entity.setDescription("Capacitación");
        entity.setActive(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy(actorId);
        entity.setUpdatedBy(actorId);
        entity.setRowVersion(0);

        InstructorAvailabilityExceptionJpaEntity saved = repository.saveAndFlush(entity);

        saved.setActive(false);
        saved.setDeletedAt(LocalDateTime.now());
        saved.setDeletedBy(actorId);
        saved.setUpdatedAt(LocalDateTime.now());
        saved.setUpdatedBy(actorId);

        InstructorAvailabilityExceptionJpaEntity updated = repository.saveAndFlush(saved);

        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getDeletedBy()).isEqualTo(actorId);
        assertThat(updated.getDeletedAt()).isNotNull();
    }
}
