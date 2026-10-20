package co.sena.actors.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LearnerAuditMappingTest {
    @Test
    void mapsMandatoryAuditColumnsFromRealLearnerDdl() throws Exception {
        for (String fieldName : new String[] {"createdBy", "updatedBy"}) {
            var field = LearnerJpaEntity.class.getDeclaredField(fieldName);
            assertThat(field.getType()).isEqualTo(UUID.class);
            Column column = field.getAnnotation(Column.class);
            assertThat(column).isNotNull();
            assertThat(column.name()).isEqualTo(fieldName.equals("createdBy") ? "created_by" : "updated_by");
            assertThat(column.nullable()).isFalse();
        }
    }
}
