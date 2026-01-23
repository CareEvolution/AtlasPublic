package org.ohdsi.webapi.generationcache;

import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.ohdsi.webapi.source.SourceEntity;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import java.util.List;

@Configuration
public class SourceDeleteListenerConfigurer {
    @PersistenceUnit
    private EntityManagerFactory emf;
    private final GenerationCacheService generationCacheService;
    private final GenerationCacheRepository generationCacheRepository;

    public SourceDeleteListenerConfigurer(GenerationCacheService generationCacheService,
            GenerationCacheRepository generationCacheRepository) {
        this.generationCacheService = generationCacheService;
        this.generationCacheRepository = generationCacheRepository;
    }

    @PostConstruct
    protected void init() {
        SessionFactoryImpl sessionFactory = emf.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.PRE_DELETE).appendListener(event -> {
            if (event.getEntity() instanceof SourceEntity source) {
                var executor = new SimpleAsyncTaskExecutor();
                try (executor) {
                    executor.submit(() -> clearSourceRelatedCaches(source)).get();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            return false;
        });
    }

    private void clearSourceRelatedCaches(SourceEntity source) {
        List<GenerationCache> caches = generationCacheRepository.findAllBySourceSourceId(source.getSourceId());
        caches.forEach(gc -> generationCacheService.removeCache(gc.getType(), source, gc.getDesignHash()));
    }
}
