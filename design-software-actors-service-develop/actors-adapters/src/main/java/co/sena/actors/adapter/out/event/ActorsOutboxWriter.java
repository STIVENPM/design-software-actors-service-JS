package co.sena.actors.adapter.out.event;

import co.sena.actors.adapter.config.TraceIds;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Writes the event in the caller's business transaction. Never talks to the broker. */
@Component
public class ActorsOutboxWriter {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public ActorsOutboxWriter(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public UUID enqueue(String eventType, UUID aggregateId, Object payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Outbox write requires an active business transaction");
        }
        UUID eventId = UUID.randomUUID();
        String correlationId = TraceIds.current();
        if (correlationId == null || correlationId.isBlank()) correlationId = eventId.toString();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("event_id", eventId.toString());
        envelope.put("event_type", eventType);
        envelope.put("version", "1.0");
        envelope.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        envelope.put("source_service", "actors-service");
        envelope.put("correlation_id", correlationId);
        envelope.put("payload", payload);
        try {
            jdbc.update("""
                    INSERT INTO actors_tracking.outbox_event
                      (event_id, event_type, aggregate_id, topic, envelope)
                    VALUES (?, ?, ?, 'actors-events', ?::jsonb)
                    """, eventId, eventType, aggregateId, mapper.writeValueAsString(envelope));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize actor event", ex);
        }
        return eventId;
    }
}
