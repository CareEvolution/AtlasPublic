package org.ohdsi.webapi.versioning.service;

import org.ohdsi.webapi.versioning.domain.CharacterizationVersion;
import org.ohdsi.webapi.versioning.repository.CharacterizationVersionRepository;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;

@Service
public class CohortCharacterizationVersionService extends VersionService<CharacterizationVersion> {
    CohortCharacterizationVersionService(EntityManager entityManager,
            CharacterizationVersionRepository repository) {
        super(entityManager, repository);
    }

    @Override
    protected String getVersionType() {
        return "CHARACTERIZATION";
    }
}
