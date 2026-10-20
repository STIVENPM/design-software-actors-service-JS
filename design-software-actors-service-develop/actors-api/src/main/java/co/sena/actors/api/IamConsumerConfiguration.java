package co.sena.actors.api;

import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.adapter.in.event.IamEventApplicationService;
import co.sena.actors.adapter.in.event.IamEventParser;
import co.sena.actors.application.port.out.ActivityLogRepositoryPort;
import co.sena.actors.application.port.out.CompetencyAssignmentRepositoryPort;
import co.sena.actors.application.port.out.IamEventInboxPort;
import co.sena.actors.application.port.out.InstructorAreaRepositoryPort;
import co.sena.actors.application.port.out.InstructorRepositoryPort;
import co.sena.actors.application.port.out.LearnerRepositoryPort;
import co.sena.actors.application.usecase.IamUserSyncUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.jdbc.core.JdbcTemplate;

/** Inbound topology names must be approved and supplied before enabling this feature. */
@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "actors.iam.consumer.enabled", havingValue = "true")
public class IamConsumerConfiguration {
    @Bean
    InitializingBean iamBrokerConfigurationVerified(
            @Value("${spring.rabbitmq.host:}") String host,
            @Value("${spring.rabbitmq.username:}") String username,
            @Value("${spring.rabbitmq.password:}") String password) {
        return () -> {
            requiredName(host, "SPRING_RABBITMQ_HOST");
            requiredName(username, "SPRING_RABBITMQ_USERNAME");
            requiredName(password, "SPRING_RABBITMQ_PASSWORD");
        };
    }

    @Bean
    InitializingBean iamInboxMigrationVerified(JdbcTemplate jdbc) {
        return () -> {
            String table = jdbc.queryForObject(
                    "SELECT to_regclass('actors_tracking.iam_processed_event')::text", String.class);
            if (table == null) {
                throw new IllegalStateException(
                        "actors-db migration for actors_tracking.iam_processed_event is required");
            }
        };
    }

    @Bean
    IamEventParser iamEventParser(ObjectMapper mapper) {
        return new IamEventParser(mapper);
    }

    @Bean
    IamEventApplicationService iamEventApplicationService(IamEventInboxPort inbox,
            InstructorRepositoryPort instructors, LearnerRepositoryPort learners,
            CompetencyAssignmentRepositoryPort assignments, InstructorAreaRepositoryPort areas,
            ActivityLogRepositoryPort logs) {
        var useCase = new IamUserSyncUseCase(inbox, instructors, learners,
                assignments, areas, logs, Clock.systemUTC(), CurrentUserProvider.SYSTEM_ACTOR_ID);
        return new IamEventApplicationService(useCase);
    }

    @Bean
    SimpleRabbitListenerContainerFactory iamRabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(4)
                .backOffOptions(1000, 4.0, 16000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }

    @Bean
    TopicExchange iamEventsExchange(@Value("${actors.iam.exchange:iam-events}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    TopicExchange iamConsumerDeadLetterExchange(@Value("${actors.iam.dlx}") String name) {
        return new TopicExchange(requiredName(name, "ACTORS_IAM_DLX"), true, false);
    }

    @Bean
    Queue iamInboundQueue(@Value("${actors.iam.queue}") String name,
            @Value("${actors.iam.dlx}") String deadLetterExchange,
            @Value("${actors.iam.dlq}") String deadLetterQueue) {
        String queueName = requiredName(name, "ACTORS_IAM_QUEUE");
        String dlqName = requiredName(deadLetterQueue, "ACTORS_IAM_DLQ");
        if (queueName.equals(dlqName)) {
            throw new IllegalArgumentException("IAM inbound queue and consumer DLQ must differ");
        }
        return new Queue(queueName, true, false, false, java.util.Map.of(
                "x-dead-letter-exchange", requiredName(deadLetterExchange, "ACTORS_IAM_DLX"),
                "x-dead-letter-routing-key", dlqName));
    }

    @Bean
    Queue iamConsumerDeadLetterQueue(@Value("${actors.iam.dlq}") String name) {
        return new Queue(requiredName(name, "ACTORS_IAM_DLQ"), true, false, false,
                java.util.Map.of("x-message-ttl", 604800000));
    }

    @Bean
    Binding iamCreatedBinding(Queue iamInboundQueue, TopicExchange iamEventsExchange) {
        return BindingBuilder.bind(iamInboundQueue).to(iamEventsExchange).with("iam.user.created");
    }

    @Bean
    Binding iamDeactivatedBinding(Queue iamInboundQueue, TopicExchange iamEventsExchange) {
        return BindingBuilder.bind(iamInboundQueue).to(iamEventsExchange).with("iam.user.deactivated");
    }

    @Bean
    Binding iamDeadLetterBinding(Queue iamConsumerDeadLetterQueue,
            TopicExchange iamConsumerDeadLetterExchange,
            @Value("${actors.iam.dlq}") String deadLetterQueue) {
        return BindingBuilder.bind(iamConsumerDeadLetterQueue)
                .to(iamConsumerDeadLetterExchange).with(deadLetterQueue);
    }

    private String requiredName(String name, String variable) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(variable + " is required when IAM consumption is enabled");
        }
        return name;
    }
}
