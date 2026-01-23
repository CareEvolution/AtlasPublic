package org.ohdsi.webapi.generationcache;

import com.cosium.spring.data.jpa.entity.graph.domain2.DynamicEntityGraph;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class GenerationCacheServiceImpl implements GenerationCacheService {
    private static final Logger log = LoggerFactory.getLogger(GenerationCacheServiceImpl.class);
    private static final String NO_PROVIDER_ERROR = "There is no generation cache provider which supports %s";
    private static final String CACHE_CREATED = "Cached results of {} with design hash = {}";
    private static final String CACHE_INVALID = "Actual results checksum doesn't match the original checksum for cache with id={}. Invalidating cache";

    private final List<GenerationCacheProvider> generationCacheProviderList;
    private final GenerationCacheRepository generationCacheRepository;
    private final SourceRepository sourceRepository;

    public GenerationCacheServiceImpl(List<GenerationCacheProvider> generationCacheProviderList,
            GenerationCacheRepository generationCacheRepository, SourceRepository sourceRepository) {
        this.generationCacheProviderList = generationCacheProviderList;
        this.generationCacheRepository = generationCacheRepository;
        this.sourceRepository = sourceRepository;
    }

    @Override
    public Integer getDesignHash(CacheableGenerationType type, String design) {
        return getProvider(type).getDesignHash(design);
    }

    @Override
    public GenerationCache getCacheOrEraseInvalid(CacheableGenerationType type, Integer designHash, Integer sourceId) {
        var source = sourceRepository.findBySourceId(sourceId);
        var generationCache = generationCacheRepository.findByTypeAndAndDesignHashAndSource(type,
                designHash, source, DynamicEntityGraph.loading().addPath("source").build());
        GenerationCacheProvider provider = getProvider(type);
        if (generationCache != null) {
            var checksum = provider.getResultsChecksum(generationCache.getSource(), generationCache.getDesignHash());
            if (Objects.equals(generationCache.getResultChecksum(), checksum)) {
                return generationCache;
            } else {
                removeCache(generationCache.getType(), generationCache.getSource(), generationCache.getDesignHash());
                log.info(CACHE_INVALID, generationCache.getId());
            }
        }
        return null;
    }

    @Override
    public String getResultsSql(GenerationCache cache) {
        return getProvider(cache.getType()).getResultsSql(cache.getDesignHash());
    }

    @Override
    public GenerationCache cacheResults(CacheableGenerationType type, Integer designHash, Integer sourceId) {
        var source = sourceRepository.findBySourceId(sourceId);
        var checksum = getProvider(type).getResultsChecksum(source, designHash);

        var generationCache = new GenerationCache();
        generationCache.setType(type);
        generationCache.setDesignHash(designHash);
        generationCache.setSource(source);
        generationCache.setResultChecksum(checksum);
        generationCache.setCreatedDate(new Date());

        generationCache = generationCacheRepository.saveAndFlush(generationCache);

        log.info(CACHE_CREATED, type, designHash);

        return generationCache;
    }

    @Override
    public void removeCache(CacheableGenerationType type, SourceEntity source, Integer designHash) {
        // Cleanup cached results
        getProvider(type).remove(source, designHash);
        // Cleanup cache record
        GenerationCache generationCache = generationCacheRepository.findByTypeAndAndDesignHashAndSource(type,
                designHash, source,  DynamicEntityGraph.loading().addPath("source").build());
        if (generationCache != null) {
            generationCacheRepository.delete(generationCache);
        }
    }

    private GenerationCacheProvider getProvider(CacheableGenerationType type) {
        return generationCacheProviderList.stream()
                .filter(p -> p.supports(type))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format(NO_PROVIDER_ERROR, type)));
    }
}
