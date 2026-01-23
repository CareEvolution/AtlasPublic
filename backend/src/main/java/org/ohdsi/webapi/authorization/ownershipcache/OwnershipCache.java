package org.ohdsi.webapi.authorization.ownershipcache;

import org.ohdsi.webapi.authorization.AnonymousUser;
import org.ohdsi.webapi.authorization.model.SecuredEntityType;
import org.ohdsi.webapi.cohortcharacterization.repository.CcRepository;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionRepository;
import org.ohdsi.webapi.cohortsample.CohortSampleRepository;
import org.ohdsi.webapi.conceptset.ConceptSetRepository;
import org.ohdsi.webapi.feanalysis.repository.FeAnalysisEntityRepository;
import org.ohdsi.webapi.source.SourceRepository;
import org.ohdsi.webapi.tag.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// TODO "fe" repos
@Service
public class OwnershipCache {
    // Using a read-write lock to secure access to ownerships.  This lock will
    // allow multiple readers but if a write lock is placed then all readers will be
    // blocked until the write lock is removed.
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final HashMap<SecuredEntityType, HashMap<Long,Long>> ownedBy = new HashMap<>();

    private final FeAnalysisEntityRepository feAnalysisEntityRepository;
    private final CcRepository ccRepository;
    private final CohortDefinitionRepository cohortDefinitionRepository;
    private final CohortSampleRepository cohortSampleRepository;
    private final ConceptSetRepository conceptSetRepository;
    private final SourceRepository sourceRepository;
    private final TagRepository tagRepository;

    public OwnershipCache(
            FeAnalysisEntityRepository feAnalysisEntityRepository,
            CcRepository ccRepository,
            CohortDefinitionRepository cohortDefinitionRepository,
            CohortSampleRepository cohortSampleRepository,
            ConceptSetRepository conceptSetRepository,
            SourceRepository sourceRepository,
            TagRepository tagRepository) {
        this.feAnalysisEntityRepository = feAnalysisEntityRepository;
        this.ccRepository = ccRepository;
        this.cohortDefinitionRepository = cohortDefinitionRepository;
        this.cohortSampleRepository = cohortSampleRepository;
        this.conceptSetRepository = conceptSetRepository;
        this.sourceRepository = sourceRepository;
        this.tagRepository = tagRepository;
    }

