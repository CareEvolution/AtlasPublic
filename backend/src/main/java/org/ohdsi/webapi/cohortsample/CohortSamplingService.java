package org.ohdsi.webapi.cohortsample;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.cohortsample.dto.CohortSampleDTO;
import org.ohdsi.webapi.cohortsample.dto.SampleElementDTO;
import org.ohdsi.webapi.cohortsample.dto.SampleParametersDTO;
import org.ohdsi.webapi.job.JobService;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.converter.UserDtoToUserEntity;
import org.ohdsi.webapi.source.SourceDaimonEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.transactiontemplates.TransactionTemplates;
import org.ohdsi.webapi.authorization.dto.UserDTO;
import org.ohdsi.webapi.util.PreparedStatementRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.ohdsi.webapi.cohortsample.dto.SampleParametersDTO.GenderDTO.GENDER_FEMALE_CONCEPT_ID;
import static org.ohdsi.webapi.cohortsample.dto.SampleParametersDTO.GenderDTO.GENDER_MALE_CONCEPT_ID;

import static org.ohdsi.webapi.Constants.Params.*;

/**
 * Service to do manage samples of a cohort definition.
 */
@Component
public class CohortSamplingService {
    final Logger log = LoggerFactory.getLogger(CohortSamplingService.class);

    private final AuthorizationManager authorizationManager;
    private final CohortSampleRepository sampleRepository;
    private final SourceService sourceService;
    private final TransactionTemplates transactionTemplates;
    private final JobService jobService;

    public CohortSamplingService(
        AuthorizationManager authorizationManager,
        CohortSampleRepository sampleRepository,
        SourceService sourceService,
        TransactionTemplates transactionTemplates,
        JobService jobService) {
        this.authorizationManager = authorizationManager;
        this.sampleRepository = sampleRepository;
        this.sourceService = sourceService;
        this.transactionTemplates = transactionTemplates;
        this.jobService = jobService;
    }

    public List<CohortSampleDTO> listSamples(int cohortDefinitionId, int sourceId) {
        return sampleRepository.findByCohortDefinitionIdAndSourceId(cohortDefinitionId, sourceId).stream()
                .map(sample -> sampleToSampleDTO(sample, null, false))
                .collect(Collectors.toList());
    }

    public CohortSampleDTO getSample(int sampleId, boolean withRecordCounts) {
        var sample = sampleRepository.findById(sampleId);
        if (sample == null) {
            throw new NotFoundException("Cohort sample with ID " + sampleId + " not found");
        }
        var source = sourceService.findBySourceId(sample.getSourceId());
        var sampleElements = findSampleElements(source, sample.getId(), withRecordCounts);
        return sampleToSampleDTO(sample, sampleElements, true);
    }

    public int countSamples(int cohortDefinitionId) {
        return sampleRepository.countSamples(cohortDefinitionId);
    }

    public int countSamples(int cohortDefinitionId, int sourceId) {
        return sampleRepository.countSamples(cohortDefinitionId, sourceId);
    }

    /**
     * Find all sample elements of a sample.
     *
     * @param source
     *         Source to use
     * @param cohortSampleId
     *         sample ID of the elements.
     * @param withRecordCounts
     *         whether to return record counts. This makes the query much slower.
     * @return list of elements.
     */
    private List<SampleElement> findSampleElements(SourceEntity source, int cohortSampleId, boolean withRecordCounts) {
        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        PreparedStatementRenderer renderer;
        Collection<String> optionalFields;

        if (withRecordCounts) {
            renderer = new PreparedStatementRenderer(source,
                    "cohortsample/sql/findElementsByCohortSampleIdWithCounts.sql",
                    new String[] { "results_schema", "CDM_schema" },
                    new String[] { source.getTableQualifier(SourceDaimonEntity.DaimonType.Results),
                            source.getTableQualifier(SourceDaimonEntity.DaimonType.CDM) },
                    "cohortSampleId", cohortSampleId);
            optionalFields = Collections.singleton("record_count");
        } else {
            renderer = new PreparedStatementRenderer(source,
                    "cohortsample/sql/findElementsByCohortSampleId.sql",
                    "results_schema",
                    source.getTableQualifier(SourceDaimonEntity.DaimonType.Results),
                    "cohortSampleId", cohortSampleId);
            optionalFields = Collections.emptySet();
        }
        // TODO query() is deprecated - still exists but need to modernize
        return jdbcTemplate.query(renderer.getSql(), renderer.getOrderedParams(),
                new CohortSampleElementRowMapper(optionalFields));
    }

