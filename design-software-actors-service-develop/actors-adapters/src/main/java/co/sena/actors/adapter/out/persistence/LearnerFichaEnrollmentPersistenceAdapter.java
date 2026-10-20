package co.sena.actors.adapter.out.persistence;

import co.sena.actors.application.port.in.ManageLearnerFichaEnrollmentUseCase.CreateCommand;
import co.sena.actors.application.port.out.LearnerFichaEnrollmentRepositoryPort;
import co.sena.actors.application.usecase.LearnerConflictException;
import co.sena.actors.domain.exception.InvalidLearnerEnrollmentException;
import co.sena.actors.domain.model.LearnerFichaEnrollment;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LearnerFichaEnrollmentPersistenceAdapter implements LearnerFichaEnrollmentRepositoryPort {

    private static final String CATEGORY = "LEARNER_ENROLLMENT_STATUS";
    private static final String ACTIVE = "ACTIVE";
    private static final String TRANSFERRED = "TRANSFERRED";

    private final LearnerFichaEnrollmentJpaRepository repository;
    private final LearnerJpaRepository learnerRepository;
    private final JdbcTemplate jdbcTemplate;

    public LearnerFichaEnrollmentPersistenceAdapter(
            LearnerFichaEnrollmentJpaRepository repository,
            LearnerJpaRepository learnerRepository,
            JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.learnerRepository = learnerRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public LearnerFichaEnrollment createEnrollment(UUID learnerId, CreateCommand command) {
        if (repository.existsByLearnerIdAndFichaId(learnerId, command.fichaId())) {
            throw new LearnerConflictException("Learner is already enrolled in ficha: " + command.fichaId());
        }

        LearnerJpaEntity learner = learnerRepository.getReferenceById(learnerId);
        LocalDate enrollmentDate = command.enrollmentDate();
        boolean transferred = TRANSFERRED.equals(statusCode(command.enrollmentStatusId()));
        UUID newEnrollmentStatusId = transferred ? statusIdByCode(ACTIVE) : command.enrollmentStatusId();

        repository.findCurrentByLearnerId(learnerId).ifPresent(current -> {
            if (current.getEnrollmentDate() != null && enrollmentDate.isBefore(current.getEnrollmentDate())) {
                throw new InvalidLearnerEnrollmentException(
                        "completion_date",
                        "completion_date must be greater than or equal to enrollment_date");
            }
            current.setCurrent(false);
            current.setCompletionDate(enrollmentDate);
            if (transferred) {
                current.setEnrollmentStatusId(command.enrollmentStatusId());
            }
            repository.save(current);
        });

        LearnerFichaEnrollmentJpaEntity created = new LearnerFichaEnrollmentJpaEntity(
                UUID.randomUUID(),
                learner,
                command.fichaId(),
                newEnrollmentStatusId,
                command.stage(),
                enrollmentDate,
                null,
                true,
                command.transferReason());

        return toDomain(repository.saveAndFlush(created));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearnerFichaEnrollment> findHistoryByLearnerId(UUID learnerId) {
        return repository.findHistoryByLearnerId(learnerId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findActiveFichaIdsByLearnerId(UUID learnerId) {
        return jdbcTemplate.query(
                """
                SELECT e.ficha_id
                FROM actors_learners.learner_ficha_enrollment e
                JOIN actors_parameterization.status s ON s.id = e.enrollment_status_id
                JOIN actors_parameterization.status_category c ON c.id = s.status_category_id
                JOIN actors_learners.learner l ON l.id = e.learner_id
                WHERE e.learner_id = ?
                  AND e.is_current = true
                  AND e.is_active = true
                  AND e.deleted_at IS NULL
                  AND l.is_active = true
                  AND l.deleted_at IS NULL
                  AND c.code = 'LEARNER_ENROLLMENT_STATUS'
                  AND s.code = 'ACTIVE'
                ORDER BY e.enrollment_date DESC, e.id
                """,
                (rs, rowNum) -> (UUID) rs.getObject("ficha_id"),
                learnerId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveLearnersByFichaId(UUID fichaId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM actors_learners.learner_ficha_enrollment e
                JOIN actors_parameterization.status s ON s.id = e.enrollment_status_id
                JOIN actors_parameterization.status_category c ON c.id = s.status_category_id
                JOIN actors_learners.learner l ON l.id = e.learner_id
                WHERE e.ficha_id = ?
                  AND e.is_current = true
                  AND e.is_active = true
                  AND e.deleted_at IS NULL
                  AND l.is_active = true
                  AND l.deleted_at IS NULL
                  AND c.code = ?
                  AND s.code = ?
                """, Long.class, fichaId, CATEGORY, ACTIVE);
        return count == null ? 0 : count;
    }

    private String statusCode(UUID statusId) {
        List<String> codes = jdbcTemplate.query(
                """
                SELECT s.code
                FROM actors_parameterization.status s
                JOIN actors_parameterization.status_category c ON s.status_category_id = c.id
                WHERE s.id = ? AND c.code = ?
                """,
                (rs, rowNum) -> rs.getString("code"),
                statusId,
                CATEGORY);
        return codes.isEmpty() ? null : codes.get(0);
    }

    private UUID statusIdByCode(String code) {
        List<UUID> ids = jdbcTemplate.query(
                """
                SELECT s.id
                FROM actors_parameterization.status s
                JOIN actors_parameterization.status_category c ON s.status_category_id = c.id
                WHERE s.code = ? AND c.code = ?
                """,
                (rs, rowNum) -> (UUID) rs.getObject("id"),
                code,
                CATEGORY);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException(
                    "Status '" + code + "' not found for " + CATEGORY + " category");
        }
        return ids.get(0);
    }

    private LearnerFichaEnrollment toDomain(LearnerFichaEnrollmentJpaEntity entity) {
        return new LearnerFichaEnrollment(
                entity.getId(),
                entity.getLearner().getId(),
                entity.getFichaId(),
                entity.getEnrollmentStatusId(),
                entity.getStage(),
                entity.getEnrollmentDate(),
                entity.getCompletionDate(),
                entity.isCurrent(),
                entity.getTransferReason());
    }
}
