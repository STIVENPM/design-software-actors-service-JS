package co.sena.actors.application.port.out;

import co.sena.actors.domain.model.LearnerFichaEnrollment;

public interface LearnerEnrollmentEventPublisherPort {

    void publishLearnerEnrolledEvent(LearnerFichaEnrollment enrollment);
}
