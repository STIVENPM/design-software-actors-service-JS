package co.sena.actors.adapter.config;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LearnerEnrollmentStatusSeed implements ApplicationRunner {

    private static final String CATEGORY = "LEARNER_ENROLLMENT_STATUS";
    private static final UUID CATEGORY_ID = UUID.fromString("10000000-0000-0000-0000-000000000007");

    private final JdbcTemplate jdbcTemplate;

    public LearnerEnrollmentStatusSeed(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("actors_parameterization", "status_category")
                || !tableExists("actors_parameterization", "status")) {
            return;
        }

        UUID categoryId = categoryId();

        seedStatus(categoryId, "ACTIVE", false, false, 10, "#16A34A");
        seedStatus(categoryId, "DROPOUT", false, true, 20, "#DC2626");
        seedStatus(categoryId, "TRANSFERRED", false, true, 30, "#2563EB");
        seedStatus(categoryId, "GRADUATED", false, true, 40, "#7C3AED");
    }

    private UUID categoryId() {
        List<UUID> existing = jdbcTemplate.query(
                "SELECT id FROM actors_parameterization.status_category WHERE code = ?",
                (rs, rowNum) -> (UUID) rs.getObject("id"),
                CATEGORY);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        jdbcTemplate.update("""
                INSERT INTO actors_parameterization.status_category
                    (id, code, name, description, applies_to_entity, is_active,
                     created_at, created_by, updated_at, updated_by)
                VALUES (?, ?, ?, ?, ?, true, now(), ?, now(), ?)
                ON CONFLICT (id) DO NOTHING
                """, CATEGORY_ID, CATEGORY, "Learner enrollment status",
                "Statuses used by learner ficha enrollments", "LEARNER_FICHA_ENROLLMENT",
                CATEGORY_ID, CATEGORY_ID);
        return CATEGORY_ID;
    }

    private void seedStatus(UUID categoryId, String code, boolean initial, boolean terminal, int displayOrder, String colorHex) {
        List<UUID> existing = jdbcTemplate.query("""
                SELECT id
                FROM actors_parameterization.status
                WHERE code = ? AND status_category_id = ?
                """, (rs, rowNum) -> (UUID) rs.getObject("id"), code, categoryId);
        if (!existing.isEmpty()) {
            return;
        }

        UUID id = UUID.nameUUIDFromBytes((categoryId + ":" + code).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        jdbcTemplate.update("""
                INSERT INTO actors_parameterization.status
                    (id, status_category_id, code, name, description, is_initial,
                     is_terminal, display_order, color_hex, is_active,
                     created_at, created_by, updated_at, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, true, now(), ?, now(), ?)
                ON CONFLICT (id) DO NOTHING
                """, id, categoryId, code, code, "Learner enrollment status: " + code,
                initial, terminal, displayOrder, colorHex, categoryId, categoryId);
    }

    private boolean tableExists(String schema, String table) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = ? AND table_name = ?
                )
                """, Boolean.class, schema, table);
        return Boolean.TRUE.equals(exists);
    }
}
