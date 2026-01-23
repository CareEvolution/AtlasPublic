package org.ohdsi.webapi.cohortanalysis;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang3.ArrayUtils;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.cohortresults.PeriodType;
import org.ohdsi.webapi.model.DBMSType;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.util.CSVRecordMapper;
import org.ohdsi.webapi.util.JoinUtils;
import org.ohdsi.webapi.util.SessionUtils;
import org.ohdsi.webapi.util.SourceUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class HeraclesQueryBuilder {
    private final static String ANALYSES_QUERY_PREFIX = "cohortanalysis/heraclesanalyses/sql/";

    private final static String INSERT_RESULT_STATEMENT = "insert into @results_schema.heracles_results (cohort_definition_id, analysis_id, stratum_1, stratum_2, stratum_3, stratum_4, stratum_5, count_value, last_update_time)\n";
    private final static String INSERT_DIST_RESULT_STATEMENT = "insert into @results_schema.heracles_results_dist (cohort_definition_id, analysis_id, stratum_1, stratum_2, stratum_3, stratum_4, stratum_5, count_value, min_value, max_value, avg_value, stdev_value, median_value, p10_value, p25_value, p75_value, p90_value, last_update_time)\n";

    private final static String[] PARAM_NAMES = new String[] {
            "vocab_schema",
            "CDM_schema", "results_schema", "refreshStats",
            "source_name",
            "smallcellcount", "runHERACLESHeel", "CDM_version", "cohort_definition_id", "list_of_analysis_ids",
            "condition_concept_ids", "drug_concept_ids", "procedure_concept_ids", "observation_concept_ids",
            "measurement_concept_ids", "cohort_period_only", "source_id", "periods", "rollupUtilizationVisit",
            "rollupUtilizationDrug",
            "includeVisitTypeUtilization", "includeDrugTypeUtilization", "includeCostConcepts", "includeCurrency" };
    private static final String UNION_ALL = "\nUNION ALL\n";

    // Columns
    private static final String COL_ANALYSIS_ID = "analysisId";
    private static final String COL_ANALYSIS_NAME = "analysisName";
    private static final String COL_SQL_FILE_NAME = "sqlFileName";
    private static final String COL_RESULTS = "results";
    private static final String COL_DIST_RESULTS = "distResults";
    private static final String COL_PARAM_NAME = "paramName";
    private static final String COL_PARAM_VALUE = "paramValue";

    // Defaults
    private static final String includeVisitTypeUtilization = "32025, 32028, 32022, 32827";
    private static final String includeDrugTypeUtilization = "38000175,38000176, 32838, 32833";
    private static final String includeCostConcepts = "31978, 31973, 31980";
    private static final String includeCurrency = "44818668";

    private Map<Integer, HeraclesAnalysis> heraclesAnalysisMap;
    private final Map<Integer, Set<HeraclesAnalysisParameter>> analysesParamsMap = new HashMap<>();
    private final StringResourceReader resourceReader;

    private final String initQuerySql;
    private final String finalizeQuerySql;
    private final String selectResultStatementSql;
    private final String selectDistResultStatementSql;

    public HeraclesQueryBuilder(StringResourceReader resourceReader) {
        this.resourceReader = resourceReader;
        initQuerySql = this.resourceReader.getText("cohortanalysis/sql/initHeraclesAnalyses.sql");
        finalizeQuerySql = this.resourceReader.getText("cohortanalysis/sql/finalizeHeraclesAnalyses.sql");
        selectResultStatementSql = this.resourceReader.getText("cohortanalysis/sql/selectHeraclesResults.sql");
        selectDistResultStatementSql = this.resourceReader.getText("cohortanalysis/sql/selectHeraclesDistResults.sql");

        var heraclesAnalysisTableCsvText = this.resourceReader.getText(
                "cohortanalysis/heraclesanalyses/csv/heraclesAnalyses.csv");
        initHeraclesAnalyses(heraclesAnalysisTableCsvText);

        var heraclesAnalysisParamsTableCsvText = this.resourceReader.getText(
                "cohortanalysis/heraclesanalyses/csv/heraclesAnalysesParams.csv");
        initAnalysesParams(heraclesAnalysisParamsTableCsvText);
    }

    private void initHeraclesAnalyses(String csvText) {
        heraclesAnalysisMap = parseCSV(csvText,
                record -> new HeraclesAnalysis(Integer.parseInt(record.get(COL_ANALYSIS_ID)),
                        record.get(COL_ANALYSIS_NAME), record.get(COL_SQL_FILE_NAME),
                        Boolean.parseBoolean(record.get(COL_RESULTS)),
                        Boolean.parseBoolean(record.get(COL_DIST_RESULTS))))
                                .stream()
                                .collect(Collectors.toMap(HeraclesAnalysis::id, analysis -> analysis));
    }

    private void initAnalysesParams(String csvText) {
        parseCSV(csvText,
                record -> new HeraclesAnalysisParameter(Integer.parseInt(record.get(COL_ANALYSIS_ID)),
                        record.get(COL_PARAM_NAME), record.get(COL_PARAM_VALUE)))
                                .forEach(p -> {
                                    Set<HeraclesAnalysisParameter> params = analysesParamsMap.getOrDefault(
                                            p.analysisId(),
                                            new HashSet<>());
                                    params.add(p);
                                    analysesParamsMap.put(p.analysisId(), params);
                                });
        heraclesAnalysisMap.values().forEach(analysis -> {
            Integer id = analysis.id();
            Set<HeraclesAnalysisParameter> params = analysesParamsMap.getOrDefault(id, new HashSet<>());
            params.add(new HeraclesAnalysisParameter(id, COL_ANALYSIS_ID, id.toString()));
            params.add(new HeraclesAnalysisParameter(id, COL_ANALYSIS_NAME, analysis.name()));
            analysesParamsMap.put(id, params);
        });
    }

    private <T> List<T> parseCSV(String source, CSVRecordMapper<T> mapper) {
        List<T> result = new ArrayList<>();
        try (Reader in = new StringReader(source)) {
            var parser = CSVFormat.Builder
                    .create(CSVFormat.RFC4180)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .get();

            var records = parser.parse(in);
            for (final var record : records) {
                result.add(mapper.mapRecord(record));
            }
        } catch (IOException e) {
            throw new BeanInitializationException("Failed to read heracles analyses");
        }
        return result;
    }

    public String buildHeraclesAnalysisQuery(CohortAnalysisTask task) {
        return new Builder(task).buildQuery();
    }

    private String[] buildAnalysisParams(CohortAnalysisTask task) {
        var resultsTableQualifier = SourceUtils.getResultsQualifier(task.getSource());
        var cdmTableQualifier = SourceUtils.getCdmQualifier(task.getSource());
        String vocabTableQualifier = SourceUtils.getVocabularyQualifier(task.getSource());

        var cohortDefinitionIds = JoinUtils.join(task.getCohortDefinitionIds());
        var analysisIds = JoinUtils.join(task.getAnalysisIds());
        var conditionIds = JoinUtils.join(task.getConditionConceptIds());
        var drugIds = JoinUtils.join(task.getDrugConceptIds());
        var procedureIds = JoinUtils.join(task.getProcedureConceptIds());
        var observationIds = JoinUtils.join(task.getObservationConceptIds());
        var measurementIds = JoinUtils.join(task.getMeasurementConceptIds());

        var concatenatedPeriods = "";
        if (CollectionUtils.isEmpty(task.getPeriods())) {
            // In this case summary stats will be calculated
            concatenatedPeriods = "''";
        } else {
            List<PeriodType> periods = CollectionUtils.isEmpty(task.getPeriods())
                    ? Arrays.asList(PeriodType.values())
                    : task.getPeriods();
            concatenatedPeriods = periods.stream()
                    .map(PeriodType::getValue)
                    .map(StringUtils::quote)
                    .collect(Collectors.joining(","));
        }

        boolean refreshStats = Arrays.asList(DBMSType.POSTGRESQL.getOhdsiDB())
                .contains(task.getSource().getSourceDialect());

        return new String[] {
                vocabTableQualifier,
                cdmTableQualifier,
                resultsTableQualifier,
                String.valueOf(refreshStats).toUpperCase(),
                task.getSource().getSourceName(),
                String.valueOf(task.getSmallCellCount()),
                String.valueOf(task.runHeraclesHeel()).toUpperCase(),
                task.getCdmVersion(),
                cohortDefinitionIds,
                analysisIds,
                conditionIds,
                drugIds,
                procedureIds,
                observationIds,
                measurementIds,
                String.valueOf(task.isCohortPeriodOnly()),
                String.valueOf(task.getSource().getSourceId()),
                concatenatedPeriods,
                String.valueOf(task.getRollupUtilizationVisit()).toUpperCase(),
                String.valueOf(task.getRollupUtilizationDrug()).toUpperCase(),
                includeVisitTypeUtilization,
                includeDrugTypeUtilization,
                includeCostConcepts,
                includeCurrency
        };
    }

    private class Builder {
        private final CohortAnalysisTask analysisTask;
        private final String[] values;
        private final List<Integer> analysesIds;
        private final List<String> resultSql = new ArrayList<>();
        private final String sessionId;

        Builder(CohortAnalysisTask analysisTask) {
            this.analysisTask = analysisTask;
            this.values = buildAnalysisParams(this.analysisTask);
            this.analysesIds = analysisTask.getAnalysisIds().stream().map(Integer::parseInt)
                    .collect(Collectors.toList());
            this.sessionId = SessionUtils.sessionId();
        }

        String buildQuery() {
            return buildQuery(initQuerySql)
                    .buildAnalysesQueries()
                    .buildSelectResultQuery()
                    .buildQuery(finalizeQuerySql)
                    .toSql();
        }

        private String translateSql(String query) {
            return SqlTranslate.translateSql(query, analysisTask.getSource().getSourceDialect(), sessionId,
                    SourceUtils.getTempQualifier(analysisTask.getSource()));
        }

        private Builder buildQuery(String query) {
            resultSql.add(translateSql(SqlRender.renderSql(query, PARAM_NAMES, values)));
            return this;
        }

        private Builder buildAnalysesQueries() {
            resultSql.add(analysesIds.stream().map(this::getAnalysisQuery).collect(Collectors.joining("\n")));
            return this;
        }

        private Builder buildSelectResultQuery() {
            StringBuilder result = new StringBuilder();
            List<String> resultsQuery = new ArrayList<>();
            List<String> distResultsQuery = new ArrayList<>();
            analysesIds.forEach(id -> {
                HeraclesAnalysis analysis = heraclesAnalysisMap.get(id);
                if (Objects.nonNull(analysis)) {
                    Pair<String[], String[]> params = getAnalysisParams(id);
                    if (analysis.hasResults()) {
                        resultsQuery.add(
                                SqlRender.renderSql(selectResultStatementSql, params.getFirst(), params.getSecond()));
                    }
                    if (analysis.hasDistResults()) {
                        distResultsQuery.add(SqlRender.renderSql(selectDistResultStatementSql, params.getFirst(),
                                params.getSecond()));
                    }
                }
            });

            if (!resultsQuery.isEmpty()) {
                result.append(SqlRender.renderSql(INSERT_RESULT_STATEMENT, PARAM_NAMES, values))
                        .append(String.join(UNION_ALL, resultsQuery))
                        .append(";")
                        .append("\n");
            }

            if (!distResultsQuery.isEmpty()) {
                result.append(SqlRender.renderSql(INSERT_DIST_RESULT_STATEMENT, PARAM_NAMES, values))
                        .append(String.join(UNION_ALL, distResultsQuery))
                        .append(";")
                        .append("\n");
            }

            resultSql.add(translateSql(result.toString()));
            return this;
        }

        private String toSql() {
            return String.join("\n", resultSql);
        }

        private String getAnalysisQuery(Integer id) {
            HeraclesAnalysis analysis = heraclesAnalysisMap.get(id);
            if (Objects.nonNull(analysis)) {
                var querySql = resourceReader.getText(ANALYSES_QUERY_PREFIX + analysis.filename());
                Pair<String[], String[]> params = getAnalysisParams(id);
                return translateSql(SqlRender.renderSql(querySql, params.getFirst(), params.getSecond()));
            } else {
                return "";
            }
        }

        private Pair<String[], String[]> getAnalysisParams(Integer id) {
            List<HeraclesAnalysisParameter> params = new ArrayList<>(
                    analysesParamsMap.getOrDefault(id, new HashSet<>()));

            int size = params.size();
            String[] analysisParamNames = new String[size];
            String[] analysisParamValues = new String[size];
            for (var i = 0; i < size; i++) {
                analysisParamNames[i] = params.get(i).paramName();
                analysisParamValues[i] = params.get(i).value();
            }
            String[] paramNames = ArrayUtils.addAll(PARAM_NAMES, analysisParamNames);
            String[] paramValues = ArrayUtils.addAll(values, analysisParamValues);
            return Pair.of(paramNames, paramValues);
        }
    }

    record HeraclesAnalysis(Integer id, String name, String filename, boolean hasResults, boolean hasDistResults) {
        @Override
            public boolean equals(Object o) {
                if (this == o)
                    return true;
                if (!(o instanceof HeraclesAnalysis analysis))
                    return false;
                return Objects.equals(id, analysis.id) &&
                        Objects.equals(name, analysis.name);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, name);
            }
        }

    record HeraclesAnalysisParameter(Integer analysisId, String paramName, String value) {
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof HeraclesAnalysisParameter(Integer id, String name, String value1)))
                return false;
            return Objects.equals(analysisId, id) &&
                    Objects.equals(paramName, name) &&
                    Objects.equals(value, value1);
        }
    }
}