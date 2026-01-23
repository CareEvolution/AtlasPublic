package org.ohdsi.webapi.generationcache;

import org.ohdsi.webapi.source.SourceEntity;

public interface GenerationCacheProvider {
    boolean supports(CacheableGenerationType type);
    Integer getDesignHash(String design);
    String getResultsChecksum(SourceEntity source, Integer designHash);
    String getResultsSql(Integer designHash);
    void remove(SourceEntity source, Integer designHash);
}
