package co.sena.actors.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearnerFichaEnrollmentJpaRepository extends JpaRepository<LearnerFichaEnrollmentJpaEntity, UUID> {

    @Query("""
            select count(e) > 0
            from LearnerFichaEnrollmentJpaEntity e
            where e.learner.id = :learnerId and e.fichaId = :fichaId
            """)
    boolean existsByLearnerIdAndFichaId(@Param("learnerId") UUID learnerId, @Param("fichaId") UUID fichaId);

    @Query("""
            select e
            from LearnerFichaEnrollmentJpaEntity e
            where e.learner.id = :learnerId and e.isCurrent = true
            """)
    Optional<LearnerFichaEnrollmentJpaEntity> findCurrentByLearnerId(@Param("learnerId") UUID learnerId);

    @Query("""
            select e
            from LearnerFichaEnrollmentJpaEntity e
            where e.learner.id = :learnerId
            order by e.enrollmentDate desc, e.id
            """)
    List<LearnerFichaEnrollmentJpaEntity> findHistoryByLearnerId(@Param("learnerId") UUID learnerId);
}
