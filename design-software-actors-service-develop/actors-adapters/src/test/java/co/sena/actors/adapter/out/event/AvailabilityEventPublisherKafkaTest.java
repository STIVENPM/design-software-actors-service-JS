package co.sena.actors.adapter.out.event;

import co.sena.actors.core.domain.InstructorAvailabilityException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = AvailabilityEventPublisherKafkaTest.TestConfig.class,
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
        }
)
@EmbeddedKafka(
        partitions = 1,
        topics = AvailabilityEventPublisherKafkaTest.TOPIC,
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:0",
                "port=0"
        }
)
class AvailabilityEventPublisherKafkaTest {

    static final String TOPIC = "actors-events";

    private static final String EVENT_TYPE =
            "actors.instructor.availability_changed";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void shouldPublishAvailabilityChangedEventToRealEmbeddedKafkaBroker() {

        Map<String, Object> producerProps =
                new HashMap<>(KafkaTestUtils.producerProps(embeddedKafka));

        producerProps.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);

        producerProps.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class);

        DefaultKafkaProducerFactory<String, Object> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);

        KafkaTemplate<String, Object> kafkaTemplate =
                new KafkaTemplate<>(producerFactory);

        AvailabilityEventPublisherAdapter adapter =
                new AvailabilityEventPublisherAdapter(kafkaTemplate);

        InstructorAvailabilityException exception =
                new InstructorAvailabilityException(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "TRAINING",
                        LocalDateTime.of(2026, 9, 13, 8, 0),
                        LocalDateTime.of(2026, 9, 13, 10, 0),
                        "QA Kafka integration test",
                        UUID.randomUUID(),
                        LocalDateTime.now(),
                        true,
                        null,
                        null
                );

        adapter.publishAvailabilityChangedEvent(exception);
        kafkaTemplate.flush();

        Map<String, Object> consumerProps =
                new HashMap<>(
                        KafkaTestUtils.consumerProps(
                                "availability-event-test",
                                "false",
                                embeddedKafka
                        )
                );

        consumerProps.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest");

        consumerProps.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);

        consumerProps.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        try (Consumer<String, String> consumer =
                     consumerFactory.createConsumer()) {

            embeddedKafka.consumeFromAnEmbeddedTopic(
                    consumer,
                    TOPIC
            );

            ConsumerRecord<String, String> received = null;

            long deadline =
                    System.currentTimeMillis() + 10_000;

            while (received == null &&
                    System.currentTimeMillis() < deadline) {

                var records =
                        consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> record : records) {
                    received = record;
                    break;
                }
            }

            assertNotNull(
                    received,
                    "El broker Kafka no recibió el evento");

            assertEquals(
                    TOPIC,
                    received.topic()
            );

            assertFalse(
                    received.value().isBlank(),
                    "El mensaje Kafka no puede estar vacío"
            );

            assertTrue(
                    received.value().contains(EVENT_TYPE),
                    "El mensaje debe contener event_type=" + EVENT_TYPE
            );
        }
    }

    static class TestConfig {
    }
}
