package co.sena.actors.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.sena.actors.adapter.out.persistence.LearnerPersistenceAdapter;
import co.sena.actors.adapter.config.LearnerEnrollmentStatusSeed;
import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.adapter.in.event.IamEventApplicationService;
import co.sena.actors.adapter.out.persistence.IamEventInboxJdbcAdapter;
import co.sena.actors.application.port.in.IamUserDeactivated;
import co.sena.actors.application.port.in.IamUserCreated;
import co.sena.actors.application.port.in.IamActorType;
import co.sena.actors.application.port.out.ActivityLogRepositoryPort;
import co.sena.actors.application.port.out.CompetencyAssignmentRepositoryPort;
import co.sena.actors.application.port.out.IamEventInboxPort;
import co.sena.actors.application.port.out.InstructorAreaRepositoryPort;
import co.sena.actors.application.port.out.InstructorRepositoryPort;
import co.sena.actors.application.port.out.LearnerRepositoryPort;
import co.sena.actors.application.usecase.IamUserSyncUseCase;
import co.sena.actors.domain.model.DocumentType;
import co.sena.actors.domain.model.Learner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.List;
import java.time.Clock;
import jakarta.persistence.EntityManager;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

/** Real actors-db DDL plus JPA insert and version-safe learner update.
 * Global Hibernate validate is blocked by actors-db's missing company_visit.first_visit_late.
 */
@SpringBootTest(classes = ActorsApiApplication.class)
@ActiveProfiles("test")
@Import(LearnerLiquibaseJpaIT.IamTestBeans.class)
class LearnerLiquibaseJpaIT {
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("actors_learner_it").withUsername("actors").withPassword("actors");

