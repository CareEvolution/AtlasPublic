package org.ohdsi.webapi.cohortcharacterization.repository;

import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// TODO fix raw use of parameterized class FeAnalysisEntity
public interface CcRepository extends JpaRepository<CohortCharacterizationEntity, Long> {
    Optional<CohortCharacterizationEntity> findById(final Long id);

    @EntityGraph(attributePaths = {"cohortDefinitions", "featureAnalyses", "stratas", "parameters", "createdBy", "modifiedBy"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("select cc from CohortCharacterizationEntity cc where cc.id = ?1")
    Optional<CohortCharacterizationEntity> findByIdEagerLoad(final Long id);

    @Query("SELECT cc FROM CohortCharacterizationEntity cc WHERE cc.name LIKE ?1 ESCAPE '\\'")
    List<CohortCharacterizationEntity> findAllByNameStartsWith(String pattern);

    Optional<CohortCharacterizationEntity> findByName(String name);

    @Query("SELECT COUNT(cc) FROM CohortCharacterizationEntity cc WHERE cc.name = :ccName and cc.id <> :ccId")
    int getCountCcWithSameName(@Param("ccId") Long ccId, @Param("ccName") String ccName);

    @Query("SELECT cc FROM CohortCharacterizationEntity cc JOIN cc.cohortDefinitions cd WHERE cd = ?1")
    List<CohortCharacterizationEntity> findByCohortDefinition(CohortDefinitionEntity cd);

    @Query("SELECT cc FROM CohortCharacterizationEntity cc JOIN cc.featureAnalyses fa WHERE fa = ?1")
    List<CohortCharacterizationEntity> findByFeatureAnalysis(FeAnalysisEntity feAnalysis);

    @Query("SELECT DISTINCT cc FROM CohortCharacterizationEntity cc JOIN FETCH cc.tags t WHERE lower(t.name) in :tagNames")
    List<CohortCharacterizationEntity> findByTags(@Param("tagNames") List<String> tagNames);

    @EntityGraph(attributePaths = { "cohortDefinitions", "featureAnalyses", "stratas", "parameters", "createdBy",
            "modifiedBy" }, type = EntityGraph.EntityGraphType.LOAD)
    @Query("select cc from CohortCharacterizationEntity cc")
    Page<CohortCharacterizationEntity> findAllEagerLoad(Pageable pageable);
}
