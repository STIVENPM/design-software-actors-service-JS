package co.sena.actors.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for instructor_contract.
 */
public interface InstructorContractJpaRepository extends JpaRepository<InstructorContractJpaEntity, UUID> {

    Optional<InstructorContractJpaEntity> findByIdAndActiveTrue(UUID id);

    List<InstructorContractJpaEntity> findByInstructorIdAndActiveTrue(UUID instructorId);

    Optional<InstructorContractJpaEntity> findByInstructorIdAndActiveTrueAndCurrentTrue(UUID instructorId);
}
