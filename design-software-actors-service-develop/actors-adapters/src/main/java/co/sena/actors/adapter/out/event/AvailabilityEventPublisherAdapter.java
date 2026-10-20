package co.sena.actors.adapter.out.event;

import co.sena.actors.adapter.config.TraceIds;
import co.sena.actors.core.domain.InstructorAvailabilityException;
import co.sena.actors.core.port.out.AvailabilityEventPublisherPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Component
public class AvailabilityEventPublisherAdapter
        implements AvailabilityEventPublisherPort {

    private static final String TOPIC = "actors-events";
    private static final String EVENT_TYPE =
            "actors.instructor.availability_changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AvailabilityEventPublisherAdapter(
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishAvailabilityChangedEvent(
            InstructorAvailabilityException exception) {

        String correlationId = TraceIds.current();

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        Map<String, Object> eventEnvelope = Map.of(
                "event_id", UUID.randomUUID().toString(),
                "event_type", EVENT_TYPE,
                "version", "1.0",
                "timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString(),
                "source_service", "actors-service",
                "correlation_id", correlationId,
                "payload", exception
        );

        kafkaTemplate.send(TOPIC, eventEnvelope.get("event_id").toString(),
                eventEnvelope);
    }
}
