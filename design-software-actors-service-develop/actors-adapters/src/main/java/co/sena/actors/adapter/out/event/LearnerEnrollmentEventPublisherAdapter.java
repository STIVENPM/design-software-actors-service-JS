package co.sena.actors.adapter.out.event;

import co.sena.actors.application.port.out.LearnerEnrollmentEventPublisherPort;
import co.sena.actors.domain.model.LearnerFichaEnrollment;
import org.springframework.stereotype.Component;

@Component
public class LearnerEnrollmentEventPublisherAdapter implements LearnerEnrollmentEventPublisherPort {
    static final String EVENT_TYPE = "actors.learner.enrolled";
    private final ActorsOutboxWriter outbox;

    public LearnerEnrollmentEventPublisherAdapter(ActorsOutboxWriter outbox) {
        this.outbox = outbox;
    }

    @Override
    public void publishLearnerEnrolledEvent(LearnerFichaEnrollment enrollment) {
        outbox.enqueue(EVENT_TYPE, enrollment.id(), enrollment);
    }
}
