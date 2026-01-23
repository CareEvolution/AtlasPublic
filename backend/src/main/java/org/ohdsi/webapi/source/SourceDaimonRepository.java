package org.ohdsi.webapi.source;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SourceDaimonRepository extends CrudRepository<SourceDaimonEntity, Integer> {

    @Query("select sd from SourceDaimon sd join sd.source s where s.deletedDate is null and sd.daimonType = :daimonType and sd.priority >= 0")
    List<SourceDaimonEntity> findByDaimonType(@Param("daimonType") SourceDaimonEntity.DaimonType daimonType);

    List<SourceDaimonEntity> findBySource(SourceEntity source);
}
