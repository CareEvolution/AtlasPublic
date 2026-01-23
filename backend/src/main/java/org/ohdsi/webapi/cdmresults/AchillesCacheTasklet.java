package org.ohdsi.webapi.cdmresults;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.ohdsi.webapi.achilles.service.AchillesCacheService;
import org.ohdsi.webapi.cdmresults.action.DomainReportActions;
import org.ohdsi.webapi.report.CDMResultsAnalysisRunner;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AchillesCacheTasklet implements Tasklet {
    public static final Map<String, BiFunction<DomainReportActions, String, Object>> simpleDomains = new HashMap<>();

    private final static String DEFAULT_CONCEPT_ID_NAME = "conceptId";
    // domain with list of scripts for getting domain data and name of the
    // concept_id column for each report
    // concept_id column name must be in camel case
    private static final Map<String, List<Pair<String, String>>> treemapDomains = new HashMap<>();

    static {
        // domains with concrete call endpoints
        simpleDomains.put(DomainNames.DASHBOARD, DomainReportActions::getRawDashboard);
        simpleDomains.put(DomainNames.PERSON, DomainReportActions::getRawPerson);
        simpleDomains.put(DomainNames.DATA_DENSITY, DomainReportActions::getRawDataDensity);
        simpleDomains.put(DomainNames.DEATH, DomainReportActions::getRawDeath);
        simpleDomains.put(DomainNames.OBSERVATION_PERIOD, DomainReportActions::getRawObservationPeriod);

        // domains with common call endpoint
        // each entry contains domain and mapping of script and concept id column name
        // in script
        treemapDomains.put("condition", new ArrayList<Pair<String, String>>() {
            {
                addAll(getDefaultColumnNames("ageAtFirstOccurrence",
                        "prevalenceByGenderAgeYear", "prevalenceByMonth"));
                addAll(getColumnNames("conditionConceptId", "byType"));
            }
        });
        treemapDomains.put("conditionera", new ArrayList<Pair<String, String>>() {
            {
                addAll(getDefaultColumnNames("ageAtFirstOccurrence", "lengthOfEra",
                        "prevalenceByGenderAgeYear", "prevalenceByMonth"));
            }
        });
        treemapDomains.put("drug", new ArrayList<Pair<String, String>>() {
            {
                addAll(getDefaultColumnNames("ageAtFirstOccurrence", "frequencyDistribution",
                        "prevalenceByGenderAgeYear", "prevalenceByMonth"));
                addAll(getColumnNames("drugConceptId", "byType", "daysSupplyDistribution",
                        "quantityDistribution", "refillsDistribution"));
            }
        });
        treemapDomains.put("drugera", new ArrayList<Pair<String, String>>() {
            {
                addAll(getDefaultColumnNames("ageAtFirstOccurrence", "lengthOfEra",
                        "prevalenceByGenderAgeYear", "prevalenceByMonth"));
            }
        });
        treemapDomains.put("measurement", new ArrayList<Pair<String, String>>() {
            {
                addAll(getDefaultColumnNames("ageAtFirstOccurrence", "frequencyDistribution",
                        "lowerLimitDistribution", "measurementValueDistribution", "prevalenceByGenderAgeYear",
                        "prevalenceByMonth", "upperLimitDistribution"));
                addAll(getColumnNames("observationConceptId", "byOperator", "byValueAsConcept"));
                addAll(getColumnNames("measurementConceptId", "byType", "recordsByUnit",
                        "valuesRelativeToNorm"));
            }
        });
        treemapDomains.put("observation", new ArrayList<Pair<String, String>>() {
            {
                addAll(getDefaultColumnNames("ageAtFirstOccurrence", "frequencyDistribution",
                        "prevalenceByGenderAgeYear", "prevalenceByMonth"));
                addAll(getColumnNames("observationConceptId", "byQualifier", "byType",
                        "byValueAsConcept"));
            }
        });
        treemapDomains.put("procedure", new ArrayList<Pair<String, String>>() {
            {
                addAll(getDefaultColumnNames("ageAtFirstOccurrence", "frequencyDistribution",
                        "prevalenceByGenderAgeYear", "prevalenceByMonth"));
                addAll(getColumnNames("procedureConceptId", "byType"));
            }
        });
        treemapDomains.put("visit", new ArrayList<Pair<String, String>>() {
            {
                addAll(getDefaultColumnNames("ageAtFirstOccurrence", "prevalenceByGenderAgeYear",
                        "prevalenceByMonth", "visitDurationByType"));
            }
        });
    }

    private final SourceEntity source;
    private final AchillesCacheService cacheService;
    private final CDMResultsAnalysisRunner analysisRunner;
    private final ObjectMapper objectMapper;
    private final SourceService sourceService;
    private final DomainReportActions domainReportActions;

    public AchillesCacheTasklet(
            SourceEntity source,
            AchillesCacheService cacheService,
            SourceService sourceService,
            DomainReportActions domainReportActions,
            CDMResultsAnalysisRunner analysisRunner,
            ObjectMapper objectMapper) {
        this.source = source;
        this.cacheService = cacheService;
        this.domainReportActions = domainReportActions;
        this.sourceService = sourceService;
        this.analysisRunner = analysisRunner;
        this.objectMapper = objectMapper;
    }

    // prepare list of reports where concept id column name is equal to default one
    private static List<Pair<String, String>> getDefaultColumnNames(String... reports) {
        return getColumnNames(DEFAULT_CONCEPT_ID_NAME, reports);
    }

    // prepare list of reports where concept id column name is custom
    private static List<Pair<String, String>> getColumnNames(String columnName, String... reports) {
        return Arrays.stream(reports)
                .map(report -> new ImmutablePair<>(report, columnName))
                .collect(Collectors.toList());
    }

    private void cacheDrilldown(String domain) {
        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);

        // get drilldown reports for all concepts
        var reports = analysisRunner.getDrilldown(jdbcTemplate, domain, null, source);

        // get reports for each concept
        var conceptNodes = splitReportByConcepts(domain, reports);
        cacheService.saveDrilldownCacheMap(source, domain, conceptNodes);
    }

    private List<Integer> getConceptIds(String domain) {
        var treeMap = domainReportActions.getRawTreeMap(domain, source.getSourceKey());
        var nodes = IntStream.range(0, treeMap.size()).mapToObj(treeMap::get);
        return nodes.map(node -> node.get("conceptId").intValue())
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<Integer, ObjectNode> splitReportByConcepts(String domain, JsonNode reports) {
        var drilldownScripts = treemapDomains.get(domain);

        if (reports.size() != drilldownScripts.size()) {
            throw new RuntimeException(
                    "Drilldown reports size must be equal to number of scripts. Check achilles caching!");
        }

        var conceptNodes = new HashMap<Integer, ObjectNode>();
        drilldownScripts.forEach(columnName -> copyItemToConceptReport(domain, reports, conceptNodes, columnName));
        return conceptNodes;
    }

    private void copyItemToConceptReport(String domain, JsonNode reports,
            Map<Integer, ObjectNode> conceptNodes, Pair<String, String> columnName) {
        var reportName = columnName.getKey();
        var conceptColumnName = columnName.getValue();

        var report = reports.get(reportName);
        var conceptIds = getConceptIds(domain);

        Objects.requireNonNull(report).forEach(item -> {
            int conceptId = item.get(conceptColumnName).intValue();

            // ignore data for concept which is absent in treemap
            if (conceptIds.contains(conceptId)) {
                var reportNode = getReport(conceptNodes, reportName, conceptId);
                reportNode.add(item);
            }
        });
    }

    private ArrayNode getReport(Map<Integer, ObjectNode> conceptNodes, String reportName, int conceptId) {
        // get node for given concept or create new one
        var conceptReport = conceptNodes.computeIfAbsent(conceptId, x -> objectMapper.createObjectNode());
        // get concept report with given name or create new one
        var reportNode = (ArrayNode) conceptReport.get(reportName);
        if (Objects.isNull(reportNode)) {
            reportNode = conceptReport.putArray(reportName);
        }
        return reportNode;
    }

    private void cacheDomain(String domain, BiFunction<DomainReportActions, String, Object> function) {
        var result = function.apply(domainReportActions, source.getSourceKey());
        cache(domain, result);
    }

    private void cacheTreemap(String domain) {
        var result = domainReportActions.getRawTreeMap(domain, source.getSourceKey());
        cache(DomainNames.TREEMAP + "_" + domain, result);
    }

    private void cache(String cacheName, Object result) {
        try {
            cacheService.createCache(source, cacheName, result);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        simpleDomains.forEach(this::cacheDomain);
        treemapDomains.keySet().forEach(this::cacheTreemap);
        treemapDomains.keySet().forEach(this::cacheDrilldown);

        return RepeatStatus.FINISHED;
    }
}
