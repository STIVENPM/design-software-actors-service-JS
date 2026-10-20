package co.sena.actors.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;

class IamRabbitListenerObservationTest {
    @Test
    void recordsConsumeObservationWithEventTypeAndEventId() {
        var captured = new AtomicReference<Observation.Context>();
        var observations = ObservationRegistry.create();
        observations.observationConfig().observationHandler(new ObservationHandler<Observation.Context>() {
            @Override public boolean supportsContext(Observation.Context context) { return true; }
            @Override public void onStop(Observation.Context context) { captured.set(context); }
        });
        var applicationService = mock(IamEventApplicationService.class);
        var listener = new IamRabbitListener(new IamEventParser(new ObjectMapper()),
                applicationService, observations);
        byte[] event = """
                {"event_id":"11111111-1111-1111-1111-111111111111",
                 "event_type":"iam.user.created","version":"1",
                 "timestamp":"2026-10-01T12:00:00Z","source_service":"iam-service",
                 "payload":{"user_id":"22222222-2222-2222-2222-222222222222",
                            "actor_type":"USER","email":"user@sena.edu.co",
                            "first_name":"User","last_name":"Test","roles":[]}}
                """.getBytes(StandardCharsets.UTF_8);

        listener.receive(new Message(event));

        verify(applicationService).process(any());
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getName()).isEqualTo("iam.event.consume");
        assertThat(captured.get().getLowCardinalityKeyValues().toString())
                .contains("event.type=iam.user.created");
        assertThat(captured.get().getHighCardinalityKeyValues().toString())
                .contains("event.id=11111111-1111-1111-1111-111111111111");
    }
}
