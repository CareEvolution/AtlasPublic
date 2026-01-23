package org.ohdsi.webapi.generationcache;

import com.cosium.spring.data.jpa.entity.graph.domain2.EntityGraph;
import com.cosium.spring.data.jpa.entity.graph.repository.EntityGraphJpaRepository;
import org.ohdsi.webapi.source.SourceEntity;

import java.util.Date;
import java.util.List;

public interface GenerationCacheRepository extends EntityGraphJpaRepository<GenerationCache, Integer> {

    GenerationCache findByTypeAndAndDesignHashAndSource(CacheableGenerationType type, Integer designHash, SourceEntity source,
            EntityGraph entityGraph);

    List<GenerationCache> findAllByCreatedDateBefore(Date date, EntityGraph entityGraph);

    List<GenerationCache> findAllBySourceSourceId(int source);
}
