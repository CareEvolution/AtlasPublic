package org.ohdsi.webapi.cohortresults;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.webapi.cohortanalysis.CohortAnalysisTask;
import org.ohdsi.webapi.cohortresults.mapper.*;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.source.SourceDaimonEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.util.PreparedStatementRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.ohdsi.webapi.util.SecurityUtils.whitelist;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CohortResultsAnalysisRunner {
    public static final String COHORT_SPECIFIC = "cohort_specific";
    public static final String COHORT_SPECIFIC_CONDITION_DRILLDOWN = "cohort_specific_condition_drilldown";
    public static final String COHORT_SPECIFIC_DRUG_DRILLDOWN = "cohort_specific_drug_drilldown";
    public static final String COHORT_SPECIFIC_PROCEDURE_DRILLDOWN = "cohort_specific_procedure_drilldown";
    public static final String COHORT_SPECIFIC_TREEMAP = "cohort_specific_treemap";
    public static final String CONDITION = "condition";
    public static final String CONDITION_DRILLDOWN = "condition_drilldown";
    public static final String CONDITION_ERA = "condition_era";
    public static final String CONDITION_ERA_DRILLDOWN = "condition_era_drilldown";
    public static final String DASHBOARD = "dashboard";
    public static final String DATA_DENSITY = "data_density";
    public static final String DEATH = "death";
    public static final String DEFAULT = "default";
    public static final String DRUG = "drug";
    public static final String DRUG_DRILLDOWN = "drug_drilldown";
    public static final String DRUG_ERA = "drug_era";
    public static final String DRUG_ERA_DRILLDOWN = "drug_era_drilldown";
    public static final String DRUG_EXPOSURE = "drug_exposure";
    public static final String HERACLES_HEEL = "heracles_heel";
    public static final String MEASUREMENT = "measurement";
    public static final String MEASUREMENT_DRILLDOWN = "measurement_drilldown";
    public static final String OBSERVATION = "observation";
    public static final String OBSERVATION_DRILLDOWN = "observation_drilldown";
    public static final String OBSERVATION_PERIOD = "observation_period";
    public static final String OBSERVATION_PERIODS = "observation_periods";
    public static final String PERSON = "person";
    public static final String PROCEDURE = "procedure";
    public static final String PROCEDURE_DRILLDOWN = "procedure_drilldown";
    public static final String VISIT = "visit";
    public static final String VISIT_DRILLDOWN = "visit_drilldown";

    private static final Logger log = LoggerFactory.getLogger(CohortResultsAnalysisRunner.class);

    public static final String BASE_SQL_PATH = "cohortresults/sql";

    public static final Integer MIN_COVARIATE_PERSON_COUNT = 10;
    public static final Integer MIN_INTERVAL_PERSON_COUNT = 10;

    private final ObjectMapper mapper;
    private final StringResourceReader resourceReader;
    private final VisualizationDataRepository visualizationDataRepository;

    public CohortResultsAnalysisRunner(
            VisualizationDataRepository visualizationDataRepository,
            ObjectMapper objectMapper,
            StringResourceReader resourceReader) {
        this.visualizationDataRepository = visualizationDataRepository;
        mapper = objectMapper;
        this.resourceReader = resourceReader;
    }

    public List<TornadoRecord> getTornadoRecords(JdbcTemplate jdbcTemplate, final int id, SourceEntity source) {
        final var sqlPath = BASE_SQL_PATH + "/tornado/getTornadoData.sql";
        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);

        var search = new String[] { "tableQualifier" };
        var replace = new String[] { resultsTableQualifier };

        var cols = new String[] { "cohortDefinitionId" };
        var colValues = new Object[] { id };

        final var psr = new PreparedStatementRenderer(source, sqlPath, search, replace,
                cols,
                colValues);
        return jdbcTemplate.query(psr.getSql(), psr.getSetter(), new TornadoMapper());
    }

    public List<ProfileSampleRecord> getProfileSampleRecords(JdbcTemplate jdbcTemplate, final int id,
            SourceEntity source) {
        final var sqlPath = BASE_SQL_PATH + "/tornado/getProfileSamples.sql";
        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);

        var search = new String[] { "tableQualifier" };
        var replace = new String[] { resultsTableQualifier };

        var cols = new String[] { "cohortDefinitionId" };
        var colValues = new Object[] { id };

        final var psr = new PreparedStatementRenderer(source, sqlPath, search, replace,
                cols,
                colValues);
        return jdbcTemplate.query(psr.getSql(), psr.getSetter(), new ProfileSampleMapper());
    }

    public List<ScatterplotRecord> getCohortConditionDrilldown(JdbcTemplate jdbcTemplate,
            final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {

        List<ScatterplotRecord> records = new ArrayList<>();
        final var psr = prepareDrillDownCohortSql("firstConditionRelativeToIndex",
                "cohortSpecific", id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (psr != null) {
            records = jdbcTemplate.query(psr.getSql(), psr.getSetter(), new ScatterplotMapper());
            if (save) {
                this.saveEntityDrilldown(id, source.getSourceId(), COHORT_SPECIFIC_CONDITION_DRILLDOWN, conceptId, records);
            }
        }

        return records;
    }

    public CohortDataDensity getCohortDataDensity(JdbcTemplate jdbcTemplate,
            final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {

        var data = new CohortDataDensity();
        boolean empty = true;

        var recordsPerPersonSql = this.renderTranslateCohortSql(
                BASE_SQL_PATH + "/datadensity/recordsperperson.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (recordsPerPersonSql != null) {
            data.setRecordsPerPerson(jdbcTemplate.query(recordsPerPersonSql.getSql(),
                    recordsPerPersonSql.getSetter(),
                    new SeriesPerPersonMapper()));
        }
        var totalRecordsSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/datadensity/totalrecords.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (totalRecordsSql != null) {
            data.setTotalRecords(jdbcTemplate.query(totalRecordsSql.getSql(), totalRecordsSql.getSetter(),
                    new SeriesPerPersonMapper()));
        }
        var conceptsPerPersonSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/datadensity/conceptsperperson.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (conceptsPerPersonSql != null) {
            data.setConceptsPerPerson(
                    jdbcTemplate.query(conceptsPerPersonSql.getSql(),
                            conceptsPerPersonSql.getSetter(),
                            new ConceptQuartileMapper()));
        }

        if (CollectionUtils.isNotEmpty(data.getRecordsPerPerson())
                || CollectionUtils.isNotEmpty(data.getTotalRecords())
                || CollectionUtils.isNotEmpty(data.getConceptsPerPerson())) {
            empty = false;
        }

        if (!empty && save) {
            saveEntity(id, source.getSourceId(), DATA_DENSITY, data);
        }

        return data;
    }

    public CohortDeathData getCohortDeathData(JdbcTemplate jdbcTemplate,
            final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {
        var data = new CohortDeathData();
        boolean empty = true;

        List<ConceptQuartileRecord> age = null;
        var ageSql = renderTranslateCohortSql(BASE_SQL_PATH + "/death/sqlAgeAtDeath.sql",
                id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (ageSql != null) {
            age = jdbcTemplate.query(ageSql.getSql(), ageSql.getSetter(), new ConceptQuartileMapper());
        }
        data.setAgetAtDeath(age);

        List<ConceptCountRecord> byType = null;
        var byTypeSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/death/sqlDeathByType.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (byTypeSql != null) {
            byType = jdbcTemplate.query(byTypeSql.getSql(), byTypeSql.getSetter(),
                    new ConceptCountMapper());
        }
        data.setDeathByType(byType);

        List<ConceptDecileRecord> prevalenceByGenderAgeYear = null;
        var prevalenceGenderAgeSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/death/sqlPrevalenceByGenderAgeYear.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalenceGenderAgeSql != null) {
            prevalenceByGenderAgeYear = jdbcTemplate.query(prevalenceGenderAgeSql.getSql(),
                    prevalenceGenderAgeSql.getSetter(), new ConceptDecileCountsMapper());
        }
        data.setPrevalenceByGenderAgeYear(prevalenceByGenderAgeYear);

        List<PrevalenceRecord> prevalenceByMonth = null;
        var prevalanceMonthSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/death/sqlPrevalenceByMonth.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalanceMonthSql != null) {
            prevalenceByMonth = jdbcTemplate.query(prevalanceMonthSql.getSql(),
                    prevalanceMonthSql.getSetter(),
                    new PrevalanceConceptMapper());
        }
        data.setPrevalenceByMonth(prevalenceByMonth);

        if (CollectionUtils.isNotEmpty(data.getAgetAtDeath())
                || CollectionUtils.isNotEmpty(data.getDeathByType())
                || CollectionUtils.isNotEmpty(data.getPrevalenceByGenderAgeYear())
                || CollectionUtils.isNotEmpty(data.getPrevalenceByMonth())) {
            empty = false;
        }
        if (!empty && save) {
            this.saveEntity(id, source.getSourceId(), DEATH, data);
        }

        return data;
    }

    public List<ScatterplotRecord> getCohortDrugDrilldown(JdbcTemplate jdbcTemplate,
            final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {

        List<ScatterplotRecord> records = new ArrayList<>();

        final var sql = prepareDrillDownCohortSql("drugOccursRelativeToIndex",
                "cohortSpecific",
                id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sql != null) {
            records = jdbcTemplate.query(sql.getSql(), sql.getSetter(), new ScatterplotMapper());
            if (save) {
                saveEntityDrilldown(id, source.getSourceId(), COHORT_SPECIFIC_DRUG_DRILLDOWN, conceptId, records);
            }
        }

        return records;
    }

    public List<HierarchicalConceptRecord> getCohortMeasurementResults(JdbcTemplate jdbcTemplate,
            final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {
        List<HierarchicalConceptRecord> res = null;

        var sql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/measurement/sqlMeasurementTreemap.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sql != null) {
            res = jdbcTemplate.query(sql.getSql(), sql.getSetter(), new HierarchicalConceptMapper());
            if (save) {
                saveEntity(id, source.getSourceId(), MEASUREMENT, res);
            }
        }
        return res;
    }

    public CohortMeasurementDrilldown getCohortMeasurementResultsDrilldown(JdbcTemplate jdbcTemplate,
            final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {
        var drilldown = new CohortMeasurementDrilldown();
        boolean empty = true;

        var ageAtFirstOccurrenceSql = prepareDrillDownCohortSql("sqlAgeAtFirstOccurrence",
                MEASUREMENT, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (ageAtFirstOccurrenceSql != null) {
            drilldown.setAgeAtFirstOccurrence(
                    jdbcTemplate.query(ageAtFirstOccurrenceSql.getSql(),
                            ageAtFirstOccurrenceSql.getSetter(),
                            new ConceptQuartileMapper()));
        }

        var sqlLowerLimitDistribution = prepareDrillDownCohortSql(
                "sqlLowerLimitDistribution",
                MEASUREMENT, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sqlLowerLimitDistribution != null) {
            drilldown.setLowerLimitDistribution(
                    jdbcTemplate.query(sqlLowerLimitDistribution.getSql(),
                            sqlLowerLimitDistribution.getSetter(),
                            new ConceptQuartileMapper()));
        }

        var sqlMeasurementValueDistribution = prepareDrillDownCohortSql(
                "sqlMeasurementValueDistribution", MEASUREMENT, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sqlMeasurementValueDistribution != null) {
            drilldown.setMeasurementValueDistribution(jdbcTemplate.query(
                    sqlMeasurementValueDistribution.getSql(),
                    sqlMeasurementValueDistribution.getSetter(), new ConceptQuartileMapper()));
        }

        var sqlUpperLimitDistribution = prepareDrillDownCohortSql(
                "sqlUpperLimitDistribution",
                MEASUREMENT, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sqlUpperLimitDistribution != null) {
            drilldown.setUpperLimitDistribution(
                    jdbcTemplate.query(sqlUpperLimitDistribution.getSql(),
                            sqlUpperLimitDistribution.getSetter(),
                            new ConceptQuartileMapper()));
        }

        var sqlMeasurementsByType = prepareDrillDownCohortSql("sqlMeasurementsByType",
                MEASUREMENT, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sqlMeasurementsByType != null) {
            drilldown.setMeasurementsByType(
                    jdbcTemplate.query(sqlMeasurementsByType.getSql(),
                            sqlMeasurementsByType.getSetter(),
                            new ConceptObservationCountMapper()));
        }

        var sqlRecordsByUnit = prepareDrillDownCohortSql("sqlRecordsByUnit", MEASUREMENT,
                id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sqlRecordsByUnit != null) {
            drilldown.setRecordsByUnit(
                    jdbcTemplate.query(sqlRecordsByUnit.getSql(), sqlRecordsByUnit.getSetter(),
                            new ConceptObservationCountMapper()));
        }

        var sqlValuesRelativeToNorm = prepareDrillDownCohortSql("sqlValuesRelativeToNorm",
                MEASUREMENT, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sqlValuesRelativeToNorm != null) {
            drilldown.setValuesRelativeToNorm(
                    jdbcTemplate.query(sqlValuesRelativeToNorm.getSql(),
                            sqlValuesRelativeToNorm.getSetter(),
                            new ConceptObservationCountMapper()));
        }

        var sqlPrevalenceByGenderAgeYear = prepareDrillDownCohortSql(
                "sqlPrevalenceByGenderAgeYear", MEASUREMENT, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sqlPrevalenceByGenderAgeYear != null) {
            drilldown.setPrevalenceByGenderAgeYear(
                    jdbcTemplate.query(sqlPrevalenceByGenderAgeYear.getSql(),
                            sqlPrevalenceByGenderAgeYear.getSetter(),
                            new ConceptDecileMapper()));
        }

        List<PrevalenceRecord> prevalenceByMonth = null;
        var prevalanceMonthSql = prepareDrillDownCohortSql("sqlPrevalenceByMonth",
                MEASUREMENT,
                id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalanceMonthSql != null) {
            prevalenceByMonth = jdbcTemplate.query(prevalanceMonthSql.getSql(),
                    prevalanceMonthSql.getSetter(),
                    new PrevalanceConceptNameMapper());
        }
        drilldown.setPrevalenceByMonth(prevalenceByMonth);

        if (CollectionUtils.isNotEmpty(drilldown.getAgeAtFirstOccurrence())
                || CollectionUtils.isNotEmpty(drilldown.getLowerLimitDistribution())
                || CollectionUtils.isNotEmpty(drilldown.getMeasurementsByType())
                || CollectionUtils.isNotEmpty(drilldown.getMeasurementValueDistribution())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByGenderAgeYear())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByMonth())
                || CollectionUtils.isNotEmpty(drilldown.getRecordsByUnit())
                || CollectionUtils.isNotEmpty(drilldown.getUpperLimitDistribution())
                || CollectionUtils.isNotEmpty(drilldown.getValuesRelativeToNorm())) {
            empty = false;
        }

        if (!empty && save) {
            this.saveEntityDrilldown(id, source.getSourceId(), MEASUREMENT_DRILLDOWN, conceptId, drilldown);
        }

        return drilldown;
    }

    public CohortObservationPeriod getCohortObservationPeriod(final JdbcTemplate jdbcTemplate,
            final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {
        var obsPeriod = new CohortObservationPeriod();
        boolean empty = true;

        var ageAtFirstSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/observationperiod/ageatfirst.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (ageAtFirstSql != null) {
            obsPeriod.setAgeAtFirst(jdbcTemplate.query(ageAtFirstSql.getSql(), ageAtFirstSql.getSetter(),
                    new ConceptDistributionMapper()));
        }

        var obsLengthSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/observationperiod/observationlength_data.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (obsLengthSql != null) {
            obsPeriod.setObservationLength(
                    jdbcTemplate.query(obsLengthSql.getSql(), obsLengthSql.getSetter(),
                            new ConceptDistributionMapper()));
        }

        var obsLengthStatsSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/observationperiod/observationlength_stats.sql", id,
                minCovariatePersonCountParam,
                minIntervalPersonCountParam, source);
        if (obsLengthStatsSql != null) {
            obsPeriod.setObservationLengthStats(
                    jdbcTemplate.query(obsLengthStatsSql.getSql(), obsLengthStatsSql.getSetter(),
                            new CohortStatsMapper()));
        }

        var obsYearStatsSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/observationperiod/observedbyyear_stats.sql", id,
                minCovariatePersonCountParam,
                minIntervalPersonCountParam, source);
        if (obsYearStatsSql != null) {
            obsPeriod.setPersonsWithContinuousObservationsByYearStats(
                    jdbcTemplate.query(obsYearStatsSql.getSql(), obsYearStatsSql.getSetter(),
                            new CohortStatsMapper()));
        }

        var personsWithContObsSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/observationperiod/observedbyyear_data.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (personsWithContObsSql != null) {
            obsPeriod.setPersonsWithContinuousObservationsByYear(
                    jdbcTemplate.query(personsWithContObsSql.getSql(),
                            personsWithContObsSql.getSetter(),
                            new ConceptDistributionMapper()));
        }

        var ageByGenderSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/observationperiod/agebygender.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (ageByGenderSql != null) {
            obsPeriod.setAgeByGender(jdbcTemplate.query(ageByGenderSql.getSql(), ageByGenderSql.getSetter(),
                    new ConceptQuartileMapper()));
        }

        var durationByGenderSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/observationperiod/observationlengthbygender.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (durationByGenderSql != null) {
            obsPeriod.setDurationByGender(
                    jdbcTemplate.query(durationByGenderSql.getSql(),
                            durationByGenderSql.getSetter(),
                            new ConceptQuartileMapper()));
        }

        var durationByAgeSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/observationperiod/observationlengthbyage.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (durationByAgeSql != null) {
            obsPeriod.setDurationByAgeDecile(
                    jdbcTemplate.query(durationByAgeSql.getSql(), durationByAgeSql.getSetter(),
                            new ConceptQuartileMapper()));
        }

        PreparedStatementRenderer cumulObsSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/observationperiod/cumulativeduration.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (cumulObsSql != null) {
            obsPeriod.setCumulativeObservation(
                    jdbcTemplate.query(cumulObsSql.getSql(), cumulObsSql.getSetter(),
                            new CumulativeObservationMapper()));
        }

        var obsByMonthSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/observationperiod/observedbymonth.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (obsByMonthSql != null) {
            obsPeriod.setObservedByMonth(
                    jdbcTemplate.query(obsByMonthSql.getSql(), obsByMonthSql.getSetter(),
                            new MonthObservationMapper()));
        }

        var obsPeriodsPerPersonSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/observationperiod/periodsperperson.sql", id,
                minCovariatePersonCountParam,
                minIntervalPersonCountParam, source);
        if (obsPeriodsPerPersonSql != null) {
            obsPeriod.setObservationPeriodsPerPerson(
                    jdbcTemplate.query(obsPeriodsPerPersonSql.getSql(),
                            obsPeriodsPerPersonSql.getSetter(),
                            new ConceptCountMapper()));
        }

        if (CollectionUtils.isNotEmpty(obsPeriod.getAgeAtFirst())
                || CollectionUtils.isNotEmpty(obsPeriod.getAgeByGender())
                || CollectionUtils.isNotEmpty(obsPeriod.getCumulativeObservation())
                || CollectionUtils.isNotEmpty(obsPeriod.getDurationByAgeDecile())
                || CollectionUtils.isNotEmpty(obsPeriod.getDurationByGender())
                || CollectionUtils.isNotEmpty(obsPeriod.getObservationLength())
                || CollectionUtils.isNotEmpty(obsPeriod.getObservationLengthStats())
                || CollectionUtils.isNotEmpty(obsPeriod.getObservationPeriodsPerPerson())
                || CollectionUtils.isNotEmpty(obsPeriod.getObservedByMonth())
                || CollectionUtils.isNotEmpty(obsPeriod.getPersonsWithContinuousObservationsByYear())
                || CollectionUtils.isNotEmpty(
                obsPeriod.getPersonsWithContinuousObservationsByYearStats())) {
            empty = false;
        }

        if (!empty && save) {
            this.saveEntity(id, source.getSourceId(), OBSERVATION_PERIOD, obsPeriod);
        }

        return obsPeriod;
    }

    public List<HierarchicalConceptRecord> getCohortObservationResults(JdbcTemplate jdbcTemplate,
            final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {

        List<HierarchicalConceptRecord> res = null;

        var sql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/observation/sqlObservationTreemap.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sql != null) {
            res = jdbcTemplate.query(sql.getSql(), sql.getSetter(), new HierarchicalConceptMapper());
            if (save) {
                saveEntity(id, source.getSourceId(), OBSERVATION, res);
            }
        }

        return res;
    }

    public CohortObservationDrilldown getCohortObservationResultsDrilldown(JdbcTemplate jdbcTemplate,
            final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {
        var drilldown = new CohortObservationDrilldown();
        boolean empty = true;

        var ageAtFirstOccurrenceSql = prepareDrillDownCohortSql("sqlAgeAtFirstOccurrence",
                OBSERVATION, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (ageAtFirstOccurrenceSql != null) {
            drilldown.setAgeAtFirstOccurrence(
                    jdbcTemplate.query(ageAtFirstOccurrenceSql.getSql(),
                            ageAtFirstOccurrenceSql.getSetter(),
                            new ConceptQuartileMapper()));
        }

        var sqlObservationValueDistribution = prepareDrillDownCohortSql(
                "sqlObservationValueDistribution", OBSERVATION, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sqlObservationValueDistribution != null) {
            drilldown.setObservationValueDistribution(jdbcTemplate.query(
                    sqlObservationValueDistribution.getSql(),
                    sqlObservationValueDistribution.getSetter(), new ConceptQuartileMapper()));
        }

        var sqlObservationsByType = prepareDrillDownCohortSql("sqlObservationsByType",
                OBSERVATION, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sqlObservationsByType != null) {
            drilldown.setObservationsByType(
                    jdbcTemplate.query(sqlObservationsByType.getSql(),
                            sqlObservationsByType.getSetter(),
                            new ConceptObservationCountMapper()));
        }

        var sqlRecordsByUnit = prepareDrillDownCohortSql("sqlRecordsByUnit", OBSERVATION,
                id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sqlRecordsByUnit != null) {
            drilldown.setRecordsByUnit(
                    jdbcTemplate.query(sqlRecordsByUnit.getSql(), sqlRecordsByUnit.getSetter(),
                            new ConceptObservationCountMapper()));
        }

        var sqlPrevalenceByGenderAgeYear = prepareDrillDownCohortSql(
                "sqlPrevalenceByGenderAgeYear", OBSERVATION, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sqlPrevalenceByGenderAgeYear != null) {
            drilldown.setPrevalenceByGenderAgeYear(
                    jdbcTemplate.query(sqlPrevalenceByGenderAgeYear.getSql(),
                            sqlPrevalenceByGenderAgeYear.getSetter(),
                            new ConceptDecileMapper()));
        }

        List<PrevalenceRecord> prevalenceByMonth = null;
        var prevalanceMonthSql = prepareDrillDownCohortSql("sqlPrevalenceByMonth",
                OBSERVATION,
                id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalanceMonthSql != null) {
            prevalenceByMonth = jdbcTemplate.query(prevalanceMonthSql.getSql(),
                    prevalanceMonthSql.getSetter(),
                    new PrevalanceConceptNameMapper());
        }
        drilldown.setPrevalenceByMonth(prevalenceByMonth);

        if (CollectionUtils.isNotEmpty(drilldown.getAgeAtFirstOccurrence())
                || CollectionUtils.isNotEmpty(drilldown.getLowerLimitDistribution())
                || CollectionUtils.isNotEmpty(drilldown.getObservationsByType())
                || CollectionUtils.isNotEmpty(drilldown.getObservationValueDistribution())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByGenderAgeYear())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByMonth())
                || CollectionUtils.isNotEmpty(drilldown.getRecordsByUnit())
                || CollectionUtils.isNotEmpty(drilldown.getUpperLimitDistribution())
                || CollectionUtils.isNotEmpty(drilldown.getValuesRelativeToNorm())) {
            empty = false;
        }

        if (!empty && save) {
            this.saveEntityDrilldown(id, source.getSourceId(), OBSERVATION_DRILLDOWN, conceptId, drilldown);
        }
        return drilldown;

    }

    public List<ScatterplotRecord> getCohortProcedureDrilldown(JdbcTemplate jdbcTemplate,
            final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {

        List<ScatterplotRecord> records = new ArrayList<>();

        final var sql = prepareDrillDownCohortSql("procedureOccursRelativeToIndex",
                "cohortSpecific", id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sql != null) {
            records = jdbcTemplate.query(sql.getSql(), sql.getSetter(), new ScatterplotMapper());
            if (save) {
                saveEntityDrilldown(id, source.getSourceId(), COHORT_SPECIFIC_PROCEDURE_DRILLDOWN, conceptId, records);
            }
        }
        return records;
    }

    public CohortProceduresDrillDown getCohortProceduresDrilldown(JdbcTemplate jdbcTemplate,
            final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {
        var drilldown = new CohortProceduresDrillDown();
        boolean empty = true;

        List<ConceptQuartileRecord> ageAtFirst = null;
        var ageAtFirstSql = prepareDrillDownCohortSql("sqlAgeAtFirstOccurrence",
                PROCEDURE, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (ageAtFirstSql != null) {
            ageAtFirst = jdbcTemplate.query(ageAtFirstSql.getSql(), ageAtFirstSql.getSetter(),
                    new ConceptQuartileMapper());
        }
        drilldown.setAgeAtFirstOccurrence(ageAtFirst);

        List<ConceptCountRecord> byType = null;
        var byTypeSql = prepareDrillDownCohortSql("sqlProceduresByType", PROCEDURE, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (byTypeSql != null) {
            byType = jdbcTemplate.query(byTypeSql.getSql(), byTypeSql.getSetter(),
                    new ConceptCountMapper());
        }
        drilldown.setProceduresByType(byType);

        List<ConceptDecileRecord> prevalenceByGenderAgeYear = null;
        var prevalenceGenderAgeSql = prepareDrillDownCohortSql(
                "sqlPrevalenceByGenderAgeYear",
                PROCEDURE, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalenceGenderAgeSql != null) {
            prevalenceByGenderAgeYear = jdbcTemplate.query(prevalenceGenderAgeSql.getSql(),
                    prevalenceGenderAgeSql.getSetter(), new ConceptDecileMapper());
        }
        drilldown.setPrevalenceByGenderAgeYear(prevalenceByGenderAgeYear);

        List<PrevalenceRecord> prevalenceByMonth = null;
        var prevalanceMonthSql = prepareDrillDownCohortSql("sqlPrevalenceByMonth",
                PROCEDURE, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalanceMonthSql != null) {
            prevalenceByMonth = jdbcTemplate.query(prevalanceMonthSql.getSql(),
                    prevalanceMonthSql.getSetter(),
                    new PrevalanceConceptMapper());
        }
        drilldown.setPrevalenceByMonth(prevalenceByMonth);

        if (CollectionUtils.isNotEmpty(drilldown.getAgeAtFirstOccurrence())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByGenderAgeYear())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByMonth())
                || CollectionUtils.isNotEmpty(drilldown.getProceduresByType())) {
            empty = false;
        }

        if (!empty && save) {
            saveEntityDrilldown(id, source.getSourceId(), PROCEDURE_DRILLDOWN, conceptId, drilldown);
        }

        return drilldown;
    }

    public CohortSpecificSummary getCohortSpecificSummary(JdbcTemplate jdbcTemplate,
            final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {
        var summary = new CohortSpecificSummary();
        boolean empty = true;

        // 1805, 1806
        var personsByDurationSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/cohortSpecific/observationPeriodTimeRelativeToIndex.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (personsByDurationSql != null) {
            summary.setPersonsByDurationFromStartToEnd(
                    jdbcTemplate.query(personsByDurationSql.getSql(),
                            personsByDurationSql.getSetter(),
                            new ObservationPeriodMapper()));
        }

        // 1815
        var monthPrevalenceSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/cohortSpecific/prevalenceByMonth.sql", id,
                minCovariatePersonCountParam,
                minIntervalPersonCountParam, source);
        if (monthPrevalenceSql != null) {
            summary.setPrevalenceByMonth(
                    jdbcTemplate.query(monthPrevalenceSql.getSql(), monthPrevalenceSql.getSetter(),
                            new PrevalanceMapper()));
        }

        // 1814
        List<ConceptDecileRecord> prevalenceByGenderAgeYear = null;
        var prevalenceGenderAgeSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/cohortSpecific/prevalenceByYearGenderSex.sql", id,
                minCovariatePersonCountParam,
                minIntervalPersonCountParam, source);
        if (prevalenceGenderAgeSql != null) {
            prevalenceByGenderAgeYear = jdbcTemplate.query(prevalenceGenderAgeSql.getSql(),
                    prevalenceGenderAgeSql.getSetter(), new ConceptDecileCountsMapper());
        }
        summary.setNumPersonsByCohortStartByGenderByAge(prevalenceByGenderAgeYear);

        // 1801
        List<ConceptQuartileRecord> ageAtIndex = null;
        var ageAtIndexSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/cohortSpecific/ageAtIndexDistribution.sql", id,
                minCovariatePersonCountParam,
                minIntervalPersonCountParam, source);
        if (ageAtIndexSql != null) {
            ageAtIndex = jdbcTemplate.query(ageAtIndexSql.getSql(), ageAtIndexSql.getSetter(),
                    new ConceptQuartileMapper());
        }
        summary.setAgeAtIndexDistribution(ageAtIndex);

        // 1803
        List<ConceptQuartileRecord> distributionAgeCohortStartByCohortStartYear = null;
        var distributionAgeCohortStartByCohortStartYearSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/cohortSpecific/distributionOfAgeAtCohortStartByCohortStartYear.sql",
                id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (distributionAgeCohortStartByCohortStartYearSql != null) {
            distributionAgeCohortStartByCohortStartYear = jdbcTemplate.query(
                    distributionAgeCohortStartByCohortStartYearSql.getSql(),
                    distributionAgeCohortStartByCohortStartYearSql.getSetter(),
                    new ConceptQuartileMapper());
        }
        summary.setDistributionAgeCohortStartByCohortStartYear(distributionAgeCohortStartByCohortStartYear);

        // 1802
        List<ConceptQuartileRecord> distributionAgeCohortStartByGender = null;
        var distributionAgeCohortStartByGenderSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/cohortSpecific/distributionOfAgeAtCohortStartByGender.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (distributionAgeCohortStartByGenderSql != null) {
            distributionAgeCohortStartByGender = jdbcTemplate.query(
                    distributionAgeCohortStartByGenderSql.getSql(),
                    distributionAgeCohortStartByGenderSql.getSetter(), new ConceptQuartileMapper());
        }
        summary.setDistributionAgeCohortStartByGender(distributionAgeCohortStartByGender);

        // 1804
        var personsInCohortFromCohortStartToEndSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/cohortSpecific/personsInCohortFromCohortStartToEnd.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (personsInCohortFromCohortStartToEndSql != null) {
            summary.setPersonsInCohortFromCohortStartToEnd(
                    jdbcTemplate.query(personsInCohortFromCohortStartToEndSql.getSql(),
                            personsInCohortFromCohortStartToEndSql.getSetter(),
                            new MonthObservationMapper()));
        }

        if (CollectionUtils.isNotEmpty(summary.getAgeAtIndexDistribution())
                || CollectionUtils.isNotEmpty(summary.getDistributionAgeCohortStartByCohortStartYear())
                || CollectionUtils.isNotEmpty(summary.getDistributionAgeCohortStartByGender())
                || CollectionUtils.isNotEmpty(summary.getNumPersonsByCohortStartByGenderByAge())
                || CollectionUtils.isNotEmpty(summary.getPersonsByDurationFromStartToEnd())
                || CollectionUtils.isNotEmpty(summary.getPersonsInCohortFromCohortStartToEnd())
                || CollectionUtils.isNotEmpty(summary.getPrevalenceByMonth())) {
            empty = false;
        }

        if (!empty && save) {
            this.saveEntity(id, source.getSourceId(), COHORT_SPECIFIC, summary);
        }

        return summary;
    }

    public CohortSpecificTreemap getCohortSpecificTreemapResults(JdbcTemplate jdbcTemplate, final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam, SourceEntity source,
            boolean save) {

        var summary = new CohortSpecificTreemap();
        boolean empty = true;

        // 1820
        var conditionOccurrencePrevalenceSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/cohortSpecific/conditionOccurrencePrevalenceOfCondition.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (conditionOccurrencePrevalenceSql != null) {
            summary.setConditionOccurrencePrevalence(
                    jdbcTemplate.query(conditionOccurrencePrevalenceSql.getSql(),
                            conditionOccurrencePrevalenceSql.getSetter(),
                            new HierarchicalConceptPrevalenceMapper()));
        }

        // 1830
        var procedureOccurrencePrevalenceSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/cohortSpecific/procedureOccurrencePrevalenceOfDrug.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (procedureOccurrencePrevalenceSql != null) {
            summary.setProcedureOccurrencePrevalence(
                    jdbcTemplate.query(procedureOccurrencePrevalenceSql.getSql(),
                            procedureOccurrencePrevalenceSql.getSetter(),
                            new HierarchicalConceptPrevalenceMapper()));
        }

        // 1870
        var drugEraPrevalenceSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/cohortSpecific/drugEraPrevalenceOfDrug.sql", id,
                minCovariatePersonCountParam,
                minIntervalPersonCountParam, source);
        if (drugEraPrevalenceSql != null) {
            summary.setDrugEraPrevalence(
                    jdbcTemplate.query(drugEraPrevalenceSql.getSql(),
                            drugEraPrevalenceSql.getSetter(),
                            new HierarchicalConceptPrevalenceMapper()));
        }

        if (CollectionUtils.isNotEmpty(summary.getConditionOccurrencePrevalence())
                || CollectionUtils.isNotEmpty(summary.getProcedureOccurrencePrevalence())
                || CollectionUtils.isNotEmpty(summary.getDrugEraPrevalence())) {
            empty = false;
        }

        if (!empty && save) {
            this.saveEntity(id, source.getSourceId(), COHORT_SPECIFIC_TREEMAP, summary);
        }

        return summary;
    }

    public CohortVisitsDrilldown getCohortVisitsDrilldown(JdbcTemplate jdbcTemplate,
            final int id,
            final int conceptId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {

        var drilldown = new CohortVisitsDrilldown();
        boolean empty = true;

        List<ConceptQuartileRecord> ageAtFirst = null;
        var ageAtFirstSql = prepareDrillDownCohortSql("sqlAgeAtFirstOccurrence", VISIT,
                id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (ageAtFirstSql != null) {
            ageAtFirst = jdbcTemplate.query(ageAtFirstSql.getSql(), ageAtFirstSql.getSetter(),
                    new ConceptQuartileMapper());
        }
        drilldown.setAgeAtFirstOccurrence(ageAtFirst);

        List<ConceptQuartileRecord> byType = null;
        var byTypeSql = prepareDrillDownCohortSql("sqlVisitDurationByType", VISIT, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (byTypeSql != null) {
            byType = jdbcTemplate.query(byTypeSql.getSql(), byTypeSql.getSetter(),
                    new ConceptQuartileMapper());
        }
        drilldown.setVisitDurationByType(byType);

        List<ConceptDecileRecord> prevalenceByGenderAgeYear = null;
        var prevalenceGenderAgeSql = prepareDrillDownCohortSql(
                "sqlPrevalenceByGenderAgeYear",
                VISIT, id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalenceGenderAgeSql != null) {
            prevalenceByGenderAgeYear = jdbcTemplate.query(prevalenceGenderAgeSql.getSql(),
                    prevalenceGenderAgeSql.getSetter(), new ConceptDecileMapper());
        }
        drilldown.setPrevalenceByGenderAgeYear(prevalenceByGenderAgeYear);

        List<PrevalenceRecord> prevalenceByMonth = null;
        var prevalanceMonthSql = prepareDrillDownCohortSql("sqlPrevalenceByMonth", VISIT,
                id,
                conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalanceMonthSql != null) {
            prevalenceByMonth = jdbcTemplate.query(prevalanceMonthSql.getSql(),
                    prevalanceMonthSql.getSetter(),
                    new PrevalanceConceptMapper());
        }
        drilldown.setPrevalenceByMonth(prevalenceByMonth);

        if (CollectionUtils.isNotEmpty(drilldown.getAgeAtFirstOccurrence())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByGenderAgeYear())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByMonth())
                || CollectionUtils.isNotEmpty(drilldown.getVisitDurationByType())) {
            empty = false;
        }

        if (!empty && save) {
            this.saveEntityDrilldown(id, source.getSourceId(), VISIT_DRILLDOWN, conceptId, drilldown);
        }

        return drilldown;
    }

    public CohortConditionEraDrilldown getConditionEraDrilldown(JdbcTemplate jdbcTemplate,
            final int id,
            final int conditionId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source, boolean save) {

        var drilldown = new CohortConditionEraDrilldown();
        boolean empty = true;

        // age at first diagnosis
        List<ConceptQuartileRecord> ageAtFirst = null;
        var ageAtFirstSql = prepareDrillDownCohortSql("sqlAgeAtFirstDiagnosis",
                "conditionera",
                id,
                conditionId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (ageAtFirstSql != null) {
            ageAtFirst = jdbcTemplate.query(ageAtFirstSql.getSql(), ageAtFirstSql.getSetter(),
                    new ConceptQuartileMapper());
        }
        drilldown.setAgeAtFirstDiagnosis(ageAtFirst);

        // length of era
        List<ConceptQuartileRecord> lengthOfEra = null;
        var lengthOfEraSql = prepareDrillDownCohortSql("sqlLengthOfEra", "conditionera",
                id,
                conditionId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (lengthOfEraSql != null) {
            lengthOfEra = jdbcTemplate.query(lengthOfEraSql.getSql(), lengthOfEraSql.getSetter(),
                    new ConceptQuartileMapper());
        }
        drilldown.setLengthOfEra(lengthOfEra);

        // prevalence by gender age year
        List<ConceptDecileRecord> prevalenceByGenderAgeYear = null;
        var prevalenceByGenderAgeYearSql = prepareDrillDownCohortSql(
                "sqlPrevalenceByGenderAgeYear", "conditionera", id,
                conditionId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalenceByGenderAgeYearSql != null) {
            prevalenceByGenderAgeYear = jdbcTemplate.query(prevalenceByGenderAgeYearSql.getSql(),
                    prevalenceByGenderAgeYearSql.getSetter(), new ConceptDecileMapper());
        }
        drilldown.setPrevalenceByGenderAgeYear(prevalenceByGenderAgeYear);

        // prevalence by month
        List<PrevalenceRecord> prevalenceByMonth = null;
        var prevalenceByMonthSql = prepareDrillDownCohortSql("sqlPrevalenceByMonth",
                "conditionera", id,
                conditionId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalenceByMonthSql != null) {
            prevalenceByMonth = jdbcTemplate.query(prevalenceByMonthSql.getSql(),
                    prevalenceByMonthSql.getSetter(),
                    new PrevalanceConceptMapper());
        }
        drilldown.setPrevalenceByMonth(prevalenceByMonth);

        if (CollectionUtils.isNotEmpty(drilldown.getAgeAtFirstDiagnosis())
                || CollectionUtils.isNotEmpty(drilldown.getLengthOfEra())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByGenderAgeYear())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByMonth())) {
            empty = false;
        }

        if (!empty && save) {
            this.saveEntityDrilldown(id, source.getSourceId(), CONDITION_ERA_DRILLDOWN, conditionId, drilldown);
        }

        return drilldown;
    }

    public List<HierarchicalConceptRecord> getConditionEraTreemap(JdbcTemplate jdbcTemplate,
            final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source, boolean save) {

        List<HierarchicalConceptRecord> res = null;

        var sql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/conditionera/sqlConditionEraTreemap.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sql != null) {
            res = jdbcTemplate.query(sql.getSql(), sql.getSetter(), new HierarchicalConceptEraMapper());
            if (save) {
                saveEntity(id, source.getSourceId(), CONDITION_ERA, res);
            }
        }

        return res;
    }

    public CohortConditionDrilldown getConditionResults(JdbcTemplate jdbcTemplate,
            final int id,
            final int conditionId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {

        var drilldown = new CohortConditionDrilldown();
        boolean empty = true;

        List<ConceptQuartileRecord> ageAtFirstDiagnosis = null;
        var ageAtFirstSql = prepareDrillDownCohortSql("sqlAgeAtFirstDiagnosis", CONDITION,
                id,
                conditionId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (ageAtFirstSql != null) {
            ageAtFirstDiagnosis = jdbcTemplate.query(ageAtFirstSql.getSql(), ageAtFirstSql.getSetter(),
                    new ConceptQuartileMapper());
        }
        drilldown.setAgeAtFirstDiagnosis(ageAtFirstDiagnosis);

        List<ConceptCountRecord> conditionsByType = null;
        var conditionsSql = prepareDrillDownCohortSql("sqlConditionsByType", CONDITION,
                id,
                conditionId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (conditionsSql != null) {
            conditionsByType = jdbcTemplate.query(conditionsSql.getSql(), conditionsSql.getSetter(),
                    new ConceptConditionCountMapper());
        }
        drilldown.setConditionsByType(conditionsByType);

        List<ConceptDecileRecord> prevalenceByGenderAgeYear = null;
        var prevalenceGenderAgeSql = prepareDrillDownCohortSql(
                "sqlPrevalenceByGenderAgeYear",
                CONDITION, id,
                conditionId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalenceGenderAgeSql != null) {
            prevalenceByGenderAgeYear = jdbcTemplate.query(prevalenceGenderAgeSql.getSql(),
                    prevalenceGenderAgeSql.getSetter(), new ConceptDecileMapper());
        }
        drilldown.setPrevalenceByGenderAgeYear(prevalenceByGenderAgeYear);

        List<PrevalenceRecord> prevalenceByMonth = null;
        var prevalanceMonthSql = prepareDrillDownCohortSql("sqlPrevalenceByMonth",
                CONDITION, id,
                conditionId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalanceMonthSql != null) {
            prevalenceByMonth = jdbcTemplate.query(prevalanceMonthSql.getSql(),
                    prevalanceMonthSql.getSetter(),
                    new PrevalanceConceptMapper());
        }
        drilldown.setPrevalenceByMonth(prevalenceByMonth);

        if (CollectionUtils.isNotEmpty(drilldown.getAgeAtFirstDiagnosis())
                || CollectionUtils.isNotEmpty(drilldown.getConditionsByType())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByGenderAgeYear())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByMonth())) {
            empty = false;
        }

        if (!empty && save) {
            this.saveEntityDrilldown(id, source.getSourceId(), CONDITION_DRILLDOWN, conditionId, drilldown);
        }
        return drilldown;
    }

    public List<HierarchicalConceptRecord> getConditionTreemap(JdbcTemplate jdbcTemplate, final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source, boolean save) {

        List<HierarchicalConceptRecord> res = null;

        var sql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/condition/sqlConditionTreemap.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sql != null) {
            res = jdbcTemplate.query(sql.getSql(), sql.getSetter(), new HierarchicalConceptMapper());
            if (save) {
                saveEntity(id, source.getSourceId(), CONDITION, res);
            }
        }

        return res;
    }

    public CohortDashboard getDashboard(JdbcTemplate jdbcTemplate,
            int id, SourceEntity source,
            Integer minCovariatePersonCountParam, Integer minIntervalPersonCountParam,
            boolean demographicsOnly,
            boolean save) {

        var dashboard = new CohortDashboard();
        boolean empty = true;

        var ageAtFirstObsSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/observationperiod/ageatfirst.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (ageAtFirstObsSql != null) {
            dashboard.setAgeAtFirstObservation(
                    jdbcTemplate.query(ageAtFirstObsSql.getSql(), ageAtFirstObsSql.getSetter(),
                            new ConceptDistributionMapper()));
        }

        var genderSql = renderTranslateCohortSql(BASE_SQL_PATH + "/person/gender.sql", id,
                minCovariatePersonCountParam,
                minIntervalPersonCountParam, source);
        if (genderSql != null) {
            dashboard.setGender(
                    jdbcTemplate.query(genderSql.getSql(), genderSql.getSetter(),
                            new ConceptCountMapper()));
        }

        if (!demographicsOnly) {
            PreparedStatementRenderer cumulObsSql = renderTranslateCohortSql(
                    BASE_SQL_PATH + "/observationperiod/cumulativeduration.sql", id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source);
            if (cumulObsSql != null) {
                dashboard.setCumulativeObservation(
                        jdbcTemplate.query(cumulObsSql.getSql(), cumulObsSql.getSetter(),
                                new CumulativeObservationMapper()));
            }

            var obsByMonthSql = renderTranslateCohortSql(
                    BASE_SQL_PATH + "/observationperiod/observedbymonth.sql", id,
                    minCovariatePersonCountParam, minIntervalPersonCountParam, source);
            if (obsByMonthSql != null) {
                dashboard.setObservedByMonth(
                        jdbcTemplate.query(obsByMonthSql.getSql(), obsByMonthSql.getSetter(),
                                new MonthObservationMapper()));
            }
        }

        if (CollectionUtils.isNotEmpty(dashboard.getAgeAtFirstObservation())
                || CollectionUtils.isNotEmpty(dashboard.getCumulativeObservation())
                || CollectionUtils.isNotEmpty(dashboard.getGender())
                || CollectionUtils.isNotEmpty(dashboard.getObservedByMonth())) {
            empty = false;
        }

        if (!empty && save) {
            this.saveEntity(id, source.getSourceId(), DASHBOARD, dashboard);
        }

        return dashboard;

    }

    public CohortDrugEraDrilldown getDrugEraResults(JdbcTemplate jdbcTemplate,
            final int id,
            final int drugId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {

        var drilldown = new CohortDrugEraDrilldown();
        boolean empty = true;

        // age at first exposure
        List<ConceptQuartileRecord> ageAtFirstExposure = null;
        var ageAtFirstExposureSql = prepareDrillDownCohortSql("sqlAgeAtFirstExposure",
                "drugera",
                id,
                drugId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (ageAtFirstExposureSql != null) {
            ageAtFirstExposure = jdbcTemplate.query(ageAtFirstExposureSql.getSql(),
                    ageAtFirstExposureSql.getSetter(),
                    new ConceptQuartileMapper());
        }
        drilldown.setAgeAtFirstExposure(ageAtFirstExposure);

        // length of era
        List<ConceptQuartileRecord> lengthOfEra = null;
        var lengthOfEraSql = prepareDrillDownCohortSql("sqlLengthOfEra", "drugera", id,
                drugId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (lengthOfEraSql != null) {
            lengthOfEra = jdbcTemplate.query(lengthOfEraSql.getSql(), lengthOfEraSql.getSetter(),
                    new ConceptQuartileMapper());
        }
        drilldown.setLengthOfEra(lengthOfEra);

        // prevalence by gender age year
        List<ConceptDecileRecord> prevalenceByGenderAgeYear = null;
        var prevalenceByGenderAgeYearSql = prepareDrillDownCohortSql(
                "sqlPrevalenceByGenderAgeYear", "drugera", id,
                drugId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalenceByGenderAgeYearSql != null) {
            prevalenceByGenderAgeYear = jdbcTemplate.query(prevalenceByGenderAgeYearSql.getSql(),
                    prevalenceByGenderAgeYearSql.getSetter(), new ConceptDecileMapper());
        }
        drilldown.setPrevalenceByGenderAgeYear(prevalenceByGenderAgeYear);

        // prevalence by month
        List<PrevalenceRecord> prevalenceByMonth = null;
        var prevalenceByMonthSql = prepareDrillDownCohortSql("sqlPrevalenceByMonth",
                "drugera",
                id,
                drugId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalenceByMonthSql != null) {
            prevalenceByMonth = jdbcTemplate.query(prevalenceByMonthSql.getSql(),
                    prevalenceByMonthSql.getSetter(),
                    new PrevalanceConceptMapper());
        }
        drilldown.setPrevalenceByMonth(prevalenceByMonth);

        if (CollectionUtils.isNotEmpty(drilldown.getAgeAtFirstExposure())
                || CollectionUtils.isNotEmpty(drilldown.getLengthOfEra())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByGenderAgeYear())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByMonth())) {
            empty = false;
        }

        if (!empty && save) {
            this.saveEntityDrilldown(id, source.getSourceId(), DRUG_ERA_DRILLDOWN, drugId, drilldown);
        }

        return drilldown;

    }

    public List<HierarchicalConceptRecord> getDrugEraTreemap(JdbcTemplate jdbcTemplate,
            final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {

        List<HierarchicalConceptRecord> res = null;

        var sql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/drugera/sqlDrugEraTreemap.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sql != null) {
            res = jdbcTemplate.query(sql.getSql(), sql.getSetter(), new HierarchicalConceptEraMapper());
            if (save) {
                saveEntity(id, source.getSourceId(), DRUG_ERA, res);
            }
        }

        return res;
    }

    public CohortDrugDrilldown getDrugResults(JdbcTemplate jdbcTemplate,
            final int id,
            final int drugId,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {

        var drilldown = new CohortDrugDrilldown();
        boolean empty = true;

        var ageAtFirstExposureSql = prepareDrillDownCohortSql("sqlAgeAtFirstExposure",
                DRUG, id,
                drugId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (ageAtFirstExposureSql != null) {
            drilldown.setAgeAtFirstExposure(
                    jdbcTemplate.query(ageAtFirstExposureSql.getSql(),
                            ageAtFirstExposureSql.getSetter(),
                            new ConceptQuartileMapper()));
        }

        var daysSupplySql = prepareDrillDownCohortSql("sqlDaysSupplyDistribution", DRUG,
                id,
                drugId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (daysSupplySql != null) {
            drilldown.setDaysSupplyDistribution(
                    jdbcTemplate.query(daysSupplySql.getSql(), daysSupplySql.getSetter(),
                            new ConceptQuartileMapper()));
        }

        var drugsByTypeSql = prepareDrillDownCohortSql("sqlDrugsByType", DRUG, id,
                drugId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (drugsByTypeSql != null) {
            drilldown.setDrugsByType(
                    jdbcTemplate.query(drugsByTypeSql.getSql(), drugsByTypeSql.getSetter(),
                            new ConceptCountMapper()));
        }

        var prevalenceByGenderAgeYearSql = prepareDrillDownCohortSql(
                "sqlPrevalenceByGenderAgeYear", DRUG, id,
                drugId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalenceByGenderAgeYearSql != null) {
            drilldown.setPrevalenceByGenderAgeYear(
                    jdbcTemplate.query(prevalenceByGenderAgeYearSql.getSql(),
                            prevalenceByGenderAgeYearSql.getSetter(),
                            new ConceptDecileMapper()));
        }

        var prevalenceByMonthSql = prepareDrillDownCohortSql("sqlPrevalenceByMonth", DRUG,
                id,
                drugId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (prevalenceByMonthSql != null) {
            drilldown.setPrevalenceByMonth(
                    jdbcTemplate.query(prevalenceByMonthSql.getSql(),
                            prevalenceByMonthSql.getSetter(),
                            new PrevalanceConceptMapper()));
        }

        var quantityDistributionSql = prepareDrillDownCohortSql("sqlQuantityDistribution",
                DRUG,
                id,
                drugId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (quantityDistributionSql != null) {
            drilldown.setQuantityDistribution(
                    jdbcTemplate.query(quantityDistributionSql.getSql(),
                            quantityDistributionSql.getSetter(),
                            new ConceptQuartileMapper()));
        }

        var refillsDistributionSql = prepareDrillDownCohortSql("sqlRefillsDistribution",
                DRUG, id,
                drugId, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (refillsDistributionSql != null) {
            drilldown.setRefillsDistribution(
                    jdbcTemplate.query(refillsDistributionSql.getSql(),
                            refillsDistributionSql.getSetter(),
                            new ConceptQuartileMapper()));
        }

        if (CollectionUtils.isNotEmpty(drilldown.getAgeAtFirstExposure())
                || CollectionUtils.isNotEmpty(drilldown.getDaysSupplyDistribution())
                || CollectionUtils.isNotEmpty(drilldown.getDrugsByType())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByGenderAgeYear())
                || CollectionUtils.isNotEmpty(drilldown.getPrevalenceByMonth())
                || CollectionUtils.isNotEmpty(drilldown.getQuantityDistribution())
                || CollectionUtils.isNotEmpty(drilldown.getRefillsDistribution())) {
            empty = false;
        }

        if (!empty && save) {
            saveEntityDrilldown(id, source.getSourceId(), DRUG_DRILLDOWN, drugId, drilldown);
        }

        return drilldown;

    }

    public List<HierarchicalConceptRecord> getDrugTreemap(JdbcTemplate jdbcTemplate,
            final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source, boolean save) {

        List<HierarchicalConceptRecord> res = null;

        var sql = renderTranslateCohortSql(BASE_SQL_PATH + "/drug/sqlDrugTreemap.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sql != null) {
            res = jdbcTemplate.query(sql.getSql(), sql.getSetter(), new HierarchicalConceptMapper());
            if (save) {
                saveEntity(id, source.getSourceId(), DRUG, res);
            }
        }

        return res;
    }

    public List<CohortAttribute> getHeraclesHeel(JdbcTemplate jdbcTemplate,
            final int id,
            SourceEntity source,
            boolean save) {
        List<CohortAttribute> attrs = new ArrayList<>();

        var sql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/heraclesHeel/sqlHeraclesHeel.sql",
                id, null, null, source);
        if (sql != null) {
            attrs = jdbcTemplate.query(sql.getSql(), sql.getSetter(), new CohortAttributeMapper());
        }
        if (save) {
            saveEntity(id, source.getSourceId(), HERACLES_HEEL, attrs);
        }

        return attrs;
    }

    /**
     * Queries for cohort analysis person results for the given cohort definition id
     *
     * @param id
     *         cohort_defintion id
     * @return CohortPersonSummary
     */
    public CohortPersonSummary getPersonResults(JdbcTemplate jdbcTemplate,
            final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            final SourceEntity source,
            boolean save) {

        var person = new CohortPersonSummary();
        boolean empty = true;

        var yobSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/person/yearofbirth_data.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (yobSql != null) {
            person.setYearOfBirth(
                    jdbcTemplate.query(yobSql.getSql(), yobSql.getSetter(),
                            new ConceptDistributionMapper()));
        }

        var yobStatSql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/person/yearofbirth_stats.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (yobStatSql != null) {
            person.setYearOfBirthStats(
                    jdbcTemplate.query(yobStatSql.getSql(), yobStatSql.getSetter(),
                            new CohortStatsMapper()));
        }

        var genderSql = renderTranslateCohortSql(BASE_SQL_PATH + "/person/gender.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (genderSql != null) {
            person.setGender(jdbcTemplate.query(genderSql.getSql(), genderSql.getSetter(),
                    new ConceptCountMapper()));
        }

        var raceSql = renderTranslateCohortSql(BASE_SQL_PATH + "/person/race.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (raceSql != null) {
            person.setRace(jdbcTemplate.query(raceSql.getSql(), raceSql.getSetter(),
                    new ConceptCountMapper()));
        }

        var ethnicitySql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/person/ethnicity.sql", id,
                minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (ethnicitySql != null) {
            person.setEthnicity(
                    jdbcTemplate.query(ethnicitySql.getSql(), ethnicitySql.getSetter(),
                            new ConceptCountMapper()));
        }

        if (CollectionUtils.isNotEmpty(person.getEthnicity())
                || CollectionUtils.isNotEmpty(person.getGender())
                || CollectionUtils.isNotEmpty(person.getRace())
                || CollectionUtils.isNotEmpty(person.getYearOfBirth())
                || CollectionUtils.isNotEmpty(person.getYearOfBirthStats())) {
            empty = false;
        }

        if (!empty && save) {
            this.saveEntity(id, source.getSourceId(), PERSON, person);
        }

        return person;
    }

    public List<HierarchicalConceptRecord> getProcedureTreemap(JdbcTemplate jdbcTemplate,
            final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {

        List<HierarchicalConceptRecord> res = null;

        var sql = renderTranslateCohortSql(
                BASE_SQL_PATH + "/procedure/sqlProcedureTreemap.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sql != null) {
            res = jdbcTemplate.query(sql.getSql(), sql.getSetter(), new HierarchicalConceptMapper());
            if (save) {
                saveEntity(id, source.getSourceId(), PROCEDURE, res);
            }
        }

        return res;
    }

    public List<HierarchicalConceptRecord> getVisitTreemap(JdbcTemplate jdbcTemplate,
            final int id,
            final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam,
            SourceEntity source,
            boolean save) {

        List<HierarchicalConceptRecord> res = null;

        var sql = renderTranslateCohortSql(BASE_SQL_PATH + "/visit/sqlVisitTreemap.sql",
                id, minCovariatePersonCountParam, minIntervalPersonCountParam, source);
        if (sql != null) {
            res = jdbcTemplate.query(sql.getSql(), sql.getSetter(), new HierarchicalConceptMapper());
            if (save) {
                saveEntity(id, source.getSourceId(), VISIT, res);
            }
        }

        return res;
    }

    protected PreparedStatementRenderer prepareDrillDownCohortSql(
            String analysisName, String analysisType, Integer id, Integer conceptId,
            final Integer minCovariatePersonCountParam, final Integer minIntervalPersonCountParam,
            SourceEntity source) {

        var sqlPath = BASE_SQL_PATH + "/" + analysisType + "/byConcept/" + analysisName + ".sql";
        return prepareCohortSql(sqlPath, id, conceptId, minCovariatePersonCountParam,
                minIntervalPersonCountParam, source);
    }

    protected PreparedStatementRenderer prepareCohortSql(
            String sqlPath, Integer id, Integer conceptId, final Integer minCovariatePersonCountParam,
            final Integer minIntervalPersonCountParam, SourceEntity source) {

        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var vocabularyTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);

        var search = new String[] { "vocab_database_schema", "ohdsi_database_schema" };
        var replace = new String[] { vocabularyTableQualifier, resultsTableQualifier };

        String[] cols;
        Object[] colValues;
        if (conceptId != null) {
            cols = new String[] { "cohortDefinitionId", "minCovariatePersonCount", "minIntervalPersonCount",
                    "conceptId" };

            colValues = new Integer[] { id,
                    minCovariatePersonCountParam == null ? MIN_COVARIATE_PERSON_COUNT
                            : minCovariatePersonCountParam,
                    minIntervalPersonCountParam == null ? MIN_INTERVAL_PERSON_COUNT
                            : minIntervalPersonCountParam,
                    conceptId };
        } else {
            cols = new String[] { "cohortDefinitionId", "minCovariatePersonCount",
                    "minIntervalPersonCount" };

            colValues = new Integer[] { id,
                    minCovariatePersonCountParam == null ? MIN_COVARIATE_PERSON_COUNT
                            : minCovariatePersonCountParam,
                    minIntervalPersonCountParam == null ? MIN_INTERVAL_PERSON_COUNT
                            : minIntervalPersonCountParam };
        }

        return new PreparedStatementRenderer(source, sqlPath, search, replace, cols, colValues);
    }

    /**
     * Passes in common params for cohort results, and performs SQL translate/render
     */
    public PreparedStatementRenderer renderTranslateCohortSql(String sqlPath, Integer id,
            final Integer minCovariatePersonCountParam, final Integer minIntervalPersonCountParam,
            SourceEntity source) {
        return prepareCohortSql(sqlPath, id, null, minCovariatePersonCountParam, minIntervalPersonCountParam,
                source);
    }

    private void saveEntity(int cohortDefinitionId, int sourceId, String visualizationKey, Object dataObject) {
        if (dataObject == null) {
            log.error("Cannot store null entity {}", visualizationKey);
            return;
        }

        if (dataObject instanceof List) {
            List<?> listObject = (List<?>) dataObject;
            if (listObject.size() == 0) {
                log.warn("No need to store empty list for {}", visualizationKey);
                return;
            }
        }

        // delete the old one
        try {
            this.visualizationDataRepository
                    .deleteByCohortDefinitionIdAndSourceIdAndVisualizationKey(cohortDefinitionId,
                            sourceId,
                            visualizationKey);

            // delete any 'drilldown'
            var drilldownData = this.visualizationDataRepository
                    .findDistinctVisualizationDataByCohortDefinitionIdAndSourceIdAndVisualizationKey(
                            cohortDefinitionId,
                            sourceId, visualizationKey + "_drilldown");
            if (CollectionUtils.isNotEmpty(drilldownData)) {
                for (VisualizationDataEntity drilldownItem : drilldownData) {
                    this.visualizationDataRepository.delete(drilldownItem);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        // save entity
        try {

            var entity = new VisualizationDataEntity();
            entity.setCohortDefinitionId(cohortDefinitionId);
            entity.setSourceId(sourceId);
            entity.setVisualizationKey(visualizationKey);
            entity.setEndTime(new Date());

            var dataString = mapper.writeValueAsString(dataObject);
            entity.setData(dataString);

            this.visualizationDataRepository.save(entity);
        } catch (Exception e) {
            log.error(whitelist(e));
        }
    }

    private void saveEntityDrilldown(int cohortDefinitionId, int sourceId, String visualizationKey, int drilldownId,
            Object dataObject) {
        if (dataObject == null) {
            log.error("Cannot store null entity {}", visualizationKey);
            return;
        }

        if (dataObject instanceof List) {
            List<?> listObject = (List<?>) dataObject;
            if (listObject.size() == 0) {
                log.warn("No need to store empty list for {}", visualizationKey);
                return;
            }
        }

        // delete the old one
        try {
            this.visualizationDataRepository
                    .deleteByCohortDefinitionIdAndSourceIdAndVisualizationKeyAndDrilldownId(
                            cohortDefinitionId,
                            sourceId, visualizationKey, drilldownId);
        } catch (Exception e) {
            log.error(whitelist(e));
        }

        // save entity
        try {
            var entity = new VisualizationDataEntity();
            entity.setCohortDefinitionId(cohortDefinitionId);
            entity.setSourceId(sourceId);
            entity.setVisualizationKey(visualizationKey);
            entity.setDrilldownId(drilldownId);
            entity.setEndTime(new Date());

            var dataString = mapper.writeValueAsString(dataObject);
            entity.setData(dataString);

            this.visualizationDataRepository.save(entity);
        } catch (Exception e) {
            log.error(whitelist(e));
        }
    }

    public int warmupData(JdbcTemplate jdbcTemplate, CohortAnalysisTask task) {
        int count = 0;
        if (task != null && task.getCohortDefinitionIds() != null && task.getVisualizations() != null) {
            for (var id : task.getCohortDefinitionIds()) {
                for (var viz : task.getVisualizations()) {
                    if (DEFAULT.equals(viz) || DASHBOARD.equals(viz)) {
                        getDashboard(jdbcTemplate, Integer.valueOf(id), task.getSource(), null,
                                null, false, true);
                        count++;
                    } else if (COHORT_SPECIFIC.equals(viz)) {
                        getCohortSpecificSummary(jdbcTemplate, Integer.valueOf(id), null, null,
                                task.getSource(), true);
                        count++;
                        // treemaps are separate
                        getCohortSpecificTreemapResults(jdbcTemplate, Integer.valueOf(id), null,
                                null, task.getSource(),
                                true);
                        count++;
                    } else if (CONDITION.equals(viz)) {
                        getConditionTreemap(jdbcTemplate, Integer.valueOf(id), null, null,
                                task.getSource(), true);
                        count++;
                    } else if (CONDITION_ERA.equals(viz)) {
                        getConditionEraTreemap(jdbcTemplate, Integer.valueOf(id), null, null,
                                task.getSource(), true);
                        count++;
                    } else if (DRUG.equals(viz) || DRUG_EXPOSURE.equals(viz)) {
                        getDrugTreemap(jdbcTemplate, Integer.valueOf(id), null, null,
                                task.getSource(), true);
                        count++;
                    } else if (DRUG_ERA.equals(viz)) {
                        getDrugEraTreemap(jdbcTemplate, Integer.valueOf(id), null, null,
                                task.getSource(), true);
                        count++;
                    } else if (PERSON.equals(viz)) {
                        getPersonResults(jdbcTemplate, Integer.valueOf(id), null, null,
                                task.getSource(), true);
                        count++;
                    } else if (OBSERVATION.equals(viz)) {
                        getCohortObservationResults(jdbcTemplate, Integer.valueOf(id), null,
                                null, task.getSource(),
                                true);
                        count++;
                    } else if (MEASUREMENT.equals(viz)) {
                        getCohortMeasurementResults(jdbcTemplate, Integer.valueOf(id), null,
                                null, task.getSource(),
                                true);
                        count++;
                    } else if (OBSERVATION_PERIOD.equals(viz) || OBSERVATION_PERIODS.equals(viz)) {
                        getCohortObservationPeriod(jdbcTemplate, Integer.valueOf(id), null,
                                null, task.getSource(),
                                true);
                        count++;
                    } else if (DATA_DENSITY.equals(viz)) {
                        getCohortDataDensity(jdbcTemplate, Integer.valueOf(id), null, null,
                                task.getSource(), true);
                        count++;
                    } else if (PROCEDURE.equals(viz)) {
                        getProcedureTreemap(jdbcTemplate, Integer.valueOf(id), null, null,
                                task.getSource(), true);
                        count++;
                    } else if (VISIT.equals(viz)) {
                        getVisitTreemap(jdbcTemplate, Integer.valueOf(id), null, null,
                                task.getSource(), true);
                        count++;
                    } else if (DEATH.equals(viz)) {
                        getCohortDeathData(jdbcTemplate, Integer.valueOf(id), null, null,
                                task.getSource(), true);
                        count++;
                    } else if (HERACLES_HEEL.equals(viz)) {
                        getHeraclesHeel(jdbcTemplate, Integer.valueOf(id), task.getSource(),
                                true);
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /* Healthcare Utilizaton Reports */

    public HealthcareExposureReport getHealthcareExposureReport(JdbcTemplate jdbcTemplate, final int cohortId,
            final WindowType window, final PeriodType periodType, SourceEntity source) {
        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        int windowAnalysisId = getSubjectAnalysisIdByWindowType(window);

        var search = new String[] { "results_schema" };
        var replace = new String[] { resultsTableQualifier };

        var report = new HealthcareExposureReport();

        var summaryPath = BASE_SQL_PATH + "/healthcareutilization/getExposureSummary.sql";
        var summaryCols = new String[] { "cohort_definition_id", "analysis_id" };
        var summaryColVals = new Object[] { cohortId, windowAnalysisId };

        var summaryPsr = new PreparedStatementRenderer(source, summaryPath, search,
                replace,
                summaryCols, summaryColVals);

        report.summary = jdbcTemplate.query(summaryPsr.getSql(), summaryPsr.getSetter(), (rs, rowNum) -> {
            HealthcareExposureReport.Summary s = new HealthcareExposureReport.Summary();
            s.personsCount = rs.getLong("person_total");
            s.exposureTotal = BigDecimal.valueOf(rs.getDouble("exposure_years_total")).setScale(2,
                    RoundingMode.HALF_UP);
            s.exposureAvg = BigDecimal.valueOf(rs.getDouble("exposure_avg_years_1k")).setScale(2,
                    RoundingMode.HALF_UP);
            return s;
        }).get(0);

        var dataPath = BASE_SQL_PATH + "/healthcareutilization/getExposureData.sql";
        var dataCols = new String[] { "cohort_definition_id", "analysis_id", "period_type" };
        var dataColVals = new Object[] { cohortId, windowAnalysisId, periodType.toString().toLowerCase() };

        var dataPsr = new PreparedStatementRenderer(source, dataPath, search, replace,
                dataCols,
                dataColVals);

        report.data = jdbcTemplate.query(dataPsr.getSql(), dataPsr.getSetter(), (rs, rowNum) -> {
            HealthcareExposureReport.ReportItem item = new HealthcareExposureReport.ReportItem();
            item.periodType = rs.getString("period_type");
            item.periodStart = rs.getDate("period_start_date");
            item.periodEnd = rs.getDate("period_end_date");
            item.personsCount = rs.getLong("person_total");
            item.personsPct = BigDecimal.valueOf(rs.getDouble("person_percent")).setScale(2,
                    RoundingMode.HALF_UP);
            item.exposureTotal = BigDecimal.valueOf(rs.getDouble("exposure_years_total")).setScale(2,
                    RoundingMode.HALF_UP);
            item.exposurePct = BigDecimal.valueOf(rs.getDouble("exposure_percent")).setScale(2,
                    RoundingMode.HALF_UP);
            item.exposureAvg = BigDecimal.valueOf(rs.getDouble("exposure_avg_years_1k")).setScale(2,
                    RoundingMode.HALF_UP);
            return item;
        });

        return report;
    }

    private int getSubjectAnalysisIdByWindowType(final WindowType window) {
        if (window == WindowType.BASELINE) {
            return 4000;
        } else if (window == WindowType.AT_RISK) {
            return 4006;
        } else {
            throw new RuntimeException("Invalid window type: " + window);
        }
    }

    public HealthcareVisitUtilizationReport getHealthcareVisitReport(JdbcTemplate jdbcTemplate, final int cohortId,
            final WindowType window, VisitStatType visitStat, final PeriodType periodType,
            final Long visitConceptId, final Long visitTypeConceptId,
            final Long costTypeConceptId, SourceEntity source) {
        var vocabularyTableQualifier = source.getTableQualifierOrNull(SourceDaimonEntity.DaimonType.Vocabulary);
        if (vocabularyTableQualifier == null) {
            vocabularyTableQualifier = source.getTableQualifierOrNull(SourceDaimonEntity.DaimonType.CDM);
        }

        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        int subjectsAnalysisId;
        int subjectWithRecordsAnalysisId;
        int visitStatAnalysisId;
        int losAnalysisId;
        int costAnalysisId;

        // set appropriate analysis IDs
        if (window == WindowType.BASELINE) {
            subjectsAnalysisId = 4000;
            subjectWithRecordsAnalysisId = 4001;
            losAnalysisId = 4005;
            costAnalysisId = 4020;
            visitStatAnalysisId = switch (visitStat) {
                case OCCURRENCE -> 4002;
                case VISIT_DATE -> 4003;
                case CARESITE_DATE -> 4004;
                default -> throw new RuntimeException("Invalid visitStat: " + visitStat);
            };
        } else if (window == WindowType.AT_RISK) {
            subjectsAnalysisId = 4006;
            subjectWithRecordsAnalysisId = 4007;
            losAnalysisId = 4011;
            costAnalysisId = 4021;
            visitStatAnalysisId = switch (visitStat) {
                case OCCURRENCE -> 4008;
                case VISIT_DATE -> 4009;
                case CARESITE_DATE -> 4010;
                default -> throw new RuntimeException("Invalid visitStat: " + visitStat);
            };
        } else {
            throw new RuntimeException("Invalid window type: " + window);
        }

        var search = new String[] { "vocabulary_schema", "results_schema" };
        var replace = new String[] { vocabularyTableQualifier, resultsTableQualifier };

        var report = new HealthcareVisitUtilizationReport();

        var reportPath = BASE_SQL_PATH + "/healthcareutilization/getVisitUtilization.sql";
        var reportSql = resourceReader.getText(reportPath);

        var visitConceptIdStr = visitConceptId == null ? "" : visitConceptId.toString();
        var visitTypeConceptIdStr = visitTypeConceptId == null ? "" : visitTypeConceptId.toString();

        var summarySql = SqlRender.renderSql(reportSql,
                new String[] { "is_summary", "visit_concept_id", "visit_type_concept_id" },
                new String[] { "TRUE", visitConceptIdStr, visitTypeConceptIdStr });

        var reportCols = new String[] { "cohort_definition_id", "subjects_analysis_id",
                "subject_with_records_analysis_id", "visit_stat_analysis_id", "los_analysis_id",
                "cost_analysis_id", "visit_concept_id", "visit_type_concept_id", "cost_type_concept_id",
                "period_type"
        };
        var colVals = new Object[] { cohortId, subjectsAnalysisId, subjectWithRecordsAnalysisId,
                visitStatAnalysisId, losAnalysisId, costAnalysisId, visitConceptIdStr,
                visitTypeConceptIdStr, costTypeConceptId == null ? "" : costTypeConceptId.toString(),
                periodType.toString().toLowerCase()
        };

        var summaryPsr = new PreparedStatementRenderer(source, summarySql, search,
                replace,
                reportCols, colVals);
        var summaryRows = jdbcTemplate.query(summaryPsr.getSql(),
                summaryPsr.getSetter(), (rs, rowNum) -> {
                    HealthcareVisitUtilizationReport.Summary s = new HealthcareVisitUtilizationReport.Summary();
                    s.personsCount = rs.getLong("person_total");
                    s.personsPct = BigDecimal.valueOf(rs.getDouble("person_percent")).setScale(2,
                            RoundingMode.HALF_UP);
                    s.visitsCount = rs.getLong("records_total");
                    s.visitsPer1000 = BigDecimal.valueOf(rs.getDouble("records_per_1000")).setScale(2,
                            RoundingMode.HALF_UP);
                    s.visitsPer1000WithVisits = BigDecimal.valueOf(rs.getDouble("records_per_1000_with_record")).setScale(2,
                            RoundingMode.HALF_UP);
                    s.visitsPer1000PerYear = BigDecimal.valueOf(rs.getDouble("records_per_1000_per_year")).setScale(2,
                            RoundingMode.HALF_UP);
                    s.lengthOfStayTotal = rs.getLong("los_total");
                    s.lengthOfStayAvg = rs.getString("los_average") == null
                            ? null
                            : BigDecimal.valueOf(rs.getDouble("los_average")).setScale(2,
                                    RoundingMode.HALF_UP);
                    s.allowed = rs.getString("allowed") == null
                            ? null
                            : BigDecimal.valueOf(rs.getDouble("allowed")).setScale(2,
                                    RoundingMode.HALF_UP);
                    s.allowedPmPm = rs.getString("allowed_pmpm") == null
                            ? null
                            : BigDecimal.valueOf(rs.getDouble("allowed_pmpm")).setScale(2,
                                    RoundingMode.HALF_UP);
                    s.charged = rs.getString("charged") == null
                            ? null
                            : BigDecimal.valueOf(rs.getDouble("charged")).setScale(2,
                                    RoundingMode.HALF_UP);
                    s.chargedPmPm = rs.getString("charged_pmpm") == null
                            ? null
                            : BigDecimal.valueOf(rs.getDouble("charged_pmpm")).setScale(2,
                                    RoundingMode.HALF_UP);
                    s.paid = rs.getString("paid") == null
                            ? null
                            : BigDecimal.valueOf(rs.getDouble("paid")).setScale(2,
                                    RoundingMode.HALF_UP);
                    s.paidPmPm = rs.getString("paid_pmpm") == null
                            ? null
                            : BigDecimal.valueOf(rs.getDouble("paid_pmpm")).setScale(2,
                                    RoundingMode.HALF_UP);
                    s.allowedChargedRatio = rs.getString("allowed_charged") == null
                            ? null
                            : BigDecimal.valueOf(rs.getDouble("allowed_charged")).setScale(2,
                                    RoundingMode.HALF_UP);
                    s.paidAllowedRatio = rs.getString("paid_allowed") == null
                            ? null
                            : BigDecimal.valueOf(rs.getDouble("paid_allowed")).setScale(2,
                                    RoundingMode.HALF_UP);

                    return s;
                });

        report.summary = summaryRows.size() > 0 ? summaryRows.get(0)
                : new HealthcareVisitUtilizationReport.Summary();

        var dataSql = SqlRender.renderSql(reportSql, new String[] { "is_summary" },
                new String[] { "FALSE" });

        var dataPsr = new PreparedStatementRenderer(source, dataSql, search, replace,
                reportCols,
                colVals);

        report.data = jdbcTemplate.query(dataPsr.getSql(), dataPsr.getSetter(), (rs, rowNum) -> {
            var item = new HealthcareVisitUtilizationReport.ReportItem();
            item.periodType = rs.getString("period_type");
            item.periodStart = rs.getDate("period_start_date");
            item.periodEnd = rs.getDate("period_end_date");
            item.personsCount = rs.getLong("person_total");
            item.personsPct = BigDecimal.valueOf(rs.getDouble("person_percent")).setScale(2,
                    RoundingMode.HALF_UP);
            item.visitsCount = rs.getLong("records_total");
            item.visitsPer1000 = BigDecimal.valueOf(rs.getDouble("records_per_1000")).setScale(2,
                    RoundingMode.HALF_UP);
            item.visitsPer1000WithVisits = BigDecimal.valueOf(rs.getDouble("records_per_1000_with_record"))
                    .setScale(2,
                            RoundingMode.HALF_UP);
            item.visitsPer1000PerYear = BigDecimal.valueOf(rs.getDouble("records_per_1000_per_year")).setScale(
                    2,
                    RoundingMode.HALF_UP);
            item.lengthOfStayTotal = rs.getLong("los_total");
            item.lengthOfStayAvg = BigDecimal.valueOf(rs.getDouble("los_average")).setScale(2,
                    RoundingMode.HALF_UP);
            item.allowed = rs.getString("allowed") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("allowed")).setScale(2, RoundingMode.HALF_UP);
            item.allowedPmPm = rs.getString("allowed_pmpm") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("allowed_pmpm")).setScale(2,
                            RoundingMode.HALF_UP);
            item.charged = rs.getString("charged") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("charged")).setScale(2, RoundingMode.HALF_UP);
            item.chargedPmPm = rs.getString("charged_pmpm") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("charged_pmpm")).setScale(2,
                            RoundingMode.HALF_UP);
            item.paid = rs.getString("paid") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("paid")).setScale(2, RoundingMode.HALF_UP);
            item.paidPmPm = rs.getString("paid_pmpm") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("paid_pmpm")).setScale(2,
                            RoundingMode.HALF_UP);
            item.allowedChargedRatio = rs.getString("allowed_charged") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("allowed_charged")).setScale(2,
                            RoundingMode.HALF_UP);
            item.paidAllowedRatio = rs.getString("paid_allowed") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("paid_allowed")).setScale(2,
                            RoundingMode.HALF_UP);

            return item;
        });

        // load visit and visit type concepts
        var conceptCols = new String[] { "cohort_definition_id", "analysis_id" };
        var conceptColValues = new Object[] { cohortId, visitStatAnalysisId };

        var visitConceptQuery = BASE_SQL_PATH + "/healthcareutilization/getVisitConceptsForAnalysis.sql";
        var visitConceptPsr = new PreparedStatementRenderer(source, visitConceptQuery,
                search,
                replace, conceptCols, conceptColValues);
        report.visitConcepts = jdbcTemplate.query(visitConceptPsr.getSql(), visitConceptPsr.getSetter(),
                (rs, rowNum) -> {
                    Concept c = new Concept();
                    c.conceptName = rs.getString("concept_name");
                    c.conceptId = rs.getLong("concept_id");
                    return c;
                });

        var visitTypeConceptQuery = BASE_SQL_PATH
                + "/healthcareutilization/getVisitTypeConceptsForAnalysis.sql";
        var visitTypeConceptPsr = new PreparedStatementRenderer(source,
                visitTypeConceptQuery,
                search, replace, conceptCols, conceptColValues);
        report.visitTypeConcepts = jdbcTemplate.query(visitTypeConceptPsr.getSql(),
                visitTypeConceptPsr.getSetter(),
                (rs, rowNum) -> {
                    Concept c = new Concept();
                    c.conceptName = rs.getString("concept_name");
                    c.conceptId = rs.getLong("concept_id");
                    return c;
                });

        return report;
    }

    public HealthcareDrugUtilizationSummary getHealthcareDrugUtilizationSummary(JdbcTemplate jdbcTemplate,
            final int cohortId, final WindowType window, Long drugTypeConceptId, Long costTypeConceptId,
            SourceEntity source) {
        var vocabularyTableQualifier = source.getTableQualifierOrNull(SourceDaimonEntity.DaimonType.Vocabulary);
        if (vocabularyTableQualifier == null) {
            vocabularyTableQualifier = source.getTableQualifierOrNull(SourceDaimonEntity.DaimonType.CDM);
        }
        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        int subjectsAnalysisId;
        int subjectWithRecordsAnalysisId;
        int drugAnalysisId;
        int daysSupplyAnalysisId;
        int quantityAnalysisId;
        int costAnalysisId;

        // set apprpriate analysis IDs
        if (window == WindowType.BASELINE) {
            subjectsAnalysisId = 4000;
            subjectWithRecordsAnalysisId = 4012;
            drugAnalysisId = 4013;
            daysSupplyAnalysisId = 4014;
            quantityAnalysisId = 4015;
            costAnalysisId = 4022;
        } else if (window == WindowType.AT_RISK) {
            subjectsAnalysisId = 4006;
            subjectWithRecordsAnalysisId = 4016;
            drugAnalysisId = 4017;
            daysSupplyAnalysisId = 4018;
            quantityAnalysisId = 4019;
            costAnalysisId = 4023;
        } else {
            throw new RuntimeException("Invalid window type: " + window);
        }

        var search = new String[] { "vocabulary_schema", "results_schema" };
        var replace = new String[] { vocabularyTableQualifier, resultsTableQualifier };

        var reportPath = BASE_SQL_PATH + "/healthcareutilization/getDrugUtilization.sql";
        var reportSql = resourceReader.getText(reportPath);

        var summarySql = SqlRender.renderSql(reportSql, new String[] { "is_summary" },
                new String[] { "TRUE" });

        var reportCols = new String[] { "cohort_definition_id", "subjects_analysis_id",
                "subject_with_records_analysis_id", "drug_analysis_id", "days_supply_analysis_id",
                "quantity_analysis_id", "cost_analysis_id", "drug_type_concept_id",
                "cost_type_concept_id", "period_type"
        };
        var summaryColVals = new Object[] { cohortId, subjectsAnalysisId, subjectWithRecordsAnalysisId,
                drugAnalysisId, daysSupplyAnalysisId, quantityAnalysisId, costAnalysisId,
                drugTypeConceptId == null ? "" : drugTypeConceptId.toString(),
                costTypeConceptId == null ? "" : costTypeConceptId.toString(), ""
        };

        var summaryPsr = new PreparedStatementRenderer(source, summarySql, search,
                replace,
                reportCols, summaryColVals);
        var summary = new HealthcareDrugUtilizationSummary();

        summary.data = jdbcTemplate.query(summaryPsr.getSql(), summaryPsr.getSetter(), (rs, rowNum) -> {
            HealthcareDrugUtilizationSummary.Record r = new HealthcareDrugUtilizationSummary.Record();
            r.drugName = rs.getString("drug_concept_name");
            r.drugId = rs.getString("stratum_2");
            r.drugCode = rs.getString("drug_concept_code");
            r.drugClass = rs.getString("drug_concept_class");
            r.drugVocabularyId = rs.getString("drug_vocabulary_id");
            r.personsCount = rs.getLong("person_total");
            r.personsPct = BigDecimal.valueOf(rs.getDouble("person_percent")).setScale(2,
                    RoundingMode.HALF_UP);
            r.exposureCount = rs.getLong("records_total");
            r.exposuresPer1000 = BigDecimal.valueOf(rs.getDouble("records_per_1000")).setScale(2,
                    RoundingMode.HALF_UP);
            r.exposurePer1000WithExposures = BigDecimal.valueOf(rs.getDouble("records_per_1000_with_record"))
                    .setScale(2,
                            RoundingMode.HALF_UP);
            r.exposurePer1000PerYear = BigDecimal.valueOf(rs.getDouble("records_per_1000_per_year")).setScale(2,
                    RoundingMode.HALF_UP);
            r.quantityTotal = rs.getLong("quantity_total");
            r.quantityAvg = BigDecimal.valueOf(rs.getDouble("quantity_average")).setScale(2,
                    RoundingMode.HALF_UP);
            r.quantityPer1000PerYear = BigDecimal.valueOf(rs.getDouble("quantity_per_1000_per_year")).setScale(
                    2,
                    RoundingMode.HALF_UP);
            r.daysSupplyTotal = rs.getLong("days_supply_total");
            r.daysSupplyAvg = BigDecimal.valueOf(rs.getDouble("days_supply_average")).setScale(2,
                    RoundingMode.HALF_UP);
            r.daysSupplyPer1000PerYear = BigDecimal.valueOf(rs.getDouble("days_supply_per_1000_per_year"))
                    .setScale(2,
                            RoundingMode.HALF_UP);
            r.allowed = rs.getString("allowed") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("allowed")).setScale(2, RoundingMode.HALF_UP);
            r.allowedPmPm = rs.getString("allowed_pmpm") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("allowed_pmpm")).setScale(2,
                            RoundingMode.HALF_UP);
            r.charged = rs.getString("charged") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("charged")).setScale(2, RoundingMode.HALF_UP);
            r.chargedPmPm = rs.getString("charged_pmpm") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("charged_pmpm")).setScale(2,
                            RoundingMode.HALF_UP);
            r.paid = rs.getString("paid") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("paid")).setScale(2, RoundingMode.HALF_UP);
            r.paidPmPm = rs.getString("paid_pmpm") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("paid_pmpm")).setScale(2,
                            RoundingMode.HALF_UP);
            r.allowedChargedRatio = rs.getString("allowed_charged") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("allowed_charged")).setScale(2,
                            RoundingMode.HALF_UP);
            r.paidAllowedRatio = rs.getString("paid_allowed") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("paid_allowed")).setScale(2,
                            RoundingMode.HALF_UP);

            return r;
        });
        return summary;
    }

    public HealthcareDrugUtilizationDetail getHealthcareDrugUtilizationReport(JdbcTemplate jdbcTemplate,
            final int cohortId, final WindowType window,
            Long drugConceptId, Long drugTypeConceptId, PeriodType periodType, Long costTypeConceptId,
            SourceEntity source) {
        var vocabularyTableQualifier = source.getTableQualifierOrNull(SourceDaimonEntity.DaimonType.Vocabulary);
        if (vocabularyTableQualifier == null) {
            vocabularyTableQualifier = source.getTableQualifierOrNull(SourceDaimonEntity.DaimonType.CDM);
        }
        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        int subjectsAnalysisId;
        int subjectWithRecordsAnalysisId;
        int drugAnalysisId;
        int daysSupplyAnalysisId;
        int quantityAnalysisId;
        int costAnalysisId;

        // set apprpriate analysis IDs
        if (window == WindowType.BASELINE) {
            subjectsAnalysisId = 4000;
            subjectWithRecordsAnalysisId = 4012;
            drugAnalysisId = 4013;
            daysSupplyAnalysisId = 4014;
            quantityAnalysisId = 4015;
            costAnalysisId = 4022;
        } else if (window == WindowType.AT_RISK) {
            subjectsAnalysisId = 4006;
            subjectWithRecordsAnalysisId = 4016;
            drugAnalysisId = 4017;
            daysSupplyAnalysisId = 4018;
            quantityAnalysisId = 4019;
            costAnalysisId = 4023;
        } else {
            throw new RuntimeException("Invalid window type: " + window);
        }

        var search = new String[] { "vocabulary_schema", "results_schema" };
        var replace = new String[] { vocabularyTableQualifier, resultsTableQualifier };

        var reportPath = BASE_SQL_PATH + "/healthcareutilization/getDrugUtilization.sql";
        var reportSql = resourceReader.getText(reportPath);

        var detailSql = SqlRender.renderSql(reportSql, new String[] { "is_summary" },
                new String[] { "FALSE" });

       var reportCols = new String[] { "cohort_definition_id", "subjects_analysis_id",
                "subject_with_records_analysis_id", "drug_analysis_id", "days_supply_analysis_id",
                "quantity_analysis_id", "cost_analysis_id", "drug_concept_id", "drug_type_concept_id",
                "cost_type_concept_id", "period_type"
        };
        var reportColVals = new Object[] { cohortId, subjectsAnalysisId, subjectWithRecordsAnalysisId,
                drugAnalysisId, daysSupplyAnalysisId, quantityAnalysisId, costAnalysisId,
                drugConceptId.toString(), drugTypeConceptId == null ? "" : drugTypeConceptId.toString(),
                costTypeConceptId == null ? "" : costTypeConceptId.toString(),
                periodType.toString().toLowerCase()
        };

        var detailPsr = new PreparedStatementRenderer(source, detailSql, search, replace,
                reportCols, reportColVals);
        var detail = new HealthcareDrugUtilizationDetail();

        detail.data = jdbcTemplate.query(detailPsr.getSql(), detailPsr.getSetter(), (rs, rowNum) -> {
            HealthcareDrugUtilizationDetail.Record r = new HealthcareDrugUtilizationDetail.Record();
            r.periodType = rs.getString("period_type");
            r.periodStart = rs.getDate("period_start_date");
            r.periodEnd = rs.getDate("period_end_date");
            r.personsCount = rs.getLong("person_total");
            r.personsPct = BigDecimal.valueOf(rs.getDouble("person_percent")).setScale(2,
                    RoundingMode.HALF_UP);
            r.exposureCount = rs.getLong("records_total");
            r.exposuresPer1000 = BigDecimal.valueOf(rs.getDouble("records_per_1000")).setScale(2,
                    RoundingMode.HALF_UP);
            r.exposurePer1000WithExposures = BigDecimal.valueOf(rs.getDouble("records_per_1000_with_record"))
                    .setScale(2,
                            RoundingMode.HALF_UP);
            r.exposurePer1000PerYear = BigDecimal.valueOf(rs.getDouble("records_per_1000_per_year")).setScale(2,
                    RoundingMode.HALF_UP);
            r.quantityTotal = rs.getLong("quantity_total");
            r.quantityAvg = BigDecimal.valueOf(rs.getDouble("quantity_average")).setScale(2,
                    RoundingMode.HALF_UP);
            r.quantityPer1000PerYear = BigDecimal.valueOf(rs.getDouble("quantity_per_1000_per_year")).setScale(
                    2,
                    RoundingMode.HALF_UP);
            r.daysSupplyTotal = rs.getLong("days_supply_total");
            r.daysSupplyAvg = BigDecimal.valueOf(rs.getDouble("days_supply_average")).setScale(2,
                    RoundingMode.HALF_UP);
            r.daysSupplyPer1000PerYear = BigDecimal.valueOf(rs.getDouble("days_supply_per_1000_per_year"))
                    .setScale(2,
                            RoundingMode.HALF_UP);
            r.allowed = rs.getString("allowed") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("allowed")).setScale(2, RoundingMode.HALF_UP);
            r.allowedPmPm = rs.getString("allowed_pmpm") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("allowed_pmpm")).setScale(2,
                            RoundingMode.HALF_UP);
            r.charged = rs.getString("charged") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("charged")).setScale(2, RoundingMode.HALF_UP);
            r.chargedPmPm = rs.getString("charged_pmpm") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("charged_pmpm")).setScale(2,
                            RoundingMode.HALF_UP);
            r.paid = rs.getString("paid") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("paid")).setScale(2, RoundingMode.HALF_UP);
            r.paidPmPm = rs.getString("paid_pmpm") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("paid_pmpm")).setScale(2,
                            RoundingMode.HALF_UP);
            r.allowedChargedRatio = rs.getString("allowed_charged") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("allowed_charged")).setScale(2,
                            RoundingMode.HALF_UP);
            r.paidAllowedRatio = rs.getString("paid_allowed") == null
                    ? null
                    : BigDecimal.valueOf(rs.getDouble("paid_allowed")).setScale(2,
                            RoundingMode.HALF_UP);
            return r;
        });

        return detail;
    }

    public List<Concept> getDrugTypes(JdbcTemplate jdbcTemplate, final int cohortId, Long drugConceptId,
            SourceEntity source) {
        var vocabularyTableQualifier = source.getTableQualifierOrNull(SourceDaimonEntity.DaimonType.Vocabulary);
        if (vocabularyTableQualifier == null) {
            vocabularyTableQualifier = source.getTableQualifierOrNull(SourceDaimonEntity.DaimonType.CDM);
        }
        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);

        var analysisList = StringUtils.join(new Integer[] { 4012, 4016 }, ",");
        var search = new String[] { "vocabulary_schema", "results_schema", "analysis_id_list" };
        var replace = new String[] { vocabularyTableQualifier, resultsTableQualifier, analysisList };

        var queryPath = BASE_SQL_PATH + "/healthcareutilization/getDrugTypes.sql";
        var reportCols = new String[] { "cohort_definition_id", "drug_concept_id" };
        var reportColVals = new Object[] { cohortId, drugConceptId };

        var detailPsr = new PreparedStatementRenderer(source, queryPath, search, replace,
                reportCols, reportColVals);

        var drugTypes = jdbcTemplate.query(detailPsr.getSql(), detailPsr.getSetter(),
                (rs, rowNum) -> {
                    Concept c = new Concept();
                    c.conceptId = Long.parseLong(rs.getString("concept_id"));
                    c.conceptName = rs.getString("concept_name");
                    return c;
                });

        return drugTypes;
    }

    public List<String> getHealthcarePeriodTypes(final JdbcTemplate jdbcTemplate, final int id,
            final WindowType window,
            final SourceEntity source) {
        final int windowAnalysisId = getSubjectAnalysisIdByWindowType(window);
        final var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);

        final var search = new String[] { "results_schema" };
        final var replace = new String[] { resultsTableQualifier };

        final var dataPath = BASE_SQL_PATH + "/healthcareutilization/getPeriodTypes.sql";
        final var dataCols = new String[] { "cohort_definition_id", "analysis_id" };
        final var dataColVals = new Object[] { id, windowAnalysisId };

        final var dataPsr = new PreparedStatementRenderer(source, dataPath, search,
                replace,
                dataCols, dataColVals);

        final var periodTypes = jdbcTemplate.query(dataPsr.getSql(), dataPsr.getSetter(),
                (rs, rowNum) -> {
                    return rs.getString("period_type");
                });

        return periodTypes;
    }
}
