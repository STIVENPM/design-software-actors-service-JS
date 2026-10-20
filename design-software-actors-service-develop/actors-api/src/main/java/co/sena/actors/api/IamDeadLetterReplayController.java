package co.sena.actors.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import java.io.IOException;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Explicit, one-message operator replay. Publish and acknowledge share an AMQP transaction. */
@RestController
@RequestMapping("/api/v1/ops/actors/iam-dlq")
@PreAuthorize("hasAuthority('ACT_INSTRUCTOR_MANAGE')")
@ConditionalOnProperty(name = "actors.iam.consumer.enabled", havingValue = "true")
public class IamDeadLetterReplayController {
    private final ConnectionFactory connections;
    private final ObjectMapper mapper;
    private final String dlq;
    private final String exchange;

    public IamDeadLetterReplayController(ConnectionFactory connections, ObjectMapper mapper,
            @Value("${actors.iam.dlq}") String dlq,
            @Value("${actors.iam.exchange:iam-events}") String exchange) {
        this.connections = connections;
        this.mapper = mapper;
        this.dlq = dlq;
        this.exchange = exchange;
    }

    @PostMapping("/replay-one")
    public ResponseEntity<Void> replayOne() throws IOException {
        Connection connection = connections.createConnection();
        Channel channel = connection.createChannel(true);
        try {
            channel.txSelect();
            GetResponse dead = channel.basicGet(dlq, false);
            if (dead == null) return ResponseEntity.noContent().build();
            JsonNode body = mapper.readTree(dead.getBody());
            String type = body.path("event_type").asText();
            if (!type.equals("iam.user.created") && !type.equals("iam.user.deactivated")) {
                throw new IllegalArgumentException("Unsupported dead-letter event type");
            }
            channel.basicPublish(exchange, type, dead.getProps(), dead.getBody());
            channel.basicAck(dead.getEnvelope().getDeliveryTag(), false);
            channel.txCommit();
            return ResponseEntity.accepted().build();
        } catch (Exception failure) {
            channel.txRollback();
            if (failure instanceof IOException io) throw io;
            throw new IllegalStateException("Could not replay IAM dead letter", failure);
        } finally {
            try { channel.close(); } catch (Exception ignored) { /* preserve original failure */ } finally { connection.close(); }
        }
    }
}
