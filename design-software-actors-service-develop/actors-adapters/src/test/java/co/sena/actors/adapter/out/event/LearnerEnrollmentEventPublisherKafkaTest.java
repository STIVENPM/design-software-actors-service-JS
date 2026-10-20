package co.sena.actors.adapter.out.event;

import co.sena.actors.domain.model.LearnerEnrollmentStage;
import co.sena.actors.domain.model.LearnerFichaEnrollment;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = LearnerEnrollmentEventPublisherKafkaTest.TestConfig.class,
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
        }
)
@EmbeddedKafka(
        partitions = 1,
        topics = LearnerEnrollmentEventPublisherAdapter.TOPIC,
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:0",
                "port=0"
        }
)
class LearnerEnrollmentEventPublisherKafkaTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void shouldPublishLearnerEnrolledEventToRealEmbeddedKafkaBroker() {
        Map<String, Object> producerProps = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafka));
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        KafkaTemplate<String, Object> kafkaTemplate =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
        LearnerEnrollmentEventPublisherAdapter adapter =
                new LearnerEnrollmentEventPublisherAdapter(kafkaTemplate);

        LearnerFichaEnrollment enrollment = new LearnerFichaEnrollment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LearnerEnrollmentStage.LECTURE,
                LocalDate.of(2026, 9, 17),
                null,
                true,
                null);

        adapter.publishLearnerEnrolledEvent(enrollment);
        kafkaTemplate.flush();

        Map<String, Object> consumerProps = new HashMap<>(
                KafkaTestUtils.consumerProps("learner-enrolled-event-test", "false", embeddedKafka));
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (Consumer<String, String> consumer =
                     new org.springframework.kafka.core.DefaultKafkaConsumerFactory<String, String>(consumerProps)
                             .createConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, LearnerEnrollmentEventPublisherAdapter.TOPIC);

            ConsumerRecord<String, String> received = null;
            long deadline = System.currentTimeMillis() + 10_000;
            while (received == null && System.currentTimeMillis() < deadline) {
                var records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    received = record;
                    break;
                }
            }

            assertNotNull(received, "El broker Kafka no recibio el evento");
            assertEquals(LearnerEnrollmentEventPublisherAdapter.TOPIC, received.topic());
            assertFalse(received.value().isBlank(), "El mensaje Kafka no puede estar vacio");
            assertTrue(
                    received.value().contains(LearnerEnrollmentEventPublisherAdapter.EVENT_TYPE),
                    "El mensaje debe contener event_type=" + LearnerEnrollmentEventPublisherAdapter.EVENT_TYPE);
        }
    }

    static class TestConfig {
    }
}
