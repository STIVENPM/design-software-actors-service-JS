package co.sena.actors.adapter.out.persistence;

import co.sena.actors.application.port.out.IamEventInboxPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Requires actors-db's pending actors_tracking.iam_processed_event migration. */
@Component
@ConditionalOnProperty(name = "actors.iam.consumer.enabled", havingValue = "true")
public class IamEventInboxJdbcAdapter implements IamEventInboxPort {
    private final JdbcTemplate jdbc;

    public IamEventInboxJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean claim(UUID eventId, UUID userId, String eventType) {
        int inserted = jdbc.update("""
                INSERT INTO actors_tracking.iam_processed_event
                    (event_id, user_id, event_type, processed_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, userId, eventType, Timestamp.from(Instant.now()));
        return inserted == 1;
    }
}
