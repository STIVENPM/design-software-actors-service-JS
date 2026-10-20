package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.ManageLearnerUseCase;
import co.sena.actors.application.port.out.LearnerEnrollmentPort;
import co.sena.actors.application.port.out.LearnerRepositoryPort;
import co.sena.actors.domain.model.Learner;
import java.util.UUID;

public class ManageLearnerUseCaseImpl implements ManageLearnerUseCase {

    private final LearnerRepositoryPort learnerRepository;
    private final LearnerEnrollmentPort learnerEnrollmentPort;

    public ManageLearnerUseCaseImpl(
            LearnerRepositoryPort learnerRepository,
            LearnerEnrollmentPort learnerEnrollmentPort) {
        this.learnerRepository = learnerRepository;
        this.learnerEnrollmentPort = learnerEnrollmentPort;
    }

    @Override
    public Learner createLearner(CreateCommand command) {
        if (learnerRepository.existsByDocumentNumber(command.documentNumber())) {
            throw new LearnerConflictException("Document number already exists: " + command.documentNumber());
        }

        var learner = Learner.create(
                UUID.randomUUID(),
                command.userId(),
                command.documentType(),
                command.documentNumber(),
                command.firstName(),
                command.lastName(),
                command.email(),
                command.phone(),
                command.birthDate());

        return learnerRepository.save(learner);
    }

    @Override
    public Learner updateLearner(UUID learnerId, UpdateCommand command) {
        var learner = learnerRepository.findById(learnerId)
                .orElseThrow(() -> new LearnerNotFoundException("Learner not found with id: " + learnerId));

        if (command.documentNumber() != null
                && !command.documentNumber().equals(learner.documentNumber())
                && learnerRepository.existsByDocumentNumber(command.documentNumber())) {
            throw new LearnerConflictException("Document number already exists: " + command.documentNumber());
        }

        var updatedLearner = learner.update(
                command.documentType(),
                command.documentNumber(),
                command.firstName(),
                command.lastName(),
                command.email(),
                command.phone(),
                command.birthDate());

        return learnerRepository.save(updatedLearner);
    }

    @Override
    public void cancelLearnerEnrollment(UUID learnerId) {
        if (learnerRepository.findById(learnerId).isEmpty()) {
            throw new LearnerNotFoundException("Learner not found with id: " + learnerId);
        }
        learnerEnrollmentPort.cancelCurrentEnrollment(learnerId);
    }
}
