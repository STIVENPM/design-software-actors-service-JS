package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.CreateInstructorCommand;
import co.sena.actors.application.port.in.ManageInstructorUseCase;
import co.sena.actors.application.port.in.PatchInstructorCommand;
import co.sena.actors.application.port.in.ReplaceInstructorCommand;
import co.sena.actors.application.port.out.InstructorRepositoryPort;
import co.sena.actors.domain.exception.DuplicateInstructorException;
import co.sena.actors.domain.exception.InstructorNotFoundException;
import co.sena.actors.domain.exception.InvalidInstructorException;
import co.sena.actors.domain.exception.StaleInstructorVersionException;
import co.sena.actors.domain.model.Instructor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * Orchestrates E1-E4. Pure core without Spring dependencies.
 * JPA (@Version) increments row_version; here the client-sent value
 * is only verified for optimistic locking.
 */
public class ManageInstructorUseCaseImpl implements ManageInstructorUseCase {

    private final InstructorRepositoryPort repository;

    public ManageInstructorUseCaseImpl(InstructorRepositoryPort repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public Instructor create(CreateInstructorCommand command, UUID actorUuid) {
        requireHoursNonNegativeOnCreate(command.defaultMaxHoursPerWeek());
        if (repository.existsByDocumentNumber(command.documentNumber())) {
            throw new DuplicateInstructorException("document_number",
                    "An instructor already exists with document_number: " + command.documentNumber());
        }
        if (command.userId() != null && repository.existsByUserId(command.userId())) {
            throw new DuplicateInstructorException("user_id",
                    "An instructor already exists with user_id: " + command.userId());
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Instructor instructor = new Instructor();
        instructor.setId(UUID.randomUUID());
        instructor.setUserId(command.userId());
        instructor.setDocumentType(command.documentType());
        instructor.setDocumentNumber(command.documentNumber().trim());
        instructor.setFirstName(command.firstName().trim());
        instructor.setLastName(command.lastName().trim());
        instructor.setEmail(command.email().trim());
        instructor.setPhone(normalizeNullable(command.phone()));
        instructor.setDefaultMaxHoursPerWeek(command.defaultMaxHoursPerWeek());
        instructor.setActive(true);
        instructor.setCreatedAt(now);
        instructor.setCreatedBy(actorUuid);
        instructor.setUpdatedAt(now);
        instructor.setUpdatedBy(actorUuid);
        instructor.setRowVersion(0);
        return repository.save(instructor);
    }

    @Override
    public Instructor getActive(UUID id) {
        return repository.findActiveById(id).orElseThrow(() -> new InstructorNotFoundException(id));
    }

    @Override
    public Instructor replace(UUID id, ReplaceInstructorCommand command, UUID actorUuid) {
        requireHoursNonNegativeOnUpdate(command.defaultMaxHoursPerWeek());
        Instructor current = getActive(id);
        checkVersion(current, command.rowVersion());
        if (repository.existsByDocumentNumberExcludingId(command.documentNumber(), id)) {
            throw new DuplicateInstructorException("document_number",
                    "An instructor already exists with document_number: " + command.documentNumber());
        }
        current.setDocumentType(command.documentType());
        current.setDocumentNumber(command.documentNumber().trim());
        current.setFirstName(command.firstName().trim());
        current.setLastName(command.lastName().trim());
        current.setEmail(command.email().trim());
        current.setPhone(normalizeNullable(command.phone()));
        current.setDefaultMaxHoursPerWeek(command.defaultMaxHoursPerWeek());
        current.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        current.setUpdatedBy(actorUuid);
        return repository.save(current);
    }

    @Override
    public Instructor patch(UUID id, PatchInstructorCommand command, UUID actorUuid) {
        if (command.defaultMaxHoursPerWeek() != null) {
            requireHoursNonNegativeOnUpdate(command.defaultMaxHoursPerWeek());
        }
        Instructor current = getActive(id);
        checkVersion(current, command.rowVersion());
        if (command.documentNumber() != null
                && repository.existsByDocumentNumberExcludingId(command.documentNumber(), id)) {
            throw new DuplicateInstructorException("document_number",
                    "An instructor already exists with document_number: " + command.documentNumber());
        }
        if (command.documentType() != null) {
            current.setDocumentType(command.documentType());
        }
        if (command.documentNumber() != null) {
            current.setDocumentNumber(command.documentNumber().trim());
        }
        if (command.firstName() != null) {
            current.setFirstName(command.firstName().trim());
        }
        if (command.lastName() != null) {
            current.setLastName(command.lastName().trim());
        }
        if (command.email() != null) {
            current.setEmail(command.email().trim());
        }
        if (command.phone() != null) {
            current.setPhone(command.phone().isBlank() ? null : command.phone().trim());
        }
        if (command.defaultMaxHoursPerWeek() != null) {
            current.setDefaultMaxHoursPerWeek(command.defaultMaxHoursPerWeek());
        }
        current.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        current.setUpdatedBy(actorUuid);
        return repository.save(current);
    }

    @Override
    public void deactivate(UUID id, UUID actorUuid) {
        Instructor current = getActive(id);
        current.setActive(false);
        current.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        current.setDeletedBy(actorUuid);
        current.setUpdatedAt(current.getDeletedAt());
        current.setUpdatedBy(actorUuid);
        repository.save(current);
    }

    private void checkVersion(Instructor current, Integer expected) {
        if (expected != null && expected.intValue() != current.getRowVersion()) {
            throw new StaleInstructorVersionException(current.getId());
        }
    }

    private void requireHoursNonNegativeOnCreate(BigDecimal hours) {
        if (hours != null && hours.signum() < 0) {
            throw new IllegalArgumentException("default_max_hours_per_week must be >= 0");
        }
    }

    private void requireHoursNonNegativeOnUpdate(BigDecimal hours) {
        if (hours != null && hours.signum() < 0) {
            throw new InvalidInstructorException("default_max_hours_per_week",
                    "default_max_hours_per_week must be >= 0");
        }
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
