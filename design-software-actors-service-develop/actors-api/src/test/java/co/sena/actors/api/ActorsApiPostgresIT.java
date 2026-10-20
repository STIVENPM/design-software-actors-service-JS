package co.sena.actors.api;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

abstract class ActorsApiPostgresIT {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("actors_test")
            .withUsername("actors")
            .withPassword("actors");

    static {
        POSTGRES.start();
        createSchemas();
    }

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.datasource.hikari.schema", () -> "actors_instructors");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "actors_instructors");
        registry.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "UTC");
    }

    private static void createSchemas() {
        try (var conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_instructors");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_companies");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_learners");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_tracking");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS actors_parameterization");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS parameterization");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to prepare actors-api test schemas", e);
        }
    }
}
