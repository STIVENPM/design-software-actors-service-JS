import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;

/** Local stack verification: two AMQP deliveries must cause one inbox and one business effect. */
public class R1InboxDedup {
    public static void main(String[] args) throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String suffix = userId.toString().substring(0, 8);
        String json = """
                {"event_id":"%s","event_type":"iam.user.created","version":"1.0",
                "timestamp":"%s","source_service":"iam-service",
                "payload":{"user_id":"%s","email":"r1-%s@example.test",
                "first_name":"R1","last_name":"Inbox","actor_type":"INSTRUCTOR",
                "document_type":"CC","document_number":"%s",
                "default_max_hours_per_week":40,"roles":[]}}
                """.formatted(eventId, Instant.now(), userId, suffix,
                        Long.toString(Math.abs(userId.getLeastSignificantBits())).substring(0, 10));
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitmq");
        factory.setUsername(System.getenv("RABBITMQ_USER"));
        factory.setPassword(System.getenv("RABBITMQ_PASSWORD"));
        try (var connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            boolean dlq = args.length > 0 && args[0].equals("dlq");
            for (int n = 0; n < (dlq ? 1 : 2); n++) {
                channel.basicPublish(dlq ? "actors-iam-dlx" : "iam-events",
                        dlq ? "actors-events.dlq" : "iam.user.created", null,
                        json.getBytes(StandardCharsets.UTF_8));
            }
        }
        if (args.length > 0 && args[0].equals("dlq")) {
            System.out.println("DLQ_SEEDED event=" + eventId + " user=" + userId);
            return;
        }
        String url = "jdbc:postgresql://postgres:5432/" + System.getenv("POSTGRES_DB");
        String sql = """
                SELECT
                  (SELECT count(*) FROM actors_tracking.iam_processed_event WHERE event_id = ?),
                  (SELECT count(*) FROM actors_instructors.instructor WHERE user_id = ?),
                  (SELECT count(*) FROM actors_tracking.activity_log WHERE new_value = ?)
                """;
        long until = System.currentTimeMillis() + 60_000;
        int inbox = 0, instructor = 0, activity = 0;
        while (System.currentTimeMillis() < until) {
            try (var db = DriverManager.getConnection(url, System.getenv("POSTGRES_USER"),
                    System.getenv("POSTGRES_PASSWORD")); var statement = db.prepareStatement(sql)) {
                statement.setObject(1, eventId);
                statement.setObject(2, userId);
                statement.setString(3, userId.toString());
                try (var rs = statement.executeQuery()) {
                    rs.next();
                    inbox = rs.getInt(1);
                    instructor = rs.getInt(2);
                    activity = rs.getInt(3);
                }
            }
            if (inbox == 1 && instructor == 1 && activity == 1) break;
            Thread.sleep(1000);
        }
        if (inbox != 1 || instructor != 1 || activity != 1) {
            throw new AssertionError("Dedup failed: inbox=" + inbox + " instructor=" +
                    instructor + " activity=" + activity);
        }
        System.out.println("DEDUP_OK event=" + eventId + " deliveries=2 inbox=1 instructor=1 activity=1");
    }
}
