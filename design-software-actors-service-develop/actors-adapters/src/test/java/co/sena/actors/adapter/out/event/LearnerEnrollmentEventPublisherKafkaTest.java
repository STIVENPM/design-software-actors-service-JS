package co.sena.actors.adapter.out.event;

import co.sena.actors.domain.model.LearnerFichaEnrollment;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class LearnerEnrollmentEventPublisherKafkaTest {
    @Test
    void writesEnrollmentToOutboxInsteadOfSendingToBroker() {
        var outbox = mock(ActorsOutboxWriter.class);
        var enrollment = mock(LearnerFichaEnrollment.class);
        UUID id = UUID.randomUUID();
        when(enrollment.id()).thenReturn(id);
        new LearnerEnrollmentEventPublisherAdapter(outbox).publishLearnerEnrolledEvent(enrollment);
        verify(outbox).enqueue("actors.learner.enrolled", id, enrollment);
    }
}
