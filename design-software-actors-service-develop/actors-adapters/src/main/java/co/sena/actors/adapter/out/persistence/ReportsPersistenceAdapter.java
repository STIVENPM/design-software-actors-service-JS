package co.sena.actors.adapter.out.persistence;

import co.sena.actors.application.port.in.InstructorLoadReportEntry;
import co.sena.actors.application.port.in.InstructorLoadReportQuery;
import co.sena.actors.application.port.in.LearnerTrackingReportEntry;
import co.sena.actors.application.port.in.LearnerTrackingReportQuery;
import co.sena.actors.application.port.out.ReportsRepositoryPort;
import io.micrometer.observation.annotation.Observed;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Observed(name = "actors.reports.repository")
public class ReportsPersistenceAdapter implements ReportsRepositoryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public ReportsPersistenceAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<LearnerTrackingReportEntry> findLearnerTracking(LearnerTrackingReportQuery query) {
        QueryParts parts = learnerTrackingParts(query);
        String sql = """
                SELECT l.id AS learner_id, l.document_number,
                       CONCAT_WS(' ', l.first_name, l.last_name) AS full_name,
                       enrollment_status.code AS enrollment_status,
                       e.ficha_id, e.stage, e.enrollment_date AS ficha_enrollment_date,
                       productive_status.code AS productive_stage_status,
                       productive.company_id AS productive_stage_company_id
                FROM actors_learners.learner l
                JOIN actors_learners.learner_ficha_enrollment e ON e.learner_id = l.id
                JOIN actors_parameterization.status enrollment_status
                  ON enrollment_status.id = e.enrollment_status_id
                LEFT JOIN LATERAL (
                    SELECT ps.company_id, ps.status_id
                    FROM actors_companies.productive_stage ps
                    WHERE ps.learner_id = l.id
                      AND ps.is_active = true AND ps.deleted_at IS NULL
                    ORDER BY ps.start_date DESC, ps.id DESC
                    LIMIT 1
                ) productive ON TRUE
                LEFT JOIN actors_parameterization.status productive_status
                  ON productive_status.id = productive.status_id
                WHERE l.is_active = true AND l.deleted_at IS NULL
                  AND e.is_active = true AND e.deleted_at IS NULL
                  AND e.is_current = true
                """ + parts.where() + " ORDER BY " + learnerSort(query.sort())
                + " LIMIT :limit OFFSET :offset";
        MapSqlParameterSource params = parts.params()
                .addValue("limit", query.pageSize())
                .addValue("offset", (query.page() - 1) * query.pageSize());
        return jdbc.query(sql, params, this::mapLearnerTracking);
    }

    @Override
    public long countLearnerTracking(LearnerTrackingReportQuery query) {
        QueryParts parts = learnerTrackingParts(query);
        String sql = """
                SELECT COUNT(*)
                FROM actors_learners.learner l
                JOIN actors_learners.learner_ficha_enrollment e ON e.learner_id = l.id
                JOIN actors_parameterization.status enrollment_status
                  ON enrollment_status.id = e.enrollment_status_id
                WHERE l.is_active = true AND l.deleted_at IS NULL
                  AND e.is_active = true AND e.deleted_at IS NULL
                  AND e.is_current = true
                """ + parts.where();
        Long count = jdbc.queryForObject(sql, parts.params(), Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public List<InstructorLoadReportEntry> findInstructorLoad(InstructorLoadReportQuery query) {
        QueryParts parts = instructorLoadParts(query);
        String sql = """
                SELECT i.id AS instructor_id, i.document_number,
                       CONCAT_WS(' ', i.first_name, i.last_name) AS full_name,
                       i.default_max_hours_per_week,
                       current_contract.weekly_hour_limit AS current_contract_weekly_hour_limit,
                       current_contract.training_center_id,
                       (SELECT COUNT(*) FROM actors_instructors.instructor_area area
                        WHERE area.instructor_id = i.id AND area.is_active = true
                          AND area.deleted_at IS NULL) AS active_areas_count,
                       (SELECT COUNT(*) FROM actors_instructors.competency_assignment competency
                        WHERE competency.instructor_id = i.id AND competency.is_active = true
                          AND competency.deleted_at IS NULL) AS active_competencies_count,
                       (SELECT COUNT(*) FROM actors_instructors.instructor_availability_exception exception
                        WHERE exception.instructor_id = i.id AND exception.is_active = true
                          AND exception.deleted_at IS NULL
                """ + exceptionRangeWhere(query) + """
                ) AS active_exceptions_count
                FROM actors_instructors.instructor i
                LEFT JOIN actors_instructors.instructor_contract current_contract
                  ON current_contract.instructor_id = i.id
                 AND current_contract.is_current = true
                 AND current_contract.is_active = true
                 AND current_contract.deleted_at IS NULL
                WHERE i.is_active = true AND i.deleted_at IS NULL
                """ + parts.where() + " ORDER BY " + instructorSort(query.sort())
                + " LIMIT :limit OFFSET :offset";
        MapSqlParameterSource params = parts.params()
                .addValue("limit", query.pageSize())
                .addValue("offset", (query.page() - 1) * query.pageSize());
        return jdbc.query(sql, params, this::mapInstructorLoad);
    }

    @Override
    public long countInstructorLoad(InstructorLoadReportQuery query) {
        QueryParts parts = instructorLoadParts(query);
        String sql = """
                SELECT COUNT(*)
                FROM actors_instructors.instructor i
                LEFT JOIN actors_instructors.instructor_contract current_contract
                  ON current_contract.instructor_id = i.id
                 AND current_contract.is_current = true
                 AND current_contract.is_active = true
                 AND current_contract.deleted_at IS NULL
                WHERE i.is_active = true AND i.deleted_at IS NULL
                """ + parts.where();
        Long count = jdbc.queryForObject(sql, parts.params(), Long.class);
        return count == null ? 0 : count;
    }

    private QueryParts learnerTrackingParts(LearnerTrackingReportQuery query) {
        StringBuilder where = new StringBuilder();
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (query.fichaId() != null) {
            where.append(" AND e.ficha_id = :fichaId");
            params.addValue("fichaId", query.fichaId());
        }
        if (query.enrollmentStatus() != null) {
            where.append(" AND enrollment_status.code = :enrollmentStatus");
            params.addValue("enrollmentStatus", query.enrollmentStatus());
        }
        if (query.from() != null) {
            where.append(" AND e.enrollment_date >= :enrollmentFrom");
            params.addValue("enrollmentFrom", query.from());
        }
        if (query.to() != null) {
            where.append(" AND e.enrollment_date < :enrollmentTo");
            params.addValue("enrollmentTo", query.to());
        }
        return new QueryParts(where.toString(), params);
    }

    private QueryParts instructorLoadParts(InstructorLoadReportQuery query) {
        StringBuilder where = new StringBuilder();
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (query.trainingCenterId() != null) {
            where.append(" AND current_contract.training_center_id = :trainingCenterId");
            params.addValue("trainingCenterId", query.trainingCenterId());
        }
        if (query.area() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM actors_instructors.instructor_area filter_area"
                    + " WHERE filter_area.instructor_id = i.id AND filter_area.is_active = true"
                    + " AND filter_area.deleted_at IS NULL AND LOWER(filter_area.area_name) = LOWER(:area))");
            params.addValue("area", query.area());
        }
        if (query.from() != null) {
            params.addValue("exceptionFrom", query.from());
        }
        if (query.to() != null) {
            params.addValue("exceptionTo", query.to());
        }
        return new QueryParts(where.toString(), params);
    }

    private String exceptionRangeWhere(InstructorLoadReportQuery query) {
        StringBuilder where = new StringBuilder();
        if (query.from() != null) {
            where.append(" AND exception.end_datetime >= :exceptionFrom");
        }
        if (query.to() != null) {
            where.append(" AND exception.start_datetime < :exceptionTo");
        }
        return where.toString();
    }

    private String learnerSort(String sort) {
        return sortExpression(sort, Map.of(
                "enrollment_date", "e.enrollment_date",
                "full_name", "l.last_name, l.first_name",
                "document_number", "l.document_number"), "e.enrollment_date DESC");
    }

    private String instructorSort(String sort) {
        return sortExpression(sort, Map.of(
                "full_name", "i.last_name, i.first_name",
                "document_number", "i.document_number",
                "default_max_hours_per_week", "i.default_max_hours_per_week"),
                "i.last_name ASC, i.first_name ASC");
    }

    private String sortExpression(String sort, Map<String, String> fields, String defaultSort) {
        if (sort == null || sort.isBlank()) {
            return defaultSort;
        }
        String[] parts = sort.split(":", -1);
        String field = fields.get(parts[0]);
        if (field == null || (parts.length > 1 && !parts[1].equalsIgnoreCase("asc")
                && !parts[1].equalsIgnoreCase("desc")) || parts.length > 2) {
            throw new IllegalArgumentException("Unsupported sort");
        }
        String direction = parts.length == 1 || parts[1].equalsIgnoreCase("asc") ? "ASC" : "DESC";
        return field + " " + direction;
    }

    private LearnerTrackingReportEntry mapLearnerTracking(ResultSet result, int row) throws SQLException {
        Date enrollmentDate = result.getDate("ficha_enrollment_date");
        return new LearnerTrackingReportEntry(
                result.getObject("learner_id", UUID.class),
                result.getString("document_number"),
                result.getString("full_name"),
                result.getString("enrollment_status"),
                result.getObject("ficha_id", UUID.class),
                result.getString("stage"),
                enrollmentDate == null ? null : enrollmentDate.toLocalDate(),
                result.getString("productive_stage_status"),
                result.getObject("productive_stage_company_id", UUID.class));
    }

    private InstructorLoadReportEntry mapInstructorLoad(ResultSet result, int row) throws SQLException {
        return new InstructorLoadReportEntry(
                result.getObject("instructor_id", UUID.class),
                result.getString("document_number"),
                result.getString("full_name"),
                result.getBigDecimal("default_max_hours_per_week"),
                result.getBigDecimal("current_contract_weekly_hour_limit"),
                result.getObject("training_center_id", UUID.class),
                result.getInt("active_areas_count"),
                result.getInt("active_competencies_count"),
                result.getInt("active_exceptions_count"));
    }

    private record QueryParts(String where, MapSqlParameterSource params) {
    }
}