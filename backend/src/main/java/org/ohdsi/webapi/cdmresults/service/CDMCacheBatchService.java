package org.ohdsi.webapi.cdmresults.service;

import org.ohdsi.webapi.cdmresults.domain.CDMCacheEntity;
import org.ohdsi.webapi.cdmresults.repository.CDMCacheRepository;
import org.ohdsi.webapi.source.SourceEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CDMCacheBatchService {
    private final CDMCacheRepository cdmCacheRepository;

    public CDMCacheBatchService(CDMCacheRepository cdmCacheRepository) {
        this.cdmCacheRepository = cdmCacheRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<CDMCacheEntity> save(SourceEntity source, List<CDMCacheEntity> entities) {
        var conceptIds = entities.stream()
                .map(CDMCacheEntity::getConceptId)
                .collect(Collectors.toList());
        var cacheEntities = cdmCacheRepository.findBySourceAndConceptIds(source.getSourceId(),
                        conceptIds)
                .stream()
                .collect(Collectors.toMap(CDMCacheEntity::getConceptId, Function.identity()));
        var modified = new ArrayList<CDMCacheEntity>();
        entities.forEach(entity -> {
            // check if the entity with given cache name already exists
            CDMCacheEntity processedEntity;
            if (cacheEntities.containsKey(entity.getConceptId())) {
                processedEntity = cacheEntities.get(entity.getConceptId());
                if (Arrays.equals(new long[] { entity.getPersonCount(), entity.getDescendantPersonCount(),
                                entity.getRecordCount(), entity.getDescendantRecordCount() },
                        new long[] { processedEntity.getPersonCount(), processedEntity.getDescendantPersonCount(),
                                processedEntity.getRecordCount(), processedEntity.getDescendantRecordCount() })) {
                    return;  // data hasn't changed, so move to next in forEach
                }
            } else {
                // if cache entity does not exist - create new one
                processedEntity = new CDMCacheEntity();
                processedEntity.setConceptId(entity.getConceptId());
                processedEntity.setSourceId(source.getSourceId());
                cacheEntities.put(processedEntity.getConceptId(), processedEntity);
            }
            processedEntity.setPersonCount(entity.getPersonCount());
            processedEntity.setDescendantPersonCount(entity.getDescendantPersonCount());
            processedEntity.setRecordCount(entity.getRecordCount());
            processedEntity.setDescendantRecordCount(entity.getDescendantRecordCount());
            modified.add(processedEntity);
        });
        if (!modified.isEmpty()) {
            cdmCacheRepository.saveAll(modified);
        }
        return new ArrayList<>(cacheEntities.values());
    }
}
