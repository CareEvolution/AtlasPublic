package org.ohdsi.webapi.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.ohdsi.webapi.report.mapper.*;
import org.ohdsi.webapi.source.SourceDaimonEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.util.PreparedStatementRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

// TODO - double-check adjusted BASE_SQL_PATH
// TODO - there are a LOT of "is always null" and "might be null" warnings.  These should
//        be checked and resolved if necessary.
// TODO - this is a huge class, seems like there should be an opportunity to rework/combine
//        with all of the other packages that have prepared SQL renderers.  Also instead of
//        concat with BASE_SQL_PATH before call, just make the renderer to the concat
@Component
public class CDMResultsAnalysisRunner {

    private static final String BASE_SQL_PATH = "cdmresults/sql";

    private static final Logger log = LoggerFactory.getLogger(CDMResultsAnalysisRunner.class);

    private static final String[] STANDARD_TABLE = new String[] { "results_database_schema", "vocab_database_schema",
            "cdm_database_schema" };

    private static final String[] DRILLDOWN_COLUMNS = new String[] { "conceptId" };
    private static final String[] DRILLDOWN_TABLE = new String[] { "results_database_schema", "vocab_database_schema" };

    private final ObjectMapper objectMapper;

    public CDMResultsAnalysisRunner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CDMDashboard getDashboard(JdbcTemplate jdbcTemplate,
            SourceEntity source) {

        CDMDashboard dashboard = new CDMDashboard();

        var summarySql = this.renderTranslateSql(BASE_SQL_PATH + "/report/person/population.sql",
                source);
        if (summarySql != null) {
            dashboard.setSummary(
                    jdbcTemplate.query(summarySql.getSql(), summarySql.getSetter(), new CDMAttributeMapper()));
        }

        var ageAtFirstObsSql = this.renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/ageatfirst.sql", source);
        if (ageAtFirstObsSql != null) {
            dashboard.setAgeAtFirstObservation(
                    jdbcTemplate.query(ageAtFirstObsSql.getSql(), ageAtFirstObsSql.getSetter(),
                            new ConceptDistributionMapper()));
        }

        var genderSql = this.renderTranslateSql(BASE_SQL_PATH + "/report/person/gender.sql",
                source);
        if (genderSql != null) {
            dashboard.setGender(
                    jdbcTemplate.query(genderSql.getSql(), genderSql.getSetter(), new ConceptCountMapper()));
        }

        var cumulObsSql = this.renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/cumulativeduration.sql", source);
        if (cumulObsSql != null) {
            dashboard.setCumulativeObservation(jdbcTemplate.query(cumulObsSql.getSql(), cumulObsSql.getSetter(),
                    new CumulativeObservationMapper()));
        }

        var obsByMonthSql = this.renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/observedbymonth.sql", source);
        if (obsByMonthSql != null) {
            dashboard.setObservedByMonth(jdbcTemplate.query(obsByMonthSql.getSql(), obsByMonthSql.getSetter(),
                    new MonthObservationMapper()));
        }

        return dashboard;
    }

