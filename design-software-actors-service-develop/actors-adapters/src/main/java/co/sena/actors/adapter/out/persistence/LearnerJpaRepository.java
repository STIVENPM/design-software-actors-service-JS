package co.sena.actors.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearnerJpaRepository extends JpaRepository<LearnerJpaEntity, UUID>, JpaSpecificationExecutor<LearnerJpaEntity> {

    boolean existsByDocumentNumber(String documentNumber);

    Optional<LearnerJpaEntity> findByUserId(UUID userId);

    @Query(value = """
            SELECT l.*
            FROM actors_learners.learner l
            WHERE l.user_id = :userId
              AND l.is_active = true
              AND l.deleted_at IS NULL
            """, nativeQuery = true)
    Optional<LearnerJpaEntity> findActiveByUserId(
            @Param("userId") UUID userId);
}
