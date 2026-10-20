package co.sena.actors.adapter.out.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** At-least-once dispatcher. A lease recovers events after process crashes. */
@Component
@ConditionalOnProperty(name = "actors.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class ActorsOutboxDispatcher {
    private static final int MAX_ATTEMPTS = 5;
    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper mapper;
    private final int batchSize;

    public ActorsOutboxDispatcher(JdbcTemplate jdbc, KafkaTemplate<String, Object> kafka,
            ObjectMapper mapper, @Value("${actors.outbox.batch-size:20}") int batchSize) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.mapper = mapper;
        this.batchSize = batchSize;
    }

    public record Event(UUID id, UUID aggregateId, String topic, String envelope, int attempts) {}

    @Scheduled(fixedDelayString = "${actors.outbox.poll-ms:1000}")
    public void scheduledDispatch() { dispatchBatch(); }

    public int dispatchBatch() {
        List<Event> events = jdbc.query("""
                WITH due AS (
                  SELECT event_id FROM actors_tracking.outbox_event
                  WHERE (status = 'PENDING' AND available_at <= now())
                     OR (status = 'SENDING' AND lease_until < now())
                  ORDER BY created_at
                  FOR UPDATE SKIP LOCKED LIMIT ?
                )
                UPDATE actors_tracking.outbox_event e
                SET status = 'SENDING', lease_until = now() + interval '45 seconds',
                    attempts = attempts + 1
                FROM due WHERE e.event_id = due.event_id
                RETURNING e.event_id, e.aggregate_id, e.topic, e.envelope::text, e.attempts
                """, (rs, row) -> new Event(
                rs.getObject("event_id", UUID.class),
                rs.getObject("aggregate_id", UUID.class),
                rs.getString("topic"), rs.getString("envelope"), rs.getInt("attempts")), batchSize);
        for (Event event : events) {
            try {
                Map<String, Object> body = mapper.readValue(event.envelope(), new TypeReference<>() {});
                kafka.send(event.topic(), event.id().toString(), body).get(10, TimeUnit.SECONDS);
                jdbc.update("""
                        UPDATE actors_tracking.outbox_event
                        SET status = 'PUBLISHED', published_at = now(), lease_until = NULL, last_error = NULL
                        WHERE event_id = ? AND status = 'SENDING'
                        """, event.id());
            } catch (Exception failure) {
                int delaySeconds = (int) Math.pow(4, Math.min(event.attempts() - 1, 4));
                String message = failure.getClass().getSimpleName() + ": " + failure.getMessage();
                if (message.length() > 1000) message = message.substring(0, 1000);
                jdbc.update("""
                        UPDATE actors_tracking.outbox_event
                        SET status = ?, available_at = now() + make_interval(secs => ?),
                            lease_until = NULL, last_error = ?
                        WHERE event_id = ? AND status = 'SENDING'
                        """, event.attempts() >= MAX_ATTEMPTS ? "DEAD" : "PENDING",
                        delaySeconds, message, event.id());
            }
        }
        return events.size();
    }

    /** Operator-triggered retry for an event that exhausted automatic attempts. */
    public boolean replayDead(UUID eventId) {
        return jdbc.update("""
                UPDATE actors_tracking.outbox_event
                SET status = 'PENDING', attempts = 0, available_at = now(),
                    lease_until = NULL, last_error = NULL
                WHERE event_id = ? AND status = 'DEAD'
                """, eventId) == 1;
    }
}
