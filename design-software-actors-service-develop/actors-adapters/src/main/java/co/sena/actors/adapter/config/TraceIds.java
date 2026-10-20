package co.sena.actors.adapter.config;

import io.opentelemetry.api.trace.Span;
import java.util.UUID;

/**
 * Resolves the error envelope trace_id (_shared.yaml#/components/schemas/Error).
 * Prefers the active OTel trace; outside a span context it falls back to a
 * same-format id (32 hex chars) so the contract never breaks.
 */
public final class TraceIds {

    private TraceIds() {
    }

    public static String current() {
        try {
            Span span = Span.current();
            if (span != null && span.getSpanContext() != null && span.getSpanContext().isValid()) {
                return span.getSpanContext().getTraceId();
            }
        } catch (Throwable ignored) {
            // Sin tracer en el classpath/contexto: fallback determinista abajo.
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
