package org.ohdsi.webapi.cohort;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CohortRepository extends CrudRepository<CohortEntity, CohortEntity> {

    @Query("SELECT c FROM CohortEntity c WHERE c.cohortDefinitionId = ?1")
    public List<CohortEntity> getAllCohortsForId(Long id);
}