    /**
     * Create a new sample in given source and cohort definition, using sample parameters.
     *
     * @param source
     *         Source to use
     * @param cohortDefinitionId
     *         cohort definition ID to sample
     * @param sampleParameters
     *         parameters to define the sample
     * @return list of elements.
     */
    public CohortSampleDTO createSample(SourceEntity source, int cohortDefinitionId, SampleParametersDTO sampleParameters) {
        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);

        var sample = new CohortSample();
        sample.setName(sampleParameters.getName());
        sample.setCohortDefinitionId(cohortDefinitionId);
        sample.setSourceId(source.getId());
        sample.setSize(sampleParameters.getSize());

        var age = sampleParameters.getAge();
        if (age != null) {
            switch (age.getMode()) {
                case LESS_THAN:
                case LESS_THAN_OR_EQUAL:
                    sample.setAgeMax(age.getValue());
                    break;
                case GREATER_THAN:
                case GREATER_THAN_OR_EQUAL:
                    sample.setAgeMin(age.getValue());
                    break;
                case EQUAL_TO:
                    sample.setAgeMin(age.getValue());
                    sample.setAgeMax(age.getValue());
                    break;
                case BETWEEN:
                case NOT_BETWEEN:
                    sample.setAgeMin(age.getMin());
                    sample.setAgeMax(age.getMax());
                    break;
            }
            sample.setAgeMode(age.getMode().getSerialName());
        }

        var gender = sampleParameters.getGender();
        if (gender != null) {
            StringBuilder sb = new StringBuilder(12);
            for (Integer conceptId : gender.getConceptIds()) {
                if (!sb.isEmpty()) {
                    sb.append(',');
                }
                sb.append(conceptId);
            }
            if (gender.isOtherNonBinary()) {
                if (!sb.isEmpty()) {
                    sb.append(',');
                }
                sb.append(-1);
            }
            sample.setGenderConceptIds(sb.toString());
        }

        var currentUser = authorizationManager.getCurrentUser();
        sample.setCreatedBy(UserDtoToUserEntity.convert(currentUser));
        sample.setCreatedDate(new Date());

        log.info("Sampling {} elements for cohort {}", sampleParameters.getSize(), cohortDefinitionId);
        final var elements = sampleElements(sampleParameters, sample, jdbcTemplate, source);

        if (elements.size() < sample.getSize()) {
            sample.setSize(elements.size());
        }

        transactionTemplates.getTransactionTemplate().execute((TransactionCallback<Void>) transactionStatus -> {
            log.debug("Saving {} sample elements for cohort {}", sample.getSize(), cohortDefinitionId);
            var updatedSample = sampleRepository.save(sample);
            insertSampledElements(source, jdbcTemplate, updatedSample.getId(), elements);
            return null;
        });

