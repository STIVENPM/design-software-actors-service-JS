package co.sena.actors.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstructorAvailabilityExceptionJpaRepository
        extends JpaRepository<InstructorAvailabilityExceptionJpaEntity, UUID> {

    @Query("""
        SELECT e
        FROM InstructorAvailabilityExceptionJpaEntity e
        WHERE e.instructorId = :instructorId
          AND e.active = true
          AND e.startDatetime < :to
          AND e.endDatetime >= :from
        """)
    List<InstructorAvailabilityExceptionJpaEntity> findOverlapping(
            @Param("instructorId") UUID instructorId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT e
        FROM InstructorAvailabilityExceptionJpaEntity e
        WHERE e.instructorId = :instructorId
          AND e.active = true
        ORDER BY e.startDatetime ASC
        """)
    List<InstructorAvailabilityExceptionJpaEntity> findByInstructorIdAndActiveTrueOrderByStartDatetimeAsc(
            @Param("instructorId") UUID instructorId
    );

    boolean existsByInstructorIdAndActiveTrue(UUID instructorId);

    @Query("""
        SELECT e
        FROM InstructorAvailabilityExceptionJpaEntity e
        WHERE e.id = :id
          AND e.instructorId = :instructorId
          AND e.active = true
        """)
    Optional<InstructorAvailabilityExceptionJpaEntity> findActiveByIdAndInstructorId(
            @Param("id") UUID id,
            @Param("instructorId") UUID instructorId
    );
}
