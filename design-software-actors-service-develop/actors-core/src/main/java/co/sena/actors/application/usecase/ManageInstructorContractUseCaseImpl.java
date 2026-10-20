package co.sena.actors.application.usecase;

import co.sena.actors.application.port.in.CreateInstructorContractCommand;
import co.sena.actors.application.port.in.ManageInstructorContractUseCase;
import co.sena.actors.application.port.in.UpdateInstructorContractCommand;
import co.sena.actors.application.port.in.Viewer;
import co.sena.actors.application.port.out.InstructorContractRepositoryPort;
import co.sena.actors.domain.exception.InstructorContractNotFoundException;
import co.sena.actors.domain.exception.InvalidInstructorContractException;
import co.sena.actors.domain.exception.StaleInstructorContractVersionException;
import co.sena.actors.domain.model.ContractType;
import co.sena.actors.domain.model.InstructorContract;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Write side for instructor contracts (E2-E3).
 * RN-ACT-01 validation (STAFF=40h, CONTRACTOR<=48h, HOURLY no límite) en capa de aplicación.
 */
public class ManageInstructorContractUseCaseImpl implements ManageInstructorContractUseCase {

    private final InstructorViewerAccess access;
    private final InstructorContractRepositoryPort repository;

    public ManageInstructorContractUseCaseImpl(
            InstructorViewerAccess access, InstructorContractRepositoryPort repository) {
        this.access = Objects.requireNonNull(access, "access");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public InstructorContract create(CreateInstructorContractCommand cmd, UUID actorId, Viewer viewer) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(actorId, "actorId");
        access.requireVisible(viewer, cmd.instructorId());
        validateDates(cmd.startDate(), cmd.endDate());
        validateBusinessRules(cmd.contractType(), cmd.weeklyHourLimit());

        InstructorContract contract = new InstructorContract();
        contract.setId(UUID.randomUUID());
        contract.setInstructorId(cmd.instructorId());
        contract.setContractType(cmd.contractType());
        contract.setStartDate(cmd.startDate());
        contract.setEndDate(cmd.endDate());
        contract.setTrainingCenterId(cmd.trainingCenterId());
        contract.setWeeklyHourLimit(cmd.weeklyHourLimit());
        contract.setCurrent(cmd.isCurrent());
        contract.setNotes(cmd.notes());
        contract.setActive(true);
        contract.setCreatedAt(OffsetDateTime.now());
        contract.setCreatedBy(actorId);
        contract.setUpdatedAt(OffsetDateTime.now());
        contract.setUpdatedBy(actorId);
        contract.setRowVersion(0);
        if (cmd.isCurrent()) {
            return repository.saveAsCurrent(contract);
        }
        return repository.save(contract);
    }

    @Override
    public InstructorContract update(
            UUID instructorId, UUID contractId, UpdateInstructorContractCommand cmd, UUID actorId, Viewer viewer) {
        Objects.requireNonNull(instructorId, "instructorId");
        Objects.requireNonNull(contractId, "contractId");
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(actorId, "actorId");
        access.requireVisible(viewer, instructorId);

        InstructorContract contract = repository.findActiveById(contractId)
                .filter(candidate -> instructorId.equals(candidate.getInstructorId()))
                .orElseThrow(() -> new InstructorContractNotFoundException(contractId));

        if (cmd.rowVersion() != null && contract.getRowVersion() != cmd.rowVersion()) {
            throw new StaleInstructorContractVersionException(contractId);
        }

        LocalDate startDate = cmd.startDate() != null ? cmd.startDate() : contract.getStartDate();
        LocalDate endDate = cmd.endDate() != null ? cmd.endDate() : contract.getEndDate();
        validateDates(startDate, endDate);
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        if (cmd.weeklyHourLimit() != null) {
            validateBusinessRules(contract.getContractType(), cmd.weeklyHourLimit());
            contract.setWeeklyHourLimit(cmd.weeklyHourLimit());
        }
        boolean promote = cmd.isCurrent() != null && cmd.isCurrent() && !contract.isCurrent();
        if (cmd.isCurrent() != null && !cmd.isCurrent() && contract.isCurrent()) {
            contract.setCurrent(false);
        }
        if (cmd.notes() != null) {
            contract.setNotes(cmd.notes());
        }

        contract.setUpdatedAt(OffsetDateTime.now());
        contract.setUpdatedBy(actorId);
        if (promote) {
            contract.setCurrent(true);
            return repository.saveAsCurrent(contract);
        }
        return repository.save(contract);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidInstructorContractException("end_date", "end_date must be >= start_date");
        }
    }

    private void validateBusinessRules(ContractType type, BigDecimal limit) {
        if (limit == null) {
            return;
        }
        if (limit.signum() < 0) {
            throw new InvalidInstructorContractException(
                    "weekly_hour_limit", "weekly_hour_limit must be >= 0");
        }
        switch (type) {
            case STAFF -> {
                if (limit.compareTo(BigDecimal.valueOf(40)) > 0) {
                    throw new InvalidInstructorContractException("weekly_hour_limit",
                            "STAFF contract weekly_hour_limit must be <= 40");
                }
            }
            case CONTRACTOR -> {
                if (limit.compareTo(BigDecimal.valueOf(48)) > 0) {
                    throw new InvalidInstructorContractException("weekly_hour_limit",
                            "CONTRACTOR contract weekly_hour_limit must be <= 48");
                }
            }
            case HOURLY -> {
                // sin límite fijo
            }
        }
    }
}
