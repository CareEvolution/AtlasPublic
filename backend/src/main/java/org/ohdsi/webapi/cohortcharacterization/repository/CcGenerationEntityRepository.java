package org.ohdsi.webapi.cohortcharacterization.repository;

import org.ohdsi.webapi.cohortcharacterization.domain.CcGenerationEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CcGenerationEntityRepository extends JpaRepository<CcGenerationEntity, Long> {

    @EntityGraph(attributePaths = {"source"}, type = EntityGraph.EntityGraphType.LOAD)
    List<CcGenerationEntity> findByCohortCharacterizationIdOrderByIdDesc(Long id);

    @EntityGraph(attributePaths = { "source" }, type = EntityGraph.EntityGraphType.LOAD)
    List<CcGenerationEntity> findByCohortCharacterizationIdAndSourceSourceKeyOrderByIdDesc(Long id, String sourceKey);

    List<CcGenerationEntity> findByStatusIn(List<String> statuses);

    Optional<CcGenerationEntity> findById(Long generationId);

    @EntityGraph(attributePaths = { "source" }, type = EntityGraph.EntityGraphType.LOAD)
    @Query("select g from CcGenerationEntity g where g.id = ?1")
    Optional<CcGenerationEntity> findByIdEagerLoad(Long generationId);
}