        return sampleToSampleDTO(sample, elements, true);
    }

    /**
     * Create a new sample in given source and cohort definition, using sample parameters.
     *
     * @param sampleId
     *         The sample to refresh
     */
    public void refreshSample(Integer sampleId) {
        var optionalSample = sampleRepository.findById(sampleId);
        if (optionalSample.isEmpty()) {
            throw new NotFoundException("Cohort sample with ID " + sampleId + " not found");
        }
        var sample = optionalSample.get();
        var source = sourceService.findBySourceId(sample.getSourceId());

        var sampleDto = sampleToSampleDTO(sample, null, true);
        var sampleParamaters = new SampleParametersDTO();
        sampleParamaters.setAge(sampleDto.getAge());
        sampleParamaters.setGender(sampleDto.getGender());
        sampleParamaters.setSize(sampleDto.getSize());
        log.info("Sampling {} elements for cohort {}", sampleParamaters.getSize(), sample.getCohortDefinitionId());
        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        final var elements = sampleElements(sampleParamaters, sample, jdbcTemplate, source);

        transactionTemplates.getTransactionTemplate().execute((TransactionCallback<Void>) transactionStatus -> {
            var deleteSql = String.format(
                    "DELETE FROM %s.cohort_sample_element WHERE cohort_sample_id = %d;",
                    source.getTableQualifier(SourceDaimonEntity.DaimonType.Results),
                    sample.getId());
            var translatedDeleteSql = SqlTranslate.translateSql(deleteSql, source.getSourceDialect(), null, null);
            jdbcTemplate.update(translatedDeleteSql);
            insertSampledElements(source, jdbcTemplate, sample.getId(), elements);
            return null;
        });
    }

    /**
     * Convert a given sample with given elements to a DTO.
     */
    private CohortSampleDTO sampleToSampleDTO(CohortSample sample, List<SampleElement> elements, boolean includeIds) {
        var sampleDTO = new CohortSampleDTO();
        sampleDTO.setId(sample.getId());
        sampleDTO.setName(sample.getName());
        sampleDTO.setSize(sample.getSize());
        if (includeIds) {
            sampleDTO.setCohortDefinitionId(sample.getCohortDefinitionId());
            sampleDTO.setSourceId(sample.getSourceId());
        }
        sampleDTO.setCreatedDate(sample.getCreatedDate());
        var createdBy = sample.getCreatedBy();
        if (createdBy != null) {
            var userDto = new UserDTO(createdBy.getId(), createdBy.getLogin(), createdBy.getName());
            sampleDTO.setCreatedBy(userDto);
        }

        var ageMode = SampleParametersDTO.AgeMode.fromSerialName(sample.getAgeMode());
        if (ageMode != null) {
            var age = new SampleParametersDTO.AgeDTO();
            age.setMode(ageMode);
            switch (ageMode) {
                case LESS_THAN:
                case LESS_THAN_OR_EQUAL:
                case EQUAL_TO:
                    age.setValue(sample.getAgeMax());
                    break;
                case GREATER_THAN:
                case GREATER_THAN_OR_EQUAL:
                    age.setValue(sample.getAgeMin());
                    break;
                case BETWEEN:
                case NOT_BETWEEN:
                    age.setMin(sample.getAgeMin());
                    age.setMax(sample.getAgeMax());
                    break;
            }
            sampleDTO.setAge(age);
        }
        if (sample.getGenderConceptIds() != null && !sample.getGenderConceptIds().isEmpty()) {
            var conceptIds = Arrays.stream(sample.getGenderConceptIds().split(","))
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());

            var genderDto = new SampleParametersDTO.GenderDTO();
            if (conceptIds.remove(Integer.valueOf(-1))) {
                genderDto.setOtherNonBinary(true);
            }

            genderDto.setConceptIds(conceptIds);
            sampleDTO.setGender(genderDto);
        }

        sampleDTO.setElements(sampleElementToDTO(elements));
        return sampleDTO;
    }

    /**
     * Convert given sample elements DTOs.
     */
    private List<SampleElementDTO> sampleElementToDTO(List<SampleElement> elements) {
        if (elements == null) {
            return null;
        }

        return elements.stream()
                .map(el -> {
                    SampleElementDTO elementDTO = new SampleElementDTO();
                    elementDTO.setRank(el.getRank());
                    elementDTO.setPersonId(String.valueOf(el.getPersonId()));
                    elementDTO.setAge(el.getAge());
                    elementDTO.setGenderConceptId(el.getGenderConceptId());
                    elementDTO.setRecordCount(el.getRecordCount());
                    return elementDTO;
                })
                .collect(Collectors.toList());
    }

    /**
     * Insert elements that have been sampled.
     */
    private void insertSampledElements(SourceEntity source, JdbcTemplate jdbcTemplate, int sampleId,
            List<SampleElement> elements) {
        if (elements.isEmpty()) {
            return;
        }

        var parameters = new String[] { "results_schema" };
        var parameterValues = new String[] { source.getTableQualifier(SourceDaimonEntity.DaimonType.Results) };
        var sqlParameters = new String[] { "cohortSampleId", "rank", "personId", "age", "genderConceptId" };

        String statement = null;
        var variables = new ArrayList<Object[]>(elements.size());
        for (var element : elements) {
            var sqlValues = new Object[] {
                    sampleId,
                    element.getRank(),
                    element.getPersonId(),
                    element.getAge(),
                    element.getGenderConceptId() };

            var renderer = new PreparedStatementRenderer(source,
                    "cohortsample/sql/insertSampleElement.sql", parameters, parameterValues, sqlParameters,
                    sqlValues);

            if (statement == null) {
                statement = renderer.getSql();
            }
            variables.add(renderer.getOrderedParams());
        }

        jdbcTemplate.batchUpdate(statement, variables);
    }

    /**
     * Sample elements based on parameters.
     */
    private List<SampleElement> sampleElements(SampleParametersDTO sampleParametersDTO, CohortSample sample,
            JdbcTemplate jdbcTemplate, SourceEntity source) {
        var expressionBuilder = new StringBuilder();

        var sqlVariables = new LinkedHashMap<String,Object>();
        sqlVariables.put("cohort_definition_id", sample.getCohortDefinitionId());

        var ageMode = SampleParametersDTO.AgeMode.fromSerialName(sample.getAgeMode());

        if (ageMode != null) {
            switch (ageMode) {
                case LESS_THAN:
                    expressionBuilder.append(" AND age < @age_max");
                    sqlVariables.put("age_max", sample.getAgeMax());
                    break;
                case LESS_THAN_OR_EQUAL:
                    expressionBuilder.append(" AND age <= @age_max");
                    sqlVariables.put("age_max", sample.getAgeMax());
                    break;
                case GREATER_THAN:
                    expressionBuilder.append(" AND age > @age_min");
                    sqlVariables.put("age_min", sample.getAgeMin());
                    break;
                case GREATER_THAN_OR_EQUAL:
                    expressionBuilder.append(" AND age >= @age_min");
                    sqlVariables.put("age_min", sample.getAgeMin());
                    break;
                case EQUAL_TO:
                    expressionBuilder.append(" AND age = @age_equal");
                    sqlVariables.put("age_equal", sample.getAgeMin());
                    break;
                case BETWEEN:
                    expressionBuilder.append(" AND age <= @age_max AND age >= @age_min");
                    sqlVariables.put("age_min", sample.getAgeMin());
                    sqlVariables.put("age_max", sample.getAgeMax());
                    break;
                case NOT_BETWEEN:
                    expressionBuilder.append(" AND age > @age_max OR age < @age_min");
                    sqlVariables.put("age_min", sample.getAgeMin());
                    sqlVariables.put("age_max", sample.getAgeMax());
                    break;
            }
        }

        var gender = sampleParametersDTO.getGender();
        if (gender != null) {
            var conceptIds = gender.getConceptIds();
            if (gender.isOtherNonBinary()) {
                if (conceptIds.isEmpty()) {
                    expressionBuilder.append(" AND gender_concept_id NOT IN (")
                            .append(GENDER_MALE_CONCEPT_ID)
                            .append(',')
                            .append(GENDER_FEMALE_CONCEPT_ID)
                            .append(')');
                } else if (conceptIds.size() == 1) {
                    if (conceptIds.getFirst() == GENDER_FEMALE_CONCEPT_ID) {
                        expressionBuilder.append(" AND gender_concept_id <> ")
                                .append(GENDER_MALE_CONCEPT_ID);

                    } else {
                        expressionBuilder.append(" AND gender_concept_id <> ")
                                .append(GENDER_FEMALE_CONCEPT_ID);
                    }
                }
                // else: all genders are selected, no where statement needed
            } else if (!conceptIds.isEmpty()) {
                expressionBuilder.append(" AND gender_concept_id IN (");
                for (int i = 0; i < conceptIds.size(); i++) {
                    if (i > 0) {
                        expressionBuilder.append(',');
                    }
                    expressionBuilder.append(conceptIds.get(i));
                }
                expressionBuilder.append(')');
            }
            // else: all genders are selected, no where statement needed
        }

        var parameterKeys = new String[] { "results_schema", "CDM_schema", "expression" };
        var parameterValues = new String[] {
                source.getTableQualifier(SourceDaimonEntity.DaimonType.Results),
                source.getTableQualifier(SourceDaimonEntity.DaimonType.CDM),
                expressionBuilder.toString() };
        var sqlVariableKeys = sqlVariables.keySet().toArray(new String[0]);
        var sqlVariableValues = Stream.of(sqlVariableKeys)
                .map(sqlVariables::get)
                .toArray(Object[]::new);

        var renderer = new PreparedStatementRenderer(source,
                "cohortsample/sql/generateSample.sql",
                parameterKeys,
                parameterValues,
                sqlVariableKeys,
                sqlVariableValues);

        jdbcTemplate.setMaxRows(sample.getSize());

        // TODO deprecated, still exists but needs modernized
        return jdbcTemplate.query(renderer.getSql(), renderer.getOrderedParams(), (rs, rowNum) -> {
            var element = new SampleElement();
            element.setRank(rowNum);
            element.setAge(rs.getInt("age"));
            element.setGenderConceptId(rs.getInt("gender_concept_id"));
            element.setPersonId(rs.getLong("person_id"));
            return element;
        });
    }

    /**
     * Delete a sample and its elements.
     */
    public void deleteSample(int cohortDefinitionId, SourceEntity source, int sampleId) {
        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        var resultsSchema = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var sql = new PreparedStatementRenderer(
                source,
                "cohortsample/sql/deleteSampleElementsById.sql",
                "results_schema",
                resultsSchema,
                "cohortSampleId",
                sampleId).getSql();
        var sample = sampleRepository.findById(sampleId);
        if (sample == null) {
            throw new NotFoundException("Sample with ID " + sampleId + " does not exist");
        }
        if (sample.getCohortDefinitionId() != cohortDefinitionId) {
            throw new BadRequestException("Cohort definition ID " + sample.getCohortDefinitionId()
                    + " does not match provided cohort definition id " + cohortDefinitionId);
        }
        if (sample.getSourceId() != source.getId()) {
            throw new BadRequestException(
                    "Source " + sample.getSourceId() + " does not match provided source " + source.getId());
        }

        transactionTemplates.getTransactionTemplate().execute((TransactionCallback<Void>) transactionStatus -> {
            sampleRepository.delete(sample);
            jdbcTemplate.update(sql, sampleId);
            return null;
        });
    }

    public void launchDeleteSamplesTasklet(int cohortDefinitionId) {
        var jobParameters = createDeleteSamplesJobParameters(cohortDefinitionId, 0, false);
        runDeleteSamplesJob(jobParameters);
    }

    public void launchDeleteSamplesTasklet(int cohortDefinitionId, int sourceId) {
        var jobParameters = createDeleteSamplesJobParameters(cohortDefinitionId, sourceId, true);
        runDeleteSamplesJob(jobParameters);
    }

    public CleanupCohortSamplesTasklet createDeleteSamplesTasklet() {
        return new CleanupCohortSamplesTasklet(
                transactionTemplates.getTransactionTemplate(),
                sampleRepository,
                sourceService);
    }

    private JobParameters createDeleteSamplesJobParameters(int cohortDefinitionId, int sourceId, boolean useSourceId) {
        var builder = new JobParametersBuilder();
        builder.addString(JOB_NAME,
                String.format("Cleanup cohort samples for cohort definition %d.", cohortDefinitionId));
        builder.addString(COHORT_DEFINITION_ID, String.valueOf(cohortDefinitionId));
        if (useSourceId) {
            builder.addString(SOURCE_ID, String.valueOf(sourceId));
            log.info("Beginning cohort cleanup for cohort definition id {} and source ID {}", cohortDefinitionId,
                    sourceId);
        } else {
            log.info("Beginning cohort cleanup for cohort definition id {}.", cohortDefinitionId);
        }
        return builder.toJobParameters();
    }

    private void runDeleteSamplesJob(JobParameters jobParameters) {
        var tasklet = createDeleteSamplesTasklet();
        Step cleanupStep = this.jobService.buildTaskletStep("cohortSample.cleanupSamples", tasklet);
        var job = this.jobService.buildJob("cleanupSamples", List.of(cleanupStep));
        this.jobService.runJob(job, jobParameters);
    }

    /**
     * Maps a SQL result to a sample element.
     */
    private static class CohortSampleElementRowMapper implements RowMapper<SampleElement> {
        private final Collection<String> optionalFields;

        CohortSampleElementRowMapper(Collection<String> optionalFields) {
            this.optionalFields = optionalFields;
        }

        @Override
        public SampleElement mapRow(ResultSet rs, int rowNum) throws SQLException {
            var sample = new SampleElement();
            sample.setRank(rs.getInt("rank_value"));
            sample.setSampleId(rs.getInt("cohort_sample_id"));
            sample.setPersonId(rs.getLong("person_id"));
            sample.setGenderConceptId(rs.getInt("gender_concept_id"));
            sample.setAge(rs.getInt("age"));
            if (optionalFields.contains("record_count")) {
                sample.setRecordCount(rs.getInt("record_count"));
            }
            return sample;
        }
    }
}
