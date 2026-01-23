package org.ohdsi.webapi.versioning.service;

import org.ohdsi.webapi.versioning.domain.CohortVersion;
import org.ohdsi.webapi.versioning.repository.CohortVersionRepository;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;

@Service
public class CohortDefinitionVersionService extends VersionService<CohortVersion> {
    CohortDefinitionVersionService(EntityManager entityManager,
            CohortVersionRepository repository) {
        super(entityManager, repository);
    }

    @Override
    protected String getVersionType() {
        return "COHORT_DEFINITION";
    }
}
