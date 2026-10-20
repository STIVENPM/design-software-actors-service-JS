package co.sena.actors.adapter.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReportRateLimiterTest {

    @Test
    void rejectsRequestAfterConfiguredLimit() {
        ReportRateLimiter limiter = new ReportRateLimiter(2);

        assertDoesNotThrow(() -> {
            limiter.check("user-1");
            limiter.check("user-1");
        });

        assertThrows(ReportRateLimitExceededException.class, () -> limiter.check("user-1"));
    }

    @Test
    void keepsLimitsIsolatedPerAuthenticatedUser() {
        ReportRateLimiter limiter = new ReportRateLimiter(1);

        assertDoesNotThrow(() -> limiter.check("user-1"));
        assertThrows(ReportRateLimitExceededException.class, () -> limiter.check("user-1"));
        assertDoesNotThrow(() -> limiter.check("user-2"));
    }
}
