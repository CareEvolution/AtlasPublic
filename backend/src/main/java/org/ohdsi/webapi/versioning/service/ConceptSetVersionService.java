package org.ohdsi.webapi.versioning.service;

import org.ohdsi.webapi.versioning.domain.ConceptSetVersion;
import org.ohdsi.webapi.versioning.repository.ConceptSetVersionRepository;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;

@Service
public class ConceptSetVersionService extends VersionService<ConceptSetVersion> {
    ConceptSetVersionService(EntityManager entityManager,
            ConceptSetVersionRepository repository) {
        super(entityManager, repository);
    }

    @Override
    protected String getVersionType() {
        return "CONCEPT_SET";
    }
}
