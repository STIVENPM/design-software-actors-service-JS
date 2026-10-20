package co.sena.actors.api;

import co.sena.actors.adapter.out.event.ActorsOutboxDispatcher;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal operator endpoint. Gateway does not expose this path by default. */
@RestController
@RequestMapping("/api/v1/ops/actors/outbox")
@PreAuthorize("hasAuthority('ACT_INSTRUCTOR_MANAGE')")
public class ActorsOutboxOperationsController {
    private final JdbcTemplate jdbc;
    private final ActorsOutboxDispatcher dispatcher;

    public ActorsOutboxOperationsController(JdbcTemplate jdbc, ActorsOutboxDispatcher dispatcher) {
        this.jdbc = jdbc;
        this.dispatcher = dispatcher;
    }

    @GetMapping("/status")
    public Map<String, Integer> status() {
        return Map.of(
                "pending", count("PENDING"),
                "sending", count("SENDING"),
                "published", count("PUBLISHED"),
                "dead", count("DEAD"));
    }

    @PostMapping("/{eventId}/replay")
    public ResponseEntity<Void> replay(@PathVariable UUID eventId) {
        return dispatcher.replayDead(eventId) ? ResponseEntity.accepted().build()
                : ResponseEntity.notFound().build();
    }

    private int count(String state) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM actors_tracking.outbox_event WHERE status = ?", Integer.class, state);
    }
}
