package co.sena.actors.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.sena.actors.application.port.in.IamActorType;
import co.sena.actors.application.port.in.IamUserCreated;
import co.sena.actors.application.port.in.IamUserDeactivated;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IamEventParserTest {

    private final IamEventParser parser = new IamEventParser(new ObjectMapper());

    @Test
    void parsesCreatedIdentityAndRecognizesButDoesNotMapRolesToProfile() {
        var event = (IamUserCreated) parser.parse(json("""
                {"event_id":"11111111-1111-1111-1111-111111111111",
                 "event_type":"iam.user.created","version":"1",
                 "timestamp":"2026-10-01T12:00:00Z","source_service":"iam-service",
                 "payload":{"user_id":"22222222-2222-2222-2222-222222222222",
                            "actor_type":"INSTRUCTOR","email":"ana@sena.edu.co",
                            "first_name":"Ana","last_name":"Ríos",
                            "document_type":"CC","document_number":"1234567890",
                            "default_max_hours_per_week":40.0,
                            "roles":[{"role_name":"INSTRUCTOR",
                                      "training_center_id":"33333333-3333-3333-3333-333333333333"},
                                     {"role_name":"EVALUATOR","training_center_id":null}]}}
                """));

        assertThat(event.eventId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(event.actorType()).isEqualTo(IamActorType.INSTRUCTOR);
        assertThat(event.firstName()).isEqualTo("Ana");
        assertThat(event.documentType()).isEqualTo("CC");
        assertThat(event.documentNumber()).isEqualTo("1234567890");
        assertThat(event.defaultMaxHoursPerWeek()).isEqualByComparingTo("40.0");
        assertThat(event.roles()).containsExactly("INSTRUCTOR", "EVALUATOR");
    }

    @Test
    void rejectsLegacyStringRolesAndMalformedRoleObjects() {
        String envelope = """
                {"event_id":"11111111-1111-1111-1111-111111111111",
                 "event_type":"iam.user.created","version":"1",
                 "timestamp":"2026-10-01T12:00:00Z","source_service":"iam-service",
                 "payload":{"user_id":"22222222-2222-2222-2222-222222222222",
                            "actor_type":"USER","email":"user@sena.edu.co",
                            "first_name":"User","last_name":"Test","roles":%s}}
                """;
        assertThatThrownBy(() -> parser.parse(json(envelope.formatted("[\"ADMIN\"]"))))
                .isInstanceOf(InvalidIamEventException.class);
        assertThatThrownBy(() -> parser.parse(json(envelope.formatted("[{\"training_center_id\":null}]"))))
                .isInstanceOf(InvalidIamEventException.class);
        assertThatThrownBy(() -> parser.parse(json(envelope.formatted(
                "[{\"role_name\":\"ADMIN\",\"training_center_id\":\"bad\"}]"))))
                .isInstanceOf(InvalidIamEventException.class);
    }

    @Test
    void rejectsCreatedPayloadThatDoesNotMatchJsonSchema() {
        assertThatThrownBy(() -> parser.parse(json("""
                {"event_id":"11111111-1111-1111-1111-111111111111",
                 "event_type":"iam.user.created","version":"1",
                 "timestamp":"2026-10-01T12:00:00Z","source_service":"iam-service",
                 "payload":{"user_id":"22222222-2222-2222-2222-222222222222",
                            "actor_type":"INSTRUCTOR","email":"ana@sena.edu.co"}}
                """))).isInstanceOf(InvalidIamEventException.class);
    }

    @Test
    void userTypeNeedsNoActorNames() {
        var event = (IamUserCreated) parser.parse(json("""
                {"event_id":"11111111-1111-1111-1111-111111111111",
                 "event_type":"iam.user.created","version":"1",
                 "timestamp":"2026-10-01T12:00:00Z","source_service":"iam-service",
                 "payload":{"user_id":"22222222-2222-2222-2222-222222222222",
                            "actor_type":"USER","email":"user@sena.edu.co",
                            "first_name":"User","last_name":"Test","roles":[]}}
                """));
        assertThat(event.actorType()).isEqualTo(IamActorType.USER);
    }

    @Test
    void parsesActualMinimalDeactivatedPayload() {
        var event = (IamUserDeactivated) parser.parse(json("""
                {"event_id":"11111111-1111-1111-1111-111111111111",
                 "event_type":"iam.user.deactivated","version":"1",
                 "timestamp":"2026-10-01T12:00:00Z","source_service":"iam-service",
                 "payload":{"user_id":"22222222-2222-2222-2222-222222222222",
                            "email":"ana@sena.edu.co",
                            "deactivated_by":"33333333-3333-3333-3333-333333333333",
                            "deactivated_at":"2026-10-01T12:00:00Z","reason":"policy"}}
                """));
        assertThat(event.userId()).isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    }

    @Test
    void rejectsDeactivatedPayloadThatDoesNotMatchJsonSchema() {
        assertThatThrownBy(() -> parser.parse(json("""
                {"event_id":"11111111-1111-1111-1111-111111111111",
                 "event_type":"iam.user.deactivated","version":"1",
                 "timestamp":"2026-10-01T12:00:00Z","source_service":"iam-service",
                 "payload":{"email":"ana@sena.edu.co","deactivated_at":"2026-10-01T12:00:00Z"}}
                """))).isInstanceOf(InvalidIamEventException.class);
    }

    @Test
    void rejectsNonUuidEventIdAndUnexpectedEventType() {
        assertThatThrownBy(() -> parser.parse(json("""
                {"event_id":"bad","event_type":"iam.user.created","version":"1",
                 "timestamp":"2026-10-01T12:00:00Z","source_service":"iam-service","payload":{}}
                """))).isInstanceOf(InvalidIamEventException.class);
        assertThatThrownBy(() -> parser.parse(json("""
                {"event_id":"11111111-1111-1111-1111-111111111111",
                 "event_type":"iam.role.assigned","version":"1",
                 "timestamp":"2026-10-01T12:00:00Z","source_service":"iam-service","payload":{}}
                """))).isInstanceOf(InvalidIamEventException.class);
    }

    private byte[] json(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
