package org.ohdsi.webapi.cohortresults;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface VisualizationDataRepository extends CrudRepository<VisualizationDataEntity, Integer> {

    List<VisualizationDataEntity> findByCohortDefinitionIdAndSourceId(
            int cohortDefinitionId,
            int sourceId
    );

    VisualizationDataEntity findByCohortDefinitionIdAndSourceIdAndVisualizationKey(
            int cohortDefinitionId,
            int sourceId,
            String visualizationKey
    );

    List<VisualizationDataEntity> findDistinctVisualizationDataByCohortDefinitionIdAndSourceIdAndVisualizationKey(
            int cohortDefinitionId,
            int sourceId,
            String visualizationKey
    );

    VisualizationDataEntity findByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(
            int cohortDefinitionId,
            int sourceId,
            String visualizationKey,
            int drilldownId
    );

    @Transactional
    Long deleteByCohortDefinitionIdAndSourceIdAndVisualizationKey(int cohortDefinitionId,
            int sourceId,
            String visualizationKey);

    @Transactional
    Long deleteByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(
            int cohortDefinitionId,
            int sourceId,
            String visualizationKey,
            int drilldownId
    );
}
