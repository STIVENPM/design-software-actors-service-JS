package co.sena.actors.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.sena.actors.application.port.in.ManageImprovementPlanUseCase;
import co.sena.actors.application.port.out.ImprovementPlanRepositoryPort;
import co.sena.actors.domain.exception.InvalidImprovementPlanException;
import co.sena.actors.domain.model.ActorImprovementPlan;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManageImprovementPlanUseCaseTest {
    private ImprovementPlanRepositoryPort repository;
    private ManageImprovementPlanUseCase useCase;
    private UUID statusId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        repository = mock(ImprovementPlanRepositoryPort.class);
        useCase = new ManageImprovementPlanUseCaseImpl(repository);
        statusId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        when(repository.statusInfo(statusId)).thenReturn(Optional.of(
                new ImprovementPlanRepositoryPort.StatusInfo(statusId, false)));
        when(repository.save(any(ActorImprovementPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsPlanForInstructor() {
        var plan = useCase.create(command(UUID.randomUUID(), null, "COMPANY_VISIT"), actorId);

        assertThat(plan.instructorId()).isNotNull();
        assertThat(plan.learnerId()).isNull();
    }

    @Test
    void createsPlanForLearner() {
        var plan = useCase.create(command(null, UUID.randomUUID(), "DISCIPLINARY"), actorId);

        assertThat(plan.learnerId()).isNotNull();
        assertThat(plan.instructorId()).isNull();
    }

    @Test
    void rejectsBothArcEnds() {
        assertThatThrownBy(() -> useCase.create(command(UUID.randomUUID(), UUID.randomUUID(), "COMPANY_VISIT"), actorId))
                .isInstanceOf(InvalidImprovementPlanException.class)
                .hasMessage("EXCLUSIVE_ARC_VIOLATION");
    }

    @Test
    void rejectsMissingArcEnds() {
        assertThatThrownBy(() -> useCase.create(command(null, null, "COMPANY_VISIT"), actorId))
                .isInstanceOf(InvalidImprovementPlanException.class)
                .hasMessage("EXCLUSIVE_ARC_VIOLATION");
    }

    @Test
    void rejectsInvalidOrigin() {
        assertThatThrownBy(() -> useCase.create(command(UUID.randomUUID(), null, "OTHER"), actorId))
                .isInstanceOf(InvalidImprovementPlanException.class)
                .hasMessage("invalid origin");
    }

    @Test
    void populatesCompletedAtWhenStatusBecomesTerminal() {
        UUID planId = UUID.randomUUID();
        UUID terminalStatusId = UUID.randomUUID();
        ActorImprovementPlan current = new ActorImprovementPlan(planId, actorId, null, "TRACKING_SESSION",
                null, "Description", "Goals", LocalDate.of(2026, 12, 31), statusId, actorId,
                java.time.OffsetDateTime.now(), null, java.time.OffsetDateTime.now(), actorId, 0,
                true, null, null);
        when(repository.findById(planId)).thenReturn(Optional.of(current));
        when(repository.statusInfo(terminalStatusId)).thenReturn(Optional.of(
                new ImprovementPlanRepositoryPort.StatusInfo(terminalStatusId, true)));

        ActorImprovementPlan updated = useCase.update(planId,
                new ManageImprovementPlanUseCase.UpdateCommand(null, null, null, terminalStatusId), actorId);

        assertThat(updated.completedAt()).isNotNull();
        assertThat(updated.statusId()).isEqualTo(terminalStatusId);
    }

        @Test
        void doesNotCompletePlanWhenStatusIsNotTerminal() {
                UUID planId = UUID.randomUUID();
                ActorImprovementPlan current = new ActorImprovementPlan(planId, actorId, null, "TRACKING_SESSION",
                                null, "Description", "Goals", LocalDate.of(2026, 12, 31), statusId, actorId,
                                java.time.OffsetDateTime.now(), null, java.time.OffsetDateTime.now(), actorId, 0,
                                true, null, null);
                when(repository.findById(planId)).thenReturn(Optional.of(current));

                ActorImprovementPlan updated = useCase.update(planId,
                                new ManageImprovementPlanUseCase.UpdateCommand(null, null, null, statusId), actorId);

                assertThat(updated.completedAt()).isNull();
        }

    private ManageImprovementPlanUseCase.CreateCommand command(
            UUID instructorId, UUID learnerId, String origin) {
        return new ManageImprovementPlanUseCase.CreateCommand(instructorId, learnerId, origin, null,
                "Description", "Specific goals", LocalDate.of(2026, 12, 31), statusId);
    }
}