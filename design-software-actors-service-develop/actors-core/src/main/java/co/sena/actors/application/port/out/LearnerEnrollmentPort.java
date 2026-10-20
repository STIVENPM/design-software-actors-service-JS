package co.sena.actors.application.port.out;

import java.util.UUID;

public interface LearnerEnrollmentPort {

    void cancelCurrentEnrollment(UUID learnerId);
}
