package org.ohdsi.webapi.feanalysis.repository;

import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType;
import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@NoRepositoryBean
public interface BaseFeAnalysisEntityRepository<T extends FeAnalysisEntity>
        extends JpaRepository<T, Integer> {
    Set<T> findAllByCohortCharacterizations(CohortCharacterizationEntity cohortCharacterization);

    List<T> findAllByType(StandardFeatureAnalysisType preset);

    Optional<T> findById(Integer id);

    Optional<T> findByName(String name);
}
