package org.ohdsi.webapi.versioning.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.ohdsi.webapi.versioning.domain.Version;
import org.ohdsi.webapi.versioning.domain.VersionBase;
import org.ohdsi.webapi.versioning.domain.VersionPK;
import org.ohdsi.webapi.versioning.dto.VersionUpdateDTO;
import org.ohdsi.webapi.versioning.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Transactional
public abstract class VersionService<T extends Version> {
    private static final Logger logger = LoggerFactory.getLogger(VersionService.class);

    private final EntityManager entityManager;
    private final VersionRepository<T> repository;

    public VersionService(
            EntityManager entityManager,
            VersionRepository<T> repository
    ) {
        this.entityManager = entityManager;
        this.repository = repository;
    }

    protected abstract String getVersionType();

    public List<VersionBase> getVersions(long assetId) {
        return repository.findAllVersions(assetId);
    }

    public T create(T assetVersion) {

        var latestVersion = repository.getLatestVersion(assetVersion.getAssetId());
        if (Objects.nonNull(latestVersion)) {
            assetVersion.setVersion(latestVersion + 1);
        } else {
            assetVersion.setVersion(1);
        }

        try {
            assetVersion = this.save(assetVersion);
        } catch (PersistenceException e) {
            var errorMsg = formatError("create", assetVersion.getPk());
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        return assetVersion;
    }

    public T update(VersionUpdateDTO updateDTO) {
        var fieldValue = repository.findById(updateDTO.getVersionPk());
        if (fieldValue.isEmpty()) {
            var errorMsg = formatError("update", updateDTO.getVersionPk());
            throw new RuntimeException(errorMsg);
        }

        T currentVersion = fieldValue.get();
        currentVersion.setComment(updateDTO.getComment());
        currentVersion.setArchived(updateDTO.isArchived());
        return save(currentVersion);
    }

    public void delete(long assetId, int version) {
        VersionPK pk = new VersionPK(assetId, version);

        T currentVersion = repository.getReferenceById(pk);
        currentVersion.setArchived(true);
    }

    public T getById(long assetId, int version) {
        VersionPK pk = new VersionPK(assetId, version);
        var fieldValue = repository.findById(pk);
        if (fieldValue.isEmpty()) {
            var errorMsg = formatError("getById", pk);
            throw new RuntimeException(errorMsg);
        }
        return fieldValue.get();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public T save(T version) {
        var retrievedVersion = repository.saveAndFlush(version);
        entityManager.refresh(retrievedVersion);

        var fieldValue = repository.findById(version.getPk());
        if (fieldValue.isEmpty()) {
            var errorMsg = formatError("save", version.getPk());
            throw new RuntimeException(errorMsg);
        }
        return fieldValue.get();
    }

    private String formatError(String crudText, VersionPK pk) {
        return String.format(
                "Cannot %s version of type %s for asset id %d and version %d",
                crudText,
                this.getVersionType(),
                pk.getAssetId(),
                pk.getVersion());
    }
}
