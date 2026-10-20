package co.sena.actors.adapter.config;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReportRateLimiter {

    private final int limit;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public ReportRateLimiter(@Value("${actors.reports.rate-limit-per-minute:60}") int limit) {
        this.limit = limit;
    }

    public void check(String key) {
        Instant now = Instant.now();
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || Duration.between(current.startedAt(), now).toMinutes() >= 1) {
                return new Window(now, new AtomicInteger(1));
            }
            current.requests().incrementAndGet();
            return current;
        });
        if (window.requests().get() > limit) {
            throw new ReportRateLimitExceededException();
        }
    }

    private record Window(Instant startedAt, AtomicInteger requests) {
    }
}