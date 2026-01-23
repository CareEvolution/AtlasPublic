package org.ohdsi.webapi.achilles.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ohdsi.webapi.achilles.domain.AchillesCacheEntity;
import org.ohdsi.webapi.achilles.repository.AchillesCacheRepository;
import org.ohdsi.webapi.jpa.WebApiJpaProperties;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AchillesCacheService {
    private static final Logger LOG = LoggerFactory.getLogger(AchillesCacheService.class);

    private final AchillesCacheRepository cacheRepository;
    private final WebApiJpaProperties webApiJpaProperties;
    private final SourceRepository sourceRepository;
    private final ObjectMapper objectMapper;

    public AchillesCacheService(
            AchillesCacheRepository cacheRepository,
            WebApiJpaProperties webApiJpaProperties,
            SourceRepository sourceRepository,
            ObjectMapper objectMapper) {
        this.cacheRepository = cacheRepository;
        this.webApiJpaProperties = webApiJpaProperties;
        this.sourceRepository = sourceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AchillesCacheEntity getCache(SourceEntity source, String cacheName) {
        return cacheRepository.findBySourceAndCacheName(source, cacheName);
    }

    @Transactional(readOnly = true)
    public List<AchillesCacheEntity> findBySourceAndNames(SourceEntity source, List<String> names) {
        return cacheRepository.findBySourceAndNames(source, names);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AchillesCacheEntity createCache(SourceEntity source, String cacheName, Object result)
            throws JsonProcessingException {
        var cacheEntity = getCache(source, cacheName);
        var cache = objectMapper.writeValueAsString(result);
        if (Objects.nonNull(cacheEntity)) {
            cacheEntity.setCache(cache);
        } else {
            cacheEntity = new AchillesCacheEntity();
            cacheEntity.setSource(source);
            cacheEntity.setCacheName(cacheName);
            cacheEntity.setCache(cache);
        }

        return cacheRepository.save(cacheEntity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveDrilldownCacheMap(SourceEntity source, String domain, Map<Integer, ObjectNode> conceptNodes) {
        if (conceptNodes.isEmpty()) { // nothing to cache
            LOG.warn(
                    "Cannot cache drilldown reports for {}, domain {}. Check if result schema contains achilles results tables.",
                    source.getSourceKey(), domain);
            return;
        }

        var batchSize = webApiJpaProperties.hibernate().jdbc().batchSize();
        var nodes = new HashMap<String, ObjectNode>(batchSize);
        for (Map.Entry<Integer, ObjectNode> entry : conceptNodes.entrySet()) {
            if (nodes.size() >= batchSize) {
                createCacheEntities(source, nodes);
                nodes.clear();
            }
            var key = entry.getKey();
            var cacheName = getCacheName(domain, key);
            nodes.put(cacheName, entry.getValue());
        }
        createCacheEntities(source, nodes);
        nodes.clear();
    }

    @Transactional()
    public void clearCache() {
        var sources = sourceRepository.findAll();
        sources.stream().forEach(this::clearCache);
    }

    @Transactional()
    public void clearCache(SourceEntity source) {
        cacheRepository.deleteBySource(source);
    }

    private void createCacheEntities(SourceEntity source, Map<String, ObjectNode> nodes) {
        var cacheEntities = getEntities(source, nodes);
        cacheRepository.saveAll(cacheEntities);
    }

    private List<AchillesCacheEntity> getEntities(SourceEntity source, Map<String, ObjectNode> nodes) {
        var cacheNames = new ArrayList<>(nodes.keySet());
        var cacheEntities = findBySourceAndNames(source, cacheNames);
        nodes.forEach((key, value) -> {
            // check if the entity with given cache name already exists
            Optional<AchillesCacheEntity> cacheEntity = cacheEntities.stream()
                    .filter(entity -> entity.getCacheName().equals(key))
                    .findAny();
            try {
                var newValue = objectMapper.writeValueAsString(value);
                if (cacheEntity.isPresent()) {
                    // if cache entity already exists update its value
                    cacheEntity.get().setCache(newValue);
                } else {
                    // if cache entity does not exist - create new one
                    var newEntity = new AchillesCacheEntity();
                    newEntity.setCacheName(key);
                    newEntity.setSource(source);
                    newEntity.setCache(newValue);
                    cacheEntities.add(newEntity);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        return cacheEntities;
    }

    private String getCacheName(String domain, int conceptId) {
        return String.format("drilldown_%s_%d", domain, conceptId);
    }
}
