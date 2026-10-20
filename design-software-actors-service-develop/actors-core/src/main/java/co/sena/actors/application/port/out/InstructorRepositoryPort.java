package co.sena.actors.application.port.out;

import co.sena.actors.domain.model.Instructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port. It never performs a physical DELETE (E4: soft delete). */
public interface InstructorRepositoryPort {

    Instructor save(Instructor instructor);

    Optional<Instructor> findActiveById(UUID id);

    Optional<Instructor> findById(UUID id);

    Optional<Instructor> findByUserId(UUID userId);

    long countActive(boolean active, String documentNumber);

    List<Instructor> findActiveSlice(
            boolean active, String documentNumber, String sortProperty, boolean ascending, int offset, int limit);

    /**
     * Same as above but restricted to instructors whose current contract
     * (active + is_current) belongs to the given training center (HU-ACT-002 join).
     */
    long countActiveInCenter(boolean active, String documentNumber, UUID trainingCenterId);

    List<Instructor> findActiveSliceInCenter(
            boolean active, String documentNumber, String sortProperty, boolean ascending, int offset, int limit,
            UUID trainingCenterId);

    /** Current contract training center, if the instructor has one. */
    Optional<UUID> findCurrentTrainingCenterId(UUID instructorId);

    boolean existsByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumberExcludingId(String documentNumber, UUID id);

    boolean existsByUserId(UUID userId);

    boolean existsByUserIdExcludingId(UUID userId, UUID id);
}
