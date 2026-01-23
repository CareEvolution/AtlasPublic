package org.ohdsi.webapi.cdmresults;

import org.ohdsi.webapi.achilles.service.AchillesCacheService;
import org.ohdsi.webapi.cdmresults.action.DomainReportActions;
import org.ohdsi.webapi.report.CDMResultsAnalysisRunner;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AchillesCacheTaskletFactory {
    private final AchillesCacheService cacheService;
    private final CDMResultsAnalysisRunner analysisRunner;
    private final ObjectMapper objectMapper;
    private final SourceService sourceService;
    private final DomainReportActions domainReportActions;

    public AchillesCacheTaskletFactory(
        AchillesCacheService cacheService, 
        CDMResultsAnalysisRunner analysisRunner,
        ObjectMapper objectMapper, 
        SourceService sourceService, 
        DomainReportActions domainReportActions) {
        this.cacheService = cacheService;
        this.analysisRunner = analysisRunner;
        this.objectMapper = objectMapper;
        this.sourceService = sourceService;
        this.domainReportActions = domainReportActions;
    }

    public AchillesCacheTasklet create(SourceEntity source) {
        return new AchillesCacheTasklet(source, cacheService, sourceService, domainReportActions, analysisRunner, objectMapper);
    }

}
