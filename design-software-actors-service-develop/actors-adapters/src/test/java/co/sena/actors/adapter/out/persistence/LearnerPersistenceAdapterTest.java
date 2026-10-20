package co.sena.actors.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.sena.actors.adapter.config.CurrentUserProvider;
import co.sena.actors.domain.model.DocumentType;
import co.sena.actors.domain.model.Learner;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LearnerPersistenceAdapterTest {
    @Test
    void updatingExistingLearnerKeepsManagedEntityAndItsVersion() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID creator = UUID.randomUUID();
        var entity = new LearnerJpaEntity(id, userId, DocumentType.CC, "123456",
                "Old", "Name", "old@sena.edu.co", null, null, true,
                LocalDateTime.now(), creator, creator, LocalDateTime.now());
        var repository = mock(LearnerJpaRepository.class);
        var currentUser = mock(CurrentUserProvider.class);
        when(currentUser.currentUserId()).thenReturn(UUID.randomUUID());
        when(repository.findById(id)).thenReturn(java.util.Optional.of(entity));
        when(repository.save(same(entity))).thenReturn(entity);
        var adapter = new LearnerPersistenceAdapter(repository, currentUser);
        var updated = new Learner(id, userId, DocumentType.CC, "123456", "New", "Name",
                "new@sena.edu.co", null, null, true, entity.getCreatedAt());

        Learner result = adapter.save(updated);

        verify(repository).save(same(entity));
        assertThat(result.firstName()).isEqualTo("New");
        assertThat(entity.getCreatedBy()).isEqualTo(creator);
    }
}