    /**
     * Queries for CDM person results for the given source
     *
     * @param jdbcTemplate
     *         JDBCTemplate
     * @return CDMPersonSummary
     */
    public CDMPersonSummary getPersonResults(JdbcTemplate jdbcTemplate,
            final SourceEntity source) {

        var person = new CDMPersonSummary();

        var summarySql = this.renderTranslateSql(BASE_SQL_PATH + "/report/person/population.sql",
                source);
        if (summarySql != null) {
            person.setSummary(
                    jdbcTemplate.query(summarySql.getSql(), summarySql.getSetter(), new CDMAttributeMapper()));
        }

        var genderSql = this.renderTranslateSql(BASE_SQL_PATH + "/report/person/gender.sql",
                source);
        if (genderSql != null) {
            person.setGender(jdbcTemplate.query(genderSql.getSql(), genderSql.getSetter(), new ConceptCountMapper()));
        }

        var raceSql = this.renderTranslateSql(BASE_SQL_PATH + "/report/person/race.sql", source);
        if (raceSql != null) {
            person.setRace(jdbcTemplate.query(raceSql.getSql(), raceSql.getSetter(), new ConceptCountMapper()));
        }

        var ethnicitySql = this.renderTranslateSql(BASE_SQL_PATH + "/report/person/ethnicity.sql",
                source);
        if (ethnicitySql != null) {
            person.setEthnicity(
                    jdbcTemplate.query(ethnicitySql.getSql(), ethnicitySql.getSetter(), new ConceptCountMapper()));
        }

        var yearOfBirthSql = this.renderTranslateSql(
                BASE_SQL_PATH + "/report/person/yearofbirth_data.sql", source);
        if (yearOfBirthSql != null) {
            person.setYearOfBirth(jdbcTemplate.query(yearOfBirthSql.getSql(), yearOfBirthSql.getSetter(),
                    new ConceptDistributionMapper()));
        }

        var yearOfBirthStatsSql = this.renderTranslateSql(
                BASE_SQL_PATH + "/report/person/yearofbirth_stats.sql", source);
        if (yearOfBirthStatsSql != null) {
            person.setYearOfBirthStats(jdbcTemplate.query(yearOfBirthStatsSql.getSql(), yearOfBirthStatsSql.getSetter(),
                    new CohortStatsMapper()));
        }

        return person;
    }

    public CDMDataDensity getDataDensityResults(JdbcTemplate jdbcTemplate, SourceEntity source) {
        CDMDataDensity cdmDataDensity = new CDMDataDensity();
        var conceptsPerPersonSql = this.renderTranslateSql(
                BASE_SQL_PATH + "/report/datadensity/conceptsperperson.sql", source);
        if (conceptsPerPersonSql != null) {
            cdmDataDensity.setConceptsPerPerson(
                    jdbcTemplate.query(conceptsPerPersonSql.getSql(), conceptsPerPersonSql.getSetter(),
                            new ConceptQuartileMapper()));
        }
        var recordsPerPersonSql = this.renderTranslateSql(
                BASE_SQL_PATH + "/report/datadensity/recordsperperson.sql", source);
        if (recordsPerPersonSql != null) {
            cdmDataDensity.setRecordsPerPerson(
                    jdbcTemplate.query(recordsPerPersonSql.getSql(), recordsPerPersonSql.getSetter(),
                            new SeriesPerPersonMapper()));
        }
        var totalRecordsSql = this.renderTranslateSql(
                BASE_SQL_PATH + "/report/datadensity/totalrecords.sql", source);
        if (totalRecordsSql != null) {
            cdmDataDensity.setTotalRecords(jdbcTemplate.query(totalRecordsSql.getSql(), totalRecordsSql.getSetter(),
                    new SeriesPerPersonMapper()));
        }
        return cdmDataDensity;
    }

