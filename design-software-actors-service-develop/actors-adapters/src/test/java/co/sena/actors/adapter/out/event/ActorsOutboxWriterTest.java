package co.sena.actors.adapter.out.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ActorsOutboxWriterTest {
    @Test
    void refusesAnOutboxWriteOutsideTheBusinessTransaction() {
        var writer = new ActorsOutboxWriter(mock(JdbcTemplate.class), new ObjectMapper());
        assertThrows(IllegalStateException.class,
                () -> writer.enqueue("actors.test", UUID.randomUUID(), Map.of("ok", true)));
    }

    @Test
    void insertsTheEventEnvelopeInsideTheBusinessTransaction() {
        var jdbc = mock(JdbcTemplate.class);
        var writer = new ActorsOutboxWriter(jdbc, new ObjectMapper());
        UUID aggregate = UUID.randomUUID();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            UUID id = writer.enqueue("actors.test", aggregate, Map.of("ok", true));
            var json = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(contains("INSERT INTO actors_tracking.outbox_event"),
                    eq(id), eq("actors.test"), eq(aggregate), json.capture());
            assertTrue(json.getValue().contains(id.toString()));
            assertTrue(json.getValue().contains("actors.test"));
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }
}
