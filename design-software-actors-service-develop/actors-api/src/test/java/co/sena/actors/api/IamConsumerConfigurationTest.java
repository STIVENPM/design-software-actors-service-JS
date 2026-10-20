package co.sena.actors.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class IamConsumerConfigurationTest {
    private final IamConsumerConfiguration config = new IamConsumerConfiguration();

    @Test
    void refusesToStartWithoutExternalInboxMigration() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(String.class))).thenReturn(null);
        assertThatThrownBy(() -> config.iamInboxMigrationVerified(jdbc).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("iam_processed_event");
    }

    @Test
    void refusesUnapprovedQueueNamesOrMissingBrokerCredentials() {
        assertThatThrownBy(() -> config.iamInboundQueue("", "dlx", "dlq"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACTORS_IAM_QUEUE");
        assertThatThrownBy(() -> config.iamInboundQueue("same", "dlx", "same"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.iamBrokerConfigurationVerified("", "", "")
                .afterPropertiesSet()).hasMessageContaining("SPRING_RABBITMQ_HOST");
    }
}
