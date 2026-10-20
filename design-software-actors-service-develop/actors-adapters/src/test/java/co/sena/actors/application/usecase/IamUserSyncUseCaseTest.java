package co.sena.actors.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.sena.actors.application.port.in.IamActorType;
import co.sena.actors.application.port.in.IamUserCreated;
import co.sena.actors.application.port.in.IamUserDeactivated;
import co.sena.actors.application.port.out.ActivityLogRepositoryPort;
import co.sena.actors.application.port.out.CompetencyAssignmentRepositoryPort;
import co.sena.actors.application.port.out.IamEventInboxPort;
import co.sena.actors.application.port.out.InstructorAreaRepositoryPort;
import co.sena.actors.application.port.out.InstructorRepositoryPort;
import co.sena.actors.application.port.out.LearnerRepositoryPort;
import co.sena.actors.domain.model.CompetencyAssignment;
import co.sena.actors.domain.model.DocumentType;
import co.sena.actors.domain.model.Instructor;
import co.sena.actors.domain.model.InstructorArea;
import co.sena.actors.domain.model.Learner;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IamUserSyncUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EVENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACTOR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SYSTEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

    @Mock IamEventInboxPort inbox;
    @Mock InstructorRepositoryPort instructors;
    @Mock LearnerRepositoryPort learners;
    @Mock CompetencyAssignmentRepositoryPort assignments;
    @Mock InstructorAreaRepositoryPort areas;
    @Mock ActivityLogRepositoryPort activityLog;

    private IamUserSyncUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new IamUserSyncUseCase(inbox, instructors, learners, assignments, areas,
                activityLog, Clock.fixed(NOW, ZoneOffset.UTC), SYSTEM_ID);
    }

    @Test
    void userTypeRecordsEventWithoutCreatingAnActorOrPersistingRoles() {
        when(inbox.claim(EVENT_ID, USER_ID, "iam.user.created")).thenReturn(true);

        assertThat(useCase.created(new IamUserCreated(EVENT_ID, USER_ID, IamActorType.USER,
                "user@sena.edu.co", null, null, List.of("SYSTEM_ADMIN"))))
                .isEqualTo(IamUserSyncUseCase.Result.PROCESSED);

        verifyNoInteractions(instructors, learners, assignments, areas, activityLog);
    }

    @Test
    void sameEventIdIsIgnoredBeforeAnyActorChange() {
        when(inbox.claim(EVENT_ID, USER_ID, "iam.user.created")).thenReturn(false);

        assertThat(useCase.created(created(IamActorType.INSTRUCTOR)))
                .isEqualTo(IamUserSyncUseCase.Result.DUPLICATE);

        verifyNoInteractions(instructors, learners, assignments, areas, activityLog);
    }

    @Test
    void existingInstructorIsLinkedByUserIdAndReceivesIamIdentityFields() {
        Instructor instructor = instructor(true);
        when(inbox.claim(EVENT_ID, USER_ID, "iam.user.created")).thenReturn(true);
        when(instructors.findByUserId(USER_ID)).thenReturn(Optional.of(instructor));
        when(learners.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(useCase.created(created(IamActorType.INSTRUCTOR)))
                .isEqualTo(IamUserSyncUseCase.Result.PROCESSED);

        assertThat(instructor.getFirstName()).isEqualTo("Ana");
        assertThat(instructor.getLastName()).isEqualTo("Ríos");
        assertThat(instructor.getEmail()).isEqualTo("ana@sena.edu.co");
        verify(instructors).save(instructor);
        verify(activityLog).save(any());
        verifyNoInteractions(assignments, areas);
    }

    @Test
    void createsInstructorWhenProfileDoesNotExistAndProvisioningFieldsArePresent() {
        when(inbox.claim(EVENT_ID, USER_ID, "iam.user.created")).thenReturn(true);
        when(instructors.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(learners.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(useCase.created(created(IamActorType.INSTRUCTOR)))
                .isEqualTo(IamUserSyncUseCase.Result.PROCESSED);

        ArgumentCaptor<Instructor> saved = ArgumentCaptor.forClass(Instructor.class);
        verify(instructors).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getValue().getDocumentType()).isEqualTo(DocumentType.CC);
        assertThat(saved.getValue().getDocumentNumber()).isEqualTo("1234567890");
        assertThat(saved.getValue().getDefaultMaxHoursPerWeek()).isEqualByComparingTo("40.0");
        assertThat(saved.getValue().getCreatedBy()).isEqualTo(SYSTEM_ID);
        verify(activityLog).save(any());
    }

    @Test
    void createsLearnerWhenProfileDoesNotExistAndProvisioningFieldsArePresent() {
        when(inbox.claim(EVENT_ID, USER_ID, "iam.user.created")).thenReturn(true);
        when(instructors.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(learners.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(useCase.created(created(IamActorType.LEARNER)))
                .isEqualTo(IamUserSyncUseCase.Result.PROCESSED);

        ArgumentCaptor<Learner> saved = ArgumentCaptor.forClass(Learner.class);
        verify(learners).save(saved.capture());
        assertThat(saved.getValue().userId()).isEqualTo(USER_ID);
        assertThat(saved.getValue().documentType()).isEqualTo(DocumentType.CC);
        assertThat(saved.getValue().documentNumber()).isEqualTo("1234567890");
        verify(activityLog).save(any());
    }

    @Test
    void deactivationDisablesInstructorAndOnlyActiveChildren() {
        Instructor instructor = instructor(true);
        CompetencyAssignment assignment = new CompetencyAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setInstructorId(ACTOR_ID);
        assignment.setActive(true);
        InstructorArea area = new InstructorArea();
        area.setId(UUID.randomUUID());
        area.setInstructorId(ACTOR_ID);
        area.setActive(true);
        when(inbox.claim(EVENT_ID, USER_ID, "iam.user.deactivated")).thenReturn(true);
        when(instructors.findByUserId(USER_ID)).thenReturn(Optional.of(instructor));
        when(learners.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(assignments.findActiveByInstructorId(ACTOR_ID)).thenReturn(List.of(assignment));
        when(areas.findActiveByInstructorId(ACTOR_ID)).thenReturn(List.of(area));

        assertThat(useCase.deactivated(new IamUserDeactivated(EVENT_ID, USER_ID)))
                .isEqualTo(IamUserSyncUseCase.Result.PROCESSED);

        assertThat(instructor.isActive()).isFalse();
        assertThat(assignment.isActive()).isFalse();
        assertThat(area.isActive()).isFalse();
        verify(instructors).save(instructor);
        verify(assignments).save(assignment);
        verify(areas).save(area);
        verify(activityLog).save(any());
    }

    @Test
    void repeatedDeactivationDoesNotWriteAnotherActivityEntry() {
        when(inbox.claim(EVENT_ID, USER_ID, "iam.user.deactivated")).thenReturn(true);
        when(instructors.findByUserId(USER_ID)).thenReturn(Optional.of(instructor(false)));
        when(learners.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(useCase.deactivated(new IamUserDeactivated(EVENT_ID, USER_ID)))
                .isEqualTo(IamUserSyncUseCase.Result.PROCESSED);

        verifyNoInteractions(assignments, areas, activityLog);
        verify(instructors, never()).save(any());
    }

    @Test
    void learnerDeactivationWritesLearnerIdInActivityLog() {
        Learner learner = new Learner(ACTOR_ID, USER_ID, null, "123", "Ana", "Ríos",
                "ana@sena.edu.co", null, null, true, LocalDateTime.of(2026, 1, 1, 0, 0));
        when(inbox.claim(EVENT_ID, USER_ID, "iam.user.deactivated")).thenReturn(true);
        when(instructors.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(learners.findByUserId(USER_ID)).thenReturn(Optional.of(learner));

        assertThat(useCase.deactivated(new IamUserDeactivated(EVENT_ID, USER_ID)))
                .isEqualTo(IamUserSyncUseCase.Result.PROCESSED);

        ArgumentCaptor<Learner> saved = ArgumentCaptor.forClass(Learner.class);
        verify(learners).save(saved.capture());
        assertThat(saved.getValue().isActive()).isFalse();
        var log = ArgumentCaptor.forClass(co.sena.actors.domain.model.ActivityLogEntry.class);
        verify(activityLog).save(log.capture());
        assertThat(log.getValue().getLearnerId()).isEqualTo(ACTOR_ID);
        assertThat(log.getValue().getInstructorId()).isNull();
        verifyNoInteractions(assignments, areas);
    }

    @Test
    void unknownUserIsNotAcknowledgedAsDeactivated() {
        when(inbox.claim(EVENT_ID, USER_ID, "iam.user.deactivated")).thenReturn(true);
        when(instructors.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(learners.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.deactivated(new IamUserDeactivated(EVENT_ID, USER_ID)))
                .isInstanceOf(UnprovisionedIamActorException.class);
        verifyNoInteractions(activityLog);
    }

    private IamUserCreated created(IamActorType type) {
        return new IamUserCreated(EVENT_ID, USER_ID, type, "ana@sena.edu.co", "Ana", "Ríos",
                "CC", "1234567890", BigDecimal.valueOf(40.0), List.of("INSTRUCTOR", "EVALUATOR"));
    }

    private Instructor instructor(boolean active) {
        Instructor instructor = new Instructor();
        instructor.setId(ACTOR_ID);
        instructor.setUserId(USER_ID);
        instructor.setFirstName("Old");
        instructor.setLastName("Name");
        instructor.setEmail("old@sena.edu.co");
        instructor.setActive(active);
        return instructor;
    }
}