    public CDMDeath getDeathResults(JdbcTemplate jdbcTemplate, SourceEntity source) {
        CDMDeath cdmDeath = new CDMDeath();
        var prevalenceByGenderAgeYearSql = this.renderTranslateSql(
                BASE_SQL_PATH + "/report/death/sqlPrevalenceByGenderAgeYear.sql", source);
        if (prevalenceByGenderAgeYearSql != null) {
            cdmDeath.setPrevalenceByGenderAgeYear(
                    jdbcTemplate.query(prevalenceByGenderAgeYearSql.getSql(), prevalenceByGenderAgeYearSql.getSetter(),
                            new ConceptDecileMapper()));
        }
        var prevalenceByMonthSql = this.renderTranslateSql(
                BASE_SQL_PATH + "/report/death/sqlPrevalenceByMonth.sql", source);
        if (prevalenceByMonthSql != null) {
            cdmDeath.setPrevalenceByMonth(
                    jdbcTemplate.query(prevalenceByMonthSql.getSql(), prevalenceByMonthSql.getSetter(),
                            new PrevalanceMapper()));
        }
        var deathByTypeSql = this.renderTranslateSql(
                BASE_SQL_PATH + "/report/death/sqlDeathByType.sql", source);
        if (deathByTypeSql != null) {
            cdmDeath.setDeathByType(
                    jdbcTemplate.query(deathByTypeSql.getSql(), deathByTypeSql.getSetter(), new ConceptCountMapper()));
        }
        var ageAtDeathSql = this.renderTranslateSql(
                BASE_SQL_PATH + "/report/death/sqlAgeAtDeath.sql", source);
        if (ageAtDeathSql != null) {
            cdmDeath.setAgeAtDeath(
                    jdbcTemplate.query(ageAtDeathSql.getSql(), ageAtDeathSql.getSetter(), new ConceptQuartileMapper()));
        }
        return cdmDeath;
    }

    public CDMObservationPeriod getObservationPeriodResults(JdbcTemplate jdbcTemplate, SourceEntity source) {
        CDMObservationPeriod obsPeriod = new CDMObservationPeriod();
        var ageAtFirstSql = renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/ageatfirst.sql", source);
        if (ageAtFirstSql != null) {
            obsPeriod.setAgeAtFirst(jdbcTemplate.query(ageAtFirstSql.getSql(), ageAtFirstSql.getSetter(),
                    new ConceptDistributionMapper()));
        }

        var obsLengthSql = renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/observationlength_data.sql", source);
        if (obsLengthSql != null) {
            obsPeriod.setObservationLength(jdbcTemplate.query(obsLengthSql.getSql(), obsLengthSql.getSetter(),
                    new ConceptDistributionMapper()));
        }

        var obsLengthStatsSql = renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/observationlength_stats.sql", source);
        if (obsLengthStatsSql != null) {
            obsPeriod.setObservationLengthStats(jdbcTemplate.query(obsLengthStatsSql.getSql(),
                    obsLengthStatsSql.getSetter(), new CohortStatsMapper()));
        }

        var obsYearStatsSql = renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/observedbyyear_stats.sql",
                source);
        if (obsYearStatsSql != null) {
            obsPeriod.setPersonsWithContinuousObservationsByYearStats(jdbcTemplate.query(obsYearStatsSql.getSql(),
                    obsYearStatsSql.getSetter(), new CohortStatsMapper()));
        }

        var personsWithContObsSql = renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/observedbyyear_data.sql", source);
        if (personsWithContObsSql != null) {
            obsPeriod.setPersonsWithContinuousObservationsByYear(jdbcTemplate.query(personsWithContObsSql.getSql(),
                    personsWithContObsSql.getSetter(), new ConceptDistributionMapper()));
        }

        var ageByGenderSql = renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/agebygender.sql", source);
        if (ageByGenderSql != null) {
            obsPeriod.setAgeByGender(jdbcTemplate.query(ageByGenderSql.getSql(), ageByGenderSql.getSetter(),
                    new ConceptQuartileMapper()));
        }

        var durationByGenderSql = renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/observationlengthbygender.sql", source);
        if (durationByGenderSql != null) {
            obsPeriod.setDurationByGender(
                    jdbcTemplate.query(durationByGenderSql.getSql(), durationByGenderSql.getSetter(),
                            new ConceptQuartileMapper()));
        }

        var durationByAgeSql = renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/observationlengthbyage.sql", source);
        if (durationByAgeSql != null) {
            obsPeriod.setDurationByAgeDecile(jdbcTemplate.query(durationByAgeSql.getSql(), durationByAgeSql.getSetter(),
                    new ConceptQuartileMapper()));
        }

