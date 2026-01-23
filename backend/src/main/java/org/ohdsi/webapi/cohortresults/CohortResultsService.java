package org.ohdsi.webapi.cohortresults;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.ohdsi.webapi.cohortanalysis.CohortAnalysis;
import org.ohdsi.webapi.cohortanalysis.CohortSummary;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionRepository;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionService;
import org.ohdsi.webapi.cohortresults.dto.GenerationInfoDTO;
import org.ohdsi.webapi.cohortresults.mapper.AnalysisResultsMapper;
import org.ohdsi.webapi.model.results.Analysis;
import org.ohdsi.webapi.model.results.AnalysisResults;
import org.ohdsi.webapi.person.CohortPerson;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.source.SourceDaimonEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.transactiontemplates.TransactionTemplates;
import org.ohdsi.webapi.util.PreparedStatementRenderer;
import org.ohdsi.webapi.util.SessionUtils;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import static org.ohdsi.webapi.util.SecurityUtils.whitelist;

import java.io.ByteArrayOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class CohortResultsService {
    final Logger log = LoggerFactory.getLogger(CohortResultsService.class);
    public static final String MIN_COVARIATE_PERSON_COUNT = "10";
    public static final String MIN_INTERVAL_PERSON_COUNT = "10";

    public static final String BASE_SQL_PATH = "cohortresults/sql";

    private final CohortDefinitionRepository cohortDefinitionRepository;
    private final CohortDefinitionService cohortDefinitionService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper mapper;
    private final CohortResultsAnalysisRunner queryRunner;
    private final SourceService sourceService;
    private final StringResourceReader resourceReader;
    private final TransactionTemplates transactionTemplates;
    private final VisualizationDataRepository visualizationDataRepository;
    private final WebApiProperties webApiProperties;

    public CohortResultsService(
            CohortDefinitionRepository cohortDefinitionRepository,
            CohortDefinitionService cohortDefinitionService,
            JdbcTemplate jdbcTemplate,
            ObjectMapper mapper,
            SourceService sourceService,
            StringResourceReader resourceReader,
            TransactionTemplates transactionTemplates,
            VisualizationDataRepository visualizationDataRepository,
            WebApiProperties webApiProperties
    ) {
        this.cohortDefinitionRepository = cohortDefinitionRepository;
        this.cohortDefinitionService = cohortDefinitionService;
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
        this.sourceService = sourceService;
        this.resourceReader = resourceReader;
        this.transactionTemplates = transactionTemplates;
        this.visualizationDataRepository = visualizationDataRepository;
        this.webApiProperties = webApiProperties;

        // TODO This is not testable.  Need to inject a factory...
        queryRunner = new CohortResultsAnalysisRunner(
                this.visualizationDataRepository,
                this.mapper,
                this.resourceReader);
    }

    public List<Map<String, String>> getCohortResultsRaw(final int id,
            final String analysisGroup,
            final String analysisName,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            String sourceKey) {

        var sqlPath = BASE_SQL_PATH + "/" + analysisGroup + "/" + analysisName + ".sql";

        var source = sourceService.findBySourceKey(sourceKey);
        try {
            PreparedStatementRenderer psr = prepareGetCohortResultsRaw(id, minCovariatePersonCountParam,
                    minIntervalPersonCountParam, sqlPath, source);
            return genericResultSetLoader(psr, source);
        } catch (Exception e) {
            log.error("Unable to translate sql for analysis {}", analysisName, e);
            return null;
        }
    }

    private PreparedStatementRenderer prepareGetCohortResultsRaw(final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam, String sqlPath,
            SourceEntity source) {

        var vocabularyTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);

        var searchStringNames = new String[] { "vocab_database_schema", "ohdsi_database_schema" };
        var replacementNames = new String[] { vocabularyTableQualifier, resultsTableQualifier };

        var variableNames = new String[] { "cohortDefinitionId", "minCovariatePersonCount",
                "minIntervalPersonCount" };
        var variableValues = new Object[] { id, (minCovariatePersonCountParam == null
                ? MIN_COVARIATE_PERSON_COUNT
                : minCovariatePersonCountParam),
                (minIntervalPersonCountParam == null
                        ? MIN_INTERVAL_PERSON_COUNT
                        : minIntervalPersonCountParam) };

        return new PreparedStatementRenderer(source, sqlPath, searchStringNames, replacementNames, variableNames,
                variableValues);
    }

    public Response exportCohortResults(int id, String sourceKey) {
        var baos = new ByteArrayOutputStream();
        var zos = new ZipOutputStream(baos);

        try {
            var source = sourceService.findBySourceKey(sourceKey);
            var sqlPath = BASE_SQL_PATH + "/raw/getAllResults.sql";
            var tqName = "tableQualifier";
            var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
            var psr = new PreparedStatementRenderer(source, sqlPath, tqName, tqValue,
                    "cohortDefinitionId", whitelist(id));

            final var resultData = new StringBuilder();
            final var resultDistributionData = new StringBuilder();

            // results export
            sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(),
                    (RowMapper<Void>) (rs, arg1) -> {
                        var metaData = rs.getMetaData();
                        int colCount = metaData.getColumnCount();
                        for (int i = 1; i <= colCount; i++) {
                            if (i > 1) {
                                resultData.append("\t");
                            }
                            resultData.append(rs.getObject(i));
                        }
                        resultData.append("\r\n");
                        return null;
                    });

            var resultsEntry = new ZipEntry("cohort_" + id + "_results.tsv");
            zos.putNextEntry(resultsEntry);
            zos.write(resultData.toString().getBytes());
            zos.closeEntry();

            // result distribution export
            sqlPath = BASE_SQL_PATH + "/raw/getAllResultDistributions.sql";
            psr = new PreparedStatementRenderer(source, sqlPath, tqName, tqValue, "cohortDefinitionId", whitelist(id));
            sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(), new RowMapper<Void>() {
                @Override
                public Void mapRow(ResultSet rs, int arg1) throws SQLException {
                    var metaData = rs.getMetaData();
                    int colCount = metaData.getColumnCount();
                    for (int i = 1; i <= colCount; i++) {
                        if (i > 1) {
                            resultDistributionData.append("\t");
                        }
                        resultDistributionData.append(rs.getObject(i));
                    }
                    resultDistributionData.append("\r\n");
                    return null;
                }
            });

            var resultsDistEntry = new ZipEntry("cohort_" + id + "_results_dist.tsv");
            zos.putNextEntry(resultsDistEntry);
            zos.write(resultDistributionData.toString().getBytes());
            zos.closeEntry();

            // include cohort definition in export
            var cohortDefinition = cohortDefinitionService.getCohortDefinitionDto(id);
            var cohortDefinitionStream = new ByteArrayOutputStream();
            mapper.writeValue(cohortDefinitionStream, cohortDefinition);
            cohortDefinitionStream.flush();

            var cohortDefinitionEntry = new ZipEntry("cohort_" + id + "_definition.json");
            zos.putNextEntry(cohortDefinitionEntry);
            zos.write(cohortDefinitionStream.toByteArray());
            zos.closeEntry();

            zos.close();
            baos.flush();
            baos.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return Response
                .ok(baos)
                .type(MediaType.APPLICATION_OCTET_STREAM)
                .build();
    }

    public Collection<String> getCompletedVisualizations(final int id, final String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        var vizData = this.visualizationDataRepository.findByCohortDefinitionIdAndSourceId(id,
                source.getSourceId());
        var completed = new HashSet<String>();
        if (CollectionUtils.isNotEmpty(vizData)) {
            for (var viz : vizData) {
                completed.add(viz.getVisualizationKey());
            }
        }
        return completed;
    }

    public TornadoReport getTornadoReport(final String sourceKey, final int cohortDefinitionId) {
        var source = sourceService.findBySourceKey(sourceKey);
        var tornadoReport = new TornadoReport();
        tornadoReport.tornadoRecords = queryRunner.getTornadoRecords(
                sourceService.getSourceJdbcTemplate(source),
                cohortDefinitionId,
                source);
        tornadoReport.profileSamples = queryRunner.getProfileSampleRecords(
                sourceService.getSourceJdbcTemplate(source),
                cohortDefinitionId, source);
        return tornadoReport;
    }

    public CohortDashboard getDashboard(final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final boolean demographicsOnly,
            final String sourceKey,
            final boolean refresh) {

        final var key = CohortResultsAnalysisRunner.DASHBOARD;
        var source = sourceService.findBySourceKey(sourceKey);
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        CohortDashboard dashboard = null;

        if (refresh || data == null) {
            dashboard = queryRunner.getDashboard(sourceService.getSourceJdbcTemplate(source), id, source,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, demographicsOnly, true);

        } else {
            try {
                dashboard = mapper.readValue(data.getData(), CohortDashboard.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return dashboard;
    }

    public List<HierarchicalConceptRecord> getConditionTreemap(String sourceKey,
            int id,
            Integer minCovariatePersonCountParam,
            Integer minIntervalPersonCountParam,
            boolean refresh) {

        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.CONDITION;
        List<HierarchicalConceptRecord> res = null;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        if (refresh || data == null) {
            res = this.queryRunner.getConditionTreemap(sourceService.getSourceJdbcTemplate(source), id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                res = mapper.readValue(data.getData(), new TypeReference<List<HierarchicalConceptRecord>>() {
                });
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return res;
    }

    public Integer getRawDistinctPersonCount(String sourceKey,
            int id,
            boolean refresh) {
        var source = sourceService.findBySourceKey(sourceKey);
        var psr = prepareGetRawDistinctPersonCount(id, source);
        var result = sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(),
                rs -> {
            // TODO warning says while statement does not loop!
                    while (rs.next()) {
                        return rs.getInt(1);
                    }
                    return null;
                });
        return result;
    }

    protected PreparedStatementRenderer prepareGetRawDistinctPersonCount(int id, SourceEntity source) {
        var sqlPath = BASE_SQL_PATH + "/raw/getTotalDistinctPeople.sql";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var tqName = "tableQualifier";
        return new PreparedStatementRenderer(source, sqlPath, tqName, tqValue, "id", id);
    }

    public CohortConditionDrilldown getConditionResults(String sourceKey,
            int id,
            int conditionId,
            Integer minCovariatePersonCountParam,
            Integer minIntervalPersonCountParam,
            boolean refresh) {
        CohortConditionDrilldown drilldown = null;
        final var key = CohortResultsAnalysisRunner.CONDITION_DRILLDOWN;
        var source = sourceService.findBySourceKey(sourceKey);
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(
                        id, source.getSourceId(), key, conditionId);

        if (refresh || data == null) {
            drilldown = this.queryRunner.getConditionResults(sourceService.getSourceJdbcTemplate(source), id, conditionId,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                drilldown = mapper.readValue(data.getData(), CohortConditionDrilldown.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return drilldown;
    }

    public List<HierarchicalConceptRecord> getConditionEraTreemap(final String sourceKey,
            final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            boolean refresh) {
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.CONDITION_ERA;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        List<HierarchicalConceptRecord> res = null;

        if (data == null || refresh) {
            res = this.queryRunner.getConditionEraTreemap(sourceService.getSourceJdbcTemplate(source), id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                res = mapper.readValue(data.getData(), new TypeReference<List<HierarchicalConceptRecord>>() {
                });
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return res;
    }

    public List<Integer> getCompletedAnalyses(
            String sourceKey,
            int id) {
        var source = sourceService.findBySourceKey(sourceKey);
        int sourceId = source.getSourceId();

        var psr = prepareGetCompletedAnalysis(id, sourceId);
        final var sql = psr.getSql();
        return jdbcTemplate.query(sql, psr.getSetter(), (resultSet, arg1) -> resultSet.getInt(1));
    }

    public GenerationInfoDTO getAnalysisProgress(
            String sourceKey,
            Integer id) {

        return transactionTemplates.getTransactionTemplateRequiresNew().execute(status -> {
            var optionalDef = cohortDefinitionRepository.findById(id);
            if (optionalDef.isEmpty()) {
                throw new NotFoundException("Cannot find cohort definition with id " + id);
            }

            var def = optionalDef.get();
            var source = sourceService.findBySourceKey(sourceKey);
            return def.getCohortAnalysisGenerationInfoList().stream()
                    .filter(cd -> Objects.equals(cd.getSourceId(), source.getSourceId()))
                    .findFirst().map(gen -> new GenerationInfoDTO(sourceKey, id, gen.getProgress()))
                    .<RuntimeException>orElseThrow(NotFoundException::new);
        });
    }

    protected PreparedStatementRenderer prepareGetCompletedAnalysis(int id, int sourceId) {
        var sqlPath = BASE_SQL_PATH + "/raw/getCompletedAnalyses.sql";
        var psr = new PreparedStatementRenderer(sourceService.findBySourceId(sourceId),
                sqlPath, new String[] { "tableQualifier" }, new String[] { webApiProperties.appDb().schemaName() },
                new String[] { "cohort_definition_id", "source_id" },
                new Object[] { id, sourceId });
        return psr;
    }

    public CohortConditionEraDrilldown getConditionEraDrilldown(final int id,
            final int conditionId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {

        CohortConditionEraDrilldown drilldown = null;
        final var key = CohortResultsAnalysisRunner.CONDITION_ERA_DRILLDOWN;
        var source = sourceService.findBySourceKey(sourceKey);
        var data = refresh ? null
                : this.visualizationDataRepository
                        .findByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(id, source.getSourceId(),
                                key,
                                conditionId);

        if (refresh || data == null) {
            drilldown = this.queryRunner.getConditionEraDrilldown(sourceService.getSourceJdbcTemplate(source), id, conditionId,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                drilldown = mapper.readValue(data.getData(), CohortConditionEraDrilldown.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return drilldown;
    }

    public List<HierarchicalConceptRecord> getDrugTreemap(final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {

        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.DRUG;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        List<HierarchicalConceptRecord> res = null;
        if (refresh || data == null) {
            res = this.queryRunner.getDrugTreemap(sourceService.getSourceJdbcTemplate(source), id, minCovariatePersonCountParam,
                    minIntervalPersonCountParam, source, true);
        } else {
            try {
                res = mapper.readValue(data.getData(), new TypeReference<>() {
                });
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return res;
    }

    public CohortDrugDrilldown getDrugResults(final int id,
            final int drugId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {
        CohortDrugDrilldown drilldown = null;
        final var key = CohortResultsAnalysisRunner.DRUG_DRILLDOWN;
        var source = sourceService.findBySourceKey(sourceKey);
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(
                        id, source.getSourceId(), key, drugId);

        if (refresh || data == null) {
            drilldown = this.queryRunner.getDrugResults(sourceService.getSourceJdbcTemplate(source), id, drugId,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                drilldown = mapper.readValue(data.getData(), CohortDrugDrilldown.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return drilldown;
    }

    public List<HierarchicalConceptRecord> getDrugEraTreemap(final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {

        var source = sourceService.findBySourceKey(sourceKey);
        List<HierarchicalConceptRecord> res = null;
        final var key = CohortResultsAnalysisRunner.DRUG_ERA;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        if (refresh || data == null) {
            res = this.queryRunner.getDrugEraTreemap(sourceService.getSourceJdbcTemplate(source), id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                res = mapper.readValue(data.getData(), new TypeReference<>() {
                });
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return res;
    }

    public CohortDrugEraDrilldown getDrugEraResults(final int id,
            final int drugId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {
        CohortDrugEraDrilldown drilldown = null;
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.DRUG_ERA_DRILLDOWN;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(
                        id, source.getSourceId(), key, drugId);

        if (refresh || data == null) {
            drilldown = this.queryRunner.getDrugEraResults(sourceService.getSourceJdbcTemplate(source), id, drugId,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                drilldown = mapper.readValue(data.getData(), CohortDrugEraDrilldown.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return drilldown;
    }

    public CohortPersonSummary getPersonResults(final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {
        CohortPersonSummary person = null;
        final var key = CohortResultsAnalysisRunner.PERSON;
        var source = sourceService.findBySourceKey(sourceKey);
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        if (refresh || data == null) {
            person = this.queryRunner.getPersonResults(sourceService.getSourceJdbcTemplate(source), id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                person = mapper.readValue(data.getData(), CohortPersonSummary.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return person;
    }

    public CohortSpecificSummary getCohortSpecificResults(final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {
        CohortSpecificSummary summary = null;
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.COHORT_SPECIFIC;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        if (refresh || data == null) {
            summary = queryRunner.getCohortSpecificSummary(sourceService.getSourceJdbcTemplate(source), id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                summary = mapper.readValue(data.getData(), CohortSpecificSummary.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return summary;
    }

    public CohortSpecificTreemap getCohortSpecificTreemapResults(final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {

        CohortSpecificTreemap summary = null;
        final var key = CohortResultsAnalysisRunner.COHORT_SPECIFIC_TREEMAP;
        var source = sourceService.findBySourceKey(sourceKey);
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        if (refresh || data == null) {
            summary = queryRunner.getCohortSpecificTreemapResults(sourceService.getSourceJdbcTemplate(source), id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                summary = mapper.readValue(data.getData(), CohortSpecificTreemap.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return summary;
    }

    public List<ScatterplotRecord> getCohortProcedureDrilldown(final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {

        List<ScatterplotRecord> records = new ArrayList<>();
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.COHORT_SPECIFIC_PROCEDURE_DRILLDOWN;
        var data = refresh ? null
                : visualizationDataRepository
                        .findByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(id, source.getSourceId(),
                                key,
                                conceptId);

        if (refresh || data == null) {
            records = this.queryRunner.getCohortProcedureDrilldown(sourceService.getSourceJdbcTemplate(source), id, conceptId,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                records = mapper.readValue(data.getData(), new TypeReference<>() {
                });
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return records;
    }

    public List<ScatterplotRecord> getCohortDrugDrilldown(final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {

        List<ScatterplotRecord> records = new ArrayList<>();
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.COHORT_SPECIFIC_DRUG_DRILLDOWN;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(
                        id, source.getSourceId(), key, conceptId);

        if (refresh || data == null) {
            records = this.queryRunner.getCohortDrugDrilldown(sourceService.getSourceJdbcTemplate(source), id, conceptId,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                records = mapper.readValue(data.getData(), new TypeReference<>() {
                });
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }
        return records;
    }

    public List<ScatterplotRecord> getCohortConditionDrilldown(final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {

        List<ScatterplotRecord> records = null;

        final var key = CohortResultsAnalysisRunner.COHORT_SPECIFIC_CONDITION_DRILLDOWN;
        var source = sourceService.findBySourceKey(sourceKey);
        var data = refresh ? null
                : this.visualizationDataRepository
                        .findByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(id, source.getSourceId(),
                                key,
                                conceptId);

        if (refresh || data == null) {
            records = this.queryRunner.getCohortConditionDrilldown(sourceService.getSourceJdbcTemplate(source), id, conceptId,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                records = mapper.readValue(data.getData(), new TypeReference<>() {
                });
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return records;
    }

    public List<HierarchicalConceptRecord> getCohortObservationResults(final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {
        List<HierarchicalConceptRecord> res = null;

        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.OBSERVATION;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        if (refresh || data == null) {
            res = this.queryRunner.getCohortObservationResults(sourceService.getSourceJdbcTemplate(source), id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                res = mapper.readValue(data.getData(), new TypeReference<>() {
                });
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return res;
    }

    public CohortObservationDrilldown getCohortObservationResultsDrilldown(final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {
        var drilldown = new CohortObservationDrilldown();
        final var key = CohortResultsAnalysisRunner.OBSERVATION_DRILLDOWN;
        var source = sourceService.findBySourceKey(sourceKey);
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(
                        id, source.getSourceId(), key, conceptId);

        if (refresh || data == null) {
            drilldown = this.queryRunner.getCohortObservationResultsDrilldown(sourceService.getSourceJdbcTemplate(source), id,
                    conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                drilldown = mapper.readValue(data.getData(), CohortObservationDrilldown.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return drilldown;
    }

    public List<HierarchicalConceptRecord> getCohortMeasurementResults(final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {
        List<HierarchicalConceptRecord> res = null;
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.MEASUREMENT;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        if (refresh || data == null) {
            res = this.queryRunner.getCohortMeasurementResults(sourceService.getSourceJdbcTemplate(source), id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                res = mapper.readValue(data.getData(), new TypeReference<>() {
                });
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return res;
    }

    public CohortMeasurementDrilldown getCohortMeasurementResultsDrilldown(final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {
        CohortMeasurementDrilldown drilldown = new CohortMeasurementDrilldown();
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.MEASUREMENT_DRILLDOWN;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(
                        id, source.getSourceId(), key, conceptId);

        if (refresh || data == null) {
            drilldown = this.queryRunner.getCohortMeasurementResultsDrilldown(sourceService.getSourceJdbcTemplate(source), id,
                    conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                drilldown = mapper.readValue(data.getData(), CohortMeasurementDrilldown.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return drilldown;
    }

    public CohortObservationPeriod getCohortObservationPeriod(final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {
        var obsPeriod = new CohortObservationPeriod();
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.OBSERVATION_PERIOD;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        if (refresh || data == null) {
            obsPeriod = this.queryRunner.getCohortObservationPeriod(sourceService.getSourceJdbcTemplate(source), id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                obsPeriod = mapper.readValue(data.getData(), CohortObservationPeriod.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return obsPeriod;
    }

    public CohortDataDensity getCohortDataDensity(final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {

        var data = new CohortDataDensity();
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.DATA_DENSITY;
        var vizData = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        if (refresh || vizData == null) {
            data = this.queryRunner.getCohortDataDensity(sourceService.getSourceJdbcTemplate(source), id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                data = mapper.readValue(vizData.getData(), CohortDataDensity.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return data;
    }

    public List<HierarchicalConceptRecord> getProcedureTreemap(final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {
        List<HierarchicalConceptRecord> res = null;
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.PROCEDURE;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        if (refresh || data == null) {
            res = this.queryRunner.getProcedureTreemap(sourceService.getSourceJdbcTemplate(source), id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                res = mapper.readValue(data.getData(), new TypeReference<>() {
                });
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return res;
    }

    public CohortProceduresDrillDown getCohortProceduresDrilldown(final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {
        var drilldown = new CohortProceduresDrillDown();
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.PROCEDURE_DRILLDOWN;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(
                        id, source.getSourceId(), key, conceptId);

        if (refresh || data == null) {
            drilldown = this.queryRunner.getCohortProceduresDrilldown(sourceService.getSourceJdbcTemplate(source), id, conceptId,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                drilldown = mapper.readValue(data.getData(), CohortProceduresDrillDown.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return drilldown;
    }

    public List<HierarchicalConceptRecord> getVisitTreemap(final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {

        List<HierarchicalConceptRecord> res = null;
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.VISIT;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        if (refresh || data == null) {
            res = queryRunner.getVisitTreemap(sourceService.getSourceJdbcTemplate(source), id, minCovariatePersonCountParam,
                    minIntervalPersonCountParam, source, true);
        } else {
            try {
                res = mapper.readValue(data.getData(), new TypeReference<>() {
                });
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return res;
    }

    public CohortVisitsDrilldown getCohortVisitsDrilldown(final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final String sourceKey,
            boolean refresh) {
        var drilldown = new CohortVisitsDrilldown();
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.VISIT_DRILLDOWN;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(
                        id, source.getSourceId(), key, conceptId);

        if (refresh || data == null) {
            drilldown = this.queryRunner.getCohortVisitsDrilldown(sourceService.getSourceJdbcTemplate(source), id, conceptId,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                drilldown = mapper.readValue(data.getData(), CohortVisitsDrilldown.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }
        return drilldown;
    }

    public CohortSummary getCohortSummaryData(
            int id,
            String sourceKey) {
        var summary = new CohortSummary();

        try {
            // total patients
            var personCount = this.getRawDistinctPersonCount(sourceKey, id, false);
            summary.setTotalPatients(String.valueOf(personCount));

            // median age
            CohortSpecificSummary cohortSpecific = this.getCohortSpecificResults(id, null, null, sourceKey, false);
            if (cohortSpecific != null && cohortSpecific.getAgeAtIndexDistribution() != null
                    && !cohortSpecific.getAgeAtIndexDistribution().isEmpty()) {
                summary.setMeanAge(String.valueOf(cohortSpecific.getAgeAtIndexDistribution().getFirst().getMedianValue()));
            }

            // TODO mean obs period.
            var dashboard = this.getDashboard(id, null, null, true, sourceKey, false);
            if (dashboard != null) {
                summary.setGenderDistribution(dashboard.getGender());
                summary.setAgeDistribution(dashboard.getAgeAtFirstObservation());
            }
        } catch (Exception e) {
            log.error(whitelist(e));
        }

        return summary;
    }

    public CohortDeathData getCohortDeathData(final int id,
            Integer minCovariatePersonCountParam,
            Integer minIntervalPersonCountParam,
            String sourceKey,
            boolean refresh) {
        var data = new CohortDeathData();
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.DEATH;
        var vizData = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        if (refresh || vizData == null) {
            data = this.queryRunner.getCohortDeathData(sourceService.getSourceJdbcTemplate(source), id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source, true);
        } else {
            try {
                data = mapper.readValue(vizData.getData(), CohortDeathData.class);
            } catch (Exception e) {
                log.error(whitelist(e));
            }
        }

        return data;
    }

    public CohortSummary getCohortSummaryAnalyses(
            int id,
            String sourceKey) {
        var summary = new CohortSummary();
        try {
            summary.setAnalyses(getCohortAnalysesForCohortDefinition(whitelist(id), sourceKey, true));
        } catch (Exception e) {
            log.error("unable to get cohort summary", e);
        }

        return summary;
    }

    public Collection<CohortPerson> getCohortMembers(
            final int id,
            String sourceKey,
            int min,
            int max) {
        var source = sourceService.findBySourceKey(sourceKey);
        var sqlPath = "cohortresults/sql/raw/getMembers.sql";
        var tqName = "tableQualifier";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var names = new String[] { "cohortDefinitionId", "min", "max" };
        var values = new Object[] { whitelist(id), whitelist(min), whitelist(max) };
        var psr = new PreparedStatementRenderer(source, sqlPath, tqName, tqValue, names, values,
                SessionUtils.sessionId());
        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(), cohortMemberMapper);
    }

    public Collection<CohortBreakdown> getCohortBreakdown(
            int id,
            String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        var sqlPath = "cohortresults/sql/raw/getCohortBreakdown.sql";
        var resultsTqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var cdmSchemaValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.CDM);
        var vocabSchemaValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var replacementStrings = new String[] { "resultsTableQualifier", "cdm_schema", "vocab_schema" };
        var replacementValues = new String[] { resultsTqValue, cdmSchemaValue, vocabSchemaValue };
        var psr = new PreparedStatementRenderer(source, sqlPath, replacementStrings, replacementValues,
                "cohortDefinitionId", id);
        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(), cohortBreakdownMapper);
    }

    public Collection<CohortPerson> getCohortMembers(
            final int id,
            String sourceKey,
            String gender,
            String age,
            String conditions,
            String drugs,
            final int rows) {
        var source = sourceService.findBySourceKey(sourceKey);
        var psr = prepareGetCohortMembers(id, gender,
                age, conditions, drugs, rows, source);
        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(), cohortMemberMapper);
    }

    protected PreparedStatementRenderer prepareGetCohortMembers(
            final int id,
            String gender,
            String age,
            String conditions,
            String drugs,
            final int rows,
            SourceEntity source) {
        var path = "cohortresults/sql/raw/getCohortBreakdownPeople.sql";
        var params = new ArrayList<String>();
        var wherecols = new ArrayList<String>();
        int groups = 1;
        if (!gender.isEmpty() && !gender.equals("''")) {
            params.add(" gender in (@gender) ");
            wherecols.add("gender");
            groups = groups * gender.split(",").length;
        }
        if (!age.isEmpty() && !age.equals("''")) {
            params.add(" age in (@age) ");
            wherecols.add("age");
            groups = groups * age.split(",").length;
        }
        if (!conditions.isEmpty() && !conditions.equals("''")) {
            params.add(" conditions in (@conditions) ");
            wherecols.add("conditions");
            groups = groups * conditions.split(",").length;
        }
        if (!drugs.isEmpty() && !"''".equals(drugs)) {
            params.add(" drugs in (@drugs) ");
            wherecols.add("drugs");
            groups = groups * drugs.split(",").length;
        }
        var clause = " where 1=1\n";
        for (var param : params) {
            clause += (" and " + param + "\n");
        }

        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var cdmTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.CDM);
        var vocabTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);

        var wherecolsStr = "";
        if (!wherecols.isEmpty()) {
            wherecolsStr += wherecols.getFirst();
            for (int i = 1; i < wherecols.size(); i++) {
                wherecolsStr += (',' + wherecols.get(i));
            }
        }
        var searchFor = new String[] { "vocab_schema", "cdm_schema", "resultsTableQualifier", "whereclause", "wherecols",
                "partition by" };
        var replaceWith = wherecols.isEmpty()
            ? new String[] {
                        vocabTableQualifier, cdmTableQualifier, resultsTableQualifier, clause, wherecolsStr, "partition by" }
            : new String[] {
                        vocabTableQualifier, cdmTableQualifier, resultsTableQualifier, clause, wherecolsStr, ""};

        var names = new String[] { "cohortDefinitionId", "gender", "age", "conditions", "drugs", "rows",
                "groups" };

        var genderArray = gender.replaceAll("'", "").split(",");
        var ageArray = age.replaceAll("'", "").split(",");

        var conditionIds = Arrays.stream(conditions.split(",")).map(NumberUtils::toInt)
                .toList();

        var values = new Object[] { id, genderArray, ageArray, conditionIds.toArray(),
                Arrays.stream(drugs.split(",")).map(NumberUtils::toInt).toArray(), rows,
                groups };
        return new PreparedStatementRenderer(source, path, searchFor, replaceWith, names, values);
    }

    public Long getCohortMemberCount(
            int id,
            String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        var sqlPath = "cohortresults/sql/raw/getMemberCount.sql";
        var tqName = "tableQualifier";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.CDM);
        var psr = new PreparedStatementRenderer(source, sqlPath, tqName, tqValue,
                "cohortDefinitionId", whitelist(id), SessionUtils.sessionId());
        return sourceService.getSourceJdbcTemplate(source).queryForObject(psr.getSql(), psr.getOrderedParams(), Long.class);
    }

    public List<CohortAnalysis> getCohortAnalysesForCohortDefinition(
            int id,
            String sourceKey,
            boolean retrieveFullDetail) {
        var source = sourceService.findBySourceKey(sourceKey);
        String sql;

        if (retrieveFullDetail) {
            sql = resourceReader.getText("cohortanalysis/sql/getCohortAnalysesForCohortFull.sql");
        } else {
            sql = resourceReader.getText("cohortanalysis/sql/getCohortAnalysesForCohort.sql");
        }
        var tqName = "ohdsi_database_schema";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var psr = new PreparedStatementRenderer(source, sql, tqName, tqValue,
                "cohortDefinitionId", whitelist(id));

        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(), cohortAnalysisMapper);
    }

    public List<ExposureCohortResult> getExposureOutcomeCohortRates(
            String sourceKey,
            ExposureCohortSearch search) {
        var source = sourceService.findBySourceKey(sourceKey);
        var psr = prepareGetExposureOutcomeCohortRates(search, source);

        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(), (rs, rowNum) -> {
            var e = new ExposureCohortResult();
            e.exposureCohortDefinitionId = rs.getString("exposure_cohort_definition_id");
            e.incidenceRate1000py = rs.getFloat("incidence_rate_1000py");
            e.numPersonsExposed = rs.getLong("num_persons_exposed");
            e.numPersonsWithOutcomePostExposure = rs.getLong("num_persons_w_outcome_post_exposure");
            e.numPersonsWithOutcomePreExposure = rs.getLong("num_persons_w_outcome_pre_exposure");
            e.outcomeCohortDefinitionId = rs.getString("outcome_cohort_definition_id");
            e.timeAtRisk = rs.getFloat("time_at_risk");
            return e;
        });
    }

    protected PreparedStatementRenderer prepareGetExposureOutcomeCohortRates(
            ExposureCohortSearch search,
            SourceEntity source) {
        var path = "cohortresults/sql/cohortSpecific/getExposureOutcomeCohortRates.sql";
        var tqName = "ohdsi_database_schema";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var names = new String[] { "exposure_cohort_definition_id", "outcome_cohort_definition_id" };
        var values = new Object[] { search.exposureCohortList, search.outcomeCohortList };
        var psr = new PreparedStatementRenderer(source, path, tqName, tqValue, names, values);
        return psr;
    }

    public List<TimeToEventResult> getTimeToEventDrilldown(
            String sourceKey,
            ExposureCohortSearch search) {
        var source = sourceService.findBySourceKey(sourceKey);
        var psr = prepareGetTimeToEventDrilldown(search, source);

        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(), (rs, rowNum) -> {
            var e = new TimeToEventResult();
            e.countValue = rs.getLong("count_value");
            e.duration = rs.getLong("duration");
            e.exposureCohortDefinitionId = rs.getString("exposure_cohort_definition_id");
            e.outcomeCohortDefinitionId = rs.getString("outcome_cohort_definition_id");
            e.pctPersons = rs.getDouble("pct_persons");
            e.recordType = rs.getString("record_type");
            return e;
        });
    }

    protected PreparedStatementRenderer prepareGetTimeToEventDrilldown(
            ExposureCohortSearch search,
            SourceEntity source) {
        var path = "cohortresults/sql/cohortSpecific/getTimeToEventDrilldown.sql";
        var tqName = "ohdsi_database_schema";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var names = new String[] { "exposure_cohort_definition_id", "outcome_cohort_definition_id" };
        var values = new Object[] { search.exposureCohortList, search.outcomeCohortList };
        return new PreparedStatementRenderer(source, path, tqName, tqValue, names, values);
    }

    public List<PredictorResult> getExposureOutcomeCohortPredictors(
            String sourceKey,
            ExposureCohortSearch search) {
        var source = sourceService.findBySourceKey(sourceKey);
        var psr = prepareGetExposureOutcomeCohortPredictors(search, source);

        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(), (rs, rowNum) -> {
            var e = new PredictorResult();
            e.absStdDiff = rs.getString("abs_std_diff");
            e.conceptId = rs.getString("concept_id");
            e.conceptName = rs.getString("concept_name");
            e.conceptWithOutcome = rs.getString("concept_w_outcome");
            e.domainId = rs.getString("domain_id");
            e.pctOutcomeWithConcept = rs.getString("pct_outcome_w_concept");
            e.pctNoOutcomeWithConcept = rs.getString("pct_nooutcome_w_concept");
            e.exposureCohortDefinitionId = rs.getString("exposure_cohort_definition_id");
            e.outcomeCohortDefinitionId = rs.getString("outcome_cohort_definition_id");
            return e;
        });
    }

    public List<CohortAttribute> getHeraclesHeel(
            int id,
            String sourceKey,
            boolean refresh) {
        List<CohortAttribute> attrs = new ArrayList<>();
        var source = sourceService.findBySourceKey(sourceKey);
        final var key = CohortResultsAnalysisRunner.HERACLES_HEEL;
        var data = refresh
                ? null
                : this.visualizationDataRepository.findByCohortDefinitionIdAndSourceIdAndVisualizationKey(id,
                        source.getSourceId(), key);

        if (refresh || data == null) {
            attrs = this.queryRunner.getHeraclesHeel(sourceService.getSourceJdbcTemplate(source), id, source, true);
        } else {
            try {
                attrs = mapper.readValue(data.getData(), new TypeReference<>() {
                });
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        return attrs;
    }

    public List<AnalysisResults> getCohortAnalysesForDataCompleteness(final int id, String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var sql = resourceReader.getText("cohortresults/sql/datacompleteness/getCohortDataCompleteness.sql");

        var psr = new PreparedStatementRenderer(source, sql, "tableQualifier",
                resultsTableQualifier, "cohortDefinitionId", id);

        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(), new AnalysisResultsMapper());
    }

    public List<DataCompletenessAttr> getDataCompleteness(int id, String sourceKey) {
        var arl = this.getCohortAnalysesForDataCompleteness(id, sourceKey);

        List<DataCompletenessAttr> dcal = new ArrayList<>();
        var resultMap = new HashMap<Integer, AnalysisResults>();

        for (var ar : arl) {
            resultMap.put(ar.getAnalysisId(), ar);
        }

        var aca = new DataCompletenessAttr();
        aca.setCovariance("0~10");
        aca.setGenderP(Float.parseFloat(resultMap.get(2001).getStratum1()));
        aca.setRaceP(Float.parseFloat(resultMap.get(2011).getStratum1()));
        aca.setEthP(Float.parseFloat(resultMap.get(2021).getStratum1()));
        dcal.add(aca);

        aca = new DataCompletenessAttr();
        aca.setCovariance("10~20");
        aca.setGenderP(Float.parseFloat(resultMap.get(2002).getStratum1()));
        aca.setRaceP(Float.parseFloat(resultMap.get(2012).getStratum1()));
        aca.setEthP(Float.parseFloat(resultMap.get(2022).getStratum1()));
        dcal.add(aca);

        aca = new DataCompletenessAttr();
        aca.setCovariance("20~30");
        aca.setGenderP(Float.parseFloat(resultMap.get(2003).getStratum1()));
        aca.setRaceP(Float.parseFloat(resultMap.get(2013).getStratum1()));
        aca.setEthP(Float.parseFloat(resultMap.get(2023).getStratum1()));
        dcal.add(aca);

        aca = new DataCompletenessAttr();
        aca.setCovariance("30~40");
        aca.setGenderP(Float.parseFloat(resultMap.get(2004).getStratum1()));
        aca.setRaceP(Float.parseFloat(resultMap.get(2014).getStratum1()));
        aca.setEthP(Float.parseFloat(resultMap.get(2024).getStratum1()));
        dcal.add(aca);

        aca = new DataCompletenessAttr();
        aca.setCovariance("40~50");
        aca.setGenderP(Float.parseFloat(resultMap.get(2005).getStratum1()));
        aca.setRaceP(Float.parseFloat(resultMap.get(2015).getStratum1()));
        aca.setEthP(Float.parseFloat(resultMap.get(2025).getStratum1()));
        dcal.add(aca);

        aca = new DataCompletenessAttr();
        aca.setCovariance("50~60");
        aca.setGenderP(Float.parseFloat(resultMap.get(2006).getStratum1()));
        aca.setRaceP(Float.parseFloat(resultMap.get(2016).getStratum1()));
        aca.setEthP(Float.parseFloat(resultMap.get(2026).getStratum1()));
        dcal.add(aca);

        aca = new DataCompletenessAttr();
        aca.setCovariance("60+");
        aca.setGenderP(Float.parseFloat(resultMap.get(2007).getStratum1()));
        aca.setRaceP(Float.parseFloat(resultMap.get(2017).getStratum1()));
        aca.setEthP(Float.parseFloat(resultMap.get(2027).getStratum1()));
        dcal.add(aca);

        return dcal;
    }

    private List<AnalysisResults> getCohortAnalysesEntropy(
            final int id,
            String sourceKey,
            int entroppAnalysisId) {
        var sql = resourceReader.getText("cohortresults/sql/entropy/getEntropy.sql");
        var source = sourceService.findBySourceKey(sourceKey);
        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);

        var searchStringNames = new String[] { "tableQualifier" };
        var replacementNames = new String[] { resultsTableQualifier };

        var variableNames = new String[] { "cohortDefinitionId", "entroppAnalysisId" };
        var variableValues = new Object[] { id, entroppAnalysisId };

        var psr = new PreparedStatementRenderer(source, sql, searchStringNames, replacementNames,
                variableNames, variableValues);
        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(), new AnalysisResultsMapper());
    }

    public List<EntropyAttr> getEntropy(int id, String sourceKey) {
        var arl = this.getCohortAnalysesEntropy(id, sourceKey, 2031);

        List<EntropyAttr> el = new ArrayList<>();
        for (var ar : arl) {
            EntropyAttr ea = new EntropyAttr();
            ea.setDate(ar.getStratum1());
            ea.setEntropy(Float.parseFloat(ar.getStratum2()));
            el.add(ea);
        }

        return el;
    }

    public List<EntropyAttr> getAllEntropy(int id, String sourceKey) {
        var arl = this.getCohortAnalysesEntropy(id, sourceKey, 2031);

        List<EntropyAttr> el = new ArrayList<EntropyAttr>();
        for (var ar : arl) {
            var ea = new EntropyAttr();
            ea.setDate(ar.getStratum1());
            ea.setEntropy(Float.parseFloat(ar.getStratum2()));
            ea.setInsitution("All sites");
            el.add(ea);
        }

        arl = this.getCohortAnalysesEntropy(id, sourceKey, 2032);

        for (var ar : arl) {
            var ea = new EntropyAttr();
            var careSite = ar.getStratum2() != null && !ar.getStratum2().trim().isEmpty()
                    ? ar.getStratum1() + ":" + ar.getStratum2().trim()
                    : ar.getStratum1();
            ea.setInsitution(careSite);
            ea.setDate(ar.getStratum3());
            ea.setEntropy(Float.parseFloat(ar.getStratum4()));
            el.add(ea);
        }

        return el;
    }

    public HealthcareExposureReport getHealthcareUtilizationExposureReport(
            final int id,
            String sourceKey,
            String windowString,
            String periodTypeString) {

        var window = WindowType.fromString(windowString);
        var periodType = PeriodType.fromString(periodTypeString);

        var source = sourceService.findBySourceKey(sourceKey);
        var exposureReport = queryRunner.getHealthcareExposureReport(
                sourceService.getSourceJdbcTemplate(source),
                id,
                window,
                periodType,
                source);
        return exposureReport;
    }

    public List<String> getHealthcareUtilizationPeriods(
            final int id,
            String sourceKey,
            String windowString) {

        var window = WindowType.fromString(windowString);

        final var source = sourceService.findBySourceKey(sourceKey);
        final var periodTypes = queryRunner.getHealthcarePeriodTypes(
                sourceService.getSourceJdbcTemplate(source), id, window,
                source);
        return periodTypes;
    }

    public HealthcareVisitUtilizationReport getHealthcareUtilizationVisitReport(
        final int id,
        String sourceKey,
        String windowString,
        String visitStatString,
        String periodTypeString,
        Long visitConcept,
        Long visitTypeConcept,
        Long costTypeConcept
    ) {
        var window = WindowType.fromString(windowString);
        var periodType = PeriodType.fromString(periodTypeString);
        var visitStat = VisitStatType.fromString(visitStatString);

        var source = sourceService.findBySourceKey(sourceKey);
        var visitUtilizationReport = queryRunner.getHealthcareVisitReport(
                sourceService.getSourceJdbcTemplate(source), id, window, visitStat, periodType, visitConcept, visitTypeConcept,
                costTypeConcept, source);
        return visitUtilizationReport;
    }

    public HealthcareDrugUtilizationSummary getHealthcareUtilizationDrugSummaryReport(
        final int id,
        String sourceKey,
        String windowString,
        Long drugTypeConceptId,
        Long costTypeConceptId
    ) {

        var window = WindowType.fromString(windowString);

        var source = sourceService.findBySourceKey(sourceKey);
        var report = queryRunner.getHealthcareDrugUtilizationSummary(
                sourceService.getSourceJdbcTemplate(source),
                id,
                window,
                drugTypeConceptId,
                costTypeConceptId,
                source);
        return report;
    }

    public HealthcareDrugUtilizationDetail getHealthcareUtilizationDrugDetailReport(
        final int id,
        String sourceKey,
        final String windowString,
        final Long drugConceptId,
        final String periodTypeString,
        final Long drugTypeConceptId,
        final Long costTypeConceptId
    ) {
        var window = WindowType.fromString(windowString);
        var periodType = PeriodType.fromString(periodTypeString);

        var source = sourceService.findBySourceKey(sourceKey);
        var report = queryRunner.getHealthcareDrugUtilizationReport(
                sourceService.getSourceJdbcTemplate(source),
                id,
                window,
                drugConceptId,
                drugTypeConceptId,
                periodType,
                costTypeConceptId,
                source);
        return report;
    }

    public List<Concept> getDrugTypes(
            final int id,
            String sourceKey,
            final Long drugConceptId) {
        var source = sourceService.findBySourceKey(sourceKey);
        return queryRunner.getDrugTypes(
                sourceService.getSourceJdbcTemplate(source),
                id,
                drugConceptId,
                source);
    }

    protected PreparedStatementRenderer prepareGetExposureOutcomeCohortPredictors(
            ExposureCohortSearch search, SourceEntity source) {
        var path = "cohortresults/sql/cohortSpecific/getExposureOutcomePredictors.sql";
        var resultsQualName = "ohdsi_database_schema";
        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var cdmQualName = "cdm_schema";
        var cdmTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.CDM);
        var vocabQualName = "vocab_schema";
        var vocabTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var searchFor = new String[] { resultsQualName, cdmQualName, vocabQualName };
        var replace = new String[] { resultsTableQualifier, cdmTableQualifier, vocabTableQualifier };
        var names = new String[] { "exposure_cohort_definition_id", "outcome_cohort_definition_id",
                "minCellCount" };
        var values = new Object[] { search.exposureCohortList, search.outcomeCohortList, search.minCellCount };

        return new PreparedStatementRenderer(source, path, searchFor, replace, names, values);
    }

    private final RowMapper<CohortPerson> cohortMemberMapper = (rs, rowNum) -> {
        var person = new CohortPerson();
        person.personId = rs.getLong("subject_id");
        person.startDate = rs.getTimestamp("cohort_start_date");
        person.endDate = rs.getTimestamp("cohort_end_date");
        return person;
    };

    private final RowMapper<CohortBreakdown> cohortBreakdownMapper = (rs, rowNum) -> {
        var group = new CohortBreakdown();
        group.people = rs.getLong("people");
        group.gender = rs.getString("gender");
        group.age = rs.getString("age");
        group.conditions = rs.getLong("conditions");
        group.drugs = rs.getLong("drugs");
        return group;
    };

    private final RowMapper<CohortAnalysis> cohortAnalysisMapper = (rs, rowNum) -> {
        final var cohortAnalysis = new CohortAnalysis();
        mapAnalysis(cohortAnalysis, rs);
        cohortAnalysis.setAnalysisComplete(rs.getInt(CohortAnalysis.ANALYSIS_COMPLETE) == 1);
        cohortAnalysis.setCohortDefinitionId(rs.getInt(CohortAnalysis.COHORT_DEFINITION_ID));
        cohortAnalysis.setLastUpdateTime(rs.getTimestamp(CohortAnalysis.LAST_UPDATE_TIME));
        return cohortAnalysis;
    };

    private void mapAnalysis(final Analysis analysis, final ResultSet rs) throws SQLException {
        analysis.setAnalysisId(rs.getInt(Analysis.ANALYSIS_ID));
        analysis.setAnalysisName(rs.getString(Analysis.ANALYSIS_NAME));
        analysis.setStratum1Name(rs.getString(Analysis.STRATUM_1_NAME));
        analysis.setStratum2Name(rs.getString(Analysis.STRATUM_2_NAME));
        analysis.setStratum3Name(rs.getString(Analysis.STRATUM_3_NAME));
        analysis.setStratum4Name(rs.getString(Analysis.STRATUM_4_NAME));
        analysis.setStratum5Name(rs.getString(Analysis.STRATUM_5_NAME));
        analysis.setAnalysisType(rs.getString(Analysis.ANALYSIS_TYPE));
    }

    List<Map<String, String>> genericResultSetLoader(PreparedStatementRenderer psr, SourceEntity source) {
        List<Map<String, String>> results = null;
        try {
            results = sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(),
                    (rs, rowNum) -> {
                        var result = new HashMap<String, String>();
                        var metaData = rs.getMetaData();
                        int colCount = metaData.getColumnCount();
                        for (int i = 1; i <= colCount; i++) {
                            var columnLabel = metaData.getColumnLabel(i);
                            var columnValue = String.valueOf(rs.getObject(i));
                            result.put(columnLabel, columnValue);
                        }
                        return result;
                    });

        } catch (Exception e) {
            log.error("Result set loading error", e);
        }
        return results;
    }
}
