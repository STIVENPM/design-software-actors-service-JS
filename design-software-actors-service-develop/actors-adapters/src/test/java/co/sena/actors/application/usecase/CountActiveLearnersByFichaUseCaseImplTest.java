package co.sena.actors.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.sena.actors.application.port.out.FichaExistencePort;
import co.sena.actors.application.port.out.LearnerFichaEnrollmentRepositoryPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CountActiveLearnersByFichaUseCaseImplTest {

    private final LearnerFichaEnrollmentRepositoryPort enrollments =
            mock(LearnerFichaEnrollmentRepositoryPort.class);
    private final FichaExistencePort fichas = mock(FichaExistencePort.class);
    private final CountActiveLearnersByFichaUseCaseImpl useCase =
            new CountActiveLearnersByFichaUseCaseImpl(enrollments, fichas);

    @Test
    void existingFichaWithoutEnrollmentsReturnsZero() {
        UUID fichaId = UUID.randomUUID();
        when(fichas.existsById(fichaId)).thenReturn(true);

        assertEquals(0, useCase.countActiveLearners(fichaId));
    }

    @Test
    void unknownFichaDoesNotCountEnrollments() {
        UUID fichaId = UUID.randomUUID();

        assertThrows(FichaNotFoundException.class, () -> useCase.countActiveLearners(fichaId));
        verifyNoInteractions(enrollments);
    }
}
