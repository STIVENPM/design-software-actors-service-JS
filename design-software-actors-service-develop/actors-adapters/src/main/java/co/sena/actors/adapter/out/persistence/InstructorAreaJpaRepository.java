package co.sena.actors.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for instructor_area.
 */
public interface InstructorAreaJpaRepository extends JpaRepository<InstructorAreaJpaEntity, UUID> {

    Optional<InstructorAreaJpaEntity> findByIdAndActiveTrue(UUID id);

    List<InstructorAreaJpaEntity> findByInstructorIdAndActiveTrue(UUID instructorId);

    boolean existsByInstructorIdAndAreaName(UUID instructorId, String areaName);

    boolean existsByInstructorIdAndAreaNameAndIdNot(UUID instructorId, String areaName, UUID id);
}
