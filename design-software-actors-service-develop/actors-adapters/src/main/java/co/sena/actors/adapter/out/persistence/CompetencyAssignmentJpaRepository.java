package co.sena.actors.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for competency_assignment.
 */
public interface CompetencyAssignmentJpaRepository extends JpaRepository<CompetencyAssignmentJpaEntity, UUID> {

    Optional<CompetencyAssignmentJpaEntity> findByIdAndActiveTrue(UUID id);

    List<CompetencyAssignmentJpaEntity> findByInstructorIdAndActiveTrue(UUID instructorId);

    boolean existsByInstructorIdAndCompetencyIdAndActiveTrue(UUID instructorId, UUID competencyId);
}
