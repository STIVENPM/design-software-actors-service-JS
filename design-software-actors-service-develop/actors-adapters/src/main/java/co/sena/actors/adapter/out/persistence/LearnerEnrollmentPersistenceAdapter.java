package co.sena.actors.adapter.out.persistence;

import co.sena.actors.application.port.out.LearnerEnrollmentPort;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LearnerEnrollmentPersistenceAdapter implements LearnerEnrollmentPort {

    private final JdbcTemplate jdbcTemplate;

    public LearnerEnrollmentPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void cancelCurrentEnrollment(UUID learnerId) {
        String queryStatusIdSql = """
            SELECT s.id 
            FROM actors_parameterization.status s
            JOIN actors_parameterization.status_category c ON s.status_category_id = c.id
            WHERE s.code = 'CANCELLED' AND c.code = 'LEARNER_ENROLLMENT_STATUS'
        """;
        
        java.util.List<UUID> statusIds = jdbcTemplate.query(queryStatusIdSql, (rs, rowNum) -> (UUID) rs.getObject("id"));
        if (statusIds.isEmpty()) {
            throw new IllegalArgumentException("Status 'CANCELLED' not found for LEARNER_ENROLLMENT_STATUS category");
        }
        
        String sql = """
            UPDATE actors_learners.learner_ficha_enrollment e
            SET enrollment_status_id = ?
            WHERE e.learner_id = ? AND e.is_current = true
        """;
        
        int updated = jdbcTemplate.update(sql, statusIds.get(0), learnerId);
        if (updated == 0) {
            throw new IllegalArgumentException("No active enrollment found for learner to cancel");
        }
    }
}
