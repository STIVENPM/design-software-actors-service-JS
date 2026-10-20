package co.sena.actors.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.sena.actors.application.port.in.GetActiveFichaByUserUseCase.ActiveFichaResult;
import co.sena.actors.application.port.out.LearnerFichaEnrollmentRepositoryPort;
import co.sena.actors.application.port.out.LearnerRepositoryPort;
import co.sena.actors.domain.model.DocumentType;
import co.sena.actors.domain.model.Learner;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetActiveFichaByUserUseCaseImplTest {

    private final LearnerRepositoryPort learnerRepository = mock(LearnerRepositoryPort.class);
    private final LearnerFichaEnrollmentRepositoryPort enrollmentRepository =
            mock(LearnerFichaEnrollmentRepositoryPort.class);
    private final GetActiveFichaByUserUseCaseImpl useCase =
            new GetActiveFichaByUserUseCaseImpl(learnerRepository, enrollmentRepository);

    private final UUID userId = UUID.randomUUID();
    private final Learner learner = new Learner(
            UUID.randomUUID(),
            userId,
            DocumentType.CC,
            "123456",
            "Ada",
            "Lovelace",
            "ada@example.com",
            null,
            null,
            true,
            null);

    @BeforeEach
    void setUp() {
        when(learnerRepository.findActiveByUserId(userId)).thenReturn(Optional.of(learner));
    }

    @Test
    void returnsActiveFichaForLearner() {
        UUID fichaId = UUID.randomUUID();
        when(enrollmentRepository.findActiveFichaIdsByLearnerId(learner.id()))
                .thenReturn(List.of(fichaId));

        assertEquals(new ActiveFichaResult(learner.id(), fichaId),
                useCase.findActiveFichaByUserId(userId));
        verify(enrollmentRepository).findActiveFichaIdsByLearnerId(learner.id());
    }

    @Test
    void returnsNullFichaWhenLearnerHasNoActiveFicha() {
        when(enrollmentRepository.findActiveFichaIdsByLearnerId(learner.id()))
                .thenReturn(List.of());

        ActiveFichaResult result = useCase.findActiveFichaByUserId(userId);

        assertEquals(learner.id(), result.learnerId());
        assertNull(result.fichaId());
    }

    @Test
    void throwsWhenLearnerDoesNotExist() {
        when(learnerRepository.findActiveByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(LearnerNotFoundException.class,
                () -> useCase.findActiveFichaByUserId(userId));
    }

    @Test
    void throwsWhenLearnerHasMultipleActiveFichas() {
        when(enrollmentRepository.findActiveFichaIdsByLearnerId(learner.id()))
                .thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID()));

        assertThrows(ActiveFichaInconsistencyException.class,
                () -> useCase.findActiveFichaByUserId(userId));
    }
}
