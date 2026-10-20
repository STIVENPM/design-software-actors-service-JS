package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.ManageLearnerFichaEnrollmentUseCase;
import co.sena.actors.application.port.out.LearnerEnrollmentEventPublisherPort;
import co.sena.actors.application.port.out.LearnerFichaEnrollmentRepositoryPort;
import co.sena.actors.application.port.out.LearnerRepositoryPort;
import co.sena.actors.domain.exception.InvalidLearnerEnrollmentException;
import co.sena.actors.domain.model.LearnerFichaEnrollment;
import java.util.UUID;

public class ManageLearnerFichaEnrollmentUseCaseImpl implements ManageLearnerFichaEnrollmentUseCase {

    private final LearnerRepositoryPort learnerRepository;
    private final LearnerFichaEnrollmentRepositoryPort enrollmentRepository;
    private final LearnerEnrollmentEventPublisherPort eventPublisher;

    public ManageLearnerFichaEnrollmentUseCaseImpl(
            LearnerRepositoryPort learnerRepository,
            LearnerFichaEnrollmentRepositoryPort enrollmentRepository,
            LearnerEnrollmentEventPublisherPort eventPublisher) {
        this.learnerRepository = learnerRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public LearnerFichaEnrollment enroll(UUID learnerId, CreateCommand command) {
        if (learnerRepository.findById(learnerId).isEmpty()) {
            throw new LearnerNotFoundException("Learner not found with id: " + learnerId);
        }
        if (command.fichaId() == null) {
            throw new IllegalArgumentException("ficha_id is required");
        }
        if (command.enrollmentStatusId() == null) {
            throw new IllegalArgumentException("enrollment_status_id is required");
        }
        if (command.stage() == null) {
            throw new IllegalArgumentException("stage is required");
        }
        if (command.enrollmentDate() == null) {
            throw new IllegalArgumentException("enrollment_date is required");
        }

        LearnerFichaEnrollment created = enrollmentRepository.createEnrollment(learnerId, command);
        if (created.completionDate() != null && created.completionDate().isBefore(created.enrollmentDate())) {
            throw new InvalidLearnerEnrollmentException(
                    "completion_date",
                    "completion_date must be greater than or equal to enrollment_date");
        }
        eventPublisher.publishLearnerEnrolledEvent(created);
        return created;
    }
}
