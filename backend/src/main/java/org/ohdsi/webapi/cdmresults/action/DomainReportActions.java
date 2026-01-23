package org.ohdsi.webapi.cdmresults.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.ohdsi.webapi.report.*;
import org.ohdsi.webapi.source.SourceService;
import org.springframework.stereotype.Component;

@Component
public class DomainReportActions {
    private final CDMResultsAnalysisRunner queryRunner;
    private final SourceService sourceService;

    public DomainReportActions(SourceService sourceService, CDMResultsAnalysisRunner queryRunner) {
        this.sourceService = sourceService;
        this.queryRunner = queryRunner;
    }


    public CDMDashboard getRawDashboard(final String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        return queryRunner.getDashboard(sourceService.getSourceJdbcTemplate(source), source);
    }

    public CDMDataDensity getRawDataDensity(String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        return this.queryRunner.getDataDensityResults(sourceService.getSourceJdbcTemplate(source), source);
    }

    public CDMDeath getRawDeath(String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        return this.queryRunner.getDeathResults(sourceService.getSourceJdbcTemplate(source), source);
    }

    public JsonNode getRawDrilldown(String domain, int conceptId, String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        return queryRunner.getDrilldown(jdbcTemplate, domain, conceptId, source);
    }

    public CDMObservationPeriod getRawObservationPeriod(String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        return this.queryRunner.getObservationPeriodResults(sourceService.getSourceJdbcTemplate(source), source);
    }

    public CDMPersonSummary getRawPerson(String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        return this.queryRunner.getPersonResults(sourceService.getSourceJdbcTemplate(source), source);
    }

    public ArrayNode getRawTreeMap(String domain, String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        return queryRunner.getTreemap(sourceService.getSourceJdbcTemplate(source), domain, source);
    }
}
