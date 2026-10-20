package co.sena.actors.adapter.out.event;

import co.sena.actors.adapter.config.TraceIds;
import co.sena.actors.application.port.out.LearnerEnrollmentEventPublisherPort;
import co.sena.actors.domain.model.LearnerFichaEnrollment;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class LearnerEnrollmentEventPublisherAdapter implements LearnerEnrollmentEventPublisherPort {

    static final String TOPIC = "actors-events";
    static final String EVENT_TYPE = "actors.learner.enrolled";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LearnerEnrollmentEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishLearnerEnrolledEvent(LearnerFichaEnrollment enrollment) {
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
                "payload", enrollment);

        kafkaTemplate.send(TOPIC, eventEnvelope.get("event_id").toString(), eventEnvelope);
    }
}
