package co.sena.actors.application.port.in;

import co.sena.actors.domain.model.DocumentType;
import co.sena.actors.domain.model.Learner;
import java.time.LocalDate;
import java.util.UUID;

public interface ManageLearnerUseCase {

    Learner createLearner(CreateCommand command);

    Learner updateLearner(UUID learnerId, UpdateCommand command);

    void cancelLearnerEnrollment(UUID learnerId);

    record CreateCommand(
            UUID userId,
            DocumentType documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDate birthDate) {}

    record UpdateCommand(
            DocumentType documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDate birthDate) {}
}
