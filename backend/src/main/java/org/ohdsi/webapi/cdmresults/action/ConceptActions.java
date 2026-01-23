package org.ohdsi.webapi.cdmresults.action;

import org.ohdsi.webapi.cdmresults.DescendantRecordAndPersonCount;
import org.ohdsi.webapi.cdmresults.converter.CDMCacheEntityToDescendantRecordAndPersonCount;
import org.ohdsi.webapi.cdmresults.domain.CDMCacheEntity;
import org.ohdsi.webapi.cdmresults.service.CDMCacheService;
import org.ohdsi.webapi.source.SourceService;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ConceptActions {
    private final CDMCacheService cdmCacheService;
    private final SourceService sourceService;

    public ConceptActions(
            SourceService sourceService,
            CDMCacheService cdmCacheService) {
        this.sourceService = sourceService;
        this.cdmCacheService = cdmCacheService;
    }

    public List<AbstractMap.SimpleEntry<Integer, List<Long>>> getConceptRecordCount(
            String sourceKey,
            List<Integer> identifiers) {

        var source = sourceService.findBySourceKey(sourceKey);
        if (source != null) {
            List<CDMCacheEntity> entities = cdmCacheService.findAndCache(source, identifiers);
            List<DescendantRecordAndPersonCount> recordCounts = entities.stream()
                    .map(CDMCacheEntityToDescendantRecordAndPersonCount::convert)
                    .collect(Collectors.toList());
            return convertToResponse(recordCounts);
        }
        return Collections.emptyList();
    }

    private List<AbstractMap.SimpleEntry<Integer, List<Long>>> convertToResponse(
            Collection<DescendantRecordAndPersonCount> conceptRecordCounts) {
        return conceptRecordCounts.stream()
                .map(c -> new AbstractMap.SimpleEntry<>(c.getId(), c.getValues()))
                .collect(Collectors.toList());
    }
}
