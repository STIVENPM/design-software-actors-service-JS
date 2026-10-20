package co.sena.actors.adapter.out.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.jdbc.core.RowMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ActorsOutboxDispatcherTest {
    @SuppressWarnings("unchecked")
    @Test
    void successfulBrokerAckMarksClaimedRowPublished() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
        UUID id = UUID.randomUUID();
        UUID aggregate = UUID.randomUUID();
        var event = new ActorsOutboxDispatcher.Event(id, aggregate, "actors-events", "{}", 1);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(20))).thenReturn(List.of(event));
        when(kafka.send(eq("actors-events"), eq(id.toString()), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        var worker = new ActorsOutboxDispatcher(jdbc, kafka, new ObjectMapper(), 20);
        assertEquals(1, worker.dispatchBatch());
        verify(jdbc).update(contains("SET status = 'PUBLISHED'"), eq(id));
    }

    @SuppressWarnings("unchecked")
    @Test
    void failedBrokerSendRequeuesWithBackoff() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
        UUID id = UUID.randomUUID();
        var event = new ActorsOutboxDispatcher.Event(id, UUID.randomUUID(), "actors-events", "{}", 2);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(20))).thenReturn(List.of(event));
        when(kafka.send(anyString(), anyString(), any())).thenThrow(new IllegalStateException("offline"));
        var worker = new ActorsOutboxDispatcher(jdbc, kafka, new ObjectMapper(), 20);
        assertEquals(1, worker.dispatchBatch());
        verify(jdbc).update(contains("make_interval"), eq("PENDING"), eq(4),
                contains("offline"), eq(id));
    }
}
