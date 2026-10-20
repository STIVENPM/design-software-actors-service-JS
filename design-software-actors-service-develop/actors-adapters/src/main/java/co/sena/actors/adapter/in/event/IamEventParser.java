package co.sena.actors.adapter.in.event;

import co.sena.actors.application.port.in.IamActorType;
import co.sena.actors.application.port.in.IamUserCreated;
import co.sena.actors.application.port.in.IamUserDeactivated;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Parses and validates IAM event envelopes against the official JSON Schemas. */
public final class IamEventParser {
    private final ObjectMapper objectMapper;
    private final JsonSchema userCreatedSchema;
    private final JsonSchema userDeactivatedSchema;

    public IamEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.userCreatedSchema = loadSchema(factory, "contracts/iam.user.created.schema.json");
        this.userDeactivatedSchema = loadSchema(factory, "contracts/iam.user.deactivated.schema.json");
    }

    public Object parse(byte[] body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("IAM envelope must be an object");
            }
            UUID eventId = UUID.fromString(requiredText(root, "event_id"));
            String type = requiredText(root, "event_type");
            validateAgainstSchema(type, root);
            requiredText(root, "version");
            Instant.parse(requiredText(root, "timestamp"));
            requiredText(root, "source_service");
            JsonNode payload = root.path("payload");
            if (!payload.isObject()) {
                throw new IllegalArgumentException("IAM payload must be an object");
            }
            UUID userId = UUID.fromString(requiredText(payload, "user_id"));
            return switch (type) {
                case "iam.user.created" -> new IamUserCreated(eventId, userId,
                        IamActorType.valueOf(requiredText(payload, "actor_type")),
                        optionalText(payload, "email"), optionalText(payload, "first_name"),
                        optionalText(payload, "last_name"), optionalText(payload, "document_type"),
                        optionalText(payload, "document_number"),
                        optionalDecimal(payload, "default_max_hours_per_week"), roles(payload));
                case "iam.user.deactivated" -> new IamUserDeactivated(eventId, userId);
                default -> throw new IllegalArgumentException("Unsupported IAM event type: " + type);
            };
        } catch (IOException | IllegalArgumentException exception) {
            throw new InvalidIamEventException("Invalid IAM event", exception);
        }
    }

    private void validateAgainstSchema(String type, JsonNode root) {
        JsonSchema schema = switch (type) {
            case "iam.user.created" -> userCreatedSchema;
            case "iam.user.deactivated" -> userDeactivatedSchema;
            default -> throw new IllegalArgumentException("Unsupported IAM event type: " + type);
        };
        Set<ValidationMessage> errors = schema.validate(root);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("IAM event does not match JSON Schema: " + errors);
        }
    }

    private static JsonSchema loadSchema(JsonSchemaFactory factory, String resource) {
        ClassLoader loader = IamEventParser.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing IAM event schema: " + resource);
            }
            return factory.getSchema(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load IAM event schema: " + resource, exception);
        }
    }

    private static String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing IAM field: " + field);
        }
        return value;
    }

    private static String optionalText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new IllegalArgumentException("IAM field must be text: " + field);
        }
        return value.asText();
    }

    private static List<String> roles(JsonNode payload) {
        JsonNode roles = payload.path("roles");
        if (roles.isMissingNode() || roles.isNull()) {
            return List.of();
        }
        if (!roles.isArray()) {
            throw new IllegalArgumentException("IAM roles must be an array");
        }
        List<String> result = new ArrayList<>();
        roles.forEach(role -> {
            if (!role.isObject()) {
                throw new IllegalArgumentException("IAM role must be an object");
            }
            String name = requiredText(role, "role_name");
            String centerId = optionalText(role, "training_center_id");
            if (centerId != null) {
                UUID.fromString(centerId);
            }
            // IAM owns role assignments. Keep the names only in the transient event value;
            // the use case never writes them to any Actors table.
            result.add(name);
        });
        return result;
    }

    private static BigDecimal optionalDecimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isNumber() && !value.isTextual()) {
            throw new IllegalArgumentException("IAM field must be decimal: " + field);
        }
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("IAM field must be decimal: " + field, exception);
        }
    }
}
