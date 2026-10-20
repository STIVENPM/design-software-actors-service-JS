package co.sena.actors.adapter.in.event;

import co.sena.actors.application.port.in.IamUserCreated;
import co.sena.actors.application.port.in.IamUserDeactivated;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "actors.iam.consumer.enabled", havingValue = "true")
public class IamRabbitListener {
    private final IamEventParser parser;
    private final IamEventApplicationService applicationService;
    private final ObservationRegistry observations;

    public IamRabbitListener(IamEventParser parser, IamEventApplicationService applicationService,
            ObservationRegistry observations) {
        this.parser = parser;
        this.applicationService = applicationService;
        this.observations = observations;
    }

    @RabbitListener(queues = "${actors.iam.queue}", containerFactory = "iamRabbitListenerContainerFactory")
    public void receive(Message message) {
        Observation observation = Observation.createNotStarted("iam.event.consume", observations);
        observation.observe(() -> {
            Object event = parser.parse(message.getBody());
            String eventType = event instanceof IamUserCreated ? "iam.user.created" : "iam.user.deactivated";
            String eventId = event instanceof IamUserCreated created
                    ? created.eventId().toString() : ((IamUserDeactivated) event).eventId().toString();
            observation.lowCardinalityKeyValue("event.type", eventType)
                    .highCardinalityKeyValue("event.id", eventId);
            applicationService.process(event);
        });
    }
}