        var cumulObsSql = renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/cumulativeduration.sql", source);
        if (cumulObsSql != null) {
            obsPeriod.setCumulativeObservation(jdbcTemplate.query(cumulObsSql.getSql(), cumulObsSql.getSetter(),
                    new CumulativeObservationMapper()));
        }

        var obsByMonthSql = renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/observedbymonth.sql", source);
        if (obsByMonthSql != null) {
            obsPeriod.setObservedByMonth(jdbcTemplate.query(obsByMonthSql.getSql(), obsByMonthSql.getSetter(),
                    new MonthObservationMapper()));
        }

        var obsPeriodsPerPersonSql = renderTranslateSql(
                BASE_SQL_PATH + "/report/observationperiod/periodsperperson.sql", source);
        if (obsPeriodsPerPersonSql != null) {
            obsPeriod.setObservationPeriodsPerPerson(jdbcTemplate.query(obsPeriodsPerPersonSql.getSql(),
                    obsPeriodsPerPersonSql.getSetter(), new ConceptCountMapper()));
        }
        return obsPeriod;
    }

    private PreparedStatementRenderer renderTranslateSql(String sqlPath, SourceEntity source) {
        return renderTranslateSql(sqlPath, null, source);
    }

    public ArrayNode getTreemap(JdbcTemplate jdbcTemplate,
            String domain,
            SourceEntity source) {
        var arrayNode = objectMapper.createArrayNode();

        var sqlPath = BASE_SQL_PATH + "/report/" + domain.toLowerCase() + "/treemap.sql";
        var sql = this.renderTranslateSql(sqlPath, source);
        if (sql != null) {
            List<JsonNode> list = jdbcTemplate.query(sql.getSql(), sql.getSetter(), new GenericRowMapper(objectMapper));
            arrayNode.addAll(list);
        }
        return arrayNode;
    }

    public JsonNode getDrilldown(JdbcTemplate jdbcTemplate,
            String domain,
            Integer conceptId,
            SourceEntity source) {
        var objectNode = objectMapper.createObjectNode();

        var cl = this.getClass().getClassLoader();
        var resolver = new PathMatchingResourcePatternResolver(cl);
        var folder = Objects.nonNull(conceptId) ? "/drilldown/" : "/drilldownsummary/";
        var pattern = BASE_SQL_PATH + "/report/" + domain.toLowerCase() + folder + "*.sql";
        try {
            Resource[] resources = resolver.getResources(pattern);
            for (Resource resource : resources) {
                var fullSqlPath = resource.getURL().getPath();
                int startIndex = fullSqlPath.indexOf(BASE_SQL_PATH);
                var sqlPath = fullSqlPath.substring(startIndex);
                PreparedStatementRenderer sql = this.renderTranslateSql(sqlPath, conceptId, source);
                if (sql != null) {
                    List<JsonNode> l = jdbcTemplate.query(sql.getSql(), sql.getSetter(),
                            new GenericRowMapper(objectMapper));
                    var analysisName = resource.getFilename().replace(".sql", "");
                    objectNode.putArray(analysisName).addAll(l);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return objectNode;
    }

    private PreparedStatementRenderer renderTranslateSql(String sqlPath, Integer conceptId, SourceEntity source) {

        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var vocabularyTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var cdmTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.CDM);

        PreparedStatementRenderer psr;
        String[] tableQualifierValues;

        if (conceptId != null) {
            tableQualifierValues = new String[] { resultsTableQualifier, vocabularyTableQualifier };
            psr = new PreparedStatementRenderer(source, sqlPath, DRILLDOWN_TABLE, tableQualifierValues,
                    DRILLDOWN_COLUMNS, new Integer[] { conceptId });
        } else {
            tableQualifierValues = new String[] { resultsTableQualifier, vocabularyTableQualifier, cdmTableQualifier };
            psr = new PreparedStatementRenderer(source, sqlPath, STANDARD_TABLE, tableQualifierValues, (String) null,
                    null);
        }
        return psr;
    }
}
