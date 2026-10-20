package co.sena.actors.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.sena.actors.adapter.out.persistence.IamEventInboxJdbcAdapter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

/** Applies the actual actors-db changelog; supports baseline and isolated migration trials. */
class IamInboxLiquibaseIT {
    @Test
    void liquibaseInboxOrExplicitTrialTableProvidesPersistentTransactionalDeduplication() throws Exception {
        String repoDirectory = System.getenv("ACTORS_DB_REPO_DIR");
        if (repoDirectory == null || !Files.isRegularFile(
                Path.of(repoDirectory, "changelog", "changelog-master.yaml"))) {
            throw new IllegalStateException("ACTORS_DB_REPO_DIR must point to actors-db checkout");
        }
        try (var postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("actors_it").withUsername("actors").withPassword("actors")) {
            postgres.start();
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(),
                    postgres.getUsername(), postgres.getPassword());
                    var resources = new FileSystemResourceAccessor(Path.of(repoDirectory).toFile());
                    var liquibase = new Liquibase("changelog/changelog-master.yaml", resources,
                            new JdbcConnection(connection))) {
                liquibase.update();
                liquibase.update(); // A second update must be safe.
            }
            DataSource dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(),
                    postgres.getUsername(), postgres.getPassword());
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM information_schema.columns
                    WHERE table_schema='actors_learners' AND table_name='learner'
                      AND column_name IN ('created_by', 'updated_by')
                    """, Integer.class)).isEqualTo(2);
            int migratedInbox = jdbc.queryForObject("""
                    SELECT count(*) FROM information_schema.tables
                    WHERE table_schema='actors_tracking' AND table_name='iam_processed_event'
                    """, Integer.class);

            var inbox = new IamEventInboxJdbcAdapter(jdbc);
            UUID eventId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            if (migratedInbox == 0) {
                assertThatThrownBy(() -> inbox.claim(eventId, userId, "iam.user.created"))
                        .hasMessageContaining("iam_processed_event");
                // Baseline checkout: trial DDL in a disposable database is not migration evidence.
                jdbc.execute("""
                        CREATE TABLE actors_tracking.iam_processed_event (
                            event_id UUID PRIMARY KEY, user_id UUID NOT NULL,
                            event_type VARCHAR(60) NOT NULL, processed_at TIMESTAMPTZ NOT NULL)
                        """);
            } else {
                assertThat(migratedInbox).isEqualTo(1);
                assertThat(jdbc.queryForObject("""
                        SELECT has_table_privilege('actors_writer',
                          'actors_tracking.iam_processed_event', 'INSERT')
                        """, Boolean.class)).isTrue();
            }
            var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
            Boolean rolledBackClaim = transaction.execute(status -> {
                assertThat(inbox.claim(eventId, userId, "iam.user.created")).isTrue();
                status.setRollbackOnly();
                return true;
            });
            assertThat(rolledBackClaim).isTrue();
            assertThat(inbox.claim(eventId, userId, "iam.user.created")).isTrue();
            assertThat(inbox.claim(eventId, userId, "iam.user.created")).isFalse();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM actors_tracking.iam_processed_event",
                    Integer.class)).isEqualTo(1);
        }
    }
}
