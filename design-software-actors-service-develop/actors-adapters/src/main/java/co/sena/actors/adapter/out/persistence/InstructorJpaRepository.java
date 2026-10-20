package co.sena.actors.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstructorJpaRepository extends JpaRepository<InstructorJpaEntity, UUID> {

    Optional<InstructorJpaEntity> findByIdAndActiveTrue(UUID id);

    Optional<InstructorJpaEntity> findByUserId(UUID userId);

    Page<InstructorJpaEntity> findAllByActiveAndDocumentNumberContainingIgnoreCase(
            boolean active, String documentNumber, Pageable pageable);

    Page<InstructorJpaEntity> findAllByActive(boolean active, Pageable pageable);

    long countByActive(boolean active);

    long countByActiveAndDocumentNumberContainingIgnoreCase(boolean active, String documentNumber);

    @Query("SELECT i FROM InstructorJpaEntity i WHERE i.active = :active"
            + " AND EXISTS (SELECT 1 FROM InstructorContractJpaEntity c WHERE c.instructorId = i.id"
            + " AND c.active = true AND c.current = true AND c.trainingCenterId = :center)")
    Page<InstructorJpaEntity> findAllByActiveAndCurrentCenter(
            @Param("active") boolean active,
            @Param("center") UUID trainingCenterId,
            Pageable pageable);

    @Query("SELECT i FROM InstructorJpaEntity i WHERE i.active = :active"
            + " AND LOWER(i.documentNumber) LIKE LOWER(CONCAT('%', :documentNumber, '%'))"
            + " AND EXISTS (SELECT 1 FROM InstructorContractJpaEntity c WHERE c.instructorId = i.id"
            + " AND c.active = true AND c.current = true AND c.trainingCenterId = :center)")
    Page<InstructorJpaEntity> findAllByActiveAndDocumentNumberAndCurrentCenter(
            @Param("active") boolean active,
            @Param("documentNumber") String documentNumber,
            @Param("center") UUID trainingCenterId,
            Pageable pageable);

    @Query("SELECT COUNT(i) FROM InstructorJpaEntity i WHERE i.active = :active"
            + " AND EXISTS (SELECT 1 FROM InstructorContractJpaEntity c WHERE c.instructorId = i.id"
            + " AND c.active = true AND c.current = true AND c.trainingCenterId = :center)")
    long countByActiveAndCurrentCenter(
            @Param("active") boolean active,
            @Param("center") UUID trainingCenterId);

    @Query("SELECT COUNT(i) FROM InstructorJpaEntity i WHERE i.active = :active"
            + " AND LOWER(i.documentNumber) LIKE LOWER(CONCAT('%', :documentNumber, '%'))"
            + " AND EXISTS (SELECT 1 FROM InstructorContractJpaEntity c WHERE c.instructorId = i.id"
            + " AND c.active = true AND c.current = true AND c.trainingCenterId = :center)")
    long countByActiveAndDocumentNumberAndCurrentCenter(
            @Param("active") boolean active,
            @Param("documentNumber") String documentNumber,
            @Param("center") UUID trainingCenterId);

    @Query("SELECT c.trainingCenterId FROM InstructorContractJpaEntity c"
            + " WHERE c.instructorId = :instructorId AND c.active = true AND c.current = true")
    Optional<UUID> findCurrentTrainingCenterId(@Param("instructorId") UUID instructorId);

    boolean existsByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumberAndIdNot(String documentNumber, UUID id);

    boolean existsByUserId(UUID userId);

    boolean existsByUserIdAndIdNot(UUID userId, UUID id);
}
