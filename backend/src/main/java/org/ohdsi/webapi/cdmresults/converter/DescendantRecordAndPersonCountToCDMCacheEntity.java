package org.ohdsi.webapi.cdmresults.converter;

import org.ohdsi.webapi.cdmresults.DescendantRecordAndPersonCount;
import org.ohdsi.webapi.cdmresults.domain.CDMCacheEntity;

public class DescendantRecordAndPersonCountToCDMCacheEntity {
    public static CDMCacheEntity convert(DescendantRecordAndPersonCount source) {
        var target = new CDMCacheEntity();
        target.setConceptId(source.getId());
        target.setRecordCount(source.getRecordCount());
        target.setDescendantRecordCount(source.getDescendantRecordCount());
        target.setPersonCount(source.getPersonCount());
        target.setDescendantPersonCount(source.getDescendantPersonCount());
        return target;
    }
}