    public void addOwnership(SecuredEntityType entityType, Long entityId, Long ownerId) {
        readWriteLock.writeLock().lock();
        try {
            ownedBy.get(entityType).put(entityId, ownerId);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public Long getOwnerId(SecuredEntityType entityType, Long entityId) {
        readWriteLock.readLock().lock();
        try {
            return ownedBy.get(entityType).get(entityId);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Transactional
    public void loadOwnerships() {
        readWriteLock.writeLock().lock();
        try {
            ownedBy.clear();
            ownedBy.put(SecuredEntityType.COHORT_CHARACTERIZATION, loadCohortCharacterizationOwners());
            ownedBy.put(SecuredEntityType.COHORT_DEFINITION, loadCohortDefinitionOwners());
            ownedBy.put(SecuredEntityType.COHORT_SAMPLE, loadCohortSampleOwners());
            ownedBy.put(SecuredEntityType.CONCEPT_SET, loadConceptSetOwners());
            ownedBy.put(SecuredEntityType.FE_ANALYSIS, loadFeAnalysisOwners());
            ownedBy.put(SecuredEntityType.SOURCE, loadSourceOwners());
            ownedBy.put(SecuredEntityType.TAG, loadTagOwners());
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public void removeOwnership(SecuredEntityType entityType, Long entityId) {
        readWriteLock.writeLock().lock();
        try {
            ownedBy.get(entityType).remove(entityId);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    // TODO these loaders have a lot of common code and can be simplified
    @Transactional
    public HashMap<Long,Long> loadCohortCharacterizationOwners() {
        var entities = ccRepository.findAll();
        var entityOwnedBy = new HashMap<Long,Long>();
        for (var entity : entities) {
            var entityId = entity.getId();
            var createdBy = entity.getCreatedBy();
            // Legacy data created by the anonymous user will have a null createdBy
            var ownerId = createdBy != null ? createdBy.getId() : AnonymousUser.ENTITY_ID;
            entityOwnedBy.put(entityId, ownerId);
        }
        return entityOwnedBy;
    }

    @Transactional
    public HashMap<Long,Long> loadCohortDefinitionOwners() {
        var entities = cohortDefinitionRepository.findAll();
        var entityOwnedBy = new HashMap<Long,Long>();
        for (var entity : entities) {
            var entityId = Long.valueOf(entity.getId());
            var createdBy = entity.getCreatedBy();
            // Legacy data created by the anonymous user will have a null createdBy
            var ownerId = createdBy != null ? createdBy.getId() : AnonymousUser.ENTITY_ID;
            entityOwnedBy.put(entityId, ownerId);
        }
        return entityOwnedBy;
    }

    @Transactional
    public HashMap<Long,Long> loadCohortSampleOwners() {
        var entities = cohortSampleRepository.findAll();
        var entityOwnedBy = new HashMap<Long,Long>();
        for (var entity : entities) {
            var entityId = Long.valueOf(entity.getId());
            var createdBy = entity.getCreatedBy();
            // Legacy data created by the anonymous user will have a null createdBy
            var ownerId = createdBy != null ? createdBy.getId() : AnonymousUser.ENTITY_ID;
            entityOwnedBy.put(entityId, ownerId);
        }
        return entityOwnedBy;
    }

    @Transactional
    public HashMap<Long,Long> loadConceptSetOwners() {
        var entities = conceptSetRepository.findAll();
        var entityOwnedBy = new HashMap<Long,Long>();
        for (var entity : entities) {
            var entityId = Long.valueOf(entity.getId());
            var createdBy = entity.getCreatedBy();
            // Legacy data created by the anonymous user will have a null createdBy
            var ownerId = createdBy != null ? createdBy.getId() : AnonymousUser.ENTITY_ID;
            entityOwnedBy.put(entityId, ownerId);
        }
        return entityOwnedBy;
    }

    @Transactional
    public HashMap<Long,Long> loadFeAnalysisOwners() {
        var entities = feAnalysisEntityRepository.findAll();
        var entityOwnedBy = new HashMap<Long,Long>();
        for (var entity : entities) {
            var entityId = Long.valueOf(entity.getId());
            var createdBy = entity.getCreatedBy();
            // Legacy data created by the anonymous user will have a null createdBy
            var ownerId = createdBy != null ? createdBy.getId() : AnonymousUser.ENTITY_ID;
            entityOwnedBy.put(entityId, ownerId);
        }
        return entityOwnedBy;
    }

    @Transactional
    public HashMap<Long,Long> loadSourceOwners() {
        var entities = sourceRepository.findAll();
        var entityOwnedBy = new HashMap<Long,Long>();
        for (var entity : entities) {
            var entityId = Long.valueOf(entity.getId());
            var createdBy = entity.getCreatedBy();
            // Legacy data created by the anonymous user will have a null createdBy
            var ownerId = createdBy != null ? createdBy.getId() : AnonymousUser.ENTITY_ID;
            entityOwnedBy.put(entityId, ownerId);
        }
        return entityOwnedBy;
    }

    @Transactional
    public HashMap<Long,Long> loadTagOwners() {
        var entities = tagRepository.findAll();
        var entityOwnedBy = new HashMap<Long,Long>();
        for (var entity : entities) {
            var entityId = Long.valueOf(entity.getId());
            var createdBy = entity.getCreatedBy();
            // Legacy data created by the anonymous user will have a null createdBy
            var ownerId = createdBy != null ? createdBy.getId() : AnonymousUser.ENTITY_ID;
            entityOwnedBy.put(entityId, ownerId);
        }
        return entityOwnedBy;
    }
}
