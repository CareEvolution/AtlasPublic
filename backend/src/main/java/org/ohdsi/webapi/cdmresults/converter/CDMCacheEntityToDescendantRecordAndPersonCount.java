package org.ohdsi.webapi.cdmresults.converter;

import org.ohdsi.webapi.cdmresults.DescendantRecordAndPersonCount;
import org.ohdsi.webapi.cdmresults.domain.CDMCacheEntity;

public class CDMCacheEntityToDescendantRecordAndPersonCount {
    public static DescendantRecordAndPersonCount convert(CDMCacheEntity source) {
        var target = new DescendantRecordAndPersonCount();
        target.setId(source.getConceptId());
        target.setRecordCount(source.getRecordCount());
        target.setDescendantRecordCount(source.getDescendantRecordCount());
        target.setPersonCount(source.getPersonCount());
        target.setDescendantPersonCount(source.getDescendantPersonCount());
        return target;
    }

}
