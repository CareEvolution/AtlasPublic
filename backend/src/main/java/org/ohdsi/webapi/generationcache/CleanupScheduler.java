package org.ohdsi.webapi.generationcache;

import com.cosium.spring.data.jpa.entity.graph.domain2.DynamicEntityGraph;
import org.apache.commons.lang3.time.DateUtils;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class CleanupScheduler {
    private final GenerationCacheService generationCacheService;
    private final GenerationCacheRepository generationCacheRepository;
    private final WebApiProperties webApiProperties;

    public CleanupScheduler(
            GenerationCacheService generationCacheService,
            GenerationCacheRepository generationCacheRepository,
            WebApiProperties webApiProperties) {
        this.generationCacheService = generationCacheService;
        this.generationCacheRepository = generationCacheRepository;
        this.webApiProperties = webApiProperties;
    }

    // TODO I *think* this dynamic ref should work in place of using the string
    //      webapi.generation-cache.cleanup-interval-ms - but need to test
    @Scheduled(fixedDelayString = "#${@webApiProperties.generationCache().cleanupIntervalMs().toString()}")
    public void removeOldCache() {
        var caches = generationCacheRepository.findAllByCreatedDateBefore(
                DateUtils.addDays(new Date(), -1 * webApiProperties.generationCache().invalidateAfterDays()),
                DynamicEntityGraph.loading().addPath("source").addPath("source.daimons").build()
        );
        caches.forEach(gc -> generationCacheService.removeCache(gc.getType(), gc.getSource(), gc.getDesignHash()));
    }
}
