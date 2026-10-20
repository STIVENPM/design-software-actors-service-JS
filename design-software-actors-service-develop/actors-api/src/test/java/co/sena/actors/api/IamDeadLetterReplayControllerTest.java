package co.sena.actors.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.AMQP;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IamDeadLetterReplayControllerTest {
    @Test
    void republishesAndAcknowledgesInOneRabbitTransaction() throws Exception {
        ConnectionFactory factory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        Channel channel = mock(Channel.class);
        when(factory.createConnection()).thenReturn(connection);
        when(connection.createChannel(true)).thenReturn(channel);
        byte[] body = "{\"event_type\":\"iam.user.created\"}".getBytes();
        AMQP.BasicProperties props = new AMQP.BasicProperties();
        when(channel.basicGet("iam.events.dlq", false)).thenReturn(
                new GetResponse(new Envelope(42, false, "", ""), props, body, 1));
        var controller = new IamDeadLetterReplayController(factory, new ObjectMapper(),
                "iam.events.dlq", "iam-events");
        assertEquals(202, controller.replayOne().getStatusCode().value());
        var order = inOrder(channel);
        order.verify(channel).txSelect();
        order.verify(channel).basicGet("iam.events.dlq", false);
        order.verify(channel).basicPublish("iam-events", "iam.user.created", props, body);
        order.verify(channel).basicAck(42, false);
        order.verify(channel).txCommit();
        verify(connection).close();
    }

    @Test
    void failedPublishRollsBackWithoutAcknowledgingTheDeadLetter() throws Exception {
        ConnectionFactory factory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        Channel channel = mock(Channel.class);
        when(factory.createConnection()).thenReturn(connection);
        when(connection.createChannel(true)).thenReturn(channel);
        byte[] body = "{\"event_type\":\"iam.user.created\"}".getBytes();
        AMQP.BasicProperties props = new AMQP.BasicProperties();
        when(channel.basicGet("iam.events.dlq", false)).thenReturn(
                new GetResponse(new Envelope(42, false, "", ""), props, body, 1));
        doThrow(new IOException("broker unavailable"))
                .when(channel).basicPublish("iam-events", "iam.user.created", props, body);
        var controller = new IamDeadLetterReplayController(factory, new ObjectMapper(),
                "iam.events.dlq", "iam-events");
        assertThrows(IOException.class, controller::replayOne);
        verify(channel).txRollback();
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(connection).close();
    }
}
