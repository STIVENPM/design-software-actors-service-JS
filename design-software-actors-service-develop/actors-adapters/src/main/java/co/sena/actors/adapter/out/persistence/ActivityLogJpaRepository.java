package co.sena.actors.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ActivityLogJpaRepository extends JpaRepository<ActivityLogJpaEntity, UUID> {

    @Query("SELECT a FROM ActivityLogJpaEntity a WHERE a.instructorId = :instructorId " +
           "ORDER BY a.recordedAt DESC")
    List<ActivityLogJpaEntity> findByInstructorIdFirstPage(
            @Param("instructorId") UUID instructorId,
            Pageable pageable);

    @Query("SELECT a FROM ActivityLogJpaEntity a WHERE a.instructorId = :instructorId " +
           "AND a.recordedAt < :cursor " +
           "ORDER BY a.recordedAt DESC")
    List<ActivityLogJpaEntity> findByInstructorIdWithCursor(
            @Param("instructorId") UUID instructorId,
            @Param("cursor") OffsetDateTime cursor,
            Pageable pageable);

    @Query("SELECT a FROM ActivityLogJpaEntity a WHERE a.learnerId = :learnerId " +
           "ORDER BY a.recordedAt DESC")
    List<ActivityLogJpaEntity> findByLearnerIdFirstPage(
            @Param("learnerId") UUID learnerId,
            Pageable pageable);

    @Query("SELECT a FROM ActivityLogJpaEntity a WHERE a.learnerId = :learnerId " +
           "AND a.recordedAt < :cursor " +
           "ORDER BY a.recordedAt DESC")
    List<ActivityLogJpaEntity> findByLearnerIdWithCursor(
            @Param("learnerId") UUID learnerId,
            @Param("cursor") OffsetDateTime cursor,
            Pageable pageable);
}
