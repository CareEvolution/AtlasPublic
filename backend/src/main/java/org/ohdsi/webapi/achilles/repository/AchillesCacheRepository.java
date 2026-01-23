package org.ohdsi.webapi.achilles.repository;

import org.ohdsi.webapi.achilles.domain.AchillesCacheEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchillesCacheRepository extends CrudRepository<AchillesCacheEntity, Long> {
    AchillesCacheEntity findBySourceAndCacheName(SourceEntity source, String type);

    @Query("select ac from AchillesCacheEntity ac where ac.source = :source and ac.cacheName in :names")
    List<AchillesCacheEntity> findBySourceAndNames(@Param("source") SourceEntity source, @Param("names") List<String> names);

    @Modifying
    @Query("delete from AchillesCacheEntity ac where ac.source = :source")
    void deleteBySource(@Param("source") SourceEntity source);
}
