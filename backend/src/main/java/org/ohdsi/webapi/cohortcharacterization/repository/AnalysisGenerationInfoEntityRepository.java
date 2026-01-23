package org.ohdsi.webapi.cohortcharacterization.repository;

import org.ohdsi.webapi.common.generation.AnalysisGenerationInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisGenerationInfoEntityRepository
        extends JpaRepository<AnalysisGenerationInfoEntity, Long> {

    Optional<AnalysisGenerationInfoEntity> findById(Long generationId);
}