    static {
        String repoDirectory = System.getenv("ACTORS_DB_REPO_DIR");
        if (repoDirectory == null || !Files.isRegularFile(
                Path.of(repoDirectory, "changelog", "changelog-master.yaml"))) {
            throw new IllegalStateException("ACTORS_DB_REPO_DIR must point to actors-db checkout");
        }
        POSTGRES.start();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(), POSTGRES.getPassword());
                var resources = new FileSystemResourceAccessor(Path.of(repoDirectory).toFile());
                var liquibase = new Liquibase("changelog/changelog-master.yaml", resources,
                        new JdbcConnection(connection))) {
            liquibase.update();
            try (var statement = connection.createStatement();
                    var result = statement.executeQuery("SELECT to_regclass('actors_tracking.iam_processed_event')")) {
                result.next();
                if (result.getString(1) == null) {
                    statement.execute("""
                            CREATE TABLE actors_tracking.iam_processed_event (
                              event_id UUID PRIMARY KEY, user_id UUID NOT NULL,
                              event_type VARCHAR(60) NOT NULL, processed_at TIMESTAMPTZ NOT NULL)
                            """);
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not apply actors-db Liquibase", exception);
        }
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        properties.add("spring.liquibase.enabled", () -> false);
        properties.add("actors.iam.consumer.enabled", () -> false);
    }

    @Autowired LearnerPersistenceAdapter learners;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired IamEventApplicationService iamEvents;
    @Autowired InstructorRepositoryPort instructorPort;
    @Autowired LearnerRepositoryPort learnerPort;
    @Autowired CompetencyAssignmentRepositoryPort assignmentPort;
    @Autowired InstructorAreaRepositoryPort areaPort;
    @Autowired ActivityLogRepositoryPort logPort;
    // Existing seed inserts fewer columns than the real status_category DDL requires.
    @MockBean LearnerEnrollmentStatusSeed existingIncompatibleSeed;

    @Test
    @Transactional
    void learnerInsertAndUpdateSatisfyAuditColumnsAndPreserveSingleRow() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Learner created = Learner.create(id, userId, DocumentType.CC, "D" + id.toString().substring(0, 10),
                "Ana", "Rios", "ana@sena.edu.co", null, null);
        learners.save(created);
        learners.save(created.update(null, null, "Ana Maria", null, null, null, null));
        entityManager.flush();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM actors_learners.learner WHERE id=? AND first_name='Ana Maria'
                    AND created_by IS NOT NULL AND updated_by IS NOT NULL
                """, Integer.class, id)).isEqualTo(1);
    }

    @Test
    void learnerDeactivationUsesLearnerActivityLogAndIsRepeatable() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Learner learner = Learner.create(id, userId, DocumentType.CC,
                "D" + id.toString().substring(0, 10), "Luis", "Diaz",
                "luis@sena.edu.co", null, null);
        learners.save(learner);
        UUID eventId = UUID.randomUUID();

        assertThat(iamEvents.process(new IamUserDeactivated(eventId, userId)))
                .isEqualTo(IamUserSyncUseCase.Result.PROCESSED);
        assertThat(iamEvents.process(new IamUserDeactivated(eventId, userId)))
                .isEqualTo(IamUserSyncUseCase.Result.DUPLICATE);
        assertThat(iamEvents.process(new IamUserDeactivated(UUID.randomUUID(), userId)))
                .isEqualTo(IamUserSyncUseCase.Result.PROCESSED);
        assertThat(jdbc.queryForObject("SELECT is_active FROM actors_learners.learner WHERE id=?",
                Boolean.class, id)).isFalse();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM actors_tracking.activity_log
                WHERE learner_id=? AND instructor_id IS NULL AND event_type='iam.user.deactivated'
                """, Integer.class, id)).isEqualTo(1);
    }

    @Test
    void existingProfilesDeduplicateByEventAndUserAcrossFreshServiceInstances() {
        UUID instructorId = UUID.randomUUID();
        UUID instructorUserId = UUID.randomUUID();
        seedInstructor(instructorUserId, instructorId, UUID.randomUUID(), UUID.randomUUID());
        UUID learnerId = UUID.randomUUID();
        UUID learnerUserId = UUID.randomUUID();
        learners.save(Learner.create(learnerId, learnerUserId, DocumentType.CC,
                "D" + learnerId.toString().substring(0, 10), "Old", "Name",
                "old@sena.edu.co", null, null));

        UUID instructorEventId = UUID.randomUUID();
        var instructorEvent = new IamUserCreated(instructorEventId, instructorUserId,
                IamActorType.INSTRUCTOR, "ana@sena.edu.co", "Ana", "Rios", List.of("INSTRUCTOR"));
        assertThat(iamEvents.process(instructorEvent)).isEqualTo(IamUserSyncUseCase.Result.PROCESSED);
        assertThat(iamEvents.process(instructorEvent)).isEqualTo(IamUserSyncUseCase.Result.DUPLICATE);

        // A new service/inbox adapter instance uses a new connection to the same PostgreSQL.
        // This checks durable state across instances, though not a whole JVM restart.
        var freshJdbc = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        var freshUseCase = new IamUserSyncUseCase(new IamEventInboxJdbcAdapter(freshJdbc),
                instructorPort, learnerPort, assignmentPort, areaPort, logPort,
                Clock.systemUTC(), CurrentUserProvider.SYSTEM_ACTOR_ID);
        assertThat(freshUseCase.created(instructorEvent)).isEqualTo(IamUserSyncUseCase.Result.DUPLICATE);

        var secondInstructorEvent = new IamUserCreated(UUID.randomUUID(), instructorUserId,
                IamActorType.INSTRUCTOR, "ana2@sena.edu.co", "Ana Maria", "Rios", List.of());
        assertThat(iamEvents.process(secondInstructorEvent)).isEqualTo(IamUserSyncUseCase.Result.PROCESSED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM actors_instructors.instructor WHERE user_id=?",
                Integer.class, instructorUserId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT first_name FROM actors_instructors.instructor WHERE id=?",
                String.class, instructorId)).isEqualTo("Ana Maria");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM actors_tracking.activity_log
                WHERE instructor_id=? AND event_type='iam.user.created'
                """, Integer.class, instructorId)).isEqualTo(2);

        var learnerEvent = new IamUserCreated(UUID.randomUUID(), learnerUserId,
                IamActorType.LEARNER, "luis@sena.edu.co", "Luis", "Diaz", List.of());
        assertThat(iamEvents.process(learnerEvent)).isEqualTo(IamUserSyncUseCase.Result.PROCESSED);
        assertThat(iamEvents.process(learnerEvent)).isEqualTo(IamUserSyncUseCase.Result.DUPLICATE);
        assertThat(iamEvents.process(new IamUserCreated(UUID.randomUUID(), learnerUserId,
                IamActorType.LEARNER, "luis2@sena.edu.co", "Luis", "Diaz", List.of())))
                .isEqualTo(IamUserSyncUseCase.Result.PROCESSED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM actors_learners.learner WHERE user_id=?",
                Integer.class, learnerUserId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM actors_tracking.activity_log
                WHERE learner_id=? AND event_type='iam.user.created'
                """, Integer.class, learnerId)).isEqualTo(2);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO actors_instructors.instructor
                  (id,user_id,document_type,document_number,first_name,last_name,email,
                   default_max_hours_per_week,created_at,created_by,updated_at,updated_by)
                VALUES (?,?, 'CC', ?, 'Duplicate','User','dup@sena.edu.co',40,
                        now(),?,now(),?)
                """, UUID.randomUUID(), instructorUserId, "D" + UUID.randomUUID().toString().substring(0, 10),
                CurrentUserProvider.SYSTEM_ACTOR_ID, CurrentUserProvider.SYSTEM_ACTOR_ID))
                .hasMessageContaining("uq_instructor_user_id");
    }

    @Test
    void instructorDeactivationCommitsDependentsAndLogAndRollsBackOnLogFailure() {
        UUID userId = UUID.randomUUID();
        UUID instructorId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        seedInstructor(userId, instructorId, areaId, assignmentId);
        UUID eventId = UUID.randomUUID();
        assertThat(iamEvents.process(new IamUserDeactivated(eventId, userId)))
                .isEqualTo(IamUserSyncUseCase.Result.PROCESSED);
        assertThat(jdbc.queryForObject("SELECT is_active FROM actors_instructors.instructor WHERE id=?",
                Boolean.class, instructorId)).isFalse();
        assertThat(jdbc.queryForObject("SELECT is_active FROM actors_instructors.instructor_area WHERE id=?",
                Boolean.class, areaId)).isFalse();
        assertThat(jdbc.queryForObject("SELECT is_active FROM actors_instructors.competency_assignment WHERE id=?",
                Boolean.class, assignmentId)).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM actors_tracking.activity_log WHERE instructor_id=?",
                Integer.class, instructorId)).isEqualTo(1);
        assertThat(iamEvents.process(new IamUserDeactivated(eventId, userId)))
                .isEqualTo(IamUserSyncUseCase.Result.DUPLICATE);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM actors_tracking.activity_log WHERE instructor_id=?",
                Integer.class, instructorId)).isEqualTo(1);

        UUID failedUserId = UUID.randomUUID();
        UUID failedInstructorId = UUID.randomUUID();
        UUID failedAreaId = UUID.randomUUID();
        UUID failedAssignmentId = UUID.randomUUID();
        seedInstructor(failedUserId, failedInstructorId, failedAreaId, failedAssignmentId);
        jdbc.execute("""
                CREATE FUNCTION actors_tracking.fail_iam_log() RETURNS trigger AS $$
                BEGIN RAISE EXCEPTION 'forced activity log failure'; END; $$ LANGUAGE plpgsql
                """);
        jdbc.execute("""
                CREATE TRIGGER fail_iam_log BEFORE INSERT ON actors_tracking.activity_log
                FOR EACH ROW EXECUTE FUNCTION actors_tracking.fail_iam_log()
                """);
        UUID failedEventId = UUID.randomUUID();
        try {
            assertThatThrownBy(() -> iamEvents.process(
                    new IamUserDeactivated(failedEventId, failedUserId)))
                    .isInstanceOf(RuntimeException.class);
            assertThat(jdbc.queryForObject("SELECT is_active FROM actors_instructors.instructor WHERE id=?",
                    Boolean.class, failedInstructorId)).isTrue();
            assertThat(jdbc.queryForObject("SELECT is_active FROM actors_instructors.instructor_area WHERE id=?",
                    Boolean.class, failedAreaId)).isTrue();
            assertThat(jdbc.queryForObject("SELECT is_active FROM actors_instructors.competency_assignment WHERE id=?",
                    Boolean.class, failedAssignmentId)).isTrue();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM actors_tracking.iam_processed_event WHERE event_id=?",
                    Integer.class, failedEventId)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER fail_iam_log ON actors_tracking.activity_log");
            jdbc.execute("DROP FUNCTION actors_tracking.fail_iam_log()");
        }
    }

    private void seedInstructor(UUID userId, UUID instructorId, UUID areaId, UUID assignmentId) {
        UUID actor = CurrentUserProvider.SYSTEM_ACTOR_ID;
        jdbc.update("""
                INSERT INTO actors_instructors.instructor
                 (id,user_id,document_type,document_number,first_name,last_name,email,
                  default_max_hours_per_week,created_at,created_by,updated_at,updated_by)
                VALUES (?,?, 'CC', ?, 'Ana','Rios','ana@sena.edu.co',40,now(),?,now(),?)
                """, instructorId, userId, "D" + instructorId.toString().substring(0, 10), actor, actor);
        jdbc.update("""
                INSERT INTO actors_instructors.instructor_area
                 (id,instructor_id,area_name,created_at,created_by,updated_at,updated_by)
                VALUES (?,?,'Software',now(),?,now(),?)
                """, areaId, instructorId, actor, actor);
        jdbc.update("""
                INSERT INTO actors_instructors.competency_assignment
                 (id,instructor_id,competency_id,certified_at,created_at,created_by,updated_at,updated_by)
                VALUES (?,?,?,current_date,now(),?,now(),?)
                """, assignmentId, instructorId, UUID.randomUUID(), actor, actor);
    }

    @TestConfiguration
    static class IamTestBeans {
        @Bean
        IamEventInboxPort testIamInbox(JdbcTemplate jdbc) {
            return new IamEventInboxJdbcAdapter(jdbc);
        }

        @Bean
        IamEventApplicationService testIamService(IamEventInboxPort inbox,
                InstructorRepositoryPort instructors, LearnerRepositoryPort learners,
                CompetencyAssignmentRepositoryPort assignments, InstructorAreaRepositoryPort areas,
                ActivityLogRepositoryPort logs) {
            return new IamEventApplicationService(new IamUserSyncUseCase(inbox, instructors, learners,
                    assignments, areas, logs, Clock.systemUTC(), CurrentUserProvider.SYSTEM_ACTOR_ID));
        }
    }
}
