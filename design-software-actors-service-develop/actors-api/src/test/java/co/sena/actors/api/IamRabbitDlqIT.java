package co.sena.actors.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import co.sena.actors.adapter.in.event.IamEventApplicationService;
import co.sena.actors.adapter.in.event.IamEventParser;
import co.sena.actors.adapter.in.event.IamRabbitListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.testcontainers.containers.GenericContainer;

/** Verifies the configured retry chain and consumer DLQ against a real RabbitMQ. */
class IamRabbitDlqIT {
    @Test
    void fourTotalAttemptsThenDeadLettersToConfiguredConsumerQueue() throws Exception {
        try (var rabbit = new GenericContainer<>("rabbitmq:3.13-management")
                .withExposedPorts(5672)
                .withEnv("RABBITMQ_DEFAULT_USER", "actors_test")
                .withEnv("RABBITMQ_DEFAULT_PASS", "actors_test")) {
            rabbit.start();
            var connectionFactory = new CachingConnectionFactory(rabbit.getHost(), rabbit.getMappedPort(5672));
            connectionFactory.setUsername("actors_test");
            connectionFactory.setPassword("actors_test");
            String suffix = UUID.randomUUID().toString();
            String queueName = "actors.iam.it." + suffix;
            String dlxName = "actors.iam.it.dlx." + suffix;
            String dlqName = "actors.iam.it.dlq." + suffix;
            var config = new IamConsumerConfiguration();
            var exchange = config.iamEventsExchange("iam-events");
            var dlx = config.iamConsumerDeadLetterExchange(dlxName);
            var queue = config.iamInboundQueue(queueName, dlxName, dlqName);
            var dlq = config.iamConsumerDeadLetterQueue(dlqName);
            var admin = new RabbitAdmin(connectionFactory);
            admin.declareExchange(exchange);
            admin.declareExchange(dlx);
            admin.declareQueue(queue);
            admin.declareQueue(dlq);
            admin.declareBinding(config.iamCreatedBinding(queue, exchange));
            admin.declareBinding(config.iamDeactivatedBinding(queue, exchange));
            admin.declareBinding(config.iamDeadLetterBinding(dlq, dlx, dlqName));
            assertThat(dlq.getArguments()).containsEntry("x-message-ttl", 604800000);
            assertThat(queue.getArguments()).containsEntry("x-dead-letter-exchange", dlxName)
                    .containsEntry("x-dead-letter-routing-key", dlqName);

            var applicationService = mock(IamEventApplicationService.class);
            List<Long> attemptNanos = new CopyOnWriteArrayList<>();
            doAnswer(invocation -> {
                attemptNanos.add(System.nanoTime());
                throw new IllegalStateException("forced rollback");
            }).when(applicationService).process(any());
            var listener = new IamRabbitListener(new IamEventParser(new ObjectMapper()),
                    applicationService, ObservationRegistry.NOOP);
            var endpoint = new SimpleRabbitListenerEndpoint();
            endpoint.setId("iam-it-" + suffix);
            endpoint.setQueueNames(queueName);
            endpoint.setMessageListener((MessageListener) listener::receive);
            SimpleMessageListenerContainer container = config
                    .iamRabbitListenerContainerFactory(connectionFactory)
                    .createListenerContainer(endpoint);
            try {
                container.afterPropertiesSet();
                container.start();
                String eventId = UUID.randomUUID().toString();
                String userId = UUID.randomUUID().toString();
                byte[] event = ("""
                        {"event_id":"%s","event_type":"iam.user.created","version":"1",
                         "timestamp":"2026-10-01T12:00:00Z","source_service":"iam-service",
                         "payload":{"user_id":"%s","actor_type":"USER","email":"user@sena.edu.co",
                                    "first_name":"User","last_name":"Test","roles":[]}}
                        """).formatted(eventId, userId).getBytes(StandardCharsets.UTF_8);
                var template = new RabbitTemplate(connectionFactory);
                template.convertAndSend("iam-events", "iam.user.created", event);
                Message dead = template.receive(dlqName, 35000);
                assertThat(dead).isNotNull();
                assertThat(dead.getBody()).isEqualTo(event);
                org.mockito.Mockito.verify(applicationService, org.mockito.Mockito.times(4)).process(any());
                assertThat(attemptNanos).hasSize(4);
                // Allow scheduler jitter while checking the actual 1/4/16 second backoff.
                long first = intervalMillis(attemptNanos, 0);
                long second = intervalMillis(attemptNanos, 1);
                long third = intervalMillis(attemptNanos, 2);
                System.out.printf("IAM consumer retry intervals (ms): %d, %d, %d%n", first, second, third);
                assertThat(first).isBetween(800L, 8000L);
                assertThat(second).isBetween(3500L, 12000L);
                assertThat(third).isBetween(15000L, 30000L);
            } finally {
                container.stop();
                connectionFactory.destroy();
            }
        }
    }

    private long intervalMillis(List<Long> attempts, int index) {
        return TimeUnit.NANOSECONDS.toMillis(attempts.get(index + 1) - attempts.get(index));
    }
}
